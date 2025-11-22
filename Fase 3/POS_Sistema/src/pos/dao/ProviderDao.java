package pos.dao;

import pos.db.Database;
import pos.model.Provider;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProviderDao {

    // ✅ Obtener todos los proveedores
    public List<Provider> listAll() {
        List<Provider> list = new ArrayList<>();
        String sql = "SELECT * FROM providers ORDER BY name";
        try (Connection conn = Database.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Provider(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("created_at") != null
                                ? LocalDate.parse(rs.getString("created_at"))
                                : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Insertar proveedor
    public void insert(Provider p) {
        String sql = "INSERT INTO providers (id, name, phone, email, address, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getPhone());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getAddress());
            ps.setString(6, p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ Actualizar proveedor existente
    public void update(Provider p) {
        String sql = "UPDATE providers SET name=?, phone=?, email=?, address=?, created_at=? WHERE id=?";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getAddress());
            ps.setString(5, p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
            ps.setString(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ Eliminar proveedor
    public void delete(String id) {
        String sql = "DELETE FROM providers WHERE id=?";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ Generar nuevo ID automático (P0001, P0002, ...)
    public String nextId() {
        String prefix = "P";
        int num = 1;
        String sql = "SELECT id FROM providers ORDER BY id DESC LIMIT 1";
        try (Connection conn = Database.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String last = rs.getString("id").replace(prefix, "");
                num = Integer.parseInt(last) + 1;
            }
        } catch (SQLException ignore) {}
        return prefix + String.format("%04d", num);
    }
}
