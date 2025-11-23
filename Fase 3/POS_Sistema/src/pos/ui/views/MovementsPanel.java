package pos.ui.views; // Paquete donde vive este panel

import pos.dao.MovementDao; // DAO para consultar movimientos históricos

import javax.swing.*; // Componentes Swing
import javax.swing.table.DefaultTableModel; // Modelo de tabla editable
import java.awt.*; // Layouts, colores
import java.io.FileWriter; // Para guardar CSV
import java.time.LocalDateTime; // Para nombre del archivo de exportación
import java.util.List; // Listas
import java.util.Objects; // Util para manejar nulls
import java.util.stream.Collectors; // Para filtrar listas

/**
 * Panel que muestra el historial de movimientos de inventario.
 * Compatible con la versión actualizada de MovementDao.
 */
public class MovementsPanel extends JPanel { // Panel principal

    private final JTextField txtCode = new JTextField(12); // Input código para filtrar
    private final JComboBox<String> cboType =              // Combo de tipo de movimiento
            new JComboBox<>(new String[]{"", "ENTRY", "EXIT", "ADJUST", "DELETE"});
    private final JButton btnFilter  = new JButton("Filtrar"); // Botón aplicar filtros
    private final JButton btnReset   = new JButton("Limpiar"); // Botón limpiar filtros
    private final JButton btnRefresh = new JButton("Actualizar"); // Botón recargar tabla
    private final JButton btnExport  = new JButton("Exportar CSV"); // Botón exportar CSV

    private final JTable table; // Tabla donde se muestran movimientos
    private final DefaultTableModel model; // Modelo que controla la tabla

    private final MovementDao dao = new MovementDao(); // DAO de movimientos

    public MovementsPanel() { // Constructor sin filtro inicial
        this("");
    }

    /** Permite abrir el historial ya filtrado por un código. */
    public MovementsPanel(String presetCode) { // Constructor con filtro pre-cargado
        setLayout(new BorderLayout(8, 8)); // Layout principal
        setBackground(Color.WHITE); // Fondo blanco limpio

        // ====== Barra de filtros ======
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); // Panel de filtros
        filters.add(new JLabel("Código:")); // Texto "Código"
        filters.add(txtCode); // Input código
        filters.add(new JLabel("Tipo:")); // Texto "Tipo"
        filters.add(cboType); // Combo tipo
        filters.add(btnFilter); // Botón filtrar
        filters.add(btnReset); // Botón limpiar
        filters.add(btnRefresh); // Botón actualizar
        filters.add(btnExport); // Botón exportar
        add(filters, BorderLayout.NORTH); // Lo agrega arriba

        // ====== Tabla ======
        model = new DefaultTableModel(new Object[]{
                "Fecha", "Tipo", "Código", "Producto", "Cantidad",
                "Stock (antes→después)", "Usuario", "Motivo"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; } // Tabla no editable
        };

        table = new JTable(model); // Crear tabla con modelo
        table.setFillsViewportHeight(true); // Que la tabla se expanda
        table.setRowHeight(22); // Alto filas
        table.getTableHeader().setReorderingAllowed(false); // Evitar arrastrar columnas
        add(new JScrollPane(table), BorderLayout.CENTER); // Agregar scroll + tabla

        if (presetCode != null && !presetCode.isBlank()) { // Si viene código pre-cargado
            txtCode.setText(presetCode); // Lo escribe en el input
        }

        // ====== Listeners ======
        btnFilter.addActionListener(e -> loadData()); // Aplicar filtros
        btnReset.addActionListener(e -> { // Limpiar filtros
            txtCode.setText(""); // Vaciar código
            cboType.setSelectedIndex(0); // Limpiar tipo
            loadData(); // Recargar completa
        });
        btnRefresh.addActionListener(e -> loadData()); // Forzar actualización
        btnExport.addActionListener(e -> exportCsv()); // Exportar CSV

        // Carga inicial
        loadData(); // Cargar movimientos al abrir panel
    }

    private void loadData() { // Cargar datos en tabla
        model.setRowCount(0); // Limpiar tabla

        String code = txtCode.getText().trim(); // Código filtro
        String type = Objects.toString(cboType.getSelectedItem(), "").trim(); // Tipo filtro

        // Si hay código → lista movimientos de ese producto
        // Si no → lista los más recientes
        List<String[]> rows = (!code.isEmpty())
                ? dao.listByCode(code, 300) // Filtrado por código
                : dao.listRecent(300); // Ultimos 300 movimientos

        // Filtro por tipo (ENTRY/EXIT/ADJUST/DELETE)
        if (!type.isEmpty()) {
            rows = rows.stream()
                    .filter(r -> {
                        int idxType = (r.length >= 9) ? 2 : 0; // Columna tipo = 2
                        return type.equalsIgnoreCase(r[idxType]); // Aplicar filtro
                    })
                    .collect(Collectors.toList());
        }

        // Cargar filas según formato de MovementDao actualizado
        for (String[] r : rows) {
            try {
                // listRecent devuelve 9 columnas:
                // [code, product_name, type, qty, reason, prev, new, user, created_at]
                String c = safe(r, 0); // Código
                String name = safe(r, 1); // Nombre producto
                String t = safe(r, 2); // Tipo movimiento
                String qty = safe(r, 3); // Cantidad movida
                String reason = safe(r, 4); // Motivo
                String prev = safe(r, 5); // Stock antes
                String now = safe(r, 6); // Stock después
                String user = safe(r, 7); // Usuario
                String when = safe(r, 8); // Fecha creación

                model.addRow(new Object[]{ when, t, c, name, qty, prev + " → " + now, user, reason });
            } catch (Exception ignore) {} // Ignorar errores en filas corruptas
        }

        if (model.getRowCount() == 0) { // Si no hay datos
            JOptionPane.showMessageDialog(this, "No se encontraron movimientos con los filtros aplicados.");
        }
    }

    private String safe(String[] arr, int idx) { // Evita ArrayIndexOutOfBounds
        return (arr != null && idx < arr.length && arr[idx] != null) ? arr[idx] : "";
    }

    private void exportCsv() { // Exportar tabla a CSV
        JFileChooser fc = new JFileChooser(); // Selector de archivos
        fc.setSelectedFile(new java.io.File(
                "movimientos_" + LocalDateTime.now().toString().replace(":", "-") + ".csv"
        )); // Nombre auto + timestamp
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return; // Cancelado

        try (FileWriter w = new FileWriter(fc.getSelectedFile(), false)) { // Crear archivo

            // Encabezados
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) w.write(",");
                w.write(model.getColumnName(c)); // Escribir nombre columna
            }
            w.write("\n");

            // Filas
            for (int r = 0; r < model.getRowCount(); r++) { // Recorrer filas
                for (int c = 0; c < model.getColumnCount(); c++) { // Recorrer columnas
                    if (c > 0) w.write(",");
                    Object val = model.getValueAt(r, c); // Valor celda
                    String s = (val == null) ? "" : val.toString().replace("\"", "\"\""); // Escapar comillas
                    w.write("\"" + s + "\""); // Escribir en CSV
                }
                w.write("\n");
            }
            JOptionPane.showMessageDialog(this, "CSV exportado correctamente."); // OK
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo exportar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

