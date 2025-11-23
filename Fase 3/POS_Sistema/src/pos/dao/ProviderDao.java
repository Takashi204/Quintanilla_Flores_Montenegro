package pos.dao;

import pos.db.Database;
import pos.model.Provider;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProviderDao {

    // Obtener todos los proveedores
    public List<Provider> listAll() {
        List<Provider> list = new ArrayList<>();
        String sql = "SELECT * FROM providers ORDER BY name"; // consulta ordenada por nombre

        try (Connection conn = Database.get();               // abrir conexión
             Statement st = conn.createStatement();          // crear statement simple
             ResultSet rs = st.executeQuery(sql)) {          // ejecutar consulta

            while (rs.next()) {
                list.add(new Provider(
                        rs.getString("id"),                   // ID proveedor (ej: P0001)
                        rs.getString("name"),                 // nombre del proveedor
                        rs.getString("phone"),                // teléfono
                        rs.getString("email"),                // email
                        rs.getString("address"),              // dirección
                        rs.getString("created_at") != null     // fecha creación (puede ser null)
                                ? LocalDate.parse(rs.getString("created_at"))
                                : null
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list; // retorno de todos los proveedores
    }

    // Insertar proveedor en la tabla
    public void insert(Provider p) {

        String sql = "INSERT INTO providers (id, name, phone, email, address, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getId());                      // ID P000X
            ps.setString(2, p.getName());                    // nombre
            ps.setString(3, p.getPhone());                   // teléfono
            ps.setString(4, p.getEmail());                   // email
            ps.setString(5, p.getAddress());                 // dirección
            ps.setString(6, p.getCreatedAt() != null
                    ? p.getCreatedAt().toString()            // pasar LocalDate → String
                    : null);

            ps.executeUpdate();                              // ejecutar insert

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Actualizar proveedor existente
    public void update(Provider p) {

        String sql = "UPDATE providers SET name=?, phone=?, email=?, address=?, created_at=? WHERE id=?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());                    // actualizar campos
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getAddress());
            ps.setString(5, p.getCreatedAt() != null
                    ? p.getCreatedAt().toString()
                    : null);
            ps.setString(6, p.getId());                      // condición WHERE id = ?

            ps.executeUpdate();                              // ejecutar update

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Eliminar un proveedor por ID
    public void delete(String id) {

        String sql = "DELETE FROM providers WHERE id=?";     // eliminar por ID

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);                             // ID a eliminar
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Generar nuevo ID automático: P0001, P0002, ...
    public String nextId() {

        String prefix = "P";                                 // prefijo fijo
        int num = 1;                                         // número inicial si no hay registros

        String sql = "SELECT id FROM providers ORDER BY id DESC LIMIT 1"; // obtener el último ID

        try (Connection conn = Database.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                String last = rs.getString("id").replace(prefix, ""); // quitar prefijo "P"
                num = Integer.parseInt(last) + 1;                     // incrementar número
            }

        } catch (SQLException ignore) {}

        return prefix + String.format("%04d", num);          // formatear P000X
    }
}
