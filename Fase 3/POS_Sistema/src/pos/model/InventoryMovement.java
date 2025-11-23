package pos.model;

import java.time.LocalDateTime;
import java.util.Objects;

// Modelo que representa un movimiento de inventario (entrada, salida o ajuste)
public class InventoryMovement {

    // Tipos de movimiento posibles en inventario
    public enum MovementType { ENTRY, EXIT, ADJUSTMENT }

    // Identificación y trazabilidad
    private int id;                 // ID único en BD (autoincremental)
    private String reference;       // Referencia externa (boleta, orden, etc.)
    private String performedBy;     // Usuario que registró el movimiento

    // Datos principales del movimiento
    private Product product;        // Producto afectado
    private int quantity;           // Cantidad afectada
    private MovementType type;      // Tipo de movimiento
    private String reason;          // Motivo del movimiento

    // Información de auditoría
    private final LocalDateTime createdAt; // Fecha y hora de creación del movimiento
    private Integer previousStock;         // Stock antes del movimiento
    private Integer resultingStock;        // Stock después del movimiento

    // --------------------------------------------------------------
    // Constructor PRIVADO: obliga a usar las fábricas estáticas
    // --------------------------------------------------------------
    private InventoryMovement(Product product, int quantity, MovementType type,
                              String reason, String performedBy, String reference) {

        this.product = Objects.requireNonNull(product, "product no puede ser null"); // valida producto
        this.type = Objects.requireNonNull(type, "type no puede ser null");          // valida tipo

        // Normalización de textos
        this.reason = (reason == null || reason.isBlank()) ? "-" : reason.trim();     // deja "-" si no hay razón
        this.performedBy = (performedBy == null || performedBy.isBlank())
                ? "system"
                : performedBy.trim();                                                  // asigna "system" si no hay usuario
        this.reference = (reference == null || reference.isBlank())
                ? null
                : reference.trim();                                                    // convierte texto vacío a null

        this.createdAt = LocalDateTime.now();     // registra fecha exacta de creación

        // Valida la cantidad según el tipo
        validateQuantity(quantity, type);
        this.quantity = quantity;                 // guarda cantidad ya validada
    }

    // --------------------------------------------------------------
    // Fábricas para crear movimientos específicos
    // --------------------------------------------------------------

    // Fábrica para registrar una ENTRADA al inventario
    public static InventoryMovement entry(Product product, int quantity, String reason, String performedBy) {
        return new InventoryMovement(product, quantity, MovementType.ENTRY, reason, performedBy, null);
    }

    // Fábrica para registrar una SALIDA del inventario
    public static InventoryMovement exit(Product product, int quantity, String reason, String performedBy) {
        return new InventoryMovement(product, quantity, MovementType.EXIT, reason, performedBy, null);
    }

    // Fábrica para AJUSTAR stock directo (stock absoluto)
    public static InventoryMovement adjustment(Product product, int newAbsoluteStock, String reason, String performedBy) {
        return new InventoryMovement(product, newAbsoluteStock, MovementType.ADJUSTMENT, reason, performedBy, null);
    }

    // --------------------------------------------------------------
    // Constructor usado cuando se carga el movimiento desde BD
    // --------------------------------------------------------------
    public InventoryMovement(int id, Product product, int quantity, MovementType type,
                             String reason, String performedBy, String reference, LocalDateTime createdAt) {

        this(product, quantity, type, reason, performedBy, reference); // usa el constructor privado
        this.id = id; // asigna el ID real desde la BD

        // Nota: createdAt es final → no se puede reemplazar por createdAt de BD
    }

    // --------------------------------------------------------------
    // Validación de cantidades según tipo de movimiento
    // --------------------------------------------------------------
    private void validateQuantity(int qty, MovementType type) {
        switch (type) {
            case ENTRY:
            case EXIT:
                // Para ENTRADA y SALIDA la cantidad debe ser > 0
                if (qty <= 0) throw new IllegalArgumentException("quantity debe ser > 0 para ENTRY/EXIT");
                break;

            case ADJUSTMENT:
                // Para AJUSTE el stock nuevo debe ser >= 0
                if (qty < 0) throw new IllegalArgumentException("quantity (nuevo stock) debe ser >= 0 para ADJUSTMENT");
                break;
        }
    }

