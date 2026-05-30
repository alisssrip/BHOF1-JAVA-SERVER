/*
    BioServer - Emulation of the long gone server for
                Biohazard Outbreak File #1 (Playstation 2)
    Copyright   (C) 2026  alissrip — https://makii.net

    Based on OBSRV by obsrv.org

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/


package bioserver;

import java.util.Date;

public class ServerMainSlim {
    public static final int GAMEPORT = 8690;

    public static void main(String[] args) {
        System.out.println("------------------------------\n" +
                "-        BHOF1-Host          -\n" +
                "-  GameServer :" + GAMEPORT + "         -\n" +
                "-                            -\n" +
                "- Based on BioServer         -\n" +
                "- (c) 2013-2019 obsrv.org    -\n" +
                "- (c) 2026 Makii.net         -\n" +
                "------------------------------\n");

        try {
            System.out.println("Initializing Packet Handler...");
            GameServerPacketHandler packethandler = new GameServerPacketHandler();
            Thread packetThread = new Thread(packethandler, "GameServer-PacketHandler");
            packetThread.start();

            System.out.println("Starting Game Server Socket Thread on port " + GAMEPORT + "...");
            GameServerThread gsserver = new GameServerThread(null, GAMEPORT, packethandler);
            Thread serverThread = new Thread(gsserver, "GameServer-MainSocket");
            serverThread.start();

            System.out.println("Starting internal game heartbeat...");
            HeartBeatThreadSlim gameHeartbeat = new HeartBeatThreadSlim(gsserver, packethandler);
            Thread heartbeatThread = new Thread(gameHeartbeat, "GameServer-InternalHeartbeat");
            heartbeatThread.start();

            Date date = new Date();
            System.out.println("\n[SUCCESS] " + date + " - Outbreak Dedicated Server is running and listening on port " + GAMEPORT);
            System.out.println("[INFO] Waiting for players. Lifecycle managed by Outbreak Hub Launcher.");

        } catch (Exception e) {
            System.err.println("[CRITICAL ERROR] Failed to start dedicated server components:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
