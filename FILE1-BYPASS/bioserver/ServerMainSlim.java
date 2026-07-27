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