/*
    BioServer - Emulation of the long gone server for
                Biohazard Outbreak File #1 (Playstation 2)
    Copyright (C) 2013-2024  obsrv.org
              (C) 2026  alissrip — https://makii.net

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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    public String gs_ip;
    public String internal_api_key;
    public String api_url;
    public String servers_api;

    public Configuration() {
        Properties prop = new Properties();
        try (InputStream inputStream = new FileInputStream("config.properties")) {
            prop.load(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read config.properties", ex);
        }

        this.gs_ip = require(prop, "gs_ip");
        this.internal_api_key = require(prop, "internal_api_key");
        this.api_url = require(prop, "api_url");
        this.servers_api = require(prop, "servers_api");
    }

    private static String require(Properties prop, String key) {
        String value = prop.getProperty(key);
        if (value == null || value.isBlank())
            throw new IllegalStateException("Missing config key: " + key);
        return value;
    }
}
