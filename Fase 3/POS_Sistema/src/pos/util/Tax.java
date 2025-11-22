package pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Utilidad de IVA (Chile) con 19% y resultados enteros (CLP). */
public final class Tax {

    /** 19% IVA Chile */
    public static final BigDecimal IVA_RATE = new BigDecimal("0.19");

    private Tax(){}

    /** IVA desde neto (ej: neto 1000 -> 190). */
    public static int ivaFromNeto(int neto) {
        BigDecimal n = new BigDecimal(neto);
        BigDecimal iva = n.multiply(IVA_RATE).setScale(0, RoundingMode.HALF_UP);
        return Money.toInt(iva);
    }

    /** Total desde neto (neto + IVA). */
    public static int totalFromNeto(int neto) {
        return neto + ivaFromNeto(neto);
    }

    /** Neto desde total (divide por 1.19, redondeo comercial). */
    public static int netoFromTotal(int total) {
        BigDecimal t = new BigDecimal(total);
        BigDecimal factor = BigDecimal.ONE.add(IVA_RATE); // 1.19
        return Money.toInt(t.divide(factor, 0, RoundingMode.HALF_UP));
    }

    /** IVA desde total (total - neto). */
    public static int ivaFromTotal(int total) {
        int neto = netoFromTotal(total);
        return total - neto;
    }
}
