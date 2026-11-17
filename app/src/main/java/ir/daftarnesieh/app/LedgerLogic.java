package ir.daftarnesieh.app;
import java.util.List;
/** Integer toman ledger; chronological transactions and exclusive end dates. */
public final class LedgerLogic {
  public static final String DEBT="DEBT", PAYMENT="PAYMENT";
  private LedgerLogic(){}
  public static long signed(String kind,long amount){
    if(amount<=0 || amount>9_000_000_000_000_000L) throw new IllegalArgumentException("مبلغ نامعتبر است");
    if(DEBT.equals(kind)) return amount;
    if(PAYMENT.equals(kind)) return -amount;
    throw new IllegalArgumentException("نوع تراکنش نامعتبر است");
  }
  public static long sum(long a,long b){return Math.addExact(a,b);}
  public static long collectible(long balance){return Math.max(0,balance);}
  public static long appliedPayment(long balance,long amount){return Math.min(Math.max(balance,0),amount);}
  public static final class Entry {
    public final long timestamp,amount; public final String kind;
    public Entry(long t,String k,long a){timestamp=t;kind=k;amount=a;}
  }
  public static final class Report {public long opening,debt,receipts,collected,closing;}
  public static Report report(List<Entry> ordered,long from,long until){
    if(until<=from) throw new IllegalArgumentException("بازه نامعتبر است");
    Report r=new Report();long balance=0;
    for(Entry e:ordered){
      if(e.timestamp>=until) break;
      if(e.timestamp<from) r.opening=sum(r.opening,signed(e.kind,e.amount));
      else if(DEBT.equals(e.kind)) r.debt=sum(r.debt,e.amount);
      else if(PAYMENT.equals(e.kind)){
        r.receipts=sum(r.receipts,e.amount);
        r.collected=sum(r.collected,appliedPayment(balance,e.amount));
      }else throw new IllegalArgumentException("تراکنش نامعتبر");
      balance=sum(balance,signed(e.kind,e.amount));
    }
    r.closing=balance;return r;
  }
}
