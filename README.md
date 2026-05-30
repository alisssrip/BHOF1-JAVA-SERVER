# Outbreak Server — Matchmaking + Bypass (Backend-driven fork)

A fork of the **OBSRV / Biohazard Outbreak Resurrection** server stack, modified
to run against an external HTTP backend instead of talking to a MariaDB database
directly. This repository contains the two Java server components:

- **FILE1 — Matchmaking / Lobby server**: the central server. It handles the
  lobby, rooms, slots, presence and routing. It decides which game server a
  player should connect to.
- **FILE1-BYPASS — Bypass / Game server**: the per-region (or self-hosted)
  relay. It forwards game traffic between the host and the players in a match.
  Regional servers and user self-hosted servers run this exact same code.

Both are derived from the original Resurrection/OBSRV server (AGPL v3) and remain
under the same license.

---

## How it differs from the original Resurrection server

The original server connected straight to a MariaDB database from Java. **This
fork removes the direct DB connection** and replaces every database operation
with calls to an HTTP backend. The Java servers no longer hold DB credentials or
SQL; they are pure protocol servers that ask a backend for everything (sessions,
users, handle/nickname pairs, game server registration, etc.).

This means you **must run your own backend** that implements the endpoints listed
in the "Backend API contract" section below. The backend is intentionally not
part of this repository — you are free to implement it in any language/stack.

Summary of changes vs. the original:

- `Database.java` (both servers): all `mysqli`/JDBC access replaced with HTTP
  calls to a configurable backend.
- Server URLs, the game-server advertised IP and the internal API key are read
  from a `config.properties` file (see Configuration) instead of being
  hardcoded.
- The matchmaking authenticates privileged operations (e.g. resetting all
  sessions on startup) with an internal API key. The bypass does **not** hold
  this key — it only performs game-server operations scoped to itself.

---

## What the Bypass server is for

When a match starts, the game traffic does not go peer-to-peer directly. Each
player connects to a game server (a regional one you run, or a self-hosted one
run by a player) and that server **relays bytes** between the match host and the
other players, in both directions. It does not interpret or persist game data —
it is a blind relay.

- **Regional bypass servers**: you run them; they stay up regardless of which
  player is hosting a given match.
- **Self-hosted bypass servers**: a player runs one on their own machine. Same
  code, same behaviour. A self-hosted server can only affect matches it hosts —
  which is the accepted trade-off of allowing community/self-hosting.

The matchmaking server queries the backend to find which game server a player
should use and routes them accordingly.

---

## Configuration

Neither server is configured by editing code. Each reads a `config.properties`
file from its working directory. **This file is gitignored and must never be
committed** — copy the provided `config.properties.example` and fill it in.

### Matchmaking (`FILE1`)

```properties
gs_ip=YOUR_PUBLIC_IP
internal_api_key=YOUR_SHARED_SECRET
api_url=https://your-backend.example/api/outbreakjava/
servers_api=https://your-backend.example/api/servers/
```

### Bypass (`FILE1-BYPASS`)

```properties
gs_ip=YOUR_PUBLIC_IP
```

The bypass does **not** take `internal_api_key`, `api_url` or `servers_api`.
Never put the internal API key in a self-hosted or regional bypass config — only
the matchmaking holds it.

---

## Setup

You can follow the original Resurrection server setup guide for the parts that
are unchanged (game files, DNS redirection for the PS2, the DNAS service, the
web front-end under `www/`). The only differences introduced by this fork are:

1. **Stand up a backend** that implements the endpoints in the API contract
   below. Point `api_url` / `servers_api` at it.
2. **Create a `config.properties`** in each server's working directory (copy the
   `.example`). The matchmaking needs the four keys; the bypass needs `gs_ip`.
3. **Use the same `internal_api_key`** value in the matchmaking config and in
   your backend, so privileged endpoints accept the matchmaking's requests.

### Building and running

```bash
# compile (adjust the gson path if needed)
javac --release 17 -cp "lib/gson-2.10.1.jar" -d bin bioserver/*.java

# run the matchmaking
java -cp "bin:lib/gson-2.10.1.jar" bioserver.ServerMain

# run the bypass
java -cp "bin:lib/gson-2.10.1.jar" bioserver.ServerMainSlim
```

Run them as systemd services for production. The `config.properties` goes in the
service's `WorkingDirectory`, not inside `bin/`.

---

## Backend API contract

Your backend must expose the following. Paths are relative to `api_url` unless
noted as relative to `servers_api`. All bodies are JSON.

