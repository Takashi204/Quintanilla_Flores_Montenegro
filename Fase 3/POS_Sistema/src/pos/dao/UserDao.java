package pos.dao;

import pos.db.Database;
import pos.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Maneja operaciones CRUD de usuarios.
 * Compatible con SQLite y utilizado por UsuariosPanel.
 */
public class UserDao {

    // === Crear tabla si no existe ===
    public static void initTable() {
        final String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                role TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT,
                password TEXT NOT NULL
            )
        """;
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al crear tabla: " + e.getMessage());
        }
    }

    // === Listar todos ===
    public static List<User> getAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al listar: " + e.getMessage());
        }
        return list;
    }

    // === Buscar por texto (id, username o rol) ===
    public static List<User> search(String q) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE id LIKE ? OR username LIKE ? OR role LIKE ?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String like = "%" + q.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Error en búsqueda: " + e.getMessage());
        }
        return list;
    }

    // === Insertar ===
    public static void insert(User u) {
        String sql = """
            INSERT INTO users (id, username, role, active, created_at, password)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getId());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getRole());
            ps.setInt(4, u.isActive() ? 1 : 0);
            ps.setString(5, u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
            ps.setString(6, u.getPassword());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al insertar usuario: " + e.getMessage());
        }
    }

    // === Actualizar ===
    public static void update(User u) {
        String sql = """
            UPDATE users SET username=?, role=?, active=?, created_at=? WHERE id=?
        """;
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getRole());
            ps.setInt(3, u.isActive() ? 1 : 0);
            ps.setString(4, u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
            ps.setString(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al actualizar usuario: " + e.getMessage());
        }
    }

    // === Eliminar ===
    public static void delete(String id) {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al eliminar usuario: " + e.getMessage());
        }
    }

    // === Reset de contraseña ===
    public static boolean resetPassword(String id, String newPass) {
        String sql = "UPDATE users SET password=? WHERE id=?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, newPass);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al resetear contraseña: " + e.getMessage());
            return false;
        }
    }

    // === Generar siguiente ID ===
    public static int nextId() {
        String sql = "SELECT COUNT(*) AS total FROM users";
        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int n = rs.next() ? rs.getInt("total") + 1 : 1;
            return n;
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al generar ID: " + e.getMessage());
            return 1;
        }
    }

    // === Mapper interno ===
    private static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getInt("active") == 1,
                rs.getString("created_at") == null ? null : LocalDate.parse(rs.getString("created_at")),
                rs.getString("password")
        );
    }
}
