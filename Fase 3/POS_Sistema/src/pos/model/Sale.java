package pos.model; // paquete donde vive el modelo Sale

import java.time.LocalDateTime; // para fecha/hora de la venta
import java.util.List;          // lista de items vendidos

public class Sale {

    // ================================
    // Atributos principales de la venta
    // ================================

    private final String id;           // ID único de la venta (boleta/factura)
    private final String docType;      // Tipo de documento (BOLETA, FACTURA, etc.)
    private final LocalDateTime ts;    // Timestamp exacto de la venta
    private List<SaleItem> items;      // Lista de productos vendidos

    private String paymentMethod;      // Método principal de pago (efectivo, tarjeta, etc.)

    // Montos por cada forma de pago (si se paga mixto)
    private int payCash;               // pago en efectivo
    private int payCard;               // pago con tarjeta
    private int payTransfer;           // pago por transferencia

    // Totales calculados
    private int neto;                  // monto neto
    private int iva;                   // impuesto
    private int total;                 // total final

    private String customerId;         // opcional: rut / id cliente
    private final String user;         // usuario/cajero que realizó la venta

    // ================================
    //        CONSTRUCTOR PRINCIPAL
    // ================================
    public Sale(String id, String docType, LocalDateTime ts, List<SaleItem> items,
                String paymentMethod, int payCash, int payCard, int payTransfer,
                int neto, int iva, int total, String customerId, String user) {

        this.id = id;                 // asignar ID
        this.docType = docType;       // tipo de documento
        this.ts = ts;                 // timestamp
        this.items = items;           // lista de items

        this.paymentMethod = paymentMethod; // método de pago seleccionado

        this.payCash = payCash;       // pago en efectivo
        this.payCard = payCard;       // pago con tarjeta
        this.payTransfer = payTransfer; // transferencia

        this.neto = neto;             // neto calculado
        this.iva = iva;               // iva calculado
        this.total = total;           // total final

        this.customerId = customerId; // id del cliente (si corresponde)
        this.user = user;             // usuario que ejecutó la venta
    }

    // ================================
    //   CONSTRUCTOR COMPATIBLE ANTIGUO
    // ================================
    // Para compatibilidad con versiones anteriores donde no se pasaba "user"
    public Sale(String id, String docType, LocalDateTime ts, List<SaleItem> items,
                String paymentMethod, int payCash, int payCard, int payTransfer,
                int neto, int iva, int total, String customerId) {

        // Llama al constructor principal, usando "admin" como usuario por defecto
        this(id, docType, ts, items, paymentMethod, payCash, payCard, payTransfer,
                neto, iva, total, customerId, "admin");
    }

    // ================================
    //             GETTERS
    // ================================

    public String getId() { return id; }                 // devuelve ID venta
    public String getDocType() { return docType; }       // devuelve tipo doc
    public LocalDateTime getTs() { return ts; }          // devuelve fecha/hora

    public String getPaymentMethod() { return paymentMethod; }     // método de pago
    public int getPayCash() { return payCash; }                     // efectivo
    public int getPayCard() { return payCard; }                     // tarjeta
    public int getPayTransfer() { return payTransfer; }             // transferencia

    public int getNeto() { return neto; }                 // neto
    public int getIva() { return iva; }                   // iva
    public int getTotal() { return total; }               // total final

    public List<SaleItem> getItems() { return items; }    // items vendidos
    public String getCustomerId() { return customerId; }  // id cliente
    public String getUser() { return user; }              // usuario/cajero

    // ================================
    //             SETTERS
    // ================================

    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; } // método pago
    public void setPayCash(int payCash) { this.payCash = payCash; }                           // efectivo
    public void setPayCard(int payCard) { this.payCard = payCard; }                           // tarjeta
    public void setPayTransfer(int payTransfer) { this.payTransfer = payTransfer; }           // transferencia

    public void setItems(List<SaleItem> items) { this.items = items; } // actualizar items
    public void setTotal(int total) { this.total = total; }             // total
    public void setNeto(int neto) { this.neto = neto; }                 // neto
    public void setIva(int iva) { this.iva = iva; }                     // iva
    public void setCustomerId(String customerId) { this.customerId = customerId; } // id cliente
}
