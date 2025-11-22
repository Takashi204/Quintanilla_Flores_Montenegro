package pos.ui.views;

import pos.dao.UserDao;
import pos.model.User;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel de administración de usuarios.
 * Permite CRUD completo y reseteo de contraseñas, conectado a UserDao.
 */
public class UsuariosPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final JTextField txtBuscar = new JTextField(20);
    private final JTable tabla = new JTable(new UsersModel());
    private final UsersModel modelo = (UsersModel) tabla.getModel();

    public UsuariosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(0xF9FAFB));

        // === Título ===
        JLabel title = new JLabel("Usuarios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(title, BorderLayout.NORTH);

        // === Barra de herramientas ===
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        JButton btnBuscar = new JButton("Buscar");
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnReset = new JButton("Reset Pass");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        barra.add(btnReset);
        add(barra, BorderLayout.PAGE_START);

        // === Tabla ===
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // === Carga inicial ===
        recargar();

        // === Acciones ===
        btnBuscar.addActionListener(e -> buscar());
        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        btnReset.addActionListener(e -> reset());
    }

    // === Métodos principales ===
    private void recargar() {
        modelo.set(UserDao.getAll());
    }

    private void buscar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            recargar();
            return;
        }
        modelo.set(UserDao.search(q));
    }

    private void nuevo() {
        JTextField user = new JTextField();
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN", "CAJERO"});
        JTextField pass = new JTextField("123456");
        JCheckBox activo = new JCheckBox("Activo", true);

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Usuario:")); p.add(user);
        p.add(new JLabel("Rol:")); p.add(rol);
        p.add(new JLabel("Contraseña:")); p.add(pass);
        p.add(new JLabel("Activo:")); p.add(activo);

        int res = JOptionPane.showConfirmDialog(this, p, "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            if (user.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "El nombre de usuario no puede estar vacío.");
                return;
            }
            User u = new User(
                    String.valueOf(UserDao.nextId()),
                    user.getText().trim(),
                    rol.getSelectedItem().toString(),
                    activo.isSelected(),
                    LocalDate.now(),
                    pass.getText().trim()
            );
            UserDao.insert(u);
            recargar();
            JOptionPane.showMessageDialog(this, "Usuario creado correctamente.");
        }
    }

    private void editar() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para editar.");
            return;
        }

        User u = modelo.getAt(row);
        JTextField user = new JTextField(u.getUsername());
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN", "CAJERO"});
        rol.setSelectedItem(u.getRole());
        JCheckBox activo = new JCheckBox("Activo", u.isActive());

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Usuario:")); p.add(user);
        p.add(new JLabel("Rol:")); p.add(rol);
        p.add(new JLabel("Activo:")); p.add(activo);

        int res = JOptionPane.showConfirmDialog(this, p, "Editar usuario", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            if (user.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "El nombre de usuario no puede estar vacío.");
                return;
            }
            u.setUsername(user.getText().trim());
            u.setRole(rol.getSelectedItem().toString());
            u.setActive(activo.isSelected());
            UserDao.update(u);
            recargar();
            JOptionPane.showMessageDialog(this, "Usuario actualizado correctamente.");
        }
    }

    private void eliminar() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar.");
            return;
        }

        User u = modelo.getAt(row);
        int ok = JOptionPane.showConfirmDialog(this,
                "¿Eliminar usuario '" + u.getUsername() + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);

        if (ok == JOptionPane.YES_OPTION) {
            UserDao.delete(u.getId());
            recargar();
            JOptionPane.showMessageDialog(this, "Usuario eliminado correctamente.");
        }
    }

    private void reset() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para cambiar contraseña.");
            return;
        }

        User u = modelo.getAt(row);
        String nueva = JOptionPane.showInputDialog(this, "Nueva contraseña para " + u.getUsername() + ":");
        if (nueva != null && !nueva.isBlank()) {
            UserDao.resetPassword(u.getId(), nueva.trim());
            JOptionPane.showMessageDialog(this, "Contraseña actualizada correctamente.");
        }
    }

    // === Modelo de tabla ===
    private static class UsersModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Usuario", "Rol", "Activo", "Creado"};
        private List<User> data = List.of();

        void set(List<User> d) {
            data = d;
            fireTableDataChanged();
        }

        User getAt(int r) {
            return data.get(r);
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            User u = data.get(r);
            return switch (c) {
                case 0 -> u.getId();
                case 1 -> u.getUsername();
                case 2 -> u.getRole();
                case 3 -> u.isActive() ? "Sí" : "No";
                case 4 -> u.getCreatedAt() == null ? "-" : DF.format(u.getCreatedAt());
                default -> "";
            };
        }
    }
}
