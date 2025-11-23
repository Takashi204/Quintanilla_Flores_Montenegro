package pos.ui.views; // Panel de administración dentro del paquete de vistas

import pos.dao.UserDao; // DAO para interactuar con usuarios en la BD
import pos.model.User; // Modelo de usuario

import javax.swing.*; // Componentes Swing
import javax.swing.table.AbstractTableModel; // Modelo de tabla
import java.awt.*; // Layouts y colores
import java.time.LocalDate; // Fechas
import java.time.format.DateTimeFormatter; // Formato fecha
import java.util.List; // Listas

/**
 * Panel de administración de usuarios.
 * Permite CRUD completo y reseteo de contraseñas, conectado a UserDao.
 */
public class UsuariosPanel extends JPanel { // Panel principal de usuarios

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // Formato de fecha

    private final JTextField txtBuscar = new JTextField(20); // Caja de texto para buscar usuarios
    private final JTable tabla = new JTable(new UsersModel()); // Tabla que muestra usuarios
    private final UsersModel modelo = (UsersModel) tabla.getModel(); // Modelo de la tabla

    public UsuariosPanel() { // Constructor
        setLayout(new BorderLayout(10, 10)); // Margen general entre componentes
        setBackground(new Color(0xF9FAFB)); // Fondo suave

        JLabel title = new JLabel("Usuarios"); // Título grande
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f)); // Estilo bold
        title.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12)); // Padding
        add(title, BorderLayout.NORTH); // Colocado arriba

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)); // Barra de herramientas
        barra.setOpaque(false); // Sin fondo

        JButton btnBuscar = new JButton("Buscar"); // Botón buscar
        JButton btnNuevo = new JButton("Nuevo"); // Crear usuario nuevo
        JButton btnEditar = new JButton("Editar"); // Editar usuario seleccionado
        JButton btnEliminar = new JButton("Eliminar"); // Eliminar usuario
        JButton btnReset = new JButton("Reset Pass"); // Resetear contraseña

        barra.add(txtBuscar); // Input buscar
        barra.add(btnBuscar); // Botón buscar
        barra.add(btnNuevo); // Botón nuevo
        barra.add(btnEditar); // Botón editar
        barra.add(btnEliminar); // Botón eliminar
        barra.add(btnReset); // Botón reset pass
        add(barra, BorderLayout.PAGE_START); // Poner la barra arriba

        tabla.setRowHeight(22); // Altura de filas
        add(new JScrollPane(tabla), BorderLayout.CENTER); // Tabla con scroll

        recargar(); // Cargar usuarios al iniciar

        btnBuscar.addActionListener(e -> buscar()); // Acción buscar
        btnNuevo.addActionListener(e -> nuevo()); // Acción nuevo
        btnEditar.addActionListener(e -> editar()); // Acción editar
        btnEliminar.addActionListener(e -> eliminar()); // Acción eliminar
        btnReset.addActionListener(e -> reset()); // Acción reset pass
    }

    private void recargar() { // Recargar tabla completa
        modelo.set(UserDao.getAll()); // Obtener todos los usuarios desde BD
    }

    private void buscar() { // Filtrar por texto
        String q = txtBuscar.getText().trim().toLowerCase(); // Texto normalizado
        if (q.isEmpty()) { recargar(); return; } // Si no hay texto, mostrar todo
        modelo.set(UserDao.search(q)); // Buscar en BD
    }

    private void nuevo() { // Crear usuario nuevo
        JTextField user = new JTextField(); // Input usuario
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN", "CAJERO"}); // Combo roles
        JTextField pass = new JTextField("123456"); // Contraseña por defecto
        JCheckBox activo = new JCheckBox("Activo", true); // Checkbox si está activo

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6)); // Formulario
        p.add(new JLabel("Usuario:")); p.add(user);
        p.add(new JLabel("Rol:")); p.add(rol);
        p.add(new JLabel("Contraseña:")); p.add(pass);
        p.add(new JLabel("Activo:")); p.add(activo);

        int res = JOptionPane.showConfirmDialog(this, p, "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION); // Dialogo
        if (res == JOptionPane.OK_OPTION) { // Si presiona OK
            if (user.getText().isBlank()) { // Validación
                JOptionPane.showMessageDialog(this, "El nombre de usuario no puede estar vacío.");
                return;
            }
            User u = new User( // Crear objeto usuario
                    String.valueOf(UserDao.nextId()), // ID autoincremental
                    user.getText().trim(), // Username
                    rol.getSelectedItem().toString(), // Rol
                    activo.isSelected(), // Estado activo
                    LocalDate.now(), // Fecha creación
                    pass.getText().trim() // Contraseña
            );
            UserDao.insert(u); // Insertar en BD
            recargar(); // Refrescar tabla
            JOptionPane.showMessageDialog(this, "Usuario creado correctamente.");
        }
    }

    private void editar() { // Editar usuario
        int row = tabla.getSelectedRow(); // Selección de tabla
        if (row < 0) { // Validación
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para editar.");
            return;
        }

        User u = modelo.getAt(row); // Usuario seleccionado
        JTextField user = new JTextField(u.getUsername()); // Input editable
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN", "CAJERO"}); // Roles
        rol.setSelectedItem(u.getRole()); // Seleccionar el actual
        JCheckBox activo = new JCheckBox("Activo", u.isActive()); // Estado

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6)); // Formulario
        p.add(new JLabel("Usuario:")); p.add(user);
        p.add(new JLabel("Rol:")); p.add(rol);
        p.add(new JLabel("Activo:")); p.add(activo);

        int res = JOptionPane.showConfirmDialog(this, p, "Editar usuario", JOptionPane.OK_CANCEL_OPTION); // Dialog
        if (res == JOptionPane.OK_OPTION) { // Si OK
            if (user.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "El nombre de usuario no puede estar vacío.");
                return;
            }
            u.setUsername(user.getText().trim()); // Actualiza nombre
            u.setRole(rol.getSelectedItem().toString()); // Actualiza rol
            u.setActive(activo.isSelected()); // Actualiza estado
            UserDao.update(u); // Guardar en BD
            recargar(); // Refrescar tabla
            JOptionPane.showMessageDialog(this, "Usuario actualizado correctamente.");
        }
    }

    private void eliminar() { // Eliminar usuario
        int row = tabla.getSelectedRow(); // Selección
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar.");
            return;
        }

        User u = modelo.getAt(row); // Usuario seleccionado
        int ok = JOptionPane.showConfirmDialog(this,
                "¿Eliminar usuario '" + u.getUsername() + "'?", // Mensaje
                "Confirmar", JOptionPane.YES_NO_OPTION);

        if (ok == JOptionPane.YES_OPTION) { // Si confirma
            UserDao.delete(u.getId()); // Eliminar BD
            recargar(); // Refrescar
            JOptionPane.showMessageDialog(this, "Usuario eliminado correctamente.");
        }
    }

    private void reset() { // Resetear contraseña
        int row = tabla.getSelectedRow(); // Selección
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para cambiar contraseña.");
            return;
        }

        User u = modelo.getAt(row); // Usuario seleccionado
        String nueva = JOptionPane.showInputDialog(this, "Nueva contraseña para " + u.getUsername() + ":"); // Input
        if (nueva != null && !nueva.isBlank()) { // Validación
            UserDao.resetPassword(u.getId(), nueva.trim()); // Actualizar BD
            JOptionPane.showMessageDialog(this, "Contraseña actualizada correctamente.");
        }
    }

    private static class UsersModel extends AbstractTableModel { // Modelo de tabla
        private final String[] cols = {"ID", "Usuario", "Rol", "Activo", "Creado"}; // Encabezados tabla
        private List<User> data = List.of(); // Datos

        void set(List<User> d) { // Cargar registros
            data = d;
            fireTableDataChanged(); // Actualizar tabla
        }

        User getAt(int r) { return data.get(r); } // Obtener usuario por fila

        @Override public int getRowCount() { return data.size(); } // Cant filas
        @Override public int getColumnCount() { return cols.length; } // Cant columnas
        @Override public String getColumnName(int c) { return cols[c]; } // Nombre columna

        @Override
        public Object getValueAt(int r, int c) { // Datos por celda
            User u = data.get(r);
            return switch (c) {
                case 0 -> u.getId(); // ID
                case 1 -> u.getUsername(); // Usuario
                case 2 -> u.getRole(); // Rol
                case 3 -> u.isActive() ? "Sí" : "No"; // Activo
                case 4 -> u.getCreatedAt() == null ? "-" : DF.format(u.getCreatedAt()); // Fecha creación
                default -> "";
            };
        }
    }
}
