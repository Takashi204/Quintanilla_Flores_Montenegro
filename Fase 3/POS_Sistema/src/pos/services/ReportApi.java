package pos.services;

import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONArray;
import pos.util.ApiClient;

public class ReportApi {

    public static JSONObject getDashboard() {
        try {
            Response res = ApiClient.get("/v1/reports/dashboard");
            String raw = res.body().string();

            if (!res.isSuccessful()) {
                System.err.println("❌ Error dashboard: " + raw);
                return null;
            }

            return new JSONObject(raw);

        } catch (Exception e) {
            System.err.println("❌ getDashboard: " + e.getMessage());
            return null;
        }
    }

    public static JSONArray getSalesRange(String desde, String hasta) {
        try {
            Response res = ApiClient.get("/v1/reports/sales-range?date_from=" + desde + "&date_to=" + hasta);
            String raw = res.body().string();

            if (!res.isSuccessful()) {
                System.err.println("❌ Error sales-range: " + raw);
                return new JSONArray();
            }

            return new JSONArray(raw);

        } catch (Exception e) {
            System.err.println("❌ getSalesRange: " + e.getMessage());
            return new JSONArray();
        }
    }

    public static JSONArray getCashMovements(String desde, String hasta) {
        try {
            Response res = ApiClient.get("/v1/reports/cash-movements?date_from=" + desde + "&date_to=" + hasta);
            String raw = res.body().string();

            if (!res.isSuccessful()) {
                System.err.println("❌ Error cash-movements: " + raw);
                return new JSONArray();
            }

            return new JSONArray(raw);

        } catch (Exception e) {
            System.err.println("❌ getCashMovements: " + e.getMessage());
            return new JSONArray();
        }
    }
}
