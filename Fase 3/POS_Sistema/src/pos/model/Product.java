package pos.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa un producto del inventario.
 * Compatible con lector de código de barras y movimientos de stock.
 */
public class Product {

    // ==============================
    // Atributos del modelo
    // ==============================

    private int id;                 // ID autoincremental en BD (0 si aún no se guarda)
    private String code;            // Código único (puede venir del lector de barras)
    private String name;            // Nombre del producto
    private String category;        // Categoría o tipo
    private int price;              // Precio unitario
    private int stock;              // Stock disponible
    private LocalDate expiry;       // Fecha de expiración (opcional)

    // ==============================
    // Constructores
    // ==============================

    // Constructor completo — usado cuando viene desde BD
    public Product(int id, String code, String name, String category, int price, int stock, LocalDate expiry) {
        this.id = id;                                      // asigna ID (si viene de BD)
        this.code = code != null ? code.trim() : "";       // trim + fallback string vacío
        this.name = name != null ? name.trim() : "";       // evita null en nombre
        this.category = category != null ? category.trim() : ""; // categoría normalizada
        this.price = Math.max(0, price);                   // evita precios negativos
        this.stock = Math.max(0, stock);                   // evita stock negativo
        this.expiry = expiry;                              // fecha puede ser null
    }

    // Constructor sin ID (para insertar nuevo)
    public Product(String code, String name, String category, int price, int stock, LocalDate expiry) {
        this(0, code, name, category, price, stock, expiry); // id = 0 hasta que BD lo genere
    }

    // Constructor sin expiry (producto sin caducidad)
    public Product(String code, String name, String category, int price, int stock) {
        this(0, code, name, category, price, stock, null);
    }

    // ==============================
    // Getters / Setters
    // ==============================

    public int getId() { return id; }                      // devuelve ID
    public void setId(int id) { this.id = id; }            // asigna ID luego de insertar

    public String getCode() { return code; }               // devuelve el código de barras
    public void setCode(String code) {                     // setea el código asegurando no-null
        this.code = code != null ? code.trim() : "";
    }

    public String getName() { return name; }               // devuelve nombre
    public void setName(String name) {                     // normaliza texto
        this.name = name != null ? name.trim() : "";
    }

    public String getCategory() { return category; }       // devuelve categoría
    public void setCategory(String category) {             // evita null
        this.category = category != null ? category.trim() : "";
    }

    public int getPrice() { return price; }                // precio unitario
    public void setPrice(int price) {                      // evita valores negativos
        this.price = Math.max(0, price);
    }

    public int getStock() { return stock; }                // stock disponible
    public void setStock(int stock) {                      // evita stock negativo
        this.stock = Math.max(0, stock);
    }

    public LocalDate getExpiry() { return expiry; }        // fecha de caducidad
    public void setExpiry(LocalDate expiry) {              // puede ser null
        this.expiry = expiry;
    }

    // ==============================
    // Métodos utilitarios del modelo
    // ==============================

    // Indica si el producto está vencido (solo si tiene expiry)
    public boolean isExpired() {
        return expiry != null && expiry.isBefore(LocalDate.now());
    }

    // Indica si tiene poco stock (umbral fijo = 3)
    public boolean hasLowStock() {
        return stock <= 3;
    }

    // Representación amigable del producto en texto
    @Override
    public String toString() {
        return String.format("[%s] %s ($%d | stock %d)", code, name, price, stock);
    }

    // Comparación semántica entre productos
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;                 // si es el mismo objeto → true
        if (!(o instanceof Product p)) return false; // si no es Product → false

        // Si ambos productos tienen ID en BD → se comparan por ID
        if (id > 0 && p.id > 0) return id == p.id;

        // Si no tienen ID → se comparan por código (único)
        return Objects.equals(code, p.code);
    }

    // hashCode consistente con equals()
    @Override
    public int hashCode() {
        return (id > 0)
                ? Objects.hash(id)     // si tiene ID usa solo ID
                : Objects.hash(code);   // si no, usa el code como identificador lógico
    }
}
