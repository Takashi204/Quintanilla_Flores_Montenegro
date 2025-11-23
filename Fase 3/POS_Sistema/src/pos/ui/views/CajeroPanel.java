package pos.ui.views; // Panel del cajero dentro del paquete de vistas

import pos.dao.InventoryDao; // Acceso a productos (CRUD inventario)
import pos.dao.MovementDao; // Registro de movimientos (entradas/salidas)
import pos.dao.CashSessionDao; // Manejo de sesiones de caja
import pos.model.CashSession; // Modelo de sesión de caja
import pos.model.Product; // Modelo de producto
import pos.model.SaleItem; // Modelo de ítem de venta (poco usado en este panel)
import pos.ui.dialogs.CheckoutDialog; // Modal de cobro
import pos.util.DataSync; // Eventos de sincronización (actualiza inventario en vivo)
import pos.util.TicketPrinter; // (no usado aún) impresión de tickets

import javax.swing.*; // Swing
import javax.swing.table.AbstractTableModel; // Para los modelos de tabla
import java.awt.*; // Layouts
import java.awt.event.*; // Eventos
import java.math.BigDecimal; // Para enviar totales al CheckoutDialog
import java.text.NumberFormat; // Formato CLP
import java.time.LocalDate; // Fecha venta
import java.time.LocalDateTime; // Fecha para movimientos
import java.util.ArrayList; // Listas
import java.util.List;
import java.util.Locale; // Local CL para formato moneda

public class CajeroPanel extends JPanel { // Panel principal del módulo cajero

    private final JTextField txtBuscarCodigo = new JTextField(14); // Campo buscar por código
    private final JTextField txtBuscarNombre = new JTextField(16); // Campo buscar por nombre
    private final JTable tblInv = new JTable(new InvModel()); // Tabla del inventario
    private final ItemsModel itemsModel = new ItemsModel(); // Modelo para el carrito
    private final JTable tblCarrito = new JTable(itemsModel); // Tabla del carrito
    private final JLabel lblTotal = new JLabel("$0"); // Total de la venta

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL")); // Formato CLP
    private boolean cajaAbierta = false; // Estado de la caja
    private CashSession sesionActual; // Información de la sesión actual

    private final String currentUser; // Usuario que está usando el POS
    private final InventoryDao inventoryDao = new InventoryDao(); // DAO inventario
    private final MovementDao movementDao = new MovementDao(); // DAO movimientos (historial)
    private final CashSessionDao cashDao = new CashSessionDao(); // DAO sesiones de caja

