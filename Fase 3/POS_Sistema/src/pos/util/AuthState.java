package pos.util;

public class AuthState {

    // Token JWT
    public static String TOKEN = null;

    // Valores reales que vienen del whoami()
    public static String USERNAME = null;  // sub
    public static String ROLE = null;      // role
    public static Integer USER_ID = null;  // uid
    public static Integer STORE_ID = null; // store_id (si la API lo envía)

    // Reset al cerrar sesión
    public static void clear() {
        TOKEN = null;
        USERNAME = null;
        ROLE = null;
        USER_ID = null;
        STORE_ID = null;
    }
}
