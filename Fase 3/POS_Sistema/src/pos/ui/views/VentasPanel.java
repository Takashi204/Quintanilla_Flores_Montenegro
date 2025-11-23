package pos.ui.views; // Panel de ventas dentro del módulo UI

import pos.dao.InventoryDao; // Acceso BD productos
import pos.dao.VentasDao; // Acceso BD ventas
import pos.model.Product; // Modelo producto
import pos.model.Sale; // Modelo venta
import pos.model.SaleItem; // Modelo ítem venta

import javax.swing.*; // Swing UI
import javax.swing.table.AbstractTableModel; // Modelo tabla
import java.awt.*; // Layouts
import java.awt.event.*; // Eventos
import java.text.NumberFormat; // Formato CLP
import java.time.LocalDateTime; // Fecha venta
import java.util.*; // HashMap, ArrayList
import java.util.List; // List
import java.util.Locale; // Locale CLP

public class VentasPanel extends JPanel { // Panel principal de ventas

    // ==== Ítem interno de línea de venta ====
    private static class SaleLine {
        private final Product product; // Producto
        private int qty; // Cantidad

        SaleLine(Product product, int qty) { this.product = product; this.qty = Math.max(1, qty); } // Construct
        public Product getProduct() { return product; } // Retorna producto
        public int getQty() { return qty; } // Retorna cantidad
        public void setQty(int q) { this.qty = Math.max(1, q); } // Set qty min 1
        public int getSubtotal() { return product.getPrice() * qty; } // Subtotal = precio * qty
    }

    // ==== Modelo tabla de ítems ====
    private static class ItemsModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Precio", "Cantidad", "Subtotal"}; // Encabezados
        private final java.util.List<SaleLine> data = new ArrayList<>(); // Lista ítems

