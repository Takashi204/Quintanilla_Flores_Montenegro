package pos.db;

import pos.model.Product;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    /** Crea la tabla si no existe. */
    public void createTableIfNotExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS products(
              code TEXT PRIMARY KEY,
              name TEXT NOT NULL,
              category TEXT,
              price INTEGER NOT NULL,
              stock INTEGER NOT NULL,
              expiry TEXT NULL
            )
        """;
        try (Connection c = Database.get(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    /** Inserta o actualiza por code (UPSERT). */
    public boolean upsert(Product p) throws SQLException {
        String sql = """
            INSERT INTO products(code,name,category,price,stock,expiry)
            VALUES(?,?,?,?,?,?)
            ON CONFLICT(code) DO UPDATE SET
              name=excluded.name,
              category=excluded.category,
              price=excluded.price,
              stock=excluded.stock,
              expiry=excluded.expiry
        """;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate() >= 1;
        }
    }

    /** Borra por código. */
    public boolean delete(String code) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE code=?")) {
            ps.setString(1, code);
            return ps.executeUpdate() >= 1;
        }
    }

    /** Busca un producto por código. */
    public Product find(String code) throws SQLException {
        String sql = "SELECT code,name,category,price,stock,expiry FROM products WHERE code=?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Lista todos los productos ordenados por nombre. */
    public List<Product> listAll() throws SQLException {
        String sql = "SELECT code,name,category,price,stock,expiry FROM products ORDER BY name";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Product> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    // ---------- helpers ----------
    private void bind(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getCode());
        ps.setString(2, p.getName());
        ps.setString(3, p.getCategory());
        ps.setInt(4, p.getPrice());
        ps.setInt(5, p.getStock());
        ps.setString(6, p.getExpiry() == null ? null : p.getExpiry().toString());
    }

    private Product map(ResultSet rs) throws SQLException {
        String code = rs.getString("code");
        String name = rs.getString("name");
        String cat  = rs.getString("category");
        int price   = rs.getInt("price");
        int stock   = rs.getInt("stock");
        String exp  = rs.getString("expiry");
        LocalDate expiry = (exp == null) ? null : LocalDate.parse(exp);
        return new Product(code, name, cat, price, stock, expiry);
    }
}
