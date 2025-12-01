package pos.services;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import pos.model.Sale;
import pos.model.SaleItem;
import pos.util.ApiClient;

public class SaleService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public boolean enviarVenta(Sale venta) throws Exception {

        JSONObject body = new JSONObject();

        // === CAMPOS REQUERIDOS POR API ===
        body.put("session_id", venta.getSession_id());
        body.put("payment_method", venta.getPayment_method());
        body.put("status", "SALE");

        body.put("customer_name",
                venta.getCustomer_name() == null ? "" : venta.getCustomer_name());

        body.put("customer_tax_id",
                venta.getCustomer_tax_id() == null ? "" : venta.getCustomer_tax_id());

        // === ITEMS ===
        JSONArray itemsArr = new JSONArray();
        for (SaleItem it : venta.getItems()) {
            JSONObject o = new JSONObject();
            o.put("product_id", it.getProduct_id());
            o.put("quantity", it.getQuantity());
            o.put("unit_price", it.getUnit_price());
            itemsArr.put(o);
        }

        body.put("items", itemsArr);

        // DEBUG
        System.out.println("JSON enviado a API:");
        System.out.println(body.toString(4));

        // === POST ===
        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.post("/v1/sales", req);

        try {
            if (!res.isSuccessful()) {

                String errorBody = res.body() != null ? res.body().string() : "sin cuerpo";

                System.out.println("❌ ERROR DETALLE API:");
                System.out.println(errorBody);

                throw new Exception(
                        "Error API al crear venta: HTTP " + res.code() + " → " + res.message()
                );
            }

            return true;

        } finally {
            if (res != null) res.close();
        }
    }
}