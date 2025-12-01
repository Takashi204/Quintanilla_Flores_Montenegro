package pos.ui.views;

import pos.model.User;

import pos.services.UserService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel conectado 100% a la API real usando UserService.
 */
public class UsuariosPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final JTextField txtBuscar = new JTextField(20);
    private final JTable tabla = new JTable(new UsersModel());
    private final UsersModel modelo = (UsersModel) tabla.getModel();

    public UsuariosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(0xF9FAFB));

        JLabel title = new JLabel("Usuarios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(title, BorderLayout.NORTH);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        JButton btnBuscar = new JButton("Buscar");
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);

        add(barra, BorderLayout.PAGE_START);

        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        recargar();

        btnBuscar.addActionListener(e -> buscar());
        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
    }

    // =============================
    //        LISTAR
    // =============================
    private void recargar() {
        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return UserService.getAll();
            }

            @Override
            protected void done() {
                try {
                    modelo.set(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            "Error al cargar usuarios:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void buscar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            recargar();
            return;
        }
        JOptionPane.showMessageDialog(this, "La API no soporta búsqueda. Uso recargar.");
        recargar();
    }

    // =============================
    //         NUEVO
    // =============================
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
        if (res != JOptionPane.OK_OPTION) return;

        if (user.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario no puede estar vacío.");
            return;
        }

        User u = new User(
        	    null,
        	    user.getText().trim(),
        	    rol.getSelectedItem().toString(),
        	    activo.isSelected(),
        	    LocalDate.now(),
        	    pass.getText().trim(),
        	    user.getText().trim()   // fullName = mismo nombre  
        	);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                UserService.create(u);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UsuariosPanel.this, "Usuario creado correctamente.");
                    recargar();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            "Error al crear usuario:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =============================
    //          EDITAR
    // =============================
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

        if (JOptionPane.showConfirmDialog(this, p, "Editar usuario", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        // 🔥 ACTUALIZAR MODELO (AQUÍ ESTABA EL ERROR)
        u.setUsername(user.getText().trim());
        u.setFullName(user.getText().trim());      // ← ESTA LÍNEA FALTABA
        u.setRole(rol.getSelectedItem().toString());
        u.setActive(activo.isSelected());

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                UserService.update(u);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UsuariosPanel.this, "Usuario actualizado.");
                    recargar();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            "Error al actualizar:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =============================
    //         ELIMINAR
    // =============================
    private void eliminar() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar.");
            return;
        }

        User u = modelo.getAt(row);

        if (JOptionPane.showConfirmDialog(this,
                "¿Eliminar usuario '" + u.getUsername() + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                UserService.delete(u.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UsuariosPanel.this, "Usuario eliminado.");
                    recargar();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            "Error al eliminar:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =============================
    //     MODELO TABLA
    // =============================
    private static class UsersModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Usuario", "Rol", "Activo", "Creado"};
        private List<User> data = List.of();

        void set(List<User> d) {
            data = d;
            fireTableDataChanged();
        }

        User getAt(int r) { return data.get(r); }

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