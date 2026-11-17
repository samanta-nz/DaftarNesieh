package ir.daftarnesieh.app;
public final class StatementSms {
  private StatementSms(){}
  public static String compose(Store.Statement s,String shop,String occupation,boolean detailed){
    StringBuilder b=new StringBuilder();
    b.append(occupation.trim()).append(" | ").append(shop.trim()).append('\n');
    b.append(detailed?"ریز صورتحساب نسیه":"خلاصه صورتحساب نسیه").append('\n');
    b.append("مشتری: ").append(s.customer.name()).append('\n');
    if(s.from==Long.MIN_VALUE) b.append("بازه: از ابتدا تا ").append(Shamsi.dateTime(Math.min(System.currentTimeMillis(),s.to-1))).append('\n');
    else b.append("بازه: ").append(Shamsi.day(s.from)).append(" تا ").append(Shamsi.day(s.to-1)).append('\n');
    b.append("مانده قبلی: ").append(Money.balanceFull(s.report.opening)).append('\n');
    if(detailed){
      b.append("────────────").append('\n');
      long running=s.report.opening;
      for(Store.Movement e:s.movements){
        b.append(Shamsi.dateTime(e.timestamp)).append('\n');
        b.append(LedgerLogic.DEBT.equals(e.kind)?"نسیه: "+e.items:"پرداخت: "+e.method);
        b.append(" | ").append(Money.full(e.amount)).append('\n');
        running=LedgerLogic.sum(running,LedgerLogic.signed(e.kind,e.amount));
        b.append("مانده: ").append(Money.balanceFull(running)).append('\n');
      }
      if(s.movements.isEmpty()) b.append("گردشی در این بازه ثبت نشده است.\n");
      b.append("────────────").append('\n');
    }
    b.append("نسیه جدید: ").append(Money.full(s.report.debt)).append('\n');
    b.append("پرداخت‌ها: ").append(Money.full(s.report.receipts)).append('\n');
    b.append("مانده پایان بازه: ").append(Money.balanceFull(s.report.closing));
    return b.toString();
  }
}
