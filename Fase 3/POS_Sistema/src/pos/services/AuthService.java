package pos.services;

import okhttp3.Response;
import org.json.JSONObject;
import pos.util.ApiClient;

public class AuthService {

    // ===========================================
    // 1) LOGIN → solo devuelve token
    // ===========================================
    public static String loginAndGetToken(String username, String password) throws Exception {

        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);

        // 🔥 RUTA CORRECTA
        Response res = ApiClient.post("/v1/auth/login", body.toString());

        if (!res.isSuccessful()) {
            throw new Exception("Credenciales incorrectas");
        }

        String raw = res.body().string();
        JSONObject json = new JSONObject(raw);

        String token = json.getString("access_token");

        // Guardamos el token global
        ApiClient.setToken(token);

        System.out.println("TOKEN GUARDADO = " + ApiClient.getToken());

        return token;
    }

    // ===========================================
    // 2) WHOAMI → obtiene datos reales del usuario
    // ===========================================
    public static JSONObject whoami() throws Exception {

        // 🔥 RUTA CORRECTA
        Response res = ApiClient.get("/v1/auth/whoami");

        if (!res.isSuccessful()) {
            throw new Exception("Error obteniendo usuario");
        }

        String raw = res.body().string();
        return new JSONObject(raw);
    }
}