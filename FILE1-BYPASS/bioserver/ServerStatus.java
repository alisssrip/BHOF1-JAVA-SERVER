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

/**
 * Client status constants
 * Extracted from PacketHandler for use in slim (GameServer-only) builds
 */
public class ServerStatus {
    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_LOBBY   = 1;
    public static final int STATUS_GAME    = 2;
    public static final int STATUS_AGLOBBY = 3;
}