        @Override public int getRowCount() { return data.size(); } // # filas
        @Override public int getColumnCount() { return cols.length; } // # columnas
        @Override public String getColumnName(int c) { return cols[c]; } // Nombre col
        @Override public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 2,3,4 -> Integer.class; // Precio, cantidad, subtotal son int
                default -> String.class; // Código/nombre str
            };
        }
        @Override public boolean isCellEditable(int r, int c) { return c == 3; } // Solo cantidad editable

        @Override public Object getValueAt(int r, int c) { // Valores tabla
            SaleLine it = data.get(r); // Ítem
            Product p = it.getProduct(); // Producto
            return switch (c) {
                case 0 -> p.getCode(); // Código
                case 1 -> p.getName(); // Nombre
                case 2 -> p.getPrice(); // Precio
                case 3 -> it.getQty(); // Cantidad
                case 4 -> it.getSubtotal(); // Subtotal
                default -> "";
            };
        }

        @Override public void setValueAt(Object v, int r, int c) { // Edit cantidad
            if (c == 3) {
                try {
                    int q = Integer.parseInt(String.valueOf(v)); // Parse
                    if (q < 1) q = 1; // mínimo
                    data.get(r).setQty(q); // set
                    fireTableRowsUpdated(r, r); // refresca
                } catch (Exception ignored) {}
            }
        }

        public void addOrIncrement(Product p, int qty) { // Agregar o aumentar
            for (int i = 0; i < data.size(); i++) {
                SaleLine it = data.get(i);
                if (it.getProduct().getCode().equals(p.getCode())) { it.setQty(it.getQty() + qty); fireTableRowsUpdated(i,i); return; }
            }
            data.add(new SaleLine(p, qty)); // Agrega
            fireTableRowsInserted(data.size()-1, data.size()-1); // Refresca
        }

        public void removeAt(int idx) { if (idx >= 0 && idx < data.size()) { data.remove(idx); fireTableDataChanged(); }} // Quitar
        public void clear() { data.clear(); fireTableDataChanged(); } // Limpiar
        public List<SaleLine> getItems() { return new ArrayList<>(data); } // Clonar
    }

    // ==== Campos UI ====
    private JTextField txtCodigo; // Campo código producto
    private JTable tbl; // Tabla de venta
    private final JLabel lblInfo = new JLabel("Listo."); // Mensaje estado
    private final ItemsModel itemsModel = new ItemsModel(); // Modelo

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL")); // Formato CLP

    private final Map<String, Product> inventory = new HashMap<>(); // Cache de productos

    public VentasPanel() { // Constructor
        setLayout(new BorderLayout(10,10)); // Layout principal
        setBackground(Color.WHITE); // Fondo blanco

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); // Barra sup
        txtCodigo = new JTextField(16); // Input código
        JButton btnAgregar = new JButton("➕ Agregar"); // Botón add
        JButton btnServicio = new JButton("🧾 Servicio/Precio abierto"); // Servicio manual
        JButton btnAdmin = new JButton("⚙️ Productos (CRUD)"); // Admin productos
        barra.add(new JLabel("Código:")); barra.add(txtCodigo); // Label + campo
        barra.add(btnAgregar); barra.add(btnServicio); barra.add(btnAdmin); // Botones
        add(barra, BorderLayout.NORTH); // Parte superior

        tbl = new JTable(itemsModel); // Crear tabla
        tbl.setRowHeight(26); // Alto filas
        add(new JScrollPane(tbl), BorderLayout.CENTER); // Scroll + tabla

        JPanel pie = new JPanel(new BorderLayout()); // Pie inferior
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); // Botonera derecha
        JButton btnQuitar = new JButton("🗑 Quitar ítem"); // Quitar ítem
        JButton btnCobrar = new JButton("💵 Cobrar (F9)"); // Cobrar
        btnCobrar.setMnemonic(KeyEvent.VK_F9); // Hotkey F9
        acciones.add(btnQuitar); acciones.add(btnCobrar); // Añadir botones
        pie.add(lblInfo, BorderLayout.WEST); // Texto info
        pie.add(acciones, BorderLayout.EAST); // Botones
        add(pie, BorderLayout.SOUTH); // Agregar pie

        txtCodigo.addActionListener(e -> agregarPorCodigo()); // Enter agrega
        btnAgregar.addActionListener(e -> agregarPorCodigo()); // Click agrega
        btnServicio.addActionListener(e -> agregarServicioManual()); // Servicio
        btnQuitar.addActionListener(e -> quitarSeleccion()); // Quitar ítem
        btnCobrar.addActionListener(e -> cobrar()); // Cobrar
        btnAdmin.addActionListener(e -> abrirAdminProductos()); // Abrir admin

        recargarInventario(); // Cargar inventario
    }

    private void recargarInventario() { // Cachear productos
        inventory.clear();
        for (Product p : InventoryDao.getAll()) inventory.put(p.getCode(), p); // Añadir
    }

    private void agregarPorCodigo() { // Añadir producto
        String code = txtCodigo.getText().trim(); // Leer
        if (code.isEmpty()) return; // Si vacío, nada

        Product p = inventory.get(code); // Buscar cache
        if (p == null) { p = InventoryDao.findByCode(code); if (p != null) inventory.put(p.getCode(), p); } // Buscar BD

        if (p == null) { JOptionPane.showMessageDialog(this,"Código no encontrado: " + code); info("Código no encontrado."); return; } // Error
        if (p.getStock() <= 0) { JOptionPane.showMessageDialog(this,"Sin stock para: " + p.getName()); return; } // Sin stock

        itemsModel.addOrIncrement(p,1); // Agregar
        txtCodigo.setText(""); txtCodigo.requestFocusInWindow(); // Limpiar + foco
        info("Agregado: " + p.getName()); // Info
    }

    private void agregarServicioManual() { // Servicio manual
        String desc = JOptionPane.showInputDialog(this,"Descripción del servicio:"); if (desc == null || desc.isBlank()) return;
        String precioStr = JOptionPane.showInputDialog(this,"Precio (CLP):"); if (precioStr == null || precioStr.isBlank()) return;

        int precio; try { precio = Integer.parseInt(precioStr.trim()); } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Precio inválido"); return; }

        Product temp = new Product("SERV-"+System.currentTimeMillis(),desc,"SERVICIO",precio,1); // Producto temporal
        itemsModel.addOrIncrement(temp,1); // Agregar
        info("Servicio agregado: " + desc); // Info
    }

    private void quitarSeleccion() { // Quitar ítem
        int row = tbl.getSelectedRow(); // Fila seleccionada
        if (row < 0) { JOptionPane.showMessageDialog(this,"Selecciona un ítem."); return; }
        itemsModel.removeAt(tbl.convertRowIndexToModel(row)); // Quitar
        info("Ítem eliminado."); // Info
    }

    private void cobrar() { // Procesar venta
        if (itemsModel.getRowCount() == 0) { JOptionPane.showMessageDialog(this,"No hay ítems en la venta."); return; } // Sin ítems

        List<SaleLine> items = itemsModel.getItems(); // Ítems
        int total = items.stream().mapToInt(SaleLine::getSubtotal).sum(); // Total

        String[] tipos = {"Boleta","Factura"}; // Documentos
        String tipoDoc = (String) JOptionPane.showInputDialog(this,"Tipo de documento:","Documento",JOptionPane.QUESTION_MESSAGE,null,tipos,tipos[0]);
        if (tipoDoc == null) return;

        String rut = null; if ("Factura".equals(tipoDoc)) rut = JOptionPane.showInputDialog(this,"RUT/Razón Social (opcional):"); // RUT opcional

        String[] pagos = {"Efectivo","Tarjeta","Transferencia","Mixto"}; // Medios
        String medioPago = (String) JOptionPane.showInputDialog(this,"Medio de pago:","Cobro",JOptionPane.QUESTION_MESSAGE,null,pagos,pagos[0]);
        if (medioPago == null) return;

        int ef=0,tj=0,tr=0;
        if ("Mixto".equals(medioPago)) { // Pago mixto
            try {
                ef = parseIntSafe(JOptionPane.showInputDialog(this,"Monto Efectivo:","0"));
                tj = parseIntSafe(JOptionPane.showInputDialog(this,"Monto Tarjeta:","0"));
                tr = parseIntSafe(JOptionPane.showInputDialog(this,"Monto Transferencia:","0"));
                if (ef + tj + tr != total) { JOptionPane.showMessageDialog(this,"La suma no coincide con el total."); return; }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Montos inválidos."); return; }
        }

        int neto = (int)Math.round(total/1.19); // Neto
        int iva = total - neto; // IVA

        for (SaleLine it : items) { // Actualizar stock
            Product p = it.getProduct();
            int nuevoStock = Math.max(0,p.getStock()-it.getQty());
            p.setStock(nuevoStock);
            InventoryDao.updateStock(p.getCode(), nuevoStock); // BD
        }

        List<SaleItem> saleItems = new ArrayList<>(); for (SaleLine it : items) saleItems.add(new SaleItem(it.getProduct(), it.getQty())); // Transform

        Sale sale = new Sale(String.valueOf(System.currentTimeMillis()), // ID
                tipoDoc, LocalDateTime.now(), saleItems, medioPago, ef,tj,tr, neto,iva,total, rut); // Crear venta

        VentasDao.save(sale); // Guardar
        imprimirTicket(sale); // Imprimir
        itemsModel.clear(); // Limpiar
        info("Venta completada: " + CLP.format(total)); // Info
        JOptionPane.showMessageDialog(this,"Venta registrada.\nTotal: " + CLP.format(total)); // Popup
    }

    private int parseIntSafe(String s) { return (s==null||s.isBlank()) ? 0 : Integer.parseInt(s.trim()); } // Parse seguro

    private void imprimirTicket(Sale sale) { // Construir ticket
        StringBuilder sb = new StringBuilder();
        sb.append(sale.getDocType()).append(" #").append(sale.getId()).append(" — ").append(sale.getTs()).append("\n");
        if (sale.getCustomerId()!=null) sb.append("Cliente: ").append(sale.getCustomerId()).append("\n");
        sb.append("--------------------------------\n");
        for (SaleItem it : sale.getItems()) sb.append(it.getProduct().getName()).append(" x").append(it.getQty()).append("  ").append(CLP.format(it.getSubtotal())).append("\n");
        sb.append("--------------------------------\n");
        sb.append("Neto: ").append(CLP.format(sale.getNeto())).append("\n");
        sb.append("IVA: ").append(CLP.format(sale.getIva())).append("\n");
        sb.append("TOTAL: ").append(CLP.format(sale.getTotal())).append("\n");
        sb.append("Pago: ").append(sale.getPaymentMethod()).append("\n");

        JTextArea ta = new JTextArea(sb.toString()); // Área impresión
        ta.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        try { ta.print(); } catch (Exception ignored) {} // Enviar a impresora
    }

    private void abrirAdminProductos() { // Abrir CRUD productos
        new ProductAdminDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
        recargarInventario(); // Recargar cache
    }

    private void info(String msg) { lblInfo.setText(msg); } // Actualiza label

    // ==== CRUD productos ====
    private static class ProductAdminDialog extends JDialog {
        private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL")); // CLP format
        private java.util.List<Product> productos = new ArrayList<>(); // Lista productos
        private final ProductModel model = new ProductModel(); // Modelo

        ProductAdminDialog(Window owner) {
            super(owner,"Productos — Admin",ModalityType.APPLICATION_MODAL); // Dialog modal
            productos = InventoryDao.getAll(); // Cargar BD
            model.set(productos); // Set modelo
            buildUI(); // Contruir UI
            setSize(740,440); // Tamaño
            setLocationRelativeTo(owner); // Center
        }

        private void buildUI() { // Construir UI
            setLayout(new BorderLayout(8,8)); // Layout
            JTable table = new JTable(model); table.setRowHeight(24); // Tabla
            add(new JScrollPane(table), BorderLayout.CENTER); // Scroll

            JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); // Botonera
            JButton btnAdd = new JButton("➕ Agregar"); // Agregar
            JButton btnEdit = new JButton("✏️ Editar"); // Editar
            JButton btnDel = new JButton("🗑 Eliminar"); // Eliminar
            JButton btnClose = new JButton("Cerrar"); // Cerrar
            acciones.add(btnAdd); acciones.add(btnEdit); acciones.add(btnDel); acciones.add(btnClose); // Botones
            add(acciones, BorderLayout.SOUTH); // Pie

            btnAdd.addActionListener(e -> agregar()); // Evento agregar
            btnEdit.addActionListener(e -> editar(table)); // Editar
            btnDel.addActionListener(e -> eliminar(table)); // Eliminar
            btnClose.addActionListener(e -> dispose()); // Cerrar
        }

        private void agregar() { // Agregar producto
            JTextField code = new JTextField(); JTextField name = new JTextField(); JTextField cat = new JTextField();
            JTextField price = new JTextField(); JTextField stock = new JTextField(); // Inputs

            JPanel p = new JPanel(new GridLayout(0,2,6,6)); // Form
            p.add(new JLabel("Código:")); p.add(code);
            p.add(new JLabel("Nombre:")); p.add(name);
            p.add(new JLabel("Categoría:")); p.add(cat);
            p.add(new JLabel("Precio:")); p.add(price);
            p.add(new JLabel("Stock:")); p.add(stock);

            if (JOptionPane.showConfirmDialog(this,p,"Nuevo producto",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return; // Cancelado

            try {
                Product prod = new Product(code.getText(),name.getText(),cat.getText(),Integer.parseInt(price.getText()),Integer.parseInt(stock.getText())); // Crear
                InventoryDao.insert(prod); model.set(InventoryDao.getAll()); // Guardar + refrescar
            } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        }

        private void editar(JTable table) { // Editar producto
            int row = table.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this,"Selecciona un producto."); return; }
            Product p = model.getAt(row); // Obtener producto

            JTextField name = new JTextField(p.getName()); JTextField cat = new JTextField(p.getCategory());
            JTextField price = new JTextField(String.valueOf(p.getPrice())); JTextField stock = new JTextField(String.valueOf(p.getStock()));

            JPanel form = new JPanel(new GridLayout(0,2,6,6)); // Form edit
            form.add(new JLabel("Código:")); form.add(new JLabel(p.getCode()));
            form.add(new JLabel("Nombre:")); form.add(name);
            form.add(new JLabel("Categoría:")); form.add(cat);
            form.add(new JLabel("Precio:")); form.add(price);
            form.add(new JLabel("Stock:")); form.add(stock);

            if (JOptionPane.showConfirmDialog(this,form,"Editar producto",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;

            try {
                p.setName(name.getText()); p.setCategory(cat.getText()); p.setPrice(Integer.parseInt(price.getText())); p.setStock(Integer.parseInt(stock.getText()));
                InventoryDao.update(p); model.set(InventoryDao.getAll()); // Guardar y actualizar
            } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Error al actualizar."); }
        }

        private void eliminar(JTable table) { // Eliminar producto
            int row = table.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this,"Selecciona un producto."); return; }
            Product p = model.getAt(row); // Seleccionado
            if (JOptionPane.showConfirmDialog(this,"¿Eliminar "+p.getName()+"?","Confirmar",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                InventoryDao.delete(p.getCode()); model.set(InventoryDao.getAll()); // Borrar y refrescar
            }
        }

        private static class ProductModel extends AbstractTableModel { // Modelo CRUD tabla
            private final String[] cols = {"Código","Nombre","Categoría","Precio","Stock"}; // Encabezados
            private List<Product> data = new ArrayList<>(); // Lista interna

            void set(List<Product> rows){ data = rows; fireTableDataChanged(); } // Refrescar
            Product getAt(int r){ return data.get(r); } // Obtener fila

            @Override public int getRowCount(){ return data.size(); } // # filas
            @Override public int getColumnCount(){ return cols.length; } // # columnas
            @Override public String getColumnName(int c){ return cols[c]; } // Nombre

            @Override public Object getValueAt(int r, int c){ // Retorna valor celda
                Product p = data.get(r);
                return switch(c){
                    case 0 -> p.getCode(); // Código
                    case 1 -> p.getName(); // Nombre
                    case 2 -> p.getCategory(); // Categoría
                    case 3 -> p.getPrice(); // Precio
                    case 4 -> p.getStock(); // Stock
                    default -> "";
                };
            }
        }
    }
}

