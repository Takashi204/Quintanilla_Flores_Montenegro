package pos.model;

import java.time.LocalDate;
import java.util.Objects;

public class Product {

    // ======================================
    // Atributos compatibles con POS + API
    // ======================================
    private int id;
    private String code;
    private String name;
    private String category;      // categoría del POS (texto)
    private int categoryId;       // categoría de la API (número)
    private int price;
    private int stock;
    private boolean active;       // API lo usa (true/false)
    private LocalDate expiry;     // POS lo usa (opcional)

    // ======================================
    // Constructores originales (POS)
    // ======================================
    public Product(int id, String code, String name, String category, int price, int stock, LocalDate expiry) {
        this.id = id;
        this.code = code != null ? code.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.category = category != null ? category.trim() : "";
        this.price = Math.max(0, price);
        this.stock = Math.max(0, stock);
        this.expiry = expiry;
        this.active = true;       // por defecto
        this.categoryId = 0;      // por defecto
    }

    public Product(String code, String name, String category, int price, int stock, LocalDate expiry) {
        this(0, code, name, category, price, stock, expiry);
    }

    public Product(String code, String name, String category, int price, int stock) {
        this(0, code, name, category, price, stock, null);
    }

    // ======================================
    // ✔ NUEVO CONSTRUCTOR PARA LA API
    // ======================================
    public Product(int id, String code, String name, int price, int stock, int categoryId, boolean active) {
        this.id = id;
        this.code = code != null ? code.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
        this.active = active;

        // Convertimos categoryId → category de texto (si quieres usarlo)
        this.category = "CAT-" + categoryId;

        // La API no maneja expiración → null
        this.expiry = null;
    }

    // ======================================
    // Getters / Setters
    // ======================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim() : ""; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : ""; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category != null ? category.trim() : ""; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = Math.max(0, price); }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = Math.max(0, stock); }

    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }

    // ======================================
    // Métodos utilitarios
    // ======================================
    public boolean isExpired() {
        return expiry != null && expiry.isBefore(LocalDate.now());
    }

    public boolean hasLowStock() {
        return stock <= 3;
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
