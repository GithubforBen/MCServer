package de.hems.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Objects;
import java.util.UUID;

public class UUIDFetcher {
    private static String getApiReponse(String url, String jsonKey) {
        try {
            URI uri = new URI(url);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            connection.connect();

            if (connection.getResponseCode() != 200) {
                return null;
            }

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            JSONObject json = new JSONObject(response.toString());
            return json.getString(jsonKey);

        } catch (URISyntaxException | IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String formatUUID(String raw) {
        return raw.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }

    public static String findNameByUUID(String uuid) {
        try {
            String apiEndPoint = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", "");
            return getApiReponse(apiEndPoint, "name");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String findNameByUUID(UUID uuid) {
        return findNameByUUID(uuid.toString());
    }

    public static UUID findUUIDByName(String name, boolean returnFormatted) {
        try {
            String apiEndPoint = "https://api.mojang.com/users/profiles/minecraft/" + name;
            String uuid = formatUUID(Objects.requireNonNull(getApiReponse(apiEndPoint, "id")));
            return UUID.fromString(uuid);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
