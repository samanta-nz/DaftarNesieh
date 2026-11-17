package ir.daftarnesieh.app;
import java.text.NumberFormat;
import java.util.Locale;
public final class Money {
  private Money(){}
  public static String format(long value){return NumberFormat.getIntegerInstance(new Locale("fa","IR")).format(value)+" تومان";}
  public static String balance(long amount){
    if(amount==0)return "تسویه‌شده";
    return (amount>0?"بدهکار: ":"بستانکار: ")+format(Math.abs(amount));
  }
  public static long parse(String typed){
    if(typed==null)throw new IllegalArgumentException("مبلغ را وارد کنید");
    StringBuilder s=new StringBuilder();
    for(char ch:typed.trim().toCharArray()){
      if(ch>='0'&&ch<='9')s.append(ch);
      else if(ch>='۰'&&ch<='۹')s.append((char)('0'+ch-'۰'));
      else if(ch>='٠'&&ch<='٩')s.append((char)('0'+ch-'٠'));
      else if(ch==','||ch=='٬'||ch==' '||ch=='\u200c')continue;
      else throw new IllegalArgumentException("فقط مبلغ عددی به تومان وارد کنید");
    }
    try{long n=Long.parseLong(s.toString());LedgerLogic.signed(LedgerLogic.DEBT,n);return n;}
    catch(NumberFormatException e){throw new IllegalArgumentException("مبلغ نامعتبر است");}
  }
}
