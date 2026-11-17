package ir.daftarnesieh.app;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

public final class LedgerUnitTest {
  @Test public void groupsPersianDigitsWhileTyping(){
    assertEquals("۱٬۲۳۴٬۵۶۷",Money.groupDigits("1234567"));
    assertEquals("۱۲۳٬۴۵۶",Money.groupDigits("۱۲۳۴۵۶"));
    assertEquals("۱۲۳٬۴۵۶",Money.groupDigits("١٢٣٤٥٦"));
    assertEquals("۰",Money.groupDigits("0000"));
    assertEquals("",Money.groupDigits(""));
  }
  @Test public void amountsHaveCorrectPersianWords(){
    assertEquals("صفر",Money.words(0));
    assertEquals("دوازده",Money.words(12));
    assertEquals("صد و یک",Money.words(101));
    assertEquals("صد و بیست و سه هزار و چهارصد و پنجاه و شش",Money.words(123456));
    assertEquals("یک میلیون و دو هزار و سه",Money.words(1_002_003));
    assertEquals("هفت میلیارد",Money.words(7_000_000_000L));
    assertEquals("۱٬۲۳۴٬۵۶۷ تومان",Money.format(1_234_567L));
  }
  @Test public void parsesGroupedAmount(){
    assertEquals(1_234_567L,Money.parse("۱٬۲۳۴٬۵۶۷"));
    assertEquals(21000L,Money.parse("21,000"));
  }
  @Test public void statementCarriesOpeningBalance(){
    LedgerLogic.Report r=LedgerLogic.report(Arrays.asList(
      new LedgerLogic.Entry(1000,LedgerLogic.DEBT,1_000_000),
      new LedgerLogic.Entry(2000,LedgerLogic.PAYMENT,250_000),
      new LedgerLogic.Entry(3000,LedgerLogic.DEBT,100_000),
      new LedgerLogic.Entry(4000,LedgerLogic.PAYMENT,200_000)),2500,5000);
    assertEquals(750_000,r.opening);
    assertEquals(100_000,r.debt);
    assertEquals(200_000,r.receipts);
    assertEquals(200_000,r.collected);
    assertEquals(650_000,r.closing);
  }
}
