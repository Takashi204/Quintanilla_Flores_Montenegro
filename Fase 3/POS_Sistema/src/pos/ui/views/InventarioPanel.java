package pos.ui.views; // Vista del panel de inventario

import pos.dao.InventoryDao; // DAO para productos
import pos.dao.MovementDao; // DAO para registrar movimientos de stock
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

    // DAOs
    private final InventoryDao dao = new InventoryDao(); // DAO productos
    private final MovementDao movementDao = new MovementDao(); // DAO movimientos

    private final String currentUser = "admin"; // Usuario actual (placeholder)

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

    private void recargar() { // Recargar tabla completa
        modelo.set(dao.listAll());
    }

    private void filtrar() { // Filtro por texto
        String q = txtBuscar.getText().trim().toLowerCase(); // Texto query
        if (q.isEmpty()) { recargar(); return; } // Si vacío → reset
        List<Product> base = dao.listAll(); // Obtener todos
        modelo.set(base.stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q)) // Coincidencia nombre
                        || (p.getCode() != null && p.getCode().toLowerCase().contains(q))) // Coincidencia código
                .collect(Collectors.toList())); // Filtrar
    }

    private Product getSeleccionado() { // Obtener producto seleccionado
        int row = tabla.getSelectedRow(); // Fila
        if (row < 0) return null; // Nada seleccionado
        return modelo.getAt(row); // Obtener producto
    }

    private Product findByCode(String code) { // Buscar por código
        return dao.findByCode(code);
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

    private void onEntrada() { // Entrada de stock
        JTextField txtCode  = new JTextField(); // Código
        JTextField txtName  = new JTextField(); // Nombre
        JTextField txtCat   = new JTextField("General"); // Categoría
        JTextField txtPrice = new JTextField("0"); // Precio
        JTextField txtExp   = new JTextField(""); // Fecha vencimiento
        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1)); // Cantidad
        JTextField txtReason= new JTextField("Entrada de stock"); // Motivo

        JPanel wrap = new JPanel(new GridLayout(0,1,4,4)); // Contenedor
        wrap.add(form2("Código:", txtCode, "Nombre:", txtName)); // Línea
        wrap.add(form2("Categoría:", txtCat, "Precio:", txtPrice));
        wrap.add(form2("Vencimiento (AAAA-MM-DD):", txtExp, "Cantidad (+):", spQty));
        wrap.add(form2("Motivo:", txtReason, "", new JLabel()));

        int ok = JOptionPane.showConfirmDialog(this, wrap, "Entrada de producto", JOptionPane.OK_CANCEL_OPTION); // Mostrar modal
        if (ok != JOptionPane.OK_OPTION) return; // Cancelado

        String code = txtCode.getText().trim();
        if (code.isEmpty()) { warn("El código es obligatorio."); return; }

        Product prod = findByCode(code); // Buscar existente
        int qty = (Integer) spQty.getValue(); // Cantidad

        if (prod == null) { // Producto nuevo
            String name = txtName.getText().trim();
            if (name.isEmpty()) { warn("El nombre es obligatorio."); return; }
            String cat = txtCat.getText().trim();
            int price;
            try { price = Integer.parseInt(txtPrice.getText().trim()); }
            catch (Exception e) { warn("Precio inválido."); return; }
            LocalDate exp = null;
            if (!txtExp.getText().trim().isEmpty()) { // Si ingresó vencimiento
                try { exp = LocalDate.parse(txtExp.getText().trim()); }
                catch (Exception e) { warn("Fecha inválida."); return; }
            }
            prod = new Product(code, name, cat, price, qty, exp); // Crear producto
            dao.insert(prod); // Insertar nuevo
        } else { // Producto existente
            int prev = prod.getStock(); // Stock previo
            prod.setStock(prev + qty); // Sumar stock
            dao.update(prod); // Actualizar BD
            movementDao.insert(code, "ENTRY", qty, prev, prod.getStock(), // Registrar movimiento
                    txtReason.getText(), currentUser, LocalDateTime.now());
        }

        info("Entrada aplicada a " + prod.getName() + ". Nuevo stock: " + prod.getStock()); // Mensaje
        recargar(); // Actualizar tabla
    }

    private void onSalida() { // Salida de stock
        Product p = getSeleccionado(); // Seleccionado
        if (p == null) { warn("Selecciona un producto."); return; }

        JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1)); // Cantidad
        JTextField txtReason = new JTextField("Salida manual"); // Motivo
        JPanel form = form2("Cantidad (−):", spQty, "Motivo:", txtReason); // Formulario

        int ok = JOptionPane.showConfirmDialog(this, form, "Salida de stock", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        int qty = (Integer) spQty.getValue();
        int prev = p.getStock();
        int newStock = Math.max(0, prev - qty); // No bajar de 0
        p.setStock(newStock);

        movementDao.insert(p.getCode(), "EXIT", qty, prev, newStock, // Movimiento
                txtReason.getText(), currentUser, LocalDateTime.now());
        dao.update(p); // Guardar cambios

        info("Salida aplicada. Stock: " + prev + " → " + newStock);
        recargar();
    }

    private void onAjuste() { // Ajuste general
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JTextField txtName = new JTextField(p.getName());
        JTextField txtCat  = new JTextField(p.getCategory());
        JSpinner spPrice   = new JSpinner(new SpinnerNumberModel(p.getPrice(), 0, 1_000_000_000, 1));
        JTextField txtExp  = new JTextField(p.getExpiry() == null ? "" : p.getExpiry().toString());
        JSpinner spStock   = new JSpinner(new SpinnerNumberModel(p.getStock(), 0, 1_000_000, 1));
        JTextField txtReason = new JTextField("Ajuste / edición manual");

        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Código:")); form.add(new JLabel(p.getCode()));
        form.add(new JLabel("Nombre:")); form.add(txtName);
        form.add(new JLabel("Categoría:")); form.add(txtCat);
        form.add(new JLabel("Precio:")); form.add(spPrice);
        form.add(new JLabel("Vencimiento (AAAA-MM-DD):")); form.add(txtExp);
        form.add(new JLabel("Nuevo stock:")); form.add(spStock);
        form.add(new JLabel("Motivo:")); form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Ajuste de producto", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        p.setName(txtName.getText().trim());
        p.setCategory(txtCat.getText().trim());
        p.setPrice((Integer) spPrice.getValue());
        if (!txtExp.getText().trim().isEmpty()) {
            try { p.setExpiry(LocalDate.parse(txtExp.getText().trim())); }
            catch (Exception e) { warn("Fecha inválida."); return; }
        }

        int prev = p.getStock();
        int newStock = (Integer) spStock.getValue();
        p.setStock(newStock);

        if (newStock != prev) {
            movementDao.insert(p.getCode(), "ADJUST", Math.abs(newStock - prev),
                    prev, newStock, txtReason.getText(), currentUser, LocalDateTime.now());
        }
        dao.update(p);

        info("Ajuste aplicado. Stock: " + prev + " → " + newStock);
        recargar();
    }

    private void onDelete() { // Eliminar producto
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        String extra = p.getStock() > 0
                ? "\n\n⚠️ Se registrará un movimiento DELETE y stock 0."
                : "";
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el producto \"" + p.getName() + "\" (" + p.getCode() + ")?"
                        + "\nEsta acción no se puede deshacer." + extra,
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        movementDao.insert(p.getCode(), "DELETE", p.getStock(), p.getStock(), 0,
                "Eliminación de producto", currentUser, LocalDateTime.now());
        dao.delete(p.getCode());

        info("Producto eliminado correctamente.");
        recargar();
    }

    private void openHistorial() { // Abrir ventana historial
        String preset = "";
        Product sel = getSeleccionado();
        if (sel != null) preset = sel.getCode();

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Historial de Movimientos", Dialog.ModalityType.MODELESS);
        d.setContentPane(new MovementsPanel(preset));
        d.setSize(900, 500);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
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

