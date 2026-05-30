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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * class for managing the database
 */
public class Database {

    private final String apiUrl;
    private final String serversApi;
    private final String internalApiKey;
    private final HttpClient client;

    public Database() {
        Configuration conf = new Configuration();
        this.apiUrl = conf.api_url;
        this.serversApi = conf.servers_api;
        this.internalApiKey = conf.internal_api_key;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // get userid of an existing session
    public String getUserid(String sessid) {
        String retval = "";
        try {
            String safeSessid = java.net.URLEncoder.encode(sessid, "UTF-8");
            URI targetURI = new URI(apiUrl + "sessions/" + safeSessid + "/user");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
            {
                retval = response.body().trim();
            } else if (response.statusCode() == 404)
            {
                Logger.getLogger(Database.class.getName()).log(Level.INFO, "session not found: " + sessid);
            } else
            {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Error searching session: HTTP " + response.statusCode());
            }

        }
        catch (Exception ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Failed to get userid by sessionid ", ex);
        }

        return retval;
    }


    // get handle/nickname list for a given userid
    public HNPairs getHNPairs(String userid) {
        HNPairs hns = new HNPairs();

        try {
            String safeUserId = java.net.URLEncoder.encode(userid, "UTF-8");
            URI targetURI = new URI(apiUrl + "users/" + safeUserId + "/hnpairs");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonResponse = response.body();

                JsonArray jsonArray = JsonParser.parseString(jsonResponse).getAsJsonArray();

                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    String handle = obj.get("handle").getAsString();
                    String nickname = obj.get("nickname").getAsString();

                    hns.add(new HNPair(handle, nickname));
                }

            } else if (response.statusCode() == 404) {
                Logger.getLogger(Database.class.getName()).log(Level.INFO, "User dont have any character created");
            } else {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Error HNPairs: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Red fail getHNPairs", ex);
        }

        return hns;
    }


    // check if a handles exists
    // returns true when handle is free
    public boolean checkHandle(String handle) {
        boolean isFree = false;

        try {
            String safeHandle = java.net.URLEncoder.encode(handle, "UTF-8");

            URI targetURI = new URI(apiUrl + "handles/" + safeHandle);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                isFree = true;
            } else if (response.statusCode() == 200) {
                isFree = false;
            } else {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Error verifying Handle: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Red network error on checkHandle", ex);
        }

        return isFree;
    }


    // insert a new handle/nickname into database
    public void createNewHNPair(Client cl) {
        try {
            String nickname;
            try {
                nickname = new String(cl.getHNPair().getNickname(), "SJIS");
            } catch (UnsupportedEncodingException ex) {
                nickname = "sjis";
            }

            String handle = new String(cl.getHNPair().getHandle());
            String userid = cl.getUserID();

            JsonObject json = new JsonObject();
            json.addProperty("userid", userid);
            json.addProperty("handle", handle);
            json.addProperty("nickname", nickname);

            URI targetURI = new URI(apiUrl + "hnpairs");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Failed to create HN Pair: HTTP " + response.statusCode());
            }

        }
        catch (Exception ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Network error in createNewHNPair", ex);
        }
    }

    // update handle/nickname of a client
    public void updateHNPair(Client cl) {
        try {
            String nickname;
            try {
                nickname = new String(cl.getHNPair().getNickname(), "SJIS");
            } catch (UnsupportedEncodingException ex) {
                nickname = "sjis";
            }

            String handle = new String(cl.getHNPair().getHandle());
            String userid = cl.getUserID();

            JsonObject json = new JsonObject();
            json.addProperty("userid", userid);
            json.addProperty("handle", handle);
            json.addProperty("nickname", nickname);

            URI targetURI = new URI(apiUrl + "hnpairs");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Failed to update HN Pair: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Network error in updateHNPair", ex);
        }
    }


    // set area, room, slot for a user
    private void setupDBrestart() {
        try {
            URI targetURI = new URI(apiUrl + "sessions/reset");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(10))
                    .header("X-Internal-Key", internalApiKey)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Failed to reset sessions on startup: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Network error in setupDBrestart", ex);
        }
    }


    // set area, room, slot for a user and a state
    public void updateClientOrigin(String userid, int state, int area, int room, int slot, boolean online) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("state", state);
            json.addProperty("area", area);
            json.addProperty("room", room);
            json.addProperty("slot", slot);
            json.addProperty("online", online);

            String safeUserId = java.net.URLEncoder.encode(userid, "UTF-8");
            URI targetURI = new URI(apiUrl + "sessions/" + safeUserId + "/origin");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "updateClientOrigin failed: HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "updateClientOrigin error", ex);
        }
    }


    // set game for a user
    public void updateClientGame(String userid, int gamenumber) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("gamenumber", gamenumber);

            String safeUserId = java.net.URLEncoder.encode(userid, "UTF-8");
            URI targetURI = new URI(apiUrl + "sessions/" + safeUserId + "/game");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Failed to update client game: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Network error in updateClientGame", ex);
        }
    }


    // get the gamenumber of a given userid
    public int getGameNumber(String userid) {
        int retval = 0;

        try {
            String safeUserId = java.net.URLEncoder.encode(userid, "UTF-8");
            URI targetURI = new URI(apiUrl + "sessions/" + safeUserId + "/game");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                retval = Integer.parseInt(response.body().trim());
            } else if (response.statusCode() == 404) {
                Logger.getLogger(Database.class.getName()).log(Level.INFO, "Session not found for game number: " + userid);
            } else {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Failed to get game number: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Network error in getGameNumber", ex);
        }

        return retval;
    }


    public String getGameServerIp(String userName) {
        String safeUserId;
        try {
            safeUserId = java.net.URLEncoder.encode(userName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "URL encode failed", ex);
            return "";
        }
        String selfhost = lookupGameServer(apiUrl + "gameservers/byusername/" + safeUserId);
        if (!selfhost.isEmpty()) {
            Logging.println("Selfhost gameserver found for " + userName + ": " + selfhost);
            return selfhost;
        }

        String regional = lookupGameServer(serversApi + "byuser/" + safeUserId);
        if (!regional.isEmpty()) {
            Logging.println("Regional server for " + userName + ": " + regional);
            return regional;
        }

        Logger.getLogger(Database.class.getName()).log(Level.INFO,
                "No selfhost or regional server found for " + userName);
        return "";
    }

    public void setOnline(String userid, boolean online) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("online", online);

            String safeUserId = java.net.URLEncoder.encode(userid, "UTF-8");
            URI targetURI = new URI(apiUrl + "sessions/" + safeUserId + "/online");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "setOnline failed: HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "setOnline error", ex);
        }
    }

    private String lookupGameServer(String url) {
        try {
            URI targetURI = new URI(url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return json.get("ip").getAsString();
            } else if (response.statusCode() == 404) {
                return "";
            } else {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING,
                        "Lookup HTTP " + response.statusCode() + " on " + url);
                return "";
            }
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.WARNING,
                    "Could not reach Yoko at " + url, ex);
            return "";
        }
    }

    // get message of the day
    public String getMOTD() {
        String retval = "";

        try {
            URI targetURI = new URI(apiUrl + "server/motd");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                retval = response.body().trim();
            } else if (response.statusCode() == 404) {
                Logger.getLogger(Database.class.getName()).log(Level.INFO, "No active MOTD found.");
            } else {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "Failed to get MOTD: HTTP " + response.statusCode());
            }

        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "Network error in getMOTD", ex);
        }

        return retval;
    }
}