    // --------------------------------------------------------------
    // Aplica el movimiento al producto real → modifica el stock
    // --------------------------------------------------------------
    public void applyToProduct() {

        if (product == null)
            throw new IllegalStateException("No hay producto asociado al movimiento");

        int current = product.getStock(); // stock actual
        this.previousStock = current;     // guarda el stock previo

        switch (type) {
            case ENTRY:
                // Suma al stock actual (con protección overflow)
                resultingStock = safeAdd(current, quantity);
                break;

            case EXIT:
                // Resta al stock pero nunca baja de 0
                resultingStock = Math.max(0, current - quantity);
                break;

            case ADJUSTMENT:
                // Stock asignado directo
                resultingStock = quantity;
                break;

            default:
                throw new IllegalStateException("Tipo de movimiento no soportado: " + type);
        }

        // Aplica el nuevo stock al producto
        product.setStock(resultingStock);
    }

    // Suma segura para evitar overflow de Integer
    private int safeAdd(int a, int b) {
        long r = (long) a + (long) b;
        if (r > Integer.MAX_VALUE) throw new ArithmeticException("Overflow de stock");
        return (int) r;
    }

    // --------------------------------------------------------------
    // GETTERS y SETTERS
    // --------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) {
        this.performedBy = (performedBy == null || performedBy.isBlank()) ? "system" : performedBy.trim();
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) {
        this.product = Objects.requireNonNull(product, "product no puede ser null");
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        validateQuantity(quantity, this.type); // valida de nuevo según tipo
        this.quantity = quantity;
    }

    public MovementType getType() { return type; }
    public void setType(MovementType type) {
        this.type = Objects.requireNonNull(type, "type no puede ser null");
        validateQuantity(this.quantity, type); // revalida cantidad
    }

    public String getReason() { return reason; }
    public void setReason(String reason) {
        this.reason = (reason == null || reason.isBlank()) ? "-" : reason.trim();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getPreviousStock() { return previousStock; }
    public Integer getResultingStock() { return resultingStock; }

    // --------------------------------------------------------------
    // Representación en texto del movimiento (útil en consola/log)
    // --------------------------------------------------------------
    @Override
    public String toString() {

        String base = String.format(
            "[%s] %s | Prod: %s (id:%s) | qty:%d | reason:%s | by:%s",
            createdAt, type,
            (product != null ? product.getName() : "null"),
            (product != null ? product.getId() : "null"),
            quantity, reason, performedBy
        );

        // Si tiene stock previo y final, agrégalo al texto
        if (previousStock != null && resultingStock != null) {
            base += String.format(" | stock: %d -> %d", previousStock, resultingStock);
        }

        if (reference != null) base += " | ref:" + reference;
        if (id > 0) base += " | id:" + id;

        return base;
    }

    // --------------------------------------------------------------
    // equals() → reglas para comparar dos movimientos
    // --------------------------------------------------------------
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof InventoryMovement)) return false;

        InventoryMovement that = (InventoryMovement) o;

        // Si ambos tienen ID asignado por BD → se comparan solo por ID
        if (this.id > 0 && that.id > 0) return this.id == that.id;

        // Si no tienen ID (objetos temporales) → compara atributos clave
        return Objects.equals(product, that.product)
                && type == that.type
                && quantity == that.quantity
                && Objects.equals(createdAt, that.createdAt);
    }

    // --------------------------------------------------------------
    // hashCode() consistente con equals()
    // --------------------------------------------------------------
    @Override
    public int hashCode() {
        return (id > 0)
                ? Objects.hash(id)
                : Objects.hash(product, type, quantity, createdAt);
    }
}

