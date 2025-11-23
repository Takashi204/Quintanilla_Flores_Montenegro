package pos.ui.views; // Panel de la vista Proveedores dentro del módulo UI

import pos.dao.ProviderDao; // DAO para CRUD real en BD de proveedores
import pos.model.Provider; // Modelo Provider (id, name, phone, email, etc.)

import javax.swing.*; // Componentes Swing
import javax.swing.table.AbstractTableModel; // Modelo para tabla
import java.awt.*; // Layouts
import java.time.LocalDate; // Fechas de creación
import java.time.format.DateTimeFormatter; // Formato DD-MM-YYYY
import java.util.List; // Listas

public class ProveedoresPanel extends JPanel { // Panel principal de proveedores

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // Formato de fecha

    private final ProviderDao dao = new ProviderDao(); // DAO que maneja BD de proveedores
    private final JTextField txtBuscar = new JTextField(22); // Input búsqueda
    private final JTable tabla = new JTable(new ProveedoresModel()); // Tabla visual
    private final ProveedoresModel modelo = (ProveedoresModel) tabla.getModel(); // Modelo de tabla

    public ProveedoresPanel() { // Constructor del panel
        setLayout(new BorderLayout(10, 10)); // Margen general
        setBackground(new Color(0xF9FAFB)); // Fondo gris claro elegante

        JLabel title = new JLabel("Proveedores"); // Título
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f)); // Tamaño grande
        title.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12)); // Padding
        add(title, BorderLayout.NORTH); // Arriba

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)); // Barra superior
        barra.setOpaque(false); // Transparente

        txtBuscar.putClientProperty("JTextField.placeholderText",
                "Buscar por nombre, teléfono, email o ID..."); // Placeholder del buscador
        
        JButton btnBuscar = new JButton("Buscar"); // Botón buscar
        JButton btnNuevo = new JButton("Nuevo"); // Crear proveedor
        JButton btnEditar = new JButton("Editar"); // Editar proveedor
        JButton btnEliminar = new JButton("Eliminar"); // Eliminar proveedor
        JButton btnRapido = new JButton("Registro rápido"); // Alta rápida

        barra.add(txtBuscar); // Input búsqueda
        barra.add(btnBuscar); // Botón buscar
        barra.add(btnNuevo); // Crear nuevo
        barra.add(btnEditar); // Editar
        barra.add(btnEliminar); // Eliminar
        barra.add(btnRapido); // Registro rápido
        add(barra, BorderLayout.PAGE_START); // Arriba

        tabla.setRowHeight(22); // Alto de filas
        add(new JScrollPane(tabla), BorderLayout.CENTER); // Scroll + tabla

        recargar(); // Cargar datos al abrir panel

        btnBuscar.addActionListener(e -> buscar()); // Listener buscar
        btnNuevo.addActionListener(e -> nuevoProveedor()); // Listener nuevo
        btnEditar.addActionListener(e -> editarProveedor()); // Listener editar
        btnEliminar.addActionListener(e -> eliminarProveedor()); // Listener eliminar
        btnRapido.addActionListener(e -> registroRapido()); // Listener registro rápido
    }

    private void recargar() { // Recargar lista desde BD
        modelo.set(dao.listAll()); // Actualizar modelo
    }

    private void buscar() { // Lógica de búsqueda
        String q = txtBuscar.getText().trim().toLowerCase(); // Texto buscado
        if (q.isEmpty()) { recargar(); return; } // Si vacío, cargar todo

        List<Provider> todos = dao.listAll(); // Lista completa
        List<Provider> filtrados = todos.stream() // Filtrar por:
                .filter(p ->
                        p.getName().toLowerCase().contains(q) // Nombre
                                || (p.getPhone() != null && p.getPhone().toLowerCase().contains(q)) // Teléfono
                                || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q)) // Email
                                || p.getId().toLowerCase().contains(q)) // ID
                .toList();

        modelo.set(filtrados); // Actualizar tabla
    }

    private void nuevoProveedor() { // Crear proveedor
        JTextField id = new JTextField(dao.nextId()); // ID sugerido
        JTextField nombre = new JTextField(); // Input nombre
        JTextField fono = new JTextField(); // Input fono
        JTextField email = new JTextField(); // Input email
        JTextField direccion = new JTextField(); // Input dirección
        JTextField fecha = new JTextField(LocalDate.now().toString()); // Fecha creación por defecto

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6)); // Formulario
        p.add(new JLabel("ID:")); p.add(id);
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Teléfono:")); p.add(fono);
        p.add(new JLabel("Email:")); p.add(email);
        p.add(new JLabel("Dirección:")); p.add(direccion);
        p.add(new JLabel("Creado (AAAA-MM-DD):")); p.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, p, "Nuevo proveedor",
                JOptionPane.OK_CANCEL_OPTION); // Mostrar diálogo
        if (res == JOptionPane.OK_OPTION) { // Si presiona OK
            try {
                LocalDate created = fecha.getText().isBlank()
                        ? LocalDate.now()
                        : LocalDate.parse(fecha.getText()); // Parse fecha

                Provider prov = new Provider(
                        id.getText(), nombre.getText(), fono.getText(),
                        email.getText(), direccion.getText(), created
                );
                dao.insert(prov); // Guardar en BD
                recargar(); // Refrescar tabla
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos"); // Error datos
            }
        }
    }

    private void editarProveedor() { // Editar proveedor
        int row = tabla.getSelectedRow(); // Fila seleccionada
        if (row < 0) { // Si no seleccionó
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor para editar");
            return;
        }
        Provider p = modelo.getAt(row); // Obtener proveedor

        JTextField nombre = new JTextField(p.getName()); // Campos con datos actuales
        JTextField fono = new JTextField(p.getPhone());
        JTextField email = new JTextField(p.getEmail());
        JTextField direccion = new JTextField(p.getAddress());
        JTextField fecha = new JTextField(p.getCreatedAt() == null ? "" : p.getCreatedAt().toString());

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6)); // Formulario editar
        panel.add(new JLabel("ID:")); panel.add(new JLabel(p.getId())); // ID no editable
        panel.add(new JLabel("Nombre:")); panel.add(nombre);
        panel.add(new JLabel("Teléfono:")); panel.add(fono);
        panel.add(new JLabel("Email:")); panel.add(email);
        panel.add(new JLabel("Dirección:")); panel.add(direccion);
        panel.add(new JLabel("Creado:")); panel.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, panel, "Editar proveedor",
                JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) { // Guardar cambios
            try {
                p.setName(nombre.getText());
                p.setPhone(fono.getText());
                p.setEmail(email.getText());
                p.setAddress(direccion.getText());
                p.setCreatedAt(fecha.getText().isBlank()
                        ? null
                        : LocalDate.parse(fecha.getText()));
                dao.update(p); // Actualizar en BD
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al editar proveedor");
            }
        }
    }

    private void eliminarProveedor() { // Eliminar proveedor
        int row = tabla.getSelectedRow(); // Fila seleccionada
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor para eliminar");
            return;
        }
        Provider p = modelo.getAt(row); // Proveedor

        int ok = JOptionPane.showConfirmDialog(this,
                "¿Eliminar " + p.getName() + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION); // Confirmar eliminación

        if (ok == JOptionPane.YES_OPTION) { // Si confirma
            dao.delete(p.getId()); // Eliminar de BD
            recargar(); // Refrescar
        }
    }

    private void registroRapido() { // Crear proveedor minimalista
        JTextField nombre = new JTextField(); // Solo nombre
        JTextField fono = new JTextField(); // Solo teléfono

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6)); // Formulario rápido
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Teléfono:")); p.add(fono);

        int res = JOptionPane.showConfirmDialog(this, p, "Registro rápido de proveedor",
                JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            try {
                Provider prov = new Provider(
                        dao.nextId(), nombre.getText(), fono.getText(),
                        "", "", LocalDate.now()
                ); // Email/dirección vacíos
                dao.insert(prov); // Insertar BD
                recargar(); // Refrescar
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error en los datos ingresados");
            }
        }
    }

    private static class ProveedoresModel extends AbstractTableModel { // Modelo tabla
        private final String[] cols =
                {"ID", "Nombre", "Teléfono", "Email", "Dirección", "Creado"}; // Cabeceras
        private List<Provider> data = List.of(); // Data inicial vacía

        public void set(List<Provider> rows) { // Setear nueva data
            data = rows;
            fireTableDataChanged(); // Actualizar tabla
        }

        public Provider getAt(int row) { return data.get(row); } // Obtener proveedor fila

        @Override public int getRowCount() { return data.size(); } // Cantidad filas
        @Override public int getColumnCount() { return cols.length; } // Cantidad columnas
        @Override public String getColumnName(int c) { return cols[c]; } // Nombre columna

        @Override
        public Object getValueAt(int r, int c) { // Valores por columna
            Provider x = data.get(r);
            return switch (c) {
                case 0 -> x.getId();
                case 1 -> x.getName();
                case 2 -> x.getPhone();
                case 3 -> x.getEmail();
                case 4 -> x.getAddress();
                case 5 -> x.getCreatedAt() == null ? "-" : DF.format(x.getCreatedAt());
                default -> "";
            };
        }
    }
}

