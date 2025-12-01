package pos.ui.views; // Vista del panel de inventario

import pos.services.InventoryService;
import pos.dao.MovementDao; // si lo tienes

import pos.model.Product; // Modelo del producto

import javax.swing.*; // Componentes UI
import javax.swing.table.AbstractTableModel; // Modelo tabla base
import java.awt.*; // Layouts / estilos
import java.time.LocalDate; // Fecha simple
import java.time.LocalDateTime; // Fecha + hora para movimientos
import java.time.format.DateTimeFormatter; // Formateo de fecha
import java.util.List; // Lista estándar
import java.util.Objects; // Validaciones
import java.util.stream.Collectors; // Para filtrar listas

/**
 * Panel principal de gestión de inventario.
 * Sin dependencias de InventoryMovement.
 */
public class InventarioPanel extends JPanel { // Panel de inventario

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // Formato fecha vencimiento

    // Barra superior
    private final JTextField txtBuscar = new JTextField(18); // Input de búsqueda
    private final JButton btnBuscar    = new JButton("Buscar"); // Botón buscar
    private final JButton btnEntrada   = new JButton("Entrada (+)"); // Entrada de stock
    private final JButton btnSalida    = new JButton("Salida (−)"); // Salida de stock
    private final JButton btnAjuste    = new JButton("Ajuste"); // Ajuste completo
    private final JButton btnHistorial = new JButton("Historial"); // Historial movimientos
    private final JButton btnEliminar  = new JButton("Eliminar"); // Eliminar producto

    // Tabla
    private final JTable tabla = new JTable(new ProductosModel()); // Tabla productos
    private final ProductosModel modelo = (ProductosModel) tabla.getModel(); // Modelo casteado

 

    // DAO movimientos local
    private final MovementDao movementDao = new MovementDao();

    // API nueva
    private final InventoryService inventoryService = new InventoryService();

    private final String currentUser = "admin";

    public InventarioPanel() { // Constructor
        setLayout(new BorderLayout(8,8)); // Layout general

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); // Barra superior
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre o código…"); // Placeholder
        top.add(txtBuscar); // Input
        top.add(btnBuscar); // Botón buscar
        top.add(btnEntrada); // Botón entrada
        top.add(btnSalida); // Botón salida
        top.add(btnAjuste); // Botón ajuste
        top.add(btnHistorial); // Botón historial
        top.add(btnEliminar); // Botón eliminar
        add(top, BorderLayout.NORTH); // Añadir arriba

        tabla.setRowHeight(22); // Alto filas
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Selección única
        add(new JScrollPane(tabla), BorderLayout.CENTER); // Tabla scrollable

        recargar(); // Cargar productos al inicio

