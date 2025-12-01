package pos.services;

import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import pos.model.User;
import pos.util.ApiClient;
import pos.util.AuthState;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class UserService {

	private static final String BASE = "/v1/admin/users";
    public static String TOKEN = "";

    // ================================
    // LISTAR USUARIOS (GET)
    // ================================
    public static List<User> getAll() throws Exception {

        Response res = ApiClient.get(BASE);

        if (!res.isSuccessful()) {
            throw new Exception("Error HTTP " + res.code());
        }

        String raw = res.body().string();

        // La API devuelve: { "items": [ {...}, {...} ] }
        JSONObject root = new JSONObject(raw);
        JSONArray arr = root.getJSONArray("items");

        List<User> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            String id = String.valueOf(o.getInt("id"));
            String username = o.getString("username");
            String fullName = o.optString("full_name", username); // fallback
            String role = o.getString("role");                    // API lo devuelve en inglés
            boolean active = o.getBoolean("is_active");

            // La API NO devuelve created_at, así que ponemos null
            LocalDate created = null;

            list.add(new User(
                    id,
                    username,
                    role,
                    active,
                    created,
                    "",          // password vacío (solo se usa en create)
                    fullName
            ));
        }

        return list;
    }

    // ================================
    // CREAR USUARIO (POST)
    // ================================
    public static void create(User u) throws Exception {

        JSONObject body = new JSONObject();
        body.put("username", u.getUsername());
        body.put("full_name", u.getUsername()); // o agrega otro campo si quieres nombre real
        body.put("password", u.getPassword());

        // Normalizar rol
        if (u.getRole().equalsIgnoreCase("ADMIN")) {
            body.put("role", "admin");
        } else {
            body.put("role", "cashier");
        }

        body.put("is_active", u.isActive());
        body.put("store_id", AuthState.STORE_ID); // ← obligatorio

        Response res = ApiClient.post(BASE, body.toString());

        if (!res.isSuccessful()) {
            throw new Exception("Error HTTP " + res.code() + " → " + res.body().string());
        }
    }

    // ================================
    // ACTUALIZAR USUARIO (PUT)
    // ================================
    public static void update(User u) throws Exception {

        JSONObject body = new JSONObject();
        body.put("username", u.getUsername());
        body.put("full_name", u.getFullName());
        body.put("role", u.getRole().equalsIgnoreCase("ADMIN") ? "admin" : "cashier");
        body.put("is_active", u.isActive());
        body.put("store_id", AuthState.STORE_ID); // ← OBLIGATORIO

        Response res = ApiClient.put("/v1/admin/users/" + u.getId(), body.toString());

        if (!res.isSuccessful()) {
            throw new Exception("Error HTTP " + res.code() + ": " + res.body().string());
        }
    }

    // ================================
    // ELIMINAR USUARIO (DELETE)
    // ================================
    public static void delete(String id) throws Exception {

        Response res = ApiClient.delete("/v1/admin/users/" + id);

        if (!res.isSuccessful()) {
            throw new Exception("Error HTTP " + res.code() + ": " + res.body().string());
        }
    }
}