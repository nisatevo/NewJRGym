package gym.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern NAME=Pattern.compile("[A-Za-z][A-Za-z .'-]{1,59}");
    private static final Pattern PHONE=Pattern.compile("(?:\\+8801|01)[3-9]\\d{8}");
    private static final Pattern EMAIL=Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final DateTimeFormatter DATE=DateTimeFormatter.ISO_LOCAL_DATE;
    private Validation(){}
    public static String required(String s,String label){if(s==null||s.trim().isEmpty())throw new IllegalArgumentException(label+" is required.");return s.trim();}
    public static String name(String s,String label){s=required(s,label);if(!NAME.matcher(s).matches())throw new IllegalArgumentException(label+" must contain 2-60 letters and may include spaces, dots, apostrophes or hyphens.");return s;}
    public static int age(String s){try{int v=Integer.parseInt(required(s,"Age"));if(v<13||v>100)throw new IllegalArgumentException("Age must be between 13 and 100.");return v;}catch(NumberFormatException e){throw new IllegalArgumentException("Age must be a whole number.");}}
    public static String phone(String s){s=required(s,"Phone").replace(" ","");if(!PHONE.matcher(s).matches())throw new IllegalArgumentException("Phone must be a valid Bangladesh mobile number, e.g. 01712345678.");return s;}
    public static String email(String s){s=required(s,"Email");if(!EMAIL.matcher(s).matches())throw new IllegalArgumentException("Enter a valid email address.");return s;}
    public static LocalDate date(String s,String label){try{return LocalDate.parse(required(s,label),DATE);}catch(DateTimeParseException e){throw new IllegalArgumentException(label+" must use YYYY-MM-DD.");}}
    public static String address(String s){s=required(s,"Address");if(s.length()<5||s.length()>150)throw new IllegalArgumentException("Address must be 5-150 characters.");return s;}
}
