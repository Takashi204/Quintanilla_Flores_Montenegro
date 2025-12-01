package pos.services;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import pos.model.Product;
import pos.util.ApiClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // =========================================================
    // 1) Obtener todos los productos
    // =========================================================
    public static List<Product> getAll() throws Exception {

        Response res = ApiClient.get("/v1/inventory/products");

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API: " + res.code());
        }

        String raw = res.body().string();
        res.close();

        JSONArray arr = new JSONArray(raw);
        List<Product> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {

            JSONObject o = arr.getJSONObject(i);

            int price = o.isNull("sale_price") ? 0 : (int) Math.round(o.getDouble("sale_price"));
            int stock = o.isNull("stock") ? 0 : o.getInt("stock");
            String desc = o.isNull("description") ? "" : o.getString("description");

            // Expiry date (API)
            String expiryStr = o.isNull("expiry_date") ? null : o.getString("expiry_date");
            LocalDate expiry = (expiryStr == null) ? null : LocalDate.parse(expiryStr);

            Product p = new Product(
                    o.getInt("id"),
                    o.getString("code"),
                    o.getString("name"),
                    desc,
                    price,
                    stock,
                    expiry
            );

            p.setActive(o.getBoolean("is_active"));
            list.add(p);
        }

        return list;
    }


    // =========================================================
    // 1.1) Obtener producto por código
    // =========================================================
    public Product getByCode(String code) throws Exception {

        Response res = ApiClient.get("/v1/inventory/products?code=" + code);

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API buscando por código: " + res.code());
        }

        String raw = res.body().string();
        res.close();

        JSONArray arr = new JSONArray(raw);
        if (arr.length() == 0) return null;

        JSONObject o = arr.getJSONObject(0);

        int price = o.isNull("sale_price") ? 0 : (int) Math.round(o.getDouble("sale_price"));
        int stock = o.isNull("stock") ? 0 : o.getInt("stock");
        String desc = o.isNull("description") ? "" : o.getString("description");

        String expiryStr = o.isNull("expiry_date") ? null : o.getString("expiry_date");
        LocalDate expiry = (expiryStr == null) ? null : LocalDate.parse(expiryStr);

        Product p = new Product(
                o.getInt("id"),
                o.getString("code"),
                o.getString("name"),
                desc,
                price,
                stock,
                expiry
        );

        p.setActive(o.getBoolean("is_active"));
        return p;
    }


    // =========================================================
    // 2) Crear producto
    // =========================================================
    public void createProduct(Product p) throws Exception {

        JSONObject body = new JSONObject();
        body.put("code", p.getCode());
        body.put("name", p.getName());
        body.put("sale_price", p.getPrice());
        body.put("cost_price", 0);
        body.put("stock", p.getStock());
        body.put("reorder_threshold", 0);
        body.put("expiry_date", p.getExpiry() == null ? JSONObject.NULL : p.getExpiry().toString());
        body.put("description", p.getCategory());
        body.put("is_active", p.isActive());

        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.post("/v1/inventory/products", req);

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API al crear producto: " + res.code());
        }

        res.close();
    }


    // =========================================================
    // 3) Actualizar producto
    // =========================================================
    public void updateProduct(Product p) throws Exception {

        JSONObject body = new JSONObject();
        body.put("name", p.getName());
        body.put("sale_price", p.getPrice());
        body.put("cost_price", 0);
        body.put("stock", p.getStock());
        body.put("reorder_threshold", 0);
        body.put("expiry_date", p.getExpiry() == null ? JSONObject.NULL : p.getExpiry().toString());
        body.put("description", p.getCategory());
        body.put("is_active", p.isActive());

        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.put("/v1/inventory/products/" + p.getId(), req);

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API al actualizar producto: " + res.code());
        }

        res.close();
    }


    // =========================================================
    // 4) Entrada de stock
    // =========================================================
    public void addStockAPI(int productId, int qty, String reason) throws Exception {

        JSONObject body = new JSONObject();
        body.put("product_id", productId);
        body.put("qty", qty);
        body.put("reason", reason);

        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.post("/v1/inventory/stock/add", req);

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API al sumar stock: " + res.code());
        }

        res.close();
    }


    // =========================================================
    // 5) Salida de stock
    // =========================================================
    public void removeStockAPI(int productId, int qty, String reason) throws Exception {

        JSONObject body = new JSONObject();
        body.put("product_id", productId);
        body.put("qty", qty);
        body.put("reason", reason);

        RequestBody req = RequestBody.create(JSON, body.toString());
        Response res = ApiClient.post("/v1/inventory/stock/remove", req);

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API al restar stock: " + res.code());
        }

        res.close();
    }


    // =========================================================
    // 6) Eliminar producto
    // =========================================================
    public void deleteProduct(int productId) throws Exception {

        Response res = ApiClient.delete("/v1/inventory/products/" + productId);

        if (!res.isSuccessful()) {
            res.close();
            throw new Exception("Error API al eliminar producto: " + res.code());
        }

        res.close();
    }
}