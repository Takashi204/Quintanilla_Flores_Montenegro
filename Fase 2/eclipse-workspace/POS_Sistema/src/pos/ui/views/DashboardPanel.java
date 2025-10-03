package pos.ui.views;

import pos.store.InMemoryStore;
import pos.repo.SaleRepo;
import pos.model.Product;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final JLabel lblHoyTotal = new JLabel("Total hoy: $0");
    private final JTable tblLow = new JTable(new LowModel());
    private final JTable tblExp = new JTable(new ExpModel());

    public DashboardPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        JLabel title = new JLabel("Dashboard — Resumen");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        lblHoyTotal.setFont(lblHoyTotal.getFont().deriveFont(Font.BOLD, 16f));
        top.add(lblHoyTotal);
        add(top, BorderLayout.PAGE_START);

        JPanel center = new JPanel(new GridLayout(1,2,10,10));
        center.setOpaque(false);
        center.add(wrap("Stock bajo (≤ 5)", new JScrollPane(tblLow)));
        center.add(wrap("Vencen en ≤ 30 días", new JScrollPane(tblExp)));
        add(center, BorderLayout.CENTER);

        recargar();
    }

    private JPanel wrap(String title, JComponent c){
        JPanel p = new JPanel(new BorderLayout());
        JLabel t = new JLabel(title);
        t.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
        p.add(t, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Llamar cuando vuelves al Dashboard para refrescar datos */
    public void recargar() {
        // Total vendido HOY (usa SaleRepo)
        LocalDate hoy = LocalDate.now();
        int totalHoy = SaleRepo.get().total(hoy, hoy);
        lblHoyTotal.setText("Total hoy: $" + totalHoy);

        // Alertas desde InMemoryStore
        ((LowModel) tblLow.getModel()).set(InMemoryStore.lowStock(5));
        ((ExpModel) tblExp.getModel()).set(InMemoryStore.expiringInDays(30));
    }

    // ===== Modelos de tabla =====
    private static class LowModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Stock"};
        private java.util.List<Product> data = java.util.List.of();
        public void set(java.util.List<Product> rows){ data = rows; fireTableDataChanged(); }
        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class ExpModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Vence"};
        private java.util.List<Product> data = java.util.List.of();
        public void set(java.util.List<Product> rows){ data = rows; fireTableDataChanged(); }
        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> (p.getExpiry()==null ? "-" : p.getExpiry());
                default -> "";
            };
        }
    }
}