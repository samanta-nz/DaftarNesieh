package ir.daftarnesieh.app;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.PersianCalendar;
import android.icu.util.ULocale;
import java.util.Date;
public final class Shamsi {
  private Shamsi(){}
  public static PersianCalendar calendar(){return new PersianCalendar();}
  private static String fmt(long epoch,String pattern){
    SimpleDateFormat format=new SimpleDateFormat(pattern,new ULocale("fa_IR@calendar=persian"));
    format.setCalendar(calendar());return format.format(new Date(epoch));
  }
  public static String day(long t){return fmt(t,"yyyy/MM/dd");}
  public static String dateTime(long t){return fmt(t,"yyyy/MM/dd  HH:mm");}
  public static long monthsAgo(int n){
    PersianCalendar c=calendar();c.add(Calendar.MONTH,-n);
    c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
    return c.getTimeInMillis();
  }
  public static long dayStart(int y,int month1,int day){
    PersianCalendar c=calendar();c.clear();c.setLenient(false);
    c.set(Calendar.YEAR,y);c.set(Calendar.MONTH,month1-1);c.set(Calendar.DAY_OF_MONTH,day);
    c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
    return c.getTimeInMillis();
  }
  public static long nextDay(long start){PersianCalendar c=calendar();c.setTimeInMillis(start);c.add(Calendar.DAY_OF_MONTH,1);return c.getTimeInMillis();}
}
