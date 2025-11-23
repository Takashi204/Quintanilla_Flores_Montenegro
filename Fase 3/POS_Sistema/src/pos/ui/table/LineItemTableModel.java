package pos.ui.table;

import pos.model.SaleItem;
import pos.util.Money;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para ítems de venta (carrito del POS).
 * Cada fila representa un SaleItem.
 * Las columnas son: Código, Descripción, Cantidad, Precio Unitario, Total.
 */
public class LineItemTableModel extends AbstractTableModel {

    // Encabezados de la tabla
    private final String[] cols = {"Código", "Descripción", "Cant.", "P.Unit", "Total"};

    // Lista interna donde guardamos los ítems agregados al carrito
    private final List<SaleItem> data = new ArrayList<>();

    // ==========================================================
    //                     API PÚBLICA DEL MODELO
    // ==========================================================

    /**
     * Agrega un item al carrito.
     * Si el producto ya existe → solo incrementa la cantidad.
     */
    public void addOrIncrement(SaleItem item) {
        String code = item.getProduct().getCode();

        // Buscar si ya existe un item con el mismo código
        for (SaleItem it : data) {
            if (it.getProduct().getCode().equalsIgnoreCase(code)) {
                // Si existe → solo sumar cantidades
                it.setQuantity(it.getQuantity() + item.getQuantity());
                fireTableDataChanged(); // refrescar tabla completa
                return;
            }
        }

        // Si no existe → agregar como nuevo
        data.add(item);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }

    /**
     * Elimina una fila de la tabla por su índice.
     */
    public void remove(int row) {
        if (row >= 0 && row < data.size()) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    /**
     * Limpia todo el carrito.
     */
    public void clear() {
        data.clear();
        fireTableDataChanged();
    }

    /**
     * Retorna una copia de todos los SaleItem.
     */
    public List<SaleItem> items() {
        return new ArrayList<>(data);
    }

    /**
     * Retorna el subtotal (suma de precios sin IVA).
     */
    public int subtotal() {
        int s = 0;
        for (SaleItem it : data) {
            // Precio unitario * cantidad
            s += it.getProduct().getPrice() * it.getQuantity();
        }
        return s;
    }

    // ==========================================================
    //              MÉTODOS OBLIGATORIOS DEL TABLE MODEL
    // ==========================================================

    @Override public int getRowCount() {
        return data.size();
    }

    @Override public int getColumnCount() {
        return cols.length;
    }

    @Override public String getColumnName(int c) {
        return cols[c];
    }

    /**
     * Define qué celdas son editables.
     * Permitimos editar:
     *  - Cantidad (col 2)
     *  - Precio unitario (col 3)
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 2 || columnIndex == 3;
    }

    /**
     * Devuelve el valor a mostrar en la celda.
     */
    @Override
    public Object getValueAt(int row, int col) {
        SaleItem it = data.get(row);

        return switch (col) {
            case 0 -> it.getProduct().getCode();       // Código del producto
            case 1 -> it.getProduct().getName();       // Nombre
            case 2 -> it.getQuantity();                // Cantidad
            case 3 -> Money.format(it.getProduct().getPrice()); // Precio unitario formateado
            case 4 -> Money.format(it.getProduct().getPrice() * it.getQuantity()); // Total fila
            default -> "";
        };
    }

    /**
     * Permite modificar cantidad o precio unitario desde la tabla.
     */
    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (row < 0 || row >= data.size()) return;

        SaleItem it = data.get(row);

        try {
            if (col == 2) { // Editar cantidad
                int q = Integer.parseInt(String.valueOf(aValue).trim());
                it.setQuantity(Math.max(1, q));

            } else if (col == 3) { // Editar precio unitario
                // Limpiar todo lo que NO sea número
                String s = String.valueOf(aValue).replaceAll("[^0-9]", "");

                int p = s.isEmpty() ? 0 : Integer.parseInt(s);
                it.getProduct().setPrice(Math.max(0, p));
            }

            fireTableRowsUpdated(row, row);

        } catch (Exception e) {
            // Si algo sale mal (texto inválido), simplemente ignoramos
        }
    }
}
