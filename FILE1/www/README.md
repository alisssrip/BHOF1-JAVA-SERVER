# OBSRV - Biohazard Outbreak Server (www)

Web front-end for the OBSRV Biohazard Outbreak File #1 server.
Based on the original work by obsrv.org, licensed under AGPL v3.

## Modifications (alissrip, 2026)

- `www/login_form.php`: replaced the local DB login with an
  integration against the Yoko backend (`/api/OutbreakJava/sessions/by-ip`).
  Session resolution is delegated to the backend instead of querying the
  local `users` table directly.

## Setup

1. Copy `www/db_cred.php.example` to `db_cred.php` and fill in your
   database credentials. The real `db_cred.php` is gitignored and must never
   be committed.
2. Serve the `www` folder with a webserver, behind TLS.
3. Point the backend URL in `login_form.php` to your own instance

## License

GNU Affero General Public License v3. See the LICENSE file.
Original OBSRV code (c) 2013-2024 obsrv.org. Modifications (c) 2026 alissrip.
