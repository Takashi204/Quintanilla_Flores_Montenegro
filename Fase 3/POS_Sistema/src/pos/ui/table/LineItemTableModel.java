package pos.ui.table;

import pos.model.SaleItem;
import pos.util.Money;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para ítems de venta (carrito del POS).
 * Compatible con SaleItem, usa CLP sin decimales.
 */
public class LineItemTableModel extends AbstractTableModel {

    private final String[] cols = {"Código", "Descripción", "Cant.", "P.Unit", "Total"};
    private final List<SaleItem> data = new ArrayList<>();

    // ---------- API ----------
    public void addOrIncrement(SaleItem item) {
        String code = item.getProduct().getCode();
        for (SaleItem it : data) {
            if (it.getProduct().getCode().equalsIgnoreCase(code)) {
                it.setQuantity(it.getQuantity() + item.getQuantity());
                fireTableDataChanged();
                return;
            }
        }
        data.add(item);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }

    public void remove(int row) {
        if (row >= 0 && row < data.size()) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    public void clear() {
        data.clear();
        fireTableDataChanged();
    }

    public List<SaleItem> items() {
        return new ArrayList<>(data);
    }

    /** Devuelve el subtotal total (sin IVA). */
    public int subtotal() {
        int s = 0;
        for (SaleItem it : data) {
            s += it.getProduct().getPrice() * it.getQuantity();
        }
        return s;
    }

    // ---------- AbstractTableModel ----------
    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Solo se puede editar cantidad y precio unitario
        return columnIndex == 2 || columnIndex == 3;
    }

    @Override
    public Object getValueAt(int row, int col) {
        SaleItem it = data.get(row);
        return switch (col) {
            case 0 -> it.getProduct().getCode();
            case 1 -> it.getProduct().getName();
            case 2 -> it.getQuantity();
            case 3 -> Money.format(it.getProduct().getPrice());
            case 4 -> Money.format(it.getProduct().getPrice() * it.getQuantity());
            default -> "";
        };
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (row < 0 || row >= data.size()) return;
        SaleItem it = data.get(row);

        try {
            if (col == 2) { // cantidad
                int q = Integer.parseInt(String.valueOf(aValue).trim());
                it.setQuantity(Math.max(1, q));
            } else if (col == 3) { // precio unitario
                String s = String.valueOf(aValue).replaceAll("[^0-9]", "");
                int p = s.isEmpty() ? 0 : Integer.parseInt(s);
                it.getProduct().setPrice(Math.max(0, p));
            }
            fireTableRowsUpdated(row, row);
        } catch (Exception e) {
            // ignora errores de formato
        }
    }
}
