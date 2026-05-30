/*
    BioServer - Emulation of the long gone server for 
                Biohazard Outbreak File #1 (Playstation 2)

    Copyright (C) 2013-2019 obsrv.org (no23@deathless.net)

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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class HNPair {
    private byte[] handle;
    private byte[] nickname;

    public HNPair(byte[] handle, byte[] nickname) {
        this.handle = handle;
        this.nickname = nickname;
    }

    public HNPair(String handle, String nickname) {
        this.handle = handle.getBytes();
        try {
            this.nickname = nickname.getBytes("SJIS");
        } catch (UnsupportedEncodingException ex) {
            this.nickname = "sjis".getBytes();
        }
    }

    public byte[] getHandle() {
        return handle;
    }

    public byte[] getNickname() {
        return nickname;
    }

    public byte[] getHNPair() {
        byte[] retval = new byte[handle.length + nickname.length];
        System.arraycopy(handle, 0, retval, 0, handle.length);
        System.arraycopy(nickname, 0, retval, handle.length, nickname.length);
        return retval;
    }
}
