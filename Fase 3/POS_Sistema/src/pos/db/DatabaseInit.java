package pos.db;

import java.sql.*;

public final class DatabaseInit {

    public static void initialize() {
        // Abre conexión a la BD y un Statement para ejecutar SQL
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {

            // ================== INVENTORY ==================
            // Crea la tabla principal del inventario si no existe
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,   -- ID autoincremental
                    code     TEXT,                                -- código único del producto
                    name     TEXT    NOT NULL,                    -- nombre del producto
                    category TEXT,                                -- categoría opcional
                    price    INTEGER NOT NULL,                    -- precio
                    stock    INTEGER NOT NULL,                    -- cantidad en stock
                    expiry   TEXT                                 -- fecha de expiración (opcional)
                )
            """);

            // Migraciones defensivas:
            // Se ejecutan por si el usuario tiene una base de datos vieja sin estas columnas
            try { st.execute("ALTER TABLE inventory ADD COLUMN code TEXT"); }     catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN category TEXT"); } catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN expiry TEXT"); }   catch (SQLException ignore) {}

            // Índice único para asegurar que el código no se repite
            try { st.execute("CREATE UNIQUE INDEX ux_inventory_code ON inventory(code)"); }
            catch (SQLException ignore) {}

            // ================== INVENTORY_MOVEMENTS ==================
            // Historial de cambios de stock (entradas, salidas, ajustes)
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory_movements (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    code        TEXT    NOT NULL,       -- código del producto afectado
                    type        TEXT    NOT NULL,       -- tipo de movimiento (ADD, REMOVE, SALE, etc.)
                    qty         INTEGER NOT NULL,       -- cantidad afectada
                    reason      TEXT,                   -- motivo del cambio
                    prev_stock  INTEGER NOT NULL,       -- stock anterior
                    new_stock   INTEGER NOT NULL,       -- stock actualizado
                    user        TEXT,                   -- usuario que realizó la acción
                    created_at  TEXT    NOT NULL        -- timestamp del movimiento
                )
            """);

            // Índice para acelerar búsquedas por código + fecha
            try { st.execute("CREATE INDEX ix_mov_code_date ON inventory_movements(code, created_at)"); }
            catch (SQLException ignore) {}

            // ================== CASH_SESSIONS ==================
            // Control de apertura/cierre de caja por usuario
            st.execute("""
                CREATE TABLE IF NOT EXISTS cash_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user TEXT NOT NULL,                 -- usuario que abrió la caja
                    open_time TEXT NOT NULL,            -- hora de apertura
                    close_time TEXT,                    -- hora de cierre (null si no se ha cerrado)
                    monto_inicial INTEGER NOT NULL,     -- dinero al abrir
                    monto_final INTEGER,                -- dinero al cerrar
                    closed INTEGER NOT NULL DEFAULT 0   -- 0 → abierta | 1 → cerrada
                )
            """);

            // Índice para acelerar consultas del tipo “sesión abierta por usuario”
            try { st.execute("CREATE INDEX ix_cash_user_state ON cash_sessions(user, closed)"); }
            catch (SQLException ignore) {}

            // ================== PROVIDERS ==================
            // Tabla de proveedores
            st.execute("""
                CREATE TABLE IF NOT EXISTS providers (
                    id TEXT PRIMARY KEY,        -- ID tipo "P0001"
                    name TEXT NOT NULL,         -- nombre
                    phone TEXT,                 -- teléfono
                    email TEXT,                 -- correo
                    address TEXT,               -- dirección
                    created_at TEXT             -- fecha de registro
                )
            """);

            // ================== SALES ==================
            // Ventas registradas en el POS
            st.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL,         -- código del producto
                    name TEXT NOT NULL,         -- nombre del producto
                    quantity INTEGER NOT NULL,  -- cantidad vendida
                    price INTEGER NOT NULL,     -- precio unitario
                    total INTEGER NOT NULL,     -- total por ítem
                    fecha TEXT NOT NULL,        -- timestamp de la venta
                    user TEXT NOT NULL,         -- usuario que vendió
                    metodo TEXT                 -- método de pago
                )
            """);

            // Agregar columna metodo si el usuario tenía una base antigua
            try { st.execute("ALTER TABLE sales ADD COLUMN metodo TEXT"); }
            catch (SQLException ignore) {}

            // Índice para búsquedas por fecha + usuario
            try { st.execute("CREATE INDEX ix_sales_date_user ON sales(fecha, user)"); }
            catch (SQLException ignore) {}

            System.out.println("✅ Base de datos lista (inventory + movements + sessions + providers + sales)");

        } catch (SQLException e) {
            System.err.println("[DatabaseInit] Error al inicializar base: " + e.getMessage());
        }
    }

    // Constructor privado: evita instanciación de la clase
    private DatabaseInit() { }
}

