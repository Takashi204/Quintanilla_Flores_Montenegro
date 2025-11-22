package pos.model;

import java.time.LocalDateTime;
import java.util.List;

public class Sale {

    private final String id;
    private final String docType;
    private final LocalDateTime ts;
    private List<SaleItem> items;

    private String paymentMethod;

    private int payCash;
    private int payCard;
    private int payTransfer;

    private int neto;
    private int iva;
    private int total;

    private String customerId;
    private final String user;

    // ================================
    //        CONSTRUCTOR PRINCIPAL
    // ================================
    public Sale(String id, String docType, LocalDateTime ts, List<SaleItem> items,
                String paymentMethod, int payCash, int payCard, int payTransfer,
                int neto, int iva, int total, String customerId, String user) {

        this.id = id;
        this.docType = docType;
        this.ts = ts;
        this.items = items;

        this.paymentMethod = paymentMethod;

        this.payCash = payCash;
        this.payCard = payCard;
        this.payTransfer = payTransfer;

        this.neto = neto;
        this.iva = iva;
        this.total = total;

        this.customerId = customerId;
        this.user = user;
    }

    // ================================
    //   CONSTRUCTOR COMPATIBLE ANTIGUO
    // ================================
    public Sale(String id, String docType, LocalDateTime ts, List<SaleItem> items,
                String paymentMethod, int payCash, int payCard, int payTransfer,
                int neto, int iva, int total, String customerId) {

        this(id, docType, ts, items, paymentMethod, payCash, payCard, payTransfer,
                neto, iva, total, customerId, "admin");
    }

    // ================================
    //             GETTERS
    // ================================
    public String getId() { return id; }
    public String getDocType() { return docType; }
    public LocalDateTime getTs() { return ts; }

    public String getPaymentMethod() { return paymentMethod; }
    public int getPayCash() { return payCash; }
    public int getPayCard() { return payCard; }
    public int getPayTransfer() { return payTransfer; }

    public int getNeto() { return neto; }
    public int getIva() { return iva; }
    public int getTotal() { return total; }

    public List<SaleItem> getItems() { return items; }
    public String getCustomerId() { return customerId; }
    public String getUser() { return user; }

    // ================================
    //             SETTERS
    // ================================
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPayCash(int payCash) { this.payCash = payCash; }
    public void setPayCard(int payCard) { this.payCard = payCard; }
    public void setPayTransfer(int payTransfer) { this.payTransfer = payTransfer; }

    public void setItems(List<SaleItem> items) { this.items = items; }
    public void setTotal(int total) { this.total = total; }
    public void setNeto(int neto) { this.neto = neto; }
    public void setIva(int iva) { this.iva = iva; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}
