package pos.ui.table;

import pos.model.InventoryMovement;
import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Modelo de tabla para mostrar los movimientos de inventario
 * (entradas, salidas y ajustes) en un JTable.
 */
public class MovementTableModel extends AbstractTableModel {

    private final String[] columns = {
        "ID", "Fecha", "Tipo", "Código", "Producto",
        "Cantidad", "Stock (antes→después)", "Usuario", "Motivo"
    };

    private List<InventoryMovement> data;

    public MovementTableModel(List<InventoryMovement> data) {
        this.data = data;
    }

    public void setData(List<InventoryMovement> data) {
        this.data = data;
        fireTableDataChanged(); // actualiza la tabla al cambiar los datos
    }

    @Override
    public int getRowCount() {
        return (data == null) ? 0 : data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (data == null || rowIndex >= data.size()) return "";
        InventoryMovement m = data.get(rowIndex);

        switch (columnIndex) {
            case 0: return m.getId();
            case 1: return m.getCreatedAt();
            case 2: return m.getType();
            case 3: return (m.getProduct() != null ? m.getProduct().getCode() : "-");
            case 4: return (m.getProduct() != null ? m.getProduct().getName() : "-");
            case 5: return m.getQuantity();
            case 6:
                Integer prev = m.getPreviousStock();
                Integer res = m.getResultingStock();
                return (prev != null && res != null) ? (prev + " → " + res) : "-";
            case 7: return m.getPerformedBy();
            case 8: return m.getReason();
            default: return "";
        }
    }

    public InventoryMovement getMovementAt(int rowIndex) {
        if (data == null || rowIndex < 0 || rowIndex >= data.size()) return null;
        return data.get(rowIndex);
    }
}