        btnBuscar.addActionListener(e -> filtrar()); // Buscar
        btnEntrada.addActionListener(e -> onEntrada()); // Entrada stock
        btnSalida.addActionListener(e -> onSalida()); // Salida stock
        btnAjuste.addActionListener(e -> onAjuste()); // Ajuste producto
        btnHistorial.addActionListener(e -> openHistorial()); // Ver historial
        btnEliminar.addActionListener(e -> onDelete()); // Eliminar producto
    }

    // ================== Utilidades ==================

    private void recargar() {

        String selectedCode = null;

        int row = tabla.getSelectedRow();
        if (row >= 0 && row < modelo.data.size()) {
            selectedCode = modelo.getAt(row).getCode();
        }

        try {
            List<Product> productos = InventoryService.getAll();
            modelo.set(productos);

            // Restaurar selección
            if (selectedCode != null) {
                for (int i = 0; i < modelo.data.size(); i++) {
                    if (modelo.getAt(i).getCode().equalsIgnoreCase(selectedCode)) {
                        tabla.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error cargando inventario: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    private void filtrar() {
        String q = txtBuscar.getText().trim().toLowerCase();

        if (q.isEmpty()) {
            recargar();
            return;
        }

        List<Product> base = modelo.data; // usar datos existentes de la API

        List<Product> filtrado = base.stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                          || (p.getCode() != null && p.getCode().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        modelo.set(filtrado);
    }


    private Product getSeleccionado() { // Obtener producto seleccionado
        int row = tabla.getSelectedRow(); // Fila
        if (row < 0) return null; // Nada seleccionado
        return modelo.getAt(row); // Obtener producto
    }

    private Product findByCode(String code) {

        if (code == null || code.isBlank()) return null;

        try {
            // Siempre buscar primero en API
            Product p = inventoryService.getByCode(code);
            if (p != null) return p;
        } catch (Exception ignored) {}

        // Si falla la API, intenta desde la tabla local
        for (Product p : modelo.data) {
            if (p.getCode().equalsIgnoreCase(code)) return p;
        }

        return null;
    }



    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Atención", JOptionPane.WARNING_MESSAGE); } // Alerta
    private void info(String msg) { JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE); } // Info

    private JPanel form2(String l1, JComponent c1, String l2, JComponent c2) { // Crea formulario en dos columnas
        JPanel p = new JPanel(new GridLayout(0,2,6,6)); // Grid 2 columnas
        p.add(new JLabel(l1)); p.add(c1); // Fila 1
        p.add(new JLabel(l2)); p.add(c2); // Fila 2
        return p;
    }

    // ================== Operaciones ==================

    private void onEntrada() {

        JTextField txtCode  = new JTextField();
        JTextField txtName  = new JTextField();
        JTextField txtCat   = new JTextField("General");
        JTextField txtPrice = new JTextField("0");
        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField txtReason= new JTextField("Entrada de stock");

        JPanel wrap = new JPanel(new GridLayout(0,1,4,4));
        wrap.add(form2("Código:", txtCode, "Nombre:", txtName));
        wrap.add(form2("Categoría:", txtCat, "Precio:", txtPrice));
        wrap.add(form2("Cantidad (+):", spQty, "Motivo:", txtReason));

        int ok = JOptionPane.showConfirmDialog(this, wrap, "Entrada de producto", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String code = txtCode.getText().trim();
        if (code.isEmpty()) { warn("El código es obligatorio."); return; }

        Product prod = findByCode(code);
        int qty = (Integer) spQty.getValue();

        // ===============================
        // 🟢 PRODUCTO NUEVO → CREAR
        // ===============================
        if (prod == null) {

            String name = txtName.getText().trim();
            if (name.isEmpty()) { warn("El nombre es obligatorio."); return; }

            int price;
            try { price = Integer.parseInt(txtPrice.getText().trim()); }
            catch (Exception e) { warn("Precio inválido."); return; }

            prod = new Product(
                    0,
                    code,
                    name,
                    txtCat.getText().trim(),
                    price,
                    qty,
                    null
            );
            prod.setActive(true);

            try {
                inventoryService.createProduct(prod);

                // Consultar ID real
                Product apiProd = inventoryService.getByCode(code);
                if (apiProd != null) prod.setId(apiProd.getId());

            } catch (Exception ex) {
                warn("Error API al crear producto: " + ex.getMessage());
                return;
            }
        }

        // ===============================
        // 🔵 PRODUCTO EXISTENTE → SUMAR STOCK
        // ===============================
        else {
            prod.setStock(prod.getStock() + qty);

            try {
                inventoryService.updateProduct(prod);
            } catch (Exception ex) {
                warn("Error API al sumar stock: " + ex.getMessage());
                return;
            }
        }

        info("Entrada aplicada correctamente.");
        recargar();
    }

    private void onSalida() {

        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField txtReason = new JTextField("Salida de stock");

        JPanel form = form2("Cantidad (–):", spQty, "Motivo:", txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Salida de stock", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        int qty = (Integer) spQty.getValue();

        if (p.getStock() < qty) {
            warn("No puedes retirar más del stock disponible.\nStock actual: " + p.getStock());
            return;
        }

        p.setStock(p.getStock() - qty);

        try {
            inventoryService.updateProduct(p);
        } catch (Exception ex) {
            warn("Error API al restar stock: " + ex.getMessage());
            return;
        }

        info("Salida aplicada correctamente.");
        recargar();
    }

    private void onAjuste() {

        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JTextField txtCode = new JTextField(p.getCode());
        JTextField txtName = new JTextField(p.getName());
        JTextField txtCat  = new JTextField(p.getCategory());
        JSpinner spPrice   = new JSpinner(new SpinnerNumberModel(p.getPrice(), 0, 1_000_000_000, 1));
        JSpinner spStock   = new JSpinner(new SpinnerNumberModel(p.getStock(), 0, 1_000_000, 1));
        JTextField txtReason = new JTextField("Ajuste / edición manual");

        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Código:")); form.add(txtCode);
        form.add(new JLabel("Nombre:")); form.add(txtName);
        form.add(new JLabel("Categoría/Descripción:")); form.add(txtCat);
        form.add(new JLabel("Precio venta:")); form.add(spPrice);
        form.add(new JLabel("Nuevo stock:")); form.add(spStock);
        form.add(new JLabel("Motivo:")); form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Ajuste de producto", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        p.setCode(txtCode.getText().trim());
        p.setName(txtName.getText().trim());
        p.setCategory(txtCat.getText().trim());
        p.setPrice((Integer) spPrice.getValue());
        p.setStock((Integer) spStock.getValue());

        try {
            inventoryService.updateProduct(p);
        } catch (Exception ex) {
            warn("Error API al actualizar producto: " + ex.getMessage());
            return;
        }

        info("✔ Ajuste aplicado correctamente.");
        recargar();
    }

    private void openHistorial() {
        JFrame f = new JFrame("Historial de Movimientos");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setSize(900, 600);
        f.setLocationRelativeTo(this);
        f.setContentPane(new MovementsPanel()); 
        f.setVisible(true);
    }
    private void onDelete() {

        Product p = getSeleccionado();
        if (p == null) {
            warn("Selecciona un producto.");
            return;
        }

        int r = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el producto \"" + p.getName() + "\"?\n" +
                "Código: " + p.getCode() +
                "\n\n⚠ Esta acción no se puede deshacer.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );

        if (r != JOptionPane.YES_OPTION) return;

        // =====================================
        // 🔵 1) Llamar API para eliminar
        // =====================================
        try {
            inventoryService.deleteProduct(p.getId());
        } catch (Exception ex) {
            warn("❌ Error API al eliminar producto: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        // =====================================
        // 📌 2) Registrar movimiento local DELETE
        // =====================================
        movementDao.insert(
                p.getCode(),
                "DELETE",
                p.getStock(),
                p.getStock(),
                0,
                "Eliminación de producto",
                currentUser,
                LocalDateTime.now()
        );

        info("✔ Producto eliminado correctamente.");
        recargar(); // recargar inventario desde API
    }

    // ================== Modelo de tabla ==================

    private static class ProductosModel extends AbstractTableModel { // Modelo tabla principal
        private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock", "Estado", "Vence"};
        private List<Product> data = List.of();

        public void set(List<Product> rows) { data = Objects.requireNonNullElse(rows, List.of()); fireTableDataChanged(); } // Cargar datos
        public Product getAt(int row) { return data.get(row); } // Obtener producto

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode(); // Código
                case 1 -> p.getName(); // Nombre
                case 2 -> p.getCategory(); // Categoría
                case 3 -> "$" + p.getPrice(); // Precio
                case 4 -> p.getStock(); // Stock
                case 5 -> p.getStock() > 0 ? "OK" : "SIN STOCK"; // Estado visual
                case 6 -> (p.getExpiry() == null) ? "-" : DF.format(p.getExpiry()); // Fecha vencimiento
                default -> ""; 
            };
        }
    }
}