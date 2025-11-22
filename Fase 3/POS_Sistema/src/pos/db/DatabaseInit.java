package pos.db;

import java.sql.*;

public final class DatabaseInit {

    public static void initialize() {
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {

            // ================== INVENTORY ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    code     TEXT,
                    name     TEXT    NOT NULL,
                    category TEXT,
                    price    INTEGER NOT NULL,
                    stock    INTEGER NOT NULL,
                    expiry   TEXT
                )
            """);

            // Migraciones defensivas
            try { st.execute("ALTER TABLE inventory ADD COLUMN code TEXT"); }     catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN category TEXT"); } catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN expiry TEXT"); }   catch (SQLException ignore) {}
            try { st.execute("CREATE UNIQUE INDEX ux_inventory_code ON inventory(code)"); } catch (SQLException ignore) {}

            // ================== INVENTORY_MOVEMENTS ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory_movements (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    code        TEXT    NOT NULL,
                    type        TEXT    NOT NULL,
                    qty         INTEGER NOT NULL,
                    reason      TEXT,
                    prev_stock  INTEGER NOT NULL,
                    new_stock   INTEGER NOT NULL,
                    user        TEXT,
                    created_at  TEXT    NOT NULL
                )
            """);
            try { st.execute("CREATE INDEX ix_mov_code_date ON inventory_movements(code, created_at)"); } catch (SQLException ignore) {}

            // ================== CASH_SESSIONS ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS cash_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user TEXT NOT NULL,
                    open_time TEXT NOT NULL,
                    close_time TEXT,
                    monto_inicial INTEGER NOT NULL,
                    monto_final INTEGER,
                    closed INTEGER NOT NULL DEFAULT 0
                )
            """);
            try { st.execute("CREATE INDEX ix_cash_user_state ON cash_sessions(user, closed)"); } catch (SQLException ignore) {}

            // ================== PROVIDERS ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS providers (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    address TEXT,
                    created_at TEXT
                )
            """);

            // ================== SALES ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price INTEGER NOT NULL,
                    total INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    user TEXT NOT NULL,
                    metodo TEXT
                )
            """);

            // Migración defensiva para bases antiguas
            try { st.execute("ALTER TABLE sales ADD COLUMN metodo TEXT"); } catch (SQLException ignore) {}

            try { st.execute("CREATE INDEX ix_sales_date_user ON sales(fecha, user)"); } catch (SQLException ignore) {}

            System.out.println("✅ Base de datos lista (inventory + movements + sessions + providers + sales)");

        } catch (SQLException e) {
            System.err.println("[DatabaseInit] Error al inicializar base: " + e.getMessage());
        }
    }

    private DatabaseInit() { }
}
