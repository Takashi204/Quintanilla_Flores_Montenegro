package pos.db;

import pos.model.Product;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    /** Crea la tabla INVENTORY si no existe y asegura columnas/índice por code. */
    public void createTableIfNotExists() throws SQLException {
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {

            // Crea tabla inventory con todas las columnas necesarias
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,   -- ID autoincremental
                    code     TEXT,                                -- código único del producto
                    name     TEXT    NOT NULL,                    -- nombre del producto
                    category TEXT,                                -- categoría
                    price    INTEGER NOT NULL,                    -- precio
                    stock    INTEGER NOT NULL,                    -- cantidad
                    expiry   TEXT                                 -- expiración opcional
                )
            """);

            // Migración defensiva (por si faltan columnas en BD antiguas)
            try { st.execute("ALTER TABLE inventory ADD COLUMN code TEXT"); }     catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN category TEXT"); } catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN expiry TEXT"); }   catch (SQLException ignore) {}

            // Index para que code no se repita (único)
            try { st.execute("CREATE UNIQUE INDEX ux_inventory_code ON inventory(code)"); }
            catch (SQLException ignore) {}
        }
    }

    /** Inserta o actualiza por 'code'. Si existe el code → UPDATE, si no → INSERT. */
    public void upsert(Product p) throws SQLException {

        // 1) Intento de UPDATE si ya existe el producto por su código
        final String sqlUpdate = """
            UPDATE inventory
               SET name = ?, category = ?, price = ?, stock = ?, expiry = ?
             WHERE code = ?
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sqlUpdate)) {

            ps.setString(1, p.getName());           // nombre
            ps.setString(2, p.getCategory());       // categoría
            ps.setInt(3,    p.getPrice());          // precio
            ps.setInt(4,    p.getStock());          // stock
            if (p.getExpiry() != null)              // fecha exp opcional
                ps.setString(5, p.getExpiry().toString());
            else
                ps.setNull(5, Types.VARCHAR);

            ps.setString(6, p.getCode());           // código filtro para update

            int updated = ps.executeUpdate();       // cuántas filas afectadas
            if (updated > 0) return;                // si actualizó → terminar
        }

        // 2) Si no actualizó nada → INSERT
        final String sqlInsert = """
            INSERT INTO inventory (code, name, category, price, stock, expiry)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sqlInsert)) {

            ps.setString(1, p.getCode());           // código nuevo
            ps.setString(2, p.getName());           // nombre
            ps.setString(3, p.getCategory());       // categoría
            ps.setInt(4,    p.getPrice());          // precio
            ps.setInt(5,    p.getStock());          // stock
            if (p.getExpiry() != null)
                ps.setString(6, p.getExpiry().toString());
            else
                ps.setNull(6, Types.VARCHAR);

            ps.executeUpdate();                     // insertar producto
        }
    }

    /** Lista todos los productos de la tabla. */
    public List<Product> listAll() throws SQLException {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory ORDER BY id DESC";

        List<Product> out = new ArrayList<>();

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                out.add(fromRow(rs));               // convertir fila → Product y añadir a lista
            }
        }
        return out;
    }

    /** Buscar por code (útil si lo necesitas). */
    public Product findByCode(String code) throws SQLException {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory WHERE code = ?";

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code);                  // filtro por código

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);  // si existe → mapear a Product
            }
        }
        return null;                                // no encontrado
    }

    /** Eliminar por code (opcional). */
    public void deleteByCode(String code) throws SQLException {
        final String sql = "DELETE FROM inventory WHERE code = ?";

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code);                  // código del producto a eliminar
            ps.executeUpdate();
        }
    }

    // ===== Mapping fila -> Product =====
    private Product fromRow(ResultSet rs) throws SQLException {

        int id          = rs.getInt("id");          // ID interno
        String code     = rs.getString("code");     // código
        String name     = rs.getString("name");     // nombre
        String category = rs.getString("category"); // categoría
        int price       = rs.getInt("price");       // precio
        int stock       = rs.getInt("stock");       // stock
        String exStr    = rs.getString("expiry");   // expiry como texto

        // Parsear expiry a LocalDate (si no es nulo)
        LocalDate exp = (exStr == null || exStr.isBlank())
                ? null
                : LocalDate.parse(exStr);

        return new Product(id, code, name, category, price, stock, exp);
    }
}
