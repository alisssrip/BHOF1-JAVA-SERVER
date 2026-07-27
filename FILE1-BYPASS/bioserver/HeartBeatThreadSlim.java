/*
    BHOF1-Host - Selfhost GameServer for
                 Biohazard Outbreak File #1 (Playstation 2)

    Based on BioServer by obsrv.org
    Copyright (C) 2013-2019 obsrv.org (no23@deathless.net)
    Modified (C) 2026 OutbreakHub

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package bioserver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keepalive thread for BHOF1-Host
 * Stripped version of HeartBeatThread — only handles GameServer cleanup,
 * no Lobby pings, no room cleanup, no autostart checks.
 */
public class HeartBeatThreadSlim implements Runnable {

    private GameServerPacketHandler gspackethandler;
    private GameServerThread gsserver;

    public HeartBeatThreadSlim(GameServerThread gsserver, GameServerPacketHandler packethandler2) {
        this.gsserver = gsserver;
        this.gspackethandler = packethandler2;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(HeartBeatThreadSlim.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (true) {
            try {
                // Remover clientes muertos del GameServer
                gspackethandler.connCheck(gsserver);

                Thread.sleep(30 * 1000);
            } catch (InterruptedException ex) {
                Logging.println("BHOF1-Host HeartBeat exception caught!");
            }
        }
    }
}
