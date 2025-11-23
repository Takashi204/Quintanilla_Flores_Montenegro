package pos.ui.dialogs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo modal para buscar productos por nombre o código.
 * - Filtro en vivo mientras escribes
 * - Tabla filtrada
 * - Doble click o Enter para seleccionar
 * - Escape o Cancelar para cerrar sin selección
 */
public class ProductSearchDialog extends JDialog {

    /**
     * Clase interna simple para representar un producto mostrado en la búsqueda.
     */
    public static class Product {
        private final String id;              // ID o código
        private final String name;            // nombre
        private final BigDecimal price;       // precio unitario

        public Product(String id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
    }

    // --- Componentes UI principales ---
    private final JTextField txtBuscar = new JTextField(); // campo de búsqueda
    private final JTable tbl = new JTable();              // tabla con resultados
    private final JButton btnAceptar = new JButton("Elegir (Enter)");  // seleccionar
    private final JButton btnCancelar = new JButton("Cancelar (Esc)"); // cerrar

    // --- Listas de trabajo ---
    private final List<Product> allProducts; // lista original de productos
    private final List<Product> filtered = new ArrayList<>(); // lista filtrada

    private Product selected; // producto finalmente elegido

    public ProductSearchDialog(Window parent, List<Product> products) {
        super(parent, "Buscar producto (F2)", ModalityType.APPLICATION_MODAL);

        // Si la lista viene null, usar lista vacía
        this.allProducts = products != null ? products : new ArrayList<>();

        // iniciar lista filtrada con todos
        this.filtered.addAll(this.allProducts);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(parent);

        initUI();     // construir interfaz
        wireEvents(); // conectar eventos
    }

    // Devuelve el producto seleccionado, o null si se cerró/canceló
    public Product getSelectedProduct() {
        return selected;
    }

    private void initUI() {

        // ---- Zona superior: campo de búsqueda ----
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JLabel lbl = new JLabel("Buscar por nombre o código:");
        top.add(lbl, BorderLayout.WEST);
        top.add(txtBuscar, BorderLayout.CENTER);

        // ---- Tabla con productos filtrados ----
        tbl.setModel(new ProductTableModel(filtered)); // modelo custom
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // solo una fila
        JScrollPane sp = new JScrollPane(tbl);

        // ---- Botones inferiores ----
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.add(btnCancelar);
        bottom.add(btnAceptar);

        // ---- Layout final del diálogo ----
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void wireEvents() {

        // --- Filtrar a medida que se escribe ---
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refiltrar(); }
            public void removeUpdate(DocumentEvent e) { refiltrar(); }
            public void changedUpdate(DocumentEvent e) { refiltrar(); }
        });

        // Enter en todo el diálogo = Elegir
        getRootPane().setDefaultButton(btnAceptar);

        // --- Doble click en la tabla = seleccionar ---
        tbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tbl.getSelectedRow() >= 0) {
                    aceptarSeleccion();
                }
            }
        });

        // --- Enter dentro de la tabla = seleccionar ---
        tbl.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && tbl.getSelectedRow() >= 0) {
                    e.consume(); // evita saltar a otra fila
                    aceptarSeleccion();
                }
            }
        });

        // --- Botón aceptar ---
        btnAceptar.addActionListener(e -> aceptarSeleccion());

        // --- Botón cancelar ---
        btnCancelar.addActionListener(e -> {
            selected = null; // sin selección
            dispose();
        });

        // --- ESC para cerrar ---
        getRootPane().registerKeyboardAction(
                e -> { selected = null; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    // Procesa selección final
    private void aceptarSeleccion() {
        int viewRow = tbl.getSelectedRow(); // fila vista

        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Selecciona un producto.",
                    "Atención",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Convertir fila "vista" a fila real del modelo
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        ProductTableModel m = (ProductTableModel) tbl.getModel();

        // Guardar producto seleccionado
        selected = m.getAt(modelRow);

        dispose(); // cerrar diálogo
    }

    // Filtro según texto escrito
    private void refiltrar() {
        String q = txtBuscar.getText().trim().toLowerCase();

        filtered.clear(); // limpiar lista filtrada

        if (q.isEmpty()) {
            filtered.addAll(allProducts); // sin filtro → todos
        } else {
            for (Product p : allProducts) {
                boolean match =
                        (p.getId() != null && p.getId().toLowerCase().contains(q)) ||
                        (p.getName() != null && p.getName().toLowerCase().contains(q));

                if (match) filtered.add(p);
            }
        }

        // avisar a la tabla que cambió su contenido
        ((AbstractTableModel) tbl.getModel()).fireTableDataChanged();

        // seleccionar la primera fila si hay resultados
        if (!filtered.isEmpty()) {
            tbl.setRowSelectionInterval(0, 0);
        }
    }

    /**
     * Modelo de tabla para mostrar productos.
     */
    private static class ProductTableModel extends AbstractTableModel {

        private final String[] cols = {"Código", "Nombre", "Precio"}; // encabezados
        private final List<Product> data; // referencia a la lista filtrada

        ProductTableModel(List<Product> data) {
            this.data = data;
        }

        public int getRowCount() { return data.size(); }

        public int getColumnCount() { return cols.length; }

        public String getColumnName(int c) { return cols[c]; }

        public boolean isCellEditable(int r, int c) { return false; } // tabla solo lectura

        public Object getValueAt(int r, int c) {
            Product p = data.get(r);

            switch (c) {
                case 0: return p.getId();                     // código
                case 1: return p.getName();                   // nombre
                case 2: return formatCurrency(p.getPrice());  // precio formateado
                default: return "";
            }
        }

        // obtener producto por fila
        public Product getAt(int r) { return data.get(r); }

        // formato precio CLP
        private static String formatCurrency(BigDecimal v) {
            if (v == null) return "";
            return "$" + v.setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString();
        }
    }
}

