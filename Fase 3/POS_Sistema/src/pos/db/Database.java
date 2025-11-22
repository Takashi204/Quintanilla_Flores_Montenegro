package pos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

public final class Database {

    private static final String DEFAULT_PATH =
            System.getProperty("user.home") + File.separator + ".pos_demo" + File.separator + "pos_cache.db";

    private static final String DEFAULT_URL = "jdbc:sqlite:" + DEFAULT_PATH;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "No se encontró el driver SQLite (sqlite-jdbc). " +
                "Asegúrate de haber agregado el JAR al Build Path.", e);
        }
    }

    public static String url() {
        String override = System.getProperty("pos.db.url");
        return (override != null && !override.isBlank()) ? override : DEFAULT_URL;
    }

    public static Connection get() throws SQLException {
        String u = url();

        if (u.startsWith("jdbc:sqlite:")) {
            String path = u.substring("jdbc:sqlite:".length());
            File dir = new File(path).getParentFile();
            if (dir != null) dir.mkdirs();
        }

        Connection cn = DriverManager.getConnection(u);

        try (Statement st = cn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
        } catch (SQLException ignore) {}

        // ============================================
        // 🔥 PARCHE AUTOMÁTICO PARA AGREGAR LA COLUMNA
        // ============================================
        try (Statement st = cn.createStatement()) {
            st.execute("""
                ALTER TABLE sales ADD COLUMN metodo TEXT DEFAULT 'EFECTIVO'
            """);
        } catch (SQLException ignore) {
            // La columna ya existe → ignorar
        }

        return cn;
    }

    private Database() {}
}
