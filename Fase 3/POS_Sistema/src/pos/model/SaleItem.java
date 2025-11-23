package pos.model; // el modelo SaleItem vive en este paquete

import java.util.Objects; // usado para equals/hashCode

public class SaleItem {

    // ================================
    // Atributos del ítem de venta
    // ================================

    private Product product; // producto involucrado en la venta
    private int quantity;    // cantidad vendida de este producto

    // ================================
    // Constructor
    // ================================
    public SaleItem(Product product, int quantity) {
        this.product = product;              // asignar producto
        this.quantity = Math.max(1, quantity); // evita cantidades 0 o negativas
    }

    // ================================
    // GETTERS
    // ================================
    public Product getProduct() { return product; }  // devuelve producto
    public int getQty() { return quantity; }         // alias de cantidad
    public int getQuantity() { return quantity; }    // cantidad vendida

    // ================================
    // SETTERS
    // ================================
    public void setProduct(Product product) { 
        this.product = product; 
    }

    public void setQuantity(int quantity) { 
        // evita poner cantidad 0 o negativa → siempre al menos 1
        this.quantity = Math.max(1, quantity); 
    }

    // ================================
    // LÓGICA DE NEGOCIO
    // ================================
    /** 
     * Calcula el subtotal del ítem:
     * subtotal = precio del producto * cantidad
     * Maneja caso null por seguridad.
     */
    public int getSubtotal() {
        int price = (product == null) ? 0 : Math.max(0, product.getPrice());
        return price * Math.max(1, quantity);
    }

    // Representación en texto útil para depuración o impresión
    @Override
    public String toString() {
        String name = (product == null) ? "(producto)" : product.getName();
        return quantity + " x " + name + " = $" + getSubtotal();
    }

    // ================================
    // EQUALITY — compara ítems por producto y cantidad
    // ================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;              // si es el mismo objeto → true
        if (!(o instanceof SaleItem)) return false;

        SaleItem that = (SaleItem) o;

        // Comparación segura del código del producto (product puede ser null)
        String codeA = (product == null) ? null : product.getCode();
        String codeB = (that.product == null) ? null : that.product.getCode();

        // Igual si tienen el mismo producto y misma cantidad
        return quantity == that.quantity && Objects.equals(codeA, codeB);
    }

    @Override
    public int hashCode() {
        String code = (product == null) ? null : product.getCode();
        return Objects.hash(code, quantity); // usa código + cantidad como identidad
    }
}