### Consumed by the matchmaking (`FILE1`)

| Method | Path | Body | Expected response |
| ------ | ---- | ---- | ----------------- |
| GET | `sessions/{sessid}/user` | — | `200` plain-text userid, or `404` |
| GET | `users/{userid}/hnpairs` | — | `200` JSON list of handle/nickname pairs, or `404` |
| GET | `handles/{handle}` | — | `200` if the handle exists, `404` if not |
| POST | `hnpairs` | `{userid, handle, nickname}` | `200`/`201` on success |
| PUT | `hnpairs` | `{userid, handle, nickname}` | `200`/`204` on success |
| PUT | `sessions/reset` | empty | `200`/`204`. **Requires header `X-Internal-Key`** |
| PUT | `sessions/{userid}/origin` | `{state, area, room, slot, online}` | `200`/`204` |
| PUT | `sessions/{userid}/game` | `{gamenumber}` | `200`/`204` |
| GET | `sessions/{userid}/game` | — | `200` plain-text game number, or `404` |
| PUT | `sessions/{userid}/online` | `{online}` | `200`/`204` |
| GET | `server/motd` | — | `200` plain-text message of the day |
| GET | `gameservers/byusername/{userid}` | — | `200` `{ip, port}`, or `404` |
| GET | `byuser/{userid}` *(relative to `servers_api`)* | — | `200` `{ip, port}`, or `404` |

### Consumed by the bypass (`FILE1-BYPASS`)

| Method | Path | Body / query | Expected response |
| ------ | ---- | ------------ | ----------------- |
| POST | `gameservers/register` | `{sessid, publicIp, port, region}` | `200`. Backend should take the real IP from the connection, not trust `publicIp` |
| PUT | `gameservers/heartbeat?sessid=...` | — | `200`/`204` |
| DELETE | `gameservers/unregister?sessid=...` | — | `200`/`204` |
| GET | `sessions/{sessid}/user` | — | `200` plain-text userid, or `404` |

### Security notes for backend implementers

- `sessions/reset` is a global, destructive operation. Protect it with the
  shared `internal_api_key` (header `X-Internal-Key`) so only the matchmaking can
  call it. Never expose this key to a bypass server.
- For `gameservers/register`, derive the public IP from the TCP connection rather
  than trusting the `publicIp` field, otherwise a server can register pointing at
  someone else's address.
- Session-scoped endpoints should validate the `sessid` and only allow changes to
  the session that owns it.

---

## Sessions and game server routing

This is the most important operational difference from the original server, and
the most common source of "it always connects to the wrong server" problems.

**The matchmaking no longer creates sessions.** In the original server the Java
process opened the session in the database itself. In this fork it does not —
your backend (e.g. the web login flow) must create the player's session row in
`ob_sessions` *before* the player enters. When the player connects, the
matchmaking only **validates** that an open session exists for them (via
`sessions/{sessid}/user`); it does not create one.

For a session to route correctly, two things must be true:

1. **The session must have a region assigned** (`ob_sessions.region`). If the
   region is `NULL`, the regional lookup cannot match anything.
2. **There must be an active server backing that region in `ob_servers`.** The
   matchmaking looks up a game server by the session's region. If no active row
   in `ob_servers` matches that region, the lookup fails.

### Routing order (fallback cascade)

When deciding which game server a player should use, the matchmaking tries, in
order:

1. The player's own self-hosted game server, if registered
   (`gameservers/byusername/{userid}` → `ob_gameservers`).
2. A regional server matching the session's region
   (`servers_api` + `byuser/{userid}` → `ob_servers`, `active = 1`).
3. **Fallback: the matchmaking server itself** acts as the game server.

That last fallback is why a misconfigured session "works" but everyone ends up on
the matchmaking box: if the session has no region, or no `ob_servers` row backs
that region, steps 1 and 2 produce nothing and step 3 catches everyone. So:

- Always assign a `region` when you create the session.
- Always have at least one active `ob_servers` row for every region you assign.

---

## Database schema

A reference schema for the `ob_*` tables the backend is expected to back is
provided in `schema/ob_schema.sql` (includes `ob_servers` for regional routing).
It is a starting point — your backend may model the data however it likes as long
as it honours the API contract and the routing rules above.

---

## License

GNU Affero General Public License v3. See the `LICENSE` file (AGPL v3).

This is a derivative work. Original OBSRV / Resurrection server code
(c) 2013-2024 obsrv.org and contributors. Backend-driven modifications
(c) 2026 alissrip <alissrip@makii.net>.