    public CajeroPanel(String currentUser) { // Constructor principal
        this.currentUser = currentUser; // Guarda nombre usuario

        setLayout(new BorderLayout(10, 10)); // Layout general
        setBackground(new Color(0xF3F4F6)); // Fondo gris moderno

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // Divide inventario | carrito
        split.setResizeWeight(0.5); // 50% cada lado
        split.setBorder(null);
        split.setLeftComponent(buildLeft()); // Panel inventario
        split.setRightComponent(buildRight()); // Panel carrito
        add(split, BorderLayout.CENTER);

        ((InvModel) tblInv.getModel()).reload(); // Carga inicial del inventario

        tblInv.addMouseListener(new MouseAdapter() { // Doble click = agregar carrito
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && cajaAbierta) agregarProductoSeleccionado();
            }
        });

        txtBuscarCodigo.addActionListener(e -> { // Enter en código = agregar
            if (cajaAbierta) agregarPorCodigo(txtBuscarCodigo.getText().trim());
        });

        registerShortcuts(); // Atajos F1 / F2 / F9
        verificarSesionAbierta(); // Ver si la caja ya estaba abierta previamente

        DataSync.addListener("inventory", () -> SwingUtilities.invokeLater(() -> {
            ((InvModel) tblInv.getModel()).reload(); // Refresca inventario cuando cambian cosas
        }));
    }

    private void registerShortcuts() { // Registra teclas rápidas
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); // Escucha global
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "abrirCaja"); // Abrir caja
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "cerrarCaja"); // Cerrar caja
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "cobrar"); // Cobrar venta

        am.put("abrirCaja", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { abrirCaja(); }
        });

        am.put("cerrarCaja", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { cerrarCaja(); }
        });

        am.put("cobrar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (cajaAbierta) abrirModalCobro();
                else JOptionPane.showMessageDialog(CajeroPanel.this, "Debe abrir la caja antes de cobrar.");
            }
        });
    }

    private JComponent buildLeft() { // Panel izquierdo: inventario
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)); // Buscadores
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodos = new JButton("Todos");

        top.add(new JLabel("Código:"));
        top.add(txtBuscarCodigo);
        top.add(new JLabel("Nombre:"));
        top.add(txtBuscarNombre);
        top.add(btnBuscar);
        top.add(btnTodos);

        p.add(top, BorderLayout.NORTH);

        tblInv.setRowHeight(24); // Estética
        p.add(new JScrollPane(tblInv), BorderLayout.CENTER);

        btnBuscar.addActionListener(e ->
                ((InvModel) tblInv.getModel()).search(txtBuscarCodigo.getText(), txtBuscarNombre.getText()));

        btnTodos.addActionListener(e ->
                ((InvModel) tblInv.getModel()).reload());

        return p;
    }

    private JComponent buildRight() { // Panel derecho: carrito + botones
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnNuevo = new JButton("Nuevo"); // Limpia carrito
        JButton btnAbrir = new JButton("Abrir Caja (F1)");
        JButton btnCerrar = new JButton("Cerrar Caja (F2)");
        top.add(btnNuevo);
        top.add(btnAbrir);
        top.add(btnCerrar);
        p.add(top, BorderLayout.NORTH);

        tblCarrito.setRowHeight(26); // Altura de filas
        p.add(new JScrollPane(tblCarrito), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        JPanel tot = new JPanel(new GridLayout(0, 2, 8, 4));
        tot.setBorder(BorderFactory.createTitledBorder("Totales"));
        tot.add(new JLabel("TOTAL:"));
        tot.add(lblTotal);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnQuitar = new JButton("Quitar ítem"); // Eliminar ítem
        JButton btnCobrar = new JButton("Cobrar (F9)");
        acciones.add(btnQuitar);
        acciones.add(btnCobrar);

        bottom.add(tot, BorderLayout.NORTH);
        bottom.add(acciones, BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> limpiarCarrito());
        btnQuitar.addActionListener(e -> quitarItem());
        btnCobrar.addActionListener(e -> abrirModalCobro());
        btnAbrir.addActionListener(e -> abrirCaja());
        btnCerrar.addActionListener(e -> cerrarCaja());

        return p;
    }

    private void verificarSesionAbierta() { // Revisa si la caja estaba abierta
        sesionActual = cashDao.getOpenSession(currentUser);
        cajaAbierta = (sesionActual != null);
    }

    private void abrirCaja() { // Abre la caja
        if (cajaAbierta) {
            JOptionPane.showMessageDialog(this, "La caja ya está abierta.");
            return;
        }

        cashDao.openSession(currentUser, 0); // Abre sesión
        sesionActual = cashDao.getOpenSession(currentUser);

        if (sesionActual == null) {
            JOptionPane.showMessageDialog(this, "Error al abrir la caja.");
            return;
        }

        cajaAbierta = true;
        JOptionPane.showMessageDialog(this, "Caja abierta correctamente.");
    }

    private void cerrarCaja() { // Cierra la caja
        if (!cajaAbierta || sesionActual == null) {
            JOptionPane.showMessageDialog(this, "No hay caja abierta.");
            return;
        }

        int totalVentas = cashDao.calcularTotalVentasDelDia(currentUser); // Suma de ventas

        cashDao.closeSession(currentUser, totalVentas); // Cierra sesión

        cajaAbierta = false;
        sesionActual = null;

        JOptionPane.showMessageDialog(this, "Caja cerrada correctamente.");
    }

    private void limpiarCarrito() { // Limpia carrito
        itemsModel.data.clear();
        itemsModel.fireTableDataChanged();
        lblTotal.setText("$0");
    }

    private void quitarItem() { // Quitar ítem
        int r = tblCarrito.getSelectedRow();
        if (r >= 0) {
            itemsModel.data.remove(r);
            itemsModel.fireTableDataChanged();
            actualizarTotal();
        }
    }

    private void actualizarTotal() { // Recalcula total
        lblTotal.setText(CLP.format(itemsModel.total()));
    }

    private void agregarPorCodigo(String code) { // Agregar escribiendo código
        if (!cajaAbierta) return;

        Product p = inventoryDao.findByCode(code);
        if (p != null) {
            if (p.getStock() <= 0) {
                JOptionPane.showMessageDialog(this, "Sin stock.");
                return;
            }
            itemsModel.addOrInc(p, 1);
            actualizarTotal();
            txtBuscarCodigo.setText("");
        }
    }

    private void agregarProductoSeleccionado() { // Agregar con doble click
        if (!cajaAbierta) return;

        int r = tblInv.getSelectedRow();
        if (r < 0) return;

        Product p = ((InvModel) tblInv.getModel()).getAt(tblInv.convertRowIndexToModel(r));

        if (p.getStock() <= 0) {
            JOptionPane.showMessageDialog(this, "Sin stock.");
            return;
        }

        itemsModel.addOrInc(p, 1);
        actualizarTotal();
    }

    private void abrirModalCobro() { // Abre el CheckoutDialog
        if (!cajaAbierta) {
            JOptionPane.showMessageDialog(this, "Debe abrir la caja.");
            return;
        }

        if (itemsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Carrito vacío.");
            return;
        }

        int total = itemsModel.total();

        CheckoutDialog dlg = new CheckoutDialog(
                SwingUtilities.getWindowAncestor(this),
                new BigDecimal(total),
                BigDecimal.ZERO // IVA en 0 (no configurado)
        );

        dlg.setVisible(true);
        CheckoutDialog.Result r = dlg.getResult();

        if (r == null) return; // Cancelado

        cobrarVenta(total, r.paymentMethod);
    }

    private void cobrarVenta(int total, String metodoPago) { // Graba la venta en BD
        try (var cn = pos.db.Database.get();
             var ps = cn.prepareStatement(
                     "INSERT INTO sales (code, name, quantity, price, total, fecha, user, metodo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

            for (Item it : itemsModel.data) {

                int nuevoStock = it.product.getStock() - it.qty;
                inventoryDao.updateStock(it.product.getCode(), nuevoStock); // Actualizar stock

                movementDao.insert( // Registrar movimiento
                        it.product.getCode(),
                        "VENTA",
                        it.qty,
                        it.product.getStock(),
                        nuevoStock,
                        "Venta realizada",
                        currentUser,
                        LocalDateTime.now()
                );

                ps.setString(1, it.product.getCode());
                ps.setString(2, it.product.getName());
                ps.setInt(3, it.qty);
                ps.setInt(4, it.product.getPrice());
                ps.setInt(5, it.product.getPrice() * it.qty);
                ps.setString(6, LocalDate.now().toString());
                ps.setString(7, currentUser);
                ps.setString(8, metodoPago);

                ps.addBatch();
            }

            ps.executeBatch(); // Ejecuta todas las ventas

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this, "Venta realizada correctamente.");
        limpiarCarrito();
    }

    // ------------------- MODELOS DE TABLA -------------------

    private class InvModel extends AbstractTableModel { // Inventario
        private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock"};
        private List<Product> data = new ArrayList<>();

        void reload() { data = new ArrayList<>(inventoryDao.listAll()); fireTableDataChanged(); }

        void search(String c, String n) { // Búsqueda
            List<Product> base = inventoryDao.listAll();
            data.clear();

            for (Product p : base) {
                boolean ok = true;
                if (c != null && !c.isBlank())
                    ok &= p.getCode().toLowerCase().contains(c.toLowerCase());
                if (n != null && !n.isBlank())
                    ok &= p.getName().toLowerCase().contains(n.toLowerCase());
                if (ok) data.add(p);
            }
            fireTableDataChanged();
        }

        Product getAt(int r) { return data.get(r); }
        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }

        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getCategory();
                case 3 -> p.getPrice();
                case 4 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class Item { // Ítem del carrito
        final Product product; int qty;
        Item(Product p, int q) { product = p; qty = q; }
    }

    private static class ItemsModel extends AbstractTableModel { // Modelo del carrito
        private final String[] cols = {"Código", "Nombre", "Precio", "Cantidad", "Subtotal"};
        private final List<Item> data = new ArrayList<>();

        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }

        public Object getValueAt(int r, int c) {
            Item it = data.get(r);
            return switch (c) {
                case 0 -> it.product.getCode();
                case 1 -> it.product.getName();
                case 2 -> it.product.getPrice();
                case 3 -> it.qty;
                case 4 -> it.product.getPrice() * it.qty;
                default -> "";
            };
        }

        public boolean isCellEditable(int r, int c) { return c == 3; } // Solo editar cantidad

        public void setValueAt(Object v, int r, int c) { // Editar cantidad
            if (c == 3) {
                try {
                    int q = Math.max(1, Integer.parseInt(v.toString().trim()));
                    if (q > data.get(r).product.getStock()) {
                        JOptionPane.showMessageDialog(null, "Stock insuficiente.");
                        return;
                    }
                    data.get(r).qty = q;
                    fireTableRowsUpdated(r, r);
                } catch (Exception ignored) {}
            }
        }

        void addOrInc(Product p, int q) { // Añadir o sumar ítem
            for (Item it : data) {
                if (it.product.getCode().equals(p.getCode())) {
                    if (it.qty + q > p.getStock()) {
                        JOptionPane.showMessageDialog(null, "Stock insuficiente.");
                        return;
                    }
                    it.qty += q;
                    fireTableDataChanged();
                    return;
                }
            }
            data.add(new Item(p, q));
            fireTableDataChanged();
        }

        int total() { // Total del carrito
            int sum = 0;
            for (Item it : data) sum += it.product.getPrice() * it.qty;
            return sum;
        }
    }
}
