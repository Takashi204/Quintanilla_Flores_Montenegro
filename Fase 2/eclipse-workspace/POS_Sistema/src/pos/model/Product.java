package pos.model;

import java.time.LocalDate;

public class Product {
    private String code;
    private String name;
    private String category;   // NUEVO
    private int price;
    private int stock;
    private LocalDate expiry;  // NUEVO

    // Constructor completo
    public Product(String code, String name, String category, int price, int stock, LocalDate expiry) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = Math.max(0, stock);
        this.expiry = expiry;
    }

    // Sobrecargas para no romper tu código viejo
    public Product(String code, String name, String category, int price, int stock) {
        this(code, name, category, price, stock, null);
    }
    public Product(String code, String name, int price, int stock) {
        this(code, name, "", price, stock, null);
    }

    // Getters y Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = Math.max(0, stock); }

    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }
}