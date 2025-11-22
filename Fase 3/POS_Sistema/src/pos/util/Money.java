package pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {

    private static final Locale CLP_LOCALE = new Locale("es", "CL");
    private static final DecimalFormatSymbols DFS;
    private static final DecimalFormat CLP_FMT;

    static {
        DFS = new DecimalFormatSymbols(CLP_LOCALE);
        DFS.setDecimalSeparator(',');
        DFS.setGroupingSeparator('.');
        CLP_FMT = new DecimalFormat("$#,##0", DFS);
        CLP_FMT.setParseBigDecimal(true);
    }

    private Money(){}

    /** Formatea entero CLP a “$1.234.567”. */
    public static String format(int amount) {
        return CLP_FMT.format(amount);
    }

    /** Parsea texto tipo “$1.234” → 1234. Si falla, 0. */
    public static int parseOrZero(String raw) {
        if (raw == null) return 0;
        String clean = raw.trim()
                .replace("$","")
                .replace(".", "")
                .replace(" ", "")
                .replace("\u00A0","")
                .replace(",", "");
        if (clean.isEmpty()) return 0;
        try {
            BigDecimal bd = new BigDecimal(clean);
            return bd.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (Exception e) {
            return 0;
        }
    }

    public static int safeAdd(int a, int b) {
        long s = (long)a + (long)b;
        if (s > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (s < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int)s;
    }

    public static int safeMul(int a, int b) {
        long m = (long)a * (long)b;
        if (m > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (m < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int)m;
    }

    public static int toInt(BigDecimal bd) {
        if (bd == null) return 0;
        return bd.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
