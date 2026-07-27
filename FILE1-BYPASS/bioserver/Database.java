package bioserver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    private final String apiUrl = "https://yoko.makii.net/api/outbreakjava/";
    private final HttpClient client;

    public Database() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

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

            if (response.statusCode() == 200) {
                retval = response.body().trim();
            } else if (response.statusCode() != 404) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "getUserid HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "getUserid error", ex);
        }
        return retval;
    }

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
            } else if (response.statusCode() != 404) {
                Logger.getLogger(Database.class.getName()).log(Level.WARNING, "getGameNumber HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "getGameNumber error", ex);
        }
        return retval;

    }
    // TEMP: gameserver register/heartbeat/unregister hasta que el launcher lo maneje
    public boolean registerGameServer(String sessid, String publicIp, int port, String region) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("sessid", sessid);
            json.addProperty("publicIp", publicIp);
            json.addProperty("port", port);
            if(region != null) json.addProperty("region", region);

            URI targetURI = new URI(apiUrl + "gameservers/register");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 200) return true;
            Logger.getLogger(Database.class.getName()).log(Level.WARNING, "register HTTP " + response.statusCode() + " body=" + response.body());
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, "register error", ex);
        }
        return false;
    }

    // TEMP
    public void heartbeatGameServer(String sessid) {
        try {
            String safe = java.net.URLEncoder.encode(sessid, "UTF-8");
            URI targetURI = new URI(apiUrl + "gameservers/heartbeat?sessid=" + safe);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.WARNING, "heartbeat error", ex);
        }
    }

    // TEMP
    public void unregisterGameServer(String sessid) {
        try {
            String safe = java.net.URLEncoder.encode(sessid, "UTF-8");
            URI targetURI = new URI(apiUrl + "gameservers/unregister?sessid=" + safe);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetURI)
                    .timeout(Duration.ofSeconds(5))
                    .DELETE()
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.WARNING, "unregister error", ex);
        }
    }
}