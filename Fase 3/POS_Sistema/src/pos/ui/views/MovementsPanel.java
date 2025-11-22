package pos.ui.views;

import pos.dao.MovementDao;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Panel que muestra el historial de movimientos de inventario.
 * Compatible con la versión actualizada de MovementDao.
 */
public class MovementsPanel extends JPanel {

    private final JTextField txtCode = new JTextField(12);
    private final JComboBox<String> cboType =
            new JComboBox<>(new String[]{"", "ENTRY", "EXIT", "ADJUST", "DELETE"});
    private final JButton btnFilter  = new JButton("Filtrar");
    private final JButton btnReset   = new JButton("Limpiar");
    private final JButton btnRefresh = new JButton("Actualizar");
    private final JButton btnExport  = new JButton("Exportar CSV");

    private final JTable table;
    private final DefaultTableModel model;

    private final MovementDao dao = new MovementDao();

    public MovementsPanel() {
        this("");
    }

    /** Permite abrir el historial ya filtrado por un código. */
    public MovementsPanel(String presetCode) {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);

        // ====== Barra de filtros ======
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filters.add(new JLabel("Código:"));
        filters.add(txtCode);
        filters.add(new JLabel("Tipo:"));
        filters.add(cboType);
        filters.add(btnFilter);
        filters.add(btnReset);
        filters.add(btnRefresh);
        filters.add(btnExport);
        add(filters, BorderLayout.NORTH);

        // ====== Tabla ======
        model = new DefaultTableModel(new Object[]{
                "Fecha", "Tipo", "Código", "Producto", "Cantidad",
                "Stock (antes→después)", "Usuario", "Motivo"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        if (presetCode != null && !presetCode.isBlank()) {
            txtCode.setText(presetCode);
        }

        // ====== Listeners ======
        btnFilter.addActionListener(e -> loadData());
        btnReset.addActionListener(e -> {
            txtCode.setText("");
            cboType.setSelectedIndex(0);
            loadData();
        });
        btnRefresh.addActionListener(e -> loadData());
        btnExport.addActionListener(e -> exportCsv());

        // Carga inicial
        loadData();
    }

    private void loadData() {
        model.setRowCount(0);

        String code = txtCode.getText().trim();
        String type = Objects.toString(cboType.getSelectedItem(), "").trim();

        // Si hay código, muestra movimientos específicos; si no, los más recientes
        List<String[]> rows = (!code.isEmpty())
                ? dao.listByCode(code, 300)
                : dao.listRecent(300);

        // Filtro por tipo
        if (!type.isEmpty()) {
            rows = rows.stream()
                    .filter(r -> {
                        int idxType = (r.length >= 9) ? 2 : 0;
                        return type.equalsIgnoreCase(r[idxType]);
                    })
                    .collect(Collectors.toList());
        }

        // Cargar filas según formato de MovementDao actualizado
        for (String[] r : rows) {
            try {
                // listRecent devuelve 9 columnas: [code, product_name, type, qty, reason, prev, new, user, created_at]
                String c = safe(r, 0);
                String name = safe(r, 1);
                String t = safe(r, 2);
                String qty = safe(r, 3);
                String reason = safe(r, 4);
                String prev = safe(r, 5);
                String now = safe(r, 6);
                String user = safe(r, 7);
                String when = safe(r, 8);

                model.addRow(new Object[]{ when, t, c, name, qty, prev + " → " + now, user, reason });
            } catch (Exception ignore) {}
        }

        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No se encontraron movimientos con los filtros aplicados.");
        }
    }

    private String safe(String[] arr, int idx) {
        return (arr != null && idx < arr.length && arr[idx] != null) ? arr[idx] : "";
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("movimientos_" + LocalDateTime.now().toString().replace(":", "-") + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter w = new FileWriter(fc.getSelectedFile(), false)) {
            // Encabezados
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) w.write(",");
                w.write(model.getColumnName(c));
            }
            w.write("\n");

            // Filas
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) w.write(",");
                    Object val = model.getValueAt(r, c);
                    String s = (val == null) ? "" : val.toString().replace("\"", "\"\"");
                    w.write("\"" + s + "\"");
                }
                w.write("\n");
            }
            JOptionPane.showMessageDialog(this, "CSV exportado correctamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo exportar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
