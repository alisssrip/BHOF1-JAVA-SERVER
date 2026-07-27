package bioserver;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

class GameServerPacketHandler implements Runnable {
    private Database db;
    private List queue = new LinkedList();
    private ClientList clients = new ClientList();
    private int packetidcounter;

    @Override
    public void run() {
        GameServerDataEvent dataEvent;
        this.db = new Database();
        packetidcounter = 0;

        while(true) {
            synchronized(queue) {
                while(queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) { }
                }
                dataEvent = (GameServerDataEvent) queue.remove(0);
            }
            dataEvent.server.send(dataEvent.socket, dataEvent.data);
        }
    }

    public int countInGamePlayers() {
        return clients.getList().size();
    }

    public ClientList getClients() {
        return clients;
    }

    public int getNextPacketID() {
        return(++packetidcounter);
    }

    void processData(GameServerThread server, SocketChannel socket, byte[] data, int count) {
        switch(data[0]) {
            case (byte)0x82:
                if(data[1] == 0x02) {
                    Packet p = new Packet(data);
                    if(p.getCmd() == Commands.GSLOGIN) {
                        if(!check_session(server, socket, p)) {
                            Logging.println("session check failed");
                        }
                    }
                }
                break;

            default:
                byte[] acopy = new byte[count];
                System.arraycopy(data, 0, acopy, 0, count);

                Client cl = clients.findClient(socket);
                if(cl == null) return;
                cl.connalive = true;
                int gamenum = cl.gamenumber;
                List cls = clients.getList();
                synchronized(queue) {
                    for(int i=0; i<cls.size(); i++) {
                        cl = (Client) cls.get(i);
                        if(cl.gamenumber == gamenum && cl.getSocket() != socket) {
                            queue.add(new GameServerDataEvent(server, cl.getSocket(), acopy));
                        }
                    }
                    queue.notify();
                }
        }
    }

    void addOutPacket(GameServerThread server, SocketChannel socket, Packet packet) {
        synchronized(queue) {
            queue.add(new GameServerDataEvent(server, socket, packet.getPacketData()));
            queue.notify();
        }
    }

    void broadcastPacket(GameServerThread server, Packet packet) {
        List cls = clients.getList();
        synchronized(queue) {
            for(int i=0; i<cls.size(); i++) {
                Client cl = (Client) cls.get(i);
                queue.add(new GameServerDataEvent(server, cl.getSocket(), packet.getPacketData()));
            }
            queue.notify();
        }
    }

    public void GSsendLogin(GameServerThread server, SocketChannel socket) {
        Packet p = new Packet(Commands.GSLOGIN, Commands.QUERY, Commands.GAMESERVER, this.getNextPacketID());
        this.addOutPacket(server, socket, p);
    }

    private boolean check_session(GameServerThread server, SocketChannel socket, Packet p) {
        int seed = p.getPacketID();
        int sessA = ((int) p.getPayload()[0] - 0x30)*10000
                + ((int) p.getPayload()[1] - 0x30)*1000
                + ((int) p.getPayload()[2] - 0x30)*100
                + ((int) p.getPayload()[3] - 0x30)*10
                + ((int) p.getPayload()[4] - 0x30);
        int sessB = ((int) p.getPayload()[5] - 0x30)*10000
                + ((int) p.getPayload()[6] - 0x30)*1000
                + ((int) p.getPayload()[7] - 0x30)*100
                + ((int) p.getPayload()[8] - 0x30)*10
                + ((int) p.getPayload()[9] - 0x30);
        String session = String.format("%04d%04d", sessA-seed, sessB-seed);

        String userid = db.getUserid(session);
        if("".equals(userid)) {
            Logging.println("invalid session " + session);
            server.disconnect(socket);
            return false;
        }

        int gamenr = db.getGameNumber(userid);
        if(gamenr <= 0) {
            Logging.println("no active game for " + userid);
            server.disconnect(socket);
            return false;
        }

        Client existing = clients.findClient(userid);
        if(existing != null) {
            List cls = clients.getList();
            for(int i=0; i<cls.size(); i++) {
                Client cl = (Client) cls.get(i);
                if(cl.getUserID().equals(userid)) {
                    Logging.println("kick double session " + userid);
                    server.disconnect(cl.getSocket());
                    this.removeClient(server, cl);
                }
            }
        }

        clients.add(new Client(socket, userid, session));
        Client cl = clients.findClient(socket);
        cl.gamenumber = gamenr;
        Logging.println("user " + userid + " joined game " + gamenr);
        return true;
    }

    public void removeClient(GameServerThread server, String userid) {
        Client cl = clients.findClient(userid);
        if(cl == null) {
            Logging.println("kick failed, " + userid + " not connected");
            return;
        }
        this.removeClient(server, cl);
        Logging.println(userid + " kicked");
    }

    public void removeNoDisconnectClient(GameServerThread server, SocketChannel socket) {
        Client cl = clients.findClient(socket);
        if(cl == null) return;
        clients.remove(cl);
    }

    public void removeClient(GameServerThread server, Client cl) {
        if(cl == null) return;
        SocketChannel socket = cl.getSocket();
        clients.remove(cl);
        server.disconnect(socket);
    }

    public void connCheck(GameServerThread server) {
        List cls = clients.getList();
        for(int i=0; i<cls.size(); i++) {
            Client cl = (Client) cls.get(i);
            if(cl.connalive) {
                cl.connalive = false;
            } else {
                Logging.println("dead client " + cl.getUserID());
                this.removeClient(server, cl);
            }
        }
    }
}