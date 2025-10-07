package pos.ui.views;

import pos.model.InventoryMovement;
import pos.repo.InMemoryMovementRepository;
import pos.ui.table.MovementTableModel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MovementsPanel extends JPanel {

    private final JTextField txtCode = new JTextField(12);
    private final JComboBox<InventoryMovement.MovementType> cmbType =
            new JComboBox<>(InventoryMovement.MovementType.values());
    private final JButton btnFilter = new JButton("Filtrar");
    private final JButton btnReset  = new JButton("Limpiar");
    private final JButton btnRefresh= new JButton("Actualizar");
    private final JButton btnExport = new JButton("Exportar CSV");

    private final MovementTableModel model;
    private final JTable table;

    public MovementsPanel() {
        setLayout(new BorderLayout(8,8));

        // Barra de filtros
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Código:"));
        filters.add(txtCode);
        filters.add(new JLabel("Tipo:"));
        filters.add(cmbType);
        filters.add(btnFilter);
        filters.add(btnReset);
        filters.add(btnRefresh);
        filters.add(btnExport);
        add(filters, BorderLayout.NORTH);

        // Tabla
        List<InventoryMovement> initial = InMemoryMovementRepository.getInstance().findAll();
        model = new MovementTableModel(initial);
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Listeners
        btnFilter.addActionListener(e -> applyFilters());
        btnReset.addActionListener(e -> resetFilters());
        btnRefresh.addActionListener(e -> refreshData());
        btnExport.addActionListener(e -> exportCsv());
    }

    private void applyFilters() {
        String code = txtCode.getText();
        InventoryMovement.MovementType type = (InventoryMovement.MovementType) cmbType.getSelectedItem();

        List<InventoryMovement> all = InMemoryMovementRepository.getInstance().findAll();

        List<InventoryMovement> filtered = all.stream()
                .filter(m -> code == null || code.isBlank()
                        || (m.getProduct() != null && code.equalsIgnoreCase(m.getProduct().getCode())))
                .filter(m -> type == null || m.getType() == type)
                .collect(Collectors.toList());

        model.setData(filtered);
    }

    private void resetFilters() {
        txtCode.setText("");
        cmbType.setSelectedItem(null);
        refreshData();
    }

    private void refreshData() {
        model.setData(InMemoryMovementRepository.getInstance().findAll());
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("movimientos_" + LocalDateTime.now().toString().replace(":","-") + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File f = chooser.getSelectedFile();
            try (java.io.PrintWriter out = new java.io.PrintWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
                // encabezado
                for (int c = 0; c < model.getColumnCount(); c++) {
                    out.print(model.getColumnName(c));
                    out.print(c == model.getColumnCount()-1 ? "\n" : ",");
                }
                // filas
                for (int r = 0; r < model.getRowCount(); r++) {
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        Object val = model.getValueAt(r, c);
                        String cell = (val == null) ? "" : val.toString().replace("\"","\"\"");
                        out.print("\"" + cell + "\"");
                        out.print(c == model.getColumnCount()-1 ? "\n" : ",");
                    }
                }
                JOptionPane.showMessageDialog(this, "CSV exportado:\n" + f.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

