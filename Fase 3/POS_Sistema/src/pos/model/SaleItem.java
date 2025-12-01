package pos.model;

import java.util.Objects;

public class SaleItem {

    // ===== CAMPOS QUE USA LA API =====
    private Integer product_id;      // ID real del producto (API)
    private int quantity;            // cantidad
    private int unit_price;          // precio unitario (API)

    // ===== CAMPOS DEL POS =====
    private String product_code;     // código del producto (POS)
    private int subtotal;            // subtotal del POS
    private Product product;         // producto completo del POS


    // ===== CONSTRUCTOR =====
    public SaleItem(Product product, int quantity) {
        this.product = product;
        this.quantity = Math.max(1, quantity);

        // POS
        this.product_code = product != null ? product.getCode() : null;
        this.subtotal = getSubtotal();

        // API
        this.product_id = product != null ? product.getId() : null;
        this.unit_price = product != null ? product.getPrice() : 0;
    }


    // ===== GETTERS API =====
    public Integer getProduct_id() { return product_id; }
    public int getQuantity() { return quantity; }
    public int getUnit_price() { return unit_price; }

    // ===== GETTERS POS =====
    public String getProduct_code() { return product_code; }
    public Product getProduct() { return product; }
    public int getQty() { return quantity; }

    public int getSubtotal() {
        if (product == null) return 0;
        return product.getPrice() * quantity;
    }


    // ===== SETTERS =====
    public void setProduct(Product product) {
        this.product = product;

        // POS
        this.product_code = product != null ? product.getCode() : null;

        // API
        this.product_id = product != null ? product.getId() : null;
        this.unit_price = product != null ? product.getPrice() : 0;

        this.subtotal = getSubtotal();
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
        this.subtotal = getSubtotal();
    }


    @Override
    public String toString() {
        String name = (product == null) ? "(producto)" : product.getName();
        return quantity + " x " + name + " = $" + getSubtotal();
    }


    // PARA QUE FUNCIONE EN TABLAS
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaleItem)) return false;
        SaleItem that = (SaleItem) o;

        String codeA = product != null ? product.getCode() : null;
        String codeB = that.product != null ? that.product.getCode() : null;

        return Objects.equals(codeA, codeB) && quantity == that.quantity;
    }

    @Override
    public int hashCode() {
        String code = product != null ? product.getCode() : null;
        return Objects.hash(code, quantity);
    }
}
