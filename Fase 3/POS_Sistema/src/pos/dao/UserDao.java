package pos.dao;

import pos.db.Database;
import pos.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO que maneja todas las operaciones CRUD de usuarios.
 * Lo usa el panel UsuariosPanel.
 */
public class UserDao {

    // Crear tabla de usuarios si no existe
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
        """; // estructura de la tabla

        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {
            st.execute(sql); // crear tabla si falta
        } catch (SQLException e) {
            System.err.println("[UserDao] Error al crear tabla: " + e.getMessage());
        }
    }

    // Obtener todos los usuarios
    public static List<User> getAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username"; // ordenados por nombre

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs)); // mapper → convertir fila a User
            }

        } catch (SQLException e) {
            System.err.println("[UserDao] Error al listar: " + e.getMessage());
        }

        return list;
    }

    // Buscar usuarios por ID, username o rol
    public static List<User> search(String q) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE id LIKE ? OR username LIKE ? OR role LIKE ?";

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            String like = "%" + q.trim() + "%";  // patrón de búsqueda
            ps.setString(1, like);               // buscar en id
            ps.setString(2, like);               // buscar en username
            ps.setString(3, like);               // buscar en role

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs)); // convertir fila
                }
            }

        } catch (SQLException e) {
            System.err.println("[UserDao] Error en búsqueda: " + e.getMessage());
        }

        return list;
    }

    // Insertar un nuevo usuario
    public static void insert(User u) {
        String sql = """
            INSERT INTO users (id, username, role, active, created_at, password)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, u.getId());                               // ID del usuario
            ps.setString(2, u.getUsername());                         // nombre
            ps.setString(3, u.getRole());                             // rol (admin/cajero)
            ps.setInt(4, u.isActive() ? 1 : 0);                       // activo 1/0
            ps.setString(5, u.getCreatedAt() == null
                    ? null
                    : u.getCreatedAt().toString());                  // fecha creación
            ps.setString(6, u.getPassword());                         // contraseña

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[UserDao] Error al insertar usuario: " + e.getMessage());
        }
    }

    // Actualizar datos de un usuario
    public static void update(User u) {
        String sql = """
            UPDATE users SET username=?, role=?, active=?, created_at=? WHERE id=?
        """;

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, u.getUsername());                         // nuevo nombre
            ps.setString(2, u.getRole());                             // nuevo rol
            ps.setInt(3, u.isActive() ? 1 : 0);                       // estado
            ps.setString(4, u.getCreatedAt() == null
                    ? null
                    : u.getCreatedAt().toString());                  // fecha creación
            ps.setString(5, u.getId());                               // condición ID

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[UserDao] Error al actualizar usuario: " + e.getMessage());
        }
    }

    // Eliminar un usuario por ID
    public static void delete(String id) {

        String sql = "DELETE FROM users WHERE id=?";

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, id);       // ID a eliminar
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[UserDao] Error al eliminar usuario: " + e.getMessage());
        }
    }

    // Cambiar contraseña de un usuario
    public static boolean resetPassword(String id, String newPass) {
        String sql = "UPDATE users SET password=? WHERE id=?";

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, newPass); // colocar nueva contraseña
            ps.setString(2, id);

            return ps.executeUpdate() > 0; // true si se actualizó

        } catch (SQLException e) {
            System.err.println("[UserDao] Error al resetear contraseña: " + e.getMessage());
            return false;
        }
    }

    // Generar siguiente ID (1, 2, 3...)
    public static int nextId() {
        String sql = "SELECT COUNT(*) AS total FROM users";

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            int n = rs.next() ? rs.getInt("total") + 1 : 1; // total+1
            return n;

        } catch (SQLException e) {
            System.err.println("[UserDao] Error al generar ID: " + e.getMessage());
            return 1;
        }
    }

    // Convertir una fila SQL → objeto User
    private static User map(ResultSet rs) throws SQLException {

        String id = rs.getString("id");
        String username = rs.getString("username");
        String role = rs.getString("role");
        boolean active = rs.getInt("active") == 1;

        LocalDate createdAt = null;
        String created = rs.getString("created_at");
        if (created != null && !created.isBlank()) {
            createdAt = LocalDate.parse(created);
        }

        String password = rs.getString("password");

        // campo nuevo en tu clase User
        String fullName = rs.getString("full_name");

        return new User(
                id,
                username,
                role,
                active,
                createdAt,
                password,
                fullName
        );
    }
}
