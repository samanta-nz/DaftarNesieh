package ir.daftarnesieh.app;
import java.text.NumberFormat;
import java.util.Locale;
public final class Money {
  private Money(){}
  public static String format(long value){return (value<0?"−":"")+groupDigits(Long.toString(Math.abs(value)))+" تومان";}
  public static String balance(long amount){
    if(amount==0)return "تسویه‌شده";
    return (amount>0?"بدهکار: ":"بستانکار: ")+format(Math.abs(amount));
  }

  private static final String[] ONES={"","یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه"};
  private static final String[] TEENS={"ده","یازده","دوازده","سیزده","چهارده","پانزده","شانزده","هفده","هجده","نوزده"};
  private static final String[] TENS={"","","بیست","سی","چهل","پنجاه","شصت","هفتاد","هشتاد","نود"};
  private static final String[] HUNDREDS={"","صد","دویست","سیصد","چهارصد","پانصد","ششصد","هفتصد","هشتصد","نهصد"};
  private static final String[] GROUPS={"","هزار","میلیون","میلیارد","تریلیون","کوادریلیون"};
  private static String block(int n){
    java.util.ArrayList<String> parts=new java.util.ArrayList<>();
    if(n>=100){parts.add(HUNDREDS[n/100]);n%=100;}
    if(n>=20){parts.add(TENS[n/10]);n%=10;}
    if(n>=10){parts.add(TEENS[n-10]);n=0;}
    if(n>0)parts.add(ONES[n]);
    return String.join(" و ",parts);
  }
  public static String words(long n){
    if(n==Long.MIN_VALUE)throw new IllegalArgumentException("مبلغ نامعتبر");
    if(n==0)return "صفر";
    boolean neg=n<0; n=Math.abs(n);
    java.util.ArrayList<String> parts=new java.util.ArrayList<>();
    int i=0;
    while(n>0){
      int b=(int)(n%1000);
      if(b>0)parts.add(0,block(b)+(i==0?"":" "+GROUPS[i]));
      n/=1000; i++;
    }
    return (neg?"منفی ":"")+String.join(" و ",parts);
  }
  public static String inWords(long n){return words(n)+" تومان";}
  public static String full(long n){return format(n)+" ("+inWords(n)+")";}
  public static String balanceFull(long n){return balance(n)+"\n"+inWords(Math.abs(n));}
  public static String groupDigits(String source){
    StringBuilder only=new StringBuilder();
    if(source!=null)for(int i=0;i<source.length();i++){
      char c=source.charAt(i);
      if(c>='0'&&c<='9')only.append(c);
      else if(c>='۰'&&c<='۹')only.append((char)('0'+c-'۰'));
      else if(c>='٠'&&c<='٩')only.append((char)('0'+c-'٠'));
    }
    if(only.length()==0)return "";
    int start=0;
    while(start<only.length()-1&&only.charAt(start)=='0')start++;
    StringBuilder out=new StringBuilder();
    for(int i=start;i<only.length();i++){
      if(i>start&&(only.length()-i)%3==0)out.append('٬');
      out.append((char)('۰'+only.charAt(i)-'0'));
    }
    return out.toString();
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
