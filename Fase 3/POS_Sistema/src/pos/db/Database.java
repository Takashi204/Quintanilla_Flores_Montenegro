package pos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

public final class Database {

    // Ruta por defecto donde se guardará la base de datos SQLite:
    // C:\Users\TUUSUARIO\.pos_demo\pos_cache.db
    private static final String DEFAULT_PATH =
            System.getProperty("user.home") + File.separator + ".pos_demo" + File.separator + "pos_cache.db";

    // URL JDBC para conectar con SQLite usando el archivo anterior
    private static final String DEFAULT_URL = "jdbc:sqlite:" + DEFAULT_PATH;

    // Bloque estático: se ejecuta una sola vez cuando carga la clase
    static {
        try {
            Class.forName("org.sqlite.JDBC");                  // Carga el driver JDBC de SQLite
        } catch (ClassNotFoundException e) {
            // Si el driver NO está agregado al proyecto, detiene el programa con un error claro
            throw new IllegalStateException(
                "No se encontró el driver SQLite (sqlite-jdbc). " +
                "Asegúrate de haber agregado el JAR al Build Path.", e);
        }
    }

    // Retorna la URL actual de la BD.
    // Si existe una propiedad del sistema llamada "pos.db.url", se usa esa.
    // Si no, se usa la ruta por defecto.
    public static String url() {
        String override = System.getProperty("pos.db.url");    // Permite cambiar la BD desde fuera
        return (override != null && !override.isBlank()) ? override : DEFAULT_URL;
    }

    // Método principal: obtiene y retorna una conexión activa a la BD
    public static Connection get() throws SQLException {
        String u = url();                                      // obtiene URL final

        // Si es SQLite, crear la carpeta si no existe
        if (u.startsWith("jdbc:sqlite:")) {
            String path = u.substring("jdbc:sqlite:".length()); // obtiene la ruta del archivo .db
            File dir = new File(path).getParentFile();          // obtiene carpeta contenedora
            if (dir != null) dir.mkdirs();                      // crea carpeta si es necesario
        }

        // Conecta con la base de datos
        Connection cn = DriverManager.getConnection(u);

        // Ajustes recomendados para SQLite
        try (Statement st = cn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");             // activa llaves foráneas
            st.execute("PRAGMA journal_mode = WAL");            // mejora concurrencia y velocidad
            st.execute("PRAGMA synchronous = NORMAL");          // mejor rendimiento con riesgo mínimo
        } catch (SQLException ignore) {}

        // =======================================================
        // PARCHE AUTOMÁTICO: Agregar columna "metodo" si no existe
        // =======================================================
        try (Statement st = cn.createStatement()) {
            st.execute("""
                ALTER TABLE sales ADD COLUMN metodo TEXT DEFAULT 'EFECTIVO'
            """);                                               // intenta agregar columna
        } catch (SQLException ignore) {
            // Si ya existe, el error se ignora → no afecta al sistema
        }

        return cn;                                             // conexión lista para usar
    }

    // Constructor privado: evita que alguien instancie Database
    private Database() {}
}
