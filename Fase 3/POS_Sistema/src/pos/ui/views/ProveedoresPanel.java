package pos.ui.views;

import pos.dao.ProviderDao;
import pos.model.Provider;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProveedoresPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ProviderDao dao = new ProviderDao(); // ✅ DAO para BD real
    private final JTextField txtBuscar = new JTextField(22);
    private final JTable tabla = new JTable(new ProveedoresModel());
    private final ProveedoresModel modelo = (ProveedoresModel) tabla.getModel();

    public ProveedoresPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(0xF9FAFB));

        // ===== Título =====
        JLabel title = new JLabel("Proveedores");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(title, BorderLayout.NORTH);

        // ===== Barra superior =====
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre, teléfono, email o ID...");
        JButton btnBuscar = new JButton("Buscar");
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnRapido = new JButton("Registro rápido");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        barra.add(btnRapido);
        add(barra, BorderLayout.PAGE_START);

        // ===== Tabla =====
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Carga inicial
        recargar();

        // Acciones
        btnBuscar.addActionListener(e -> buscar());
        btnNuevo.addActionListener(e -> nuevoProveedor());
        btnEditar.addActionListener(e -> editarProveedor());
        btnEliminar.addActionListener(e -> eliminarProveedor());
        btnRapido.addActionListener(e -> registroRapido());
    }

    // ===== Lógica =====
    private void recargar() {
        modelo.set(dao.listAll());
    }

    private void buscar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            recargar();
            return;
        }

        List<Provider> todos = dao.listAll();
        List<Provider> filtrados = todos.stream()
                .filter(p ->
                        p.getName().toLowerCase().contains(q)
                                || (p.getPhone() != null && p.getPhone().toLowerCase().contains(q))
                                || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))
                                || p.getId().toLowerCase().contains(q))
                .toList();

        modelo.set(filtrados);
    }

    private void nuevoProveedor() {
        JTextField id = new JTextField(dao.nextId());
        JTextField nombre = new JTextField();
        JTextField fono = new JTextField();
        JTextField email = new JTextField();
        JTextField direccion = new JTextField();
        JTextField fecha = new JTextField(LocalDate.now().toString());

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("ID:"));
        p.add(id);
        p.add(new JLabel("Nombre:"));
        p.add(nombre);
        p.add(new JLabel("Teléfono:"));
        p.add(fono);
        p.add(new JLabel("Email:"));
        p.add(email);
        p.add(new JLabel("Dirección:"));
        p.add(direccion);
        p.add(new JLabel("Creado (AAAA-MM-DD):"));
        p.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, p, "Nuevo proveedor", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                LocalDate created = fecha.getText().isBlank() ? LocalDate.now() : LocalDate.parse(fecha.getText());
                Provider prov = new Provider(
                        id.getText(),
                        nombre.getText(),
                        fono.getText(),
                        email.getText(),
                        direccion.getText(),
                        created
                );
                dao.insert(prov);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos");
            }
        }
    }

    private void editarProveedor() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor para editar");
            return;
        }
        Provider p = modelo.getAt(row);

        JTextField nombre = new JTextField(p.getName());
        JTextField fono = new JTextField(p.getPhone());
        JTextField email = new JTextField(p.getEmail());
        JTextField direccion = new JTextField(p.getAddress());
        JTextField fecha = new JTextField(p.getCreatedAt() == null ? "" : p.getCreatedAt().toString());

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("ID:"));
        panel.add(new JLabel(p.getId()));
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Teléfono:"));
        panel.add(fono);
        panel.add(new JLabel("Email:"));
        panel.add(email);
        panel.add(new JLabel("Dirección:"));
        panel.add(direccion);
        panel.add(new JLabel("Creado:"));
        panel.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, panel, "Editar proveedor", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                p.setName(nombre.getText());
                p.setPhone(fono.getText());
                p.setEmail(email.getText());
                p.setAddress(direccion.getText());
                p.setCreatedAt(fecha.getText().isBlank() ? null : LocalDate.parse(fecha.getText()));
                dao.update(p);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al editar proveedor");
            }
        }
    }

    private void eliminarProveedor() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor para eliminar");
            return;
        }
        Provider p = modelo.getAt(row);
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar " + p.getName() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            dao.delete(p.getId());
            recargar();
        }
    }

    private void registroRapido() {
        JTextField nombre = new JTextField();
        JTextField fono = new JTextField();

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Nombre:"));
        p.add(nombre);
        p.add(new JLabel("Teléfono:"));
        p.add(fono);

        int res = JOptionPane.showConfirmDialog(this, p, "Registro rápido de proveedor", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Provider prov = new Provider(
                        dao.nextId(),
                        nombre.getText(),
                        fono.getText(),
                        "", "", LocalDate.now()
                );
                dao.insert(prov);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error en los datos ingresados");
            }
        }
    }

    // ===== Modelo de tabla =====
    private static class ProveedoresModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Nombre", "Teléfono", "Email", "Dirección", "Creado"};
        private List<Provider> data = List.of();

        public void set(List<Provider> rows) {
            data = rows;
            fireTableDataChanged();
        }

        public Provider getAt(int row) {
            return data.get(row);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Object getValueAt(int r, int c) {
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

