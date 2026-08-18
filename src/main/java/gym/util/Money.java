package gym.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money(){}
    public static long parseCents(String text){
        if(text==null || text.isBlank()) throw new IllegalArgumentException("Amount is required.");
        try { BigDecimal v=new BigDecimal(text.trim()).setScale(2,RoundingMode.HALF_UP); if(v.compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("Amount must be greater than 0."); return v.movePointRight(2).longValueExact(); }
        catch(NumberFormatException|ArithmeticException e){ throw new IllegalArgumentException("Enter a valid amount, e.g. 2500 or 2500.50."); }
    }
    public static String format(long cents){return "৳ " + BigDecimal.valueOf(cents,2).setScale(2,RoundingMode.HALF_UP).toPlainString();}
}
