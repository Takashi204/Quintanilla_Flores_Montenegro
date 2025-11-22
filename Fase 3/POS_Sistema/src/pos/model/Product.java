package pos.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa un producto del inventario.
 * Compatible con lector de código de barras y movimientos de stock.
 */
public class Product {

    private int id;
    private String code;
    private String name;
    private String category;
    private int price;
    private int stock;
    private LocalDate expiry;

    // ==== Constructores ====
    public Product(int id, String code, String name, String category, int price, int stock, LocalDate expiry) {
        this.id = id;
        this.code = code != null ? code.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.category = category != null ? category.trim() : "";
        this.price = Math.max(0, price);
        this.stock = Math.max(0, stock);
        this.expiry = expiry;
    }

    public Product(String code, String name, String category, int price, int stock, LocalDate expiry) {
        this(0, code, name, category, price, stock, expiry);
    }

    public Product(String code, String name, String category, int price, int stock) {
        this(0, code, name, category, price, stock, null);
    }

    // ==== Getters / Setters ====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim() : ""; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : ""; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category != null ? category.trim() : ""; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = Math.max(0, price); }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = Math.max(0, stock); }

    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }

    // ==== Métodos utilitarios ====
    public boolean isExpired() {
        return expiry != null && expiry.isBefore(LocalDate.now());
    }

    public boolean hasLowStock() {
        return stock <= 3; // Umbral configurable
    }

    @Override
    public String toString() {
        return String.format("[%s] %s ($%d | stock %d)", code, name, price, stock);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        if (id > 0 && p.id > 0) return id == p.id;
        return Objects.equals(code, p.code);
    }

    @Override
    public int hashCode() {
        return (id > 0) ? Objects.hash(id) : Objects.hash(code);
    }
}
