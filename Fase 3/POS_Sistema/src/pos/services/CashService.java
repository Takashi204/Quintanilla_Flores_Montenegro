package pos.services;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import pos.util.ApiClient;

public class CashService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // NO vamos a confiar en memoria vieja, pero dejamos la variable
    private static Integer activeSessionId = null;

    // =========================================================
    // 🔥 RESET (para forzar recargar sesión desde API)
    // =========================================================
    public static void resetSession() {
        activeSessionId = null;
    }

    // =========================================================
    // 1) ABRIR CAJA
    // POST /v1/cash/open
    // =========================================================
    public static int openCash(int registerId, int openingAmount) throws Exception {

        JSONObject body = new JSONObject();
        body.put("register_id", registerId);
        body.put("opening_amount", openingAmount);

        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.post("/v1/cash/open", req);

        String raw = res.body().string();
        if (!res.isSuccessful()) {
            throw new Exception("Error abriendo caja: " + raw);
        }

        JSONObject json = new JSONObject(raw);
        int sessionId = json.getInt("id");

        // Guardamos la nueva sesión
        activeSessionId = sessionId;

        System.out.println("🟢 Caja abierta con éxito. session_id = " + sessionId);

        return sessionId;
    }

    // =========================================================
    // 2) OBTENER CAJA ACTIVA (siempre consulta API)
    // GET /v1/cash/active
    // =========================================================
    public static int getActiveSessionId() throws Exception {

        // ❗ Siempre consultamos a la API para evitar usar sesiones antiguas
        Response res = ApiClient.get("/v1/cash/active");
        String raw = res.body().string();

        if (!res.isSuccessful()) {
            throw new Exception("No hay caja activa (" + raw + ")");
        }

        JSONObject json = new JSONObject(raw);

        int sessionId = json.getInt("id");
        activeSessionId = sessionId; // actualizamos siempre

        System.out.println("🟢 Caja activa API detectada (refrescada). session_id = " + sessionId);

        return sessionId;
    }

    // =========================================================
    // 3) CERRAR CAJA
    // POST /v1/cash/close
    // =========================================================
    public static void closeCash(int sessionId) throws Exception {

        JSONObject body = new JSONObject();
        body.put("session_id", sessionId);
        body.put("declared_amount", 0);
        body.put("closing_amount", 0);
        body.put("note", "");

        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.post("/v1/cash/close", req);

        String raw = res.body().string();

        if (!res.isSuccessful()) {
            throw new Exception("Error cerrando caja: " + raw);
        }

        System.out.println("🔴 Caja cerrada correctamente.");
        activeSessionId = null; // la caja ya no existe
    }
}