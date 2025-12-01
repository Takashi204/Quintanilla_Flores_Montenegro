package pos.util;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    // ⚠️ Cambia esta URL por la URL real de tu API
	public static String BASE_URL = "http://140.84.173.136";

	
	public static OkHttpClient client = new OkHttpClient();

    private static String token = null;


    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // ============================================================
    // TOKEN
    // ============================================================
    public static void setToken(String t) {
        token = t;
    }

    public static String getToken() {
        return token;
    }

    private static Request.Builder addHeaders(Request.Builder builder) {
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        builder.addHeader("Content-Type", "application/json");
        return builder;
    }

    // ============================================================
    // GET
    // ============================================================
    public static Response get(String endpoint) throws Exception {

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get();

        addHeaders(builder);

        return client.newCall(builder.build()).execute();
    }

    // ============================================================
    // POST (STRING)
    // ============================================================
    public static Response post(String endpoint, String jsonBody) throws Exception {

        RequestBody body = RequestBody.create(JSON_MEDIA, jsonBody);

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body);

        addHeaders(builder);

        return client.newCall(builder.build()).execute();
    }

    // ============================================================
    // POST (RequestBody DIRECTO)
    // ============================================================
    public static Response post(String endpoint, RequestBody body) throws Exception {

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body);

        addHeaders(builder);

        return client.newCall(builder.build()).execute();
    }

    // ============================================================
    // PUT (STRING)
    // ============================================================
    public static Response put(String endpoint, String jsonBody) throws Exception {

        RequestBody body = RequestBody.create(JSON_MEDIA, jsonBody);

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(body);

        addHeaders(builder);

        return client.newCall(builder.build()).execute();
    }

    // ============================================================
    // PUT (RequestBody DIRECTO)
    // ============================================================
    public static Response put(String endpoint, RequestBody body) throws Exception {

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(body);

        addHeaders(builder);

        return client.newCall(builder.build()).execute();
    }

    // ============================================================
    // DELETE
    // ============================================================
    public static Response delete(String endpoint) throws Exception {

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .delete();

        addHeaders(builder);

        return client.newCall(builder.build()).execute();
    }
}