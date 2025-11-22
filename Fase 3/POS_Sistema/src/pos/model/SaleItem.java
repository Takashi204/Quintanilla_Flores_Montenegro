package pos.model;

import java.util.Objects;

public class SaleItem {

    private Product product;
    private int quantity;

    public SaleItem(Product product, int quantity) {
        this.product = product;
        this.quantity = Math.max(1, quantity);
    }

    // ================================
    //            GETTERS
    // ================================
    public Product getProduct() { return product; }
    public int getQty() { return quantity; }
    public int getQuantity() { return quantity; }

    // ================================
    //            SETTERS
    // ================================
    public void setProduct(Product product) { this.product = product; }
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); }

    // ================================
    //            LÓGICA
    // ================================
    /** Subtotal = price * qty (null-safe). */
    public int getSubtotal() {
        int price = (product == null) ? 0 : Math.max(0, product.getPrice());
        return price * Math.max(1, quantity);
    }

    @Override
    public String toString() {
        String name = (product == null) ? "(producto)" : product.getName();
        return quantity + " x " + name + " = $" + getSubtotal();
    }

    // ================================
    //            EQUALITY
    // ================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaleItem)) return false;

        SaleItem that = (SaleItem) o;
        String codeA = (product == null) ? null : product.getCode();
        String codeB = (that.product == null) ? null : that.product.getCode();

        return quantity == that.quantity && Objects.equals(codeA, codeB);
    }

    @Override
    public int hashCode() {
        String code = (product == null) ? null : product.getCode();
        return Objects.hash(code, quantity);
    }
}
