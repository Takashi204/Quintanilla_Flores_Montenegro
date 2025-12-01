package pos.model;

import java.time.LocalDateTime;
import java.util.List;

public class Sale {

    // ===== CAMPOS QUE USA LA API (FastAPI) =====
    private int session_id;                 // API requiere sesión de caja
    private String payment_method;          // "cash", "card", "transfer"
    private String status = "SALE";         // siempre "SALE"
    public String customer_name = null;     // nombre cliente
    public String customer_tax_id = null;   // rut cliente
    private List<SaleItem> items;           // items en formato API

    // ===== CAMPOS INTERNOS DEL POS =====
    private final String id;
    private final String docType;
    private final LocalDateTime ts;

    private String paymentMethod;
    private int payCash;
    private int payCard;
    private int payTransfer;
    private int neto;
    private int iva;
    private int total;
    private String customerId;
    private final String user;


    // ======================================================
    // 1) 👉 CONSTRUCTOR PARA API (SE USA AL COBRAR)
    // ======================================================
    public Sale(int sessionId, String paymentMethod, List<SaleItem> items) {
        this.id = null;
        this.docType = null;
        this.ts = LocalDateTime.now();

        // API
        this.session_id = sessionId;
        this.payment_method = paymentMethod.toLowerCase();
        this.items = items;

        // POS (solo para evitar null)
        this.paymentMethod = paymentMethod;
        this.total = 0;
        this.customerId = null;
        this.user = null;
    }


    // ======================================================
    // 2) 👉 CONSTRUCTOR ORIGINAL DEL POS (NO TOCAR)
    // ======================================================
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

        this.payment_method = paymentMethod.toLowerCase();
    }


    // ======================================================
    // 3) 👉 FACTORY METHOD: Construir venta desde POS (API READY)
    // ======================================================
    public static Sale fromPOS(List<?> itemsPOS, String docType, String paymentMethod, String customerRut) {

        List<SaleItem> apiItems = new java.util.ArrayList<>();

        for (Object o : itemsPOS) {
            pos.ui.views.VentasPanel.SaleLine line =
                    (pos.ui.views.VentasPanel.SaleLine) o;

            SaleItem si = new SaleItem(
                    line.getProduct(),
                    line.getQty()
            );

            apiItems.add(si);
        }

        // 🚫 NO poner un session_id aquí (se agrega después en VentasPanel)
        Sale sale = new Sale(
                0,                // TEMPORAL
                paymentMethod,
                apiItems
        );

        if ("Factura".equalsIgnoreCase(docType)) {
            sale.customer_name = customerRut;
            sale.customer_tax_id = customerRut;
        }

        return sale;
    }


    // ===== GETTERS API =====
    public int getSession_id() { return session_id; }
    public String getPayment_method() { return payment_method; }
    public String getStatus() { return status; }
    public String getCustomer_name() { return customer_name; }
    public String getCustomer_tax_id() { return customer_tax_id; }
    public List<SaleItem> getItems() { return items; }

    // ===== GETTERS POS =====
    public int getTotal() { return total; }
    public String getId() { return id; }
    public String getDocType() { return docType; }
    public LocalDateTime getTs() { return ts; }
    public String getPaymentMethod() { return paymentMethod; }
    public int getPayCash() { return payCash; }
    public int getPayCard() { return payCard; }
    public int getPayTransfer() { return payTransfer; }
    public int getNeto() { return neto; }
    public int getIva() { return iva; }
    public String getCustomerId() { return customerId; }
    public String getUser() { return user; }

    // ===== SETTERS API =====
    public void setSession_id(int session_id) { this.session_id = session_id; }
    public void setItems(List<SaleItem> items) { this.items = items; }

}
