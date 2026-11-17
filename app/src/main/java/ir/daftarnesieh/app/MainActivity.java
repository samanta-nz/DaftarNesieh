package ir.daftarnesieh.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.icu.util.Calendar;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Native Persian RTL UI, no network and no direct SMS permission. */
public final class MainActivity extends Activity {
  private static final int BACKUP_CREATE=101,BACKUP_OPEN=102;
  private static final int DASHBOARD=0,CUSTOMERS=1,DETAIL=2,SETTINGS=3,SETUP=4;
  private static final int[] ACCENTS={0xff087e70,0xff5944a8,0xff2255ae,0xffad531d,0xffa82b5f};
  private static final String[] ACCENT_NAMES={"فیروزه‌ای","بنفش","آبی","کهربایی","صورتی"};
  private Store store;
  private SharedPreferences prefs;
  private int page=DASHBOARD,period=0;
  private long customerId=-1;
  private Long customFrom=null,customUntil=null;
  private LinearLayout root,body;
  private int bg,surface,surface2,fg,muted,border,accent,positive,negative;
  private boolean dark;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    store=new Store(this);
    prefs=getSharedPreferences("shop",MODE_PRIVATE);
    page=prefs.getString("shopName","").trim().isEmpty()?SETUP:DASHBOARD;
    draw();
  }
  @Override public void onBackPressed(){
    if(page==DETAIL){page=CUSTOMERS;clearCustom();draw();}
    else if(page==CUSTOMERS||page==SETTINGS){page=DASHBOARD;draw();}
    else super.onBackPressed();
  }
  private int d(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private int fontStep(){return Math.max(0,Math.min(10,prefs.getInt("fontStep",0)));}
  private int fs(int base){return base+fontStep();}
  private void palette(){
    int n=getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    dark=n==Configuration.UI_MODE_NIGHT_YES;
    bg=dark?0xff101219:0xfff5f7fb;surface=dark?0xff1a2030:Color.WHITE;
    surface2=dark?0xff252d3e:0xffedf1f7;
    fg=dark?0xfff7f9fd:0xff1b2634;muted=dark?0xffa5b0c3:0xff667589;
    border=dark?0xff30394a:0xffdfe5ee;
    accent=ACCENTS[Math.max(0,Math.min(ACCENTS.length-1,prefs.getInt("theme",0)))];
    positive=dark?0xffff8a8a:0xffc34045;negative=dark?0xff76e3b0:0xff088464;
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    getWindow().getDecorView().setSystemUiVisibility(dark?0:
      View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }
  private GradientDrawable rounded(int color,int radius){
    GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;
  }
  private GradientDrawable outlined(int color,int radius){
    GradientDrawable g=rounded(color,radius);g.setStroke(d(1),border);return g;
  }
  private LinearLayout vertical(){
    LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
    l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;
  }
  private LinearLayout horizontal(){
    LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;
  }
  private LinearLayout.LayoutParams lp(int w,int h){
    return new LinearLayout.LayoutParams(w<0?w:d(w),h<0?h:d(h));
  }
  private void gap(LinearLayout parent,int h){parent.addView(new View(this),lp(1,h));}
  private TextView text(String s,int size,boolean bold,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(fs(size));t.setTextColor(color);
    t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;
  }
  private void title(String s){
    body.addView(text(s,21,true,fg),lp(-1,-2));gap(body,12);
  }
  private LinearLayout card(){
    LinearLayout c=vertical();c.setPadding(d(16),d(16),d(16),d(16));
    c.setBackground(outlined(surface,20));c.setElevation(d(1));return c;
  }
  private void place(LinearLayout parent,View v,int bottom){
    LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=d(bottom);parent.addView(v,p);
  }
  private Button button(String label,boolean filled,Runnable run){
    Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(fs(14));
    b.setMinHeight(d(48+fontStep()));b.setPadding(d(12),0,d(12),0);
    b.setTextColor(filled?Color.WHITE:accent);
    b.setBackground(filled?rounded(accent,16):outlined(surface,16));
    b.setOnClickListener(v->run.run());return b;
  }
  private void action(LinearLayout parent,String label,boolean filled,Runnable run){
    place(parent,button(label,filled,run),10);
  }
  private void clickRow(LinearLayout parent,String heading,String sub,Runnable run){
    LinearLayout c=card();c.addView(text(heading,16,true,fg));
    if(sub!=null&&!sub.isEmpty()){gap(c,5);c.addView(text(sub,12,false,muted));}
    c.setOnClickListener(v->run.run());place(parent,c,9);
  }
  private LinearLayout field(String name,String hint,int type){
    LinearLayout f=vertical();f.addView(text(name,13,true,muted));gap(f,5);
    EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(fs(16));
    e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(fg);e.setInputType(type);
    e.setPadding(d(12),d(9),d(12),d(9));e.setBackground(outlined(surface2,12));
    f.addView(e,lp(-1,49+fontStep()));return f;
  }
  private EditText edit(LinearLayout f){return (EditText)f.getChildAt(2);}
  private void draw(){
    palette();root=vertical();root.setBackgroundColor(bg);setContentView(root);
    ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);
    body=vertical();body.setPadding(d(18),d(22),d(18),d(24));sc.addView(body);
    root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
    try{
      if(page==SETUP)setup();
      else if(page==DASHBOARD)dashboard();
      else if(page==CUSTOMERS)customerList();
      else if(page==DETAIL)detail();
      else if(page==SETTINGS)settings();
    }catch(Exception e){error(e);}
    if(page!=SETUP)navigation();
  }
  private void navigation(){
    LinearLayout bar=horizontal();bar.setPadding(d(7),d(7),d(7),d(7));
    bar.setBackground(outlined(surface,18));
    String[] titles={"▦ داشبورد","♙ مشتریان","⚙ تنظیمات"};
    int[] pages={DASHBOARD,CUSTOMERS,SETTINGS};
    for(int i=0;i<pages.length;i++){
      final int target=pages[i];
      Button b=button(titles[i],page==target,()->{page=target;draw();});
      b.setTextSize(fs(12));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(47),1);
      p.setMargins(d(3),0,d(3),0);bar.addView(b,p);
    }
    LinearLayout.LayoutParams p=lp(-1,-2);
    p.setMargins(d(10),0,d(10),d(8));root.addView(bar,p);
  }
  private String shop(){return prefs.getString("shopName","");}
  private String occupation(){return prefs.getString("occupation","");}
  private void setup(){
    gap(body,23);
    TextView symbol=text("◈",55,true,accent);symbol.setGravity(Gravity.CENTER);body.addView(symbol);
    TextView name=text("دفتر نسیه",29,true,fg);name.setGravity(Gravity.CENTER);body.addView(name);
    TextView desc=text("مدیریت حساب نسیه مشتری‌های مغازه",14,false,muted);desc.setGravity(Gravity.CENTER);body.addView(desc);
    gap(body,25);
    LinearLayout c=card();place(body,c,0);
    LinearLayout s=field("نام مغازه","مثلاً سوپرمارکت بهار",InputType.TYPE_CLASS_TEXT);
    LinearLayout o=field("عنوان شغل","مثلاً سوپرمارکت",InputType.TYPE_CLASS_TEXT);
    c.addView(s);gap(c,17);c.addView(o);gap(c,20);
    action(c,"شروع و ذخیره اطلاعات",true,()->{
      String n=edit(s).getText().toString().trim(),work=edit(o).getText().toString().trim();
      if(n.isEmpty()||work.isEmpty()||n.length()>120||work.length()>120){
        notice("نام مغازه و شغل را وارد کنید (حداکثر ۱۲۰ حرف)");return;
      }
      prefs.edit().putString("shopName",n).putString("occupation",work).apply();
      page=DASHBOARD;draw();
    });
    gap(body,14);body.addView(text("نام و عنوان شغل در ابتدای پیامک صورتحساب درج می‌شود.",13,false,muted));
    gap(body,14);action(body,"بازیابی فایل بکاپ قبلی",false,this::selectRestore);
  }
  private long from(){return customFrom!=null?customFrom:(period==0?Long.MIN_VALUE:Shamsi.monthsAgo(period));}
  private long until(){return customUntil!=null?customUntil:(period==0?Long.MAX_VALUE:System.currentTimeMillis()+1);}
  private void clearCustom(){customFrom=null;customUntil=null;}
  private void filters(LinearLayout parent,boolean customAllowed){
    LinearLayout options=horizontal();
    String[] names={"همه","۱ ماه","۳ ماه","۶ ماه","۱ سال"};
    int[] periods={0,1,3,6,12};
    for(int i=0;i<periods.length;i++){
      final int v=periods[i];Button b=button(names[i],customFrom==null&&period==v,()->{
        period=v;clearCustom();draw();
      });
      b.setTextSize(fs(12));LinearLayout.LayoutParams p=lp(-2,39+fontStep());
      p.leftMargin=d(5);options.addView(b,p);
    }
    android.widget.HorizontalScrollView sc=new android.widget.HorizontalScrollView(this);
    sc.setHorizontalScrollBarEnabled(false);sc.addView(options);place(parent,sc,7);
    if(customAllowed)action(parent,customFrom==null?"انتخاب بازه شمسی دلخواه":
      "بازه: "+Shamsi.day(customFrom)+" تا "+Shamsi.day(customUntil-1),false,this::chooseCustomRange);
  }
  private LinearLayout statCard(String label,long value,int color){
    LinearLayout c=card();c.addView(text(label,12,true,muted));gap(c,8);
    c.addView(text(Money.format(value),16,true,color));gap(c,5);
    c.addView(text(Money.inWords(value),11,false,muted));return c;
  }
  private void statPair(LinearLayout parent,LinearLayout a,LinearLayout b){
    LinearLayout row=horizontal();
    LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,-2,1);
    LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,-2,1);
    p1.leftMargin=d(5);p2.rightMargin=d(5);
    row.addView(a,p1);row.addView(b,p2);place(parent,row,10);
  }
  private void dashboard(){
    LinearLayout banner=vertical();banner.setPadding(d(20),d(19),d(20),d(19));
    banner.setBackground(rounded(accent,22));
    banner.addView(text("دفتر حساب • "+occupation(),12,false,Color.WHITE));gap(banner,7);
    banner.addView(text(shop(),25,true,Color.WHITE));gap(banner,8);
    banner.addView(text("امروز: "+Shamsi.dateTime(System.currentTimeMillis()),12,false,Color.WHITE));
    place(body,banner,20);
    title("نمای کلی حساب‌ها");filters(body,false);
    Store.Stats stats=store.stats(from(),until());
    statPair(body,statCard("کل نسیه‌های بازه",stats.debts,positive),statCard("کل دریافتی‌های بازه",stats.receipts,accent));
    statPair(body,statCard("وصول از بدهی‌ها",stats.collected,negative),statCard("مانده قابل وصول",stats.receivable,positive));
    body.addView(text("مانده قابل وصول در پایان بازه محاسبه می‌شود. پرداختی اضافی مشتری جزء وصول از بدهی محسوب نمی‌شود.",11,false,muted));
    gap(body,20);title("عملیات سریع");
    LinearLayout row=horizontal();
    Button debt=button("＋ ثبت نسیه",true,()->chooseCustomer(c->entryForm(c,LedgerLogic.DEBT)));
    Button payment=button("✓ دریافت وجه",false,()->chooseCustomer(c->entryForm(c,LedgerLogic.PAYMENT)));
    LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,d(53),1);
    LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,d(53),1);
    a.leftMargin=d(5);b.rightMargin=d(5);row.addView(debt,a);row.addView(payment,b);
    place(body,row,18);title("بدهی مشتریان");
    List<Store.Customer> clients=store.customers("");
    Collections.sort(clients,(x,y)->Long.compare(y.balance,x.balance));
    int count=0;
    for(Store.Customer c:clients){
      if(c.balance>0&&count++<6)clickRow(body,c.name(),Money.balanceFull(c.balance),()->openCustomer(c.id));
    }
    if(count==0)body.addView(text("هنوز بدهی ثبت نشده است.",14,false,muted));
  }
  private void customerList(){
    title("مشتریان");action(body,"＋ مشتری جدید",true,()->customerForm(this::openCustomer));
    EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(fs(15));
    search.setHint("جستجو با نام یا شماره تماس");search.setTextColor(fg);search.setHintTextColor(muted);
    search.setPadding(d(15),d(10),d(15),d(10));search.setBackground(outlined(surface,16));
    place(body,search,13);
    LinearLayout result=vertical();body.addView(result,lp(-1,-2));
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){renderCustomers(result,s.toString());}
      public void afterTextChanged(Editable e){}
    });
    renderCustomers(result,"");
  }
  private void renderCustomers(LinearLayout output,String query){
    output.removeAllViews();List<Store.Customer> clients=store.customers(query);
    if(clients.isEmpty()){output.addView(text("مشتری‌ای یافت نشد.",14,false,muted));return;}
    for(Store.Customer c:clients)clickRow(output,c.name(),c.phone+" • "+Money.balanceFull(c.balance)+(c.notes.isEmpty()?"":"\n"+c.notes),()->openCustomer(c.id));
  }
  private void openCustomer(long id){customerId=id;page=DETAIL;clearCustom();draw();}
  private void detail(){
    Store.Customer c=store.customer(customerId);
    if(c==null){page=CUSTOMERS;draw();return;}
    TextView back=text("‹ بازگشت به مشتریان",13,true,accent);
    back.setOnClickListener(v->{page=CUSTOMERS;draw();});place(body,back,12);
    LinearLayout head=card();head.addView(text(c.name(),25,true,fg));
    gap(head,5);head.addView(text(c.phone,15,false,muted));
    if(c.notes!=null&&!c.notes.isEmpty()){gap(head,7);head.addView(text("توضیحات: "+c.notes,13,false,muted));}
    gap(head,9);
    head.addView(text(Money.balance(c.balance),19,true,c.balance>0?positive:(c.balance<0?negative:accent)));
    gap(head,4);head.addView(text(Money.inWords(Math.abs(c.balance)),13,false,muted));
    place(body,head,15);
    LinearLayout row=horizontal();
    Button debt=button("＋ نسیه",true,()->entryForm(c,LedgerLogic.DEBT));
    Button paid=button("✓ پرداخت",false,()->entryForm(c,LedgerLogic.PAYMENT));
    LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,d(49),1);
    LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,d(49),1);
    a.leftMargin=d(4);b.rightMargin=d(4);row.addView(debt,a);row.addView(paid,b);
    place(body,row,8);
    action(body,"✉ ارسال صورتحساب با پیامک",false,()->chooseSms(c));gap(body,10);
    title("گردش حساب");filters(body,true);
    Store.Statement s=store.statement(c.id,from(),until());
    LinearLayout card=card();
    card.addView(text("مانده قبلی: "+Money.balanceFull(s.report.opening),13,true,fg));gap(card,8);
    card.addView(text("نسیه دوره: "+Money.full(s.report.debt),13,false,muted));gap(card,6);
    card.addView(text("پرداخت دوره: "+Money.full(s.report.receipts),13,false,muted));gap(card,8);
    card.addView(text("مانده پایان: "+Money.balanceFull(s.report.closing),14,true,accent));
    place(body,card,13);
    if(s.movements.isEmpty()){body.addView(text("تراکنشی در بازه نیست.",13,false,muted));return;}
    List<Store.Movement> reverse=new ArrayList<>(s.movements);Collections.reverse(reverse);
    for(Store.Movement e:reverse){
      LinearLayout c2=card();boolean isDebt=LedgerLogic.DEBT.equals(e.kind);
      c2.addView(text((isDebt?"نسیه | ":"پرداخت | ")+(isDebt?e.items:e.method),15,true,fg));gap(c2,6);
      LinearLayout sub=horizontal();
      sub.addView(text(Shamsi.dateTime(e.timestamp),12,false,muted),new LinearLayout.LayoutParams(0,-2,1));
      sub.addView(text((isDebt?"＋ ":"− ")+Money.format(e.amount),13,true,isDebt?positive:negative));
      c2.addView(sub);gap(c2,4);
      c2.addView(text(Money.inWords(e.amount),12,false,muted));
      place(body,c2,8);
    }
  }
  private void customerForm(Consumer<Long> next){
    LinearLayout f=vertical();f.setPadding(d(16),d(8),d(16),0);
    LinearLayout first=field("نام","نام",InputType.TYPE_CLASS_TEXT);
    LinearLayout last=field("نام خانوادگی","نام خانوادگی",InputType.TYPE_CLASS_TEXT);
    LinearLayout phone=field("شماره تماس","۰۹۱۲...",InputType.TYPE_CLASS_PHONE);
    LinearLayout notes=field("توضیحات مشتری (اختیاری)","آدرس، توضیحات یا نکات مربوط به مشتری",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    EditText notesText=edit(notes);notesText.setSingleLine(false);notesText.setMinLines(3);
    notesText.setLayoutParams(lp(-1,95+fontStep()*3));
    f.addView(first);gap(f,12);f.addView(last);gap(f,12);f.addView(phone);gap(f,12);f.addView(notes);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("مشتری جدید")
      .setView(f).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
      try{
        long id=store.addCustomer(edit(first).getText().toString(),edit(last).getText().toString(),
          edit(phone).getText().toString(),edit(notes).getText().toString());
        dialog.dismiss();next.accept(id);
      }catch(Exception e){notice(e.getMessage());}
    }));
    dialog.show();
  }
  private void chooseCustomer(Consumer<Store.Customer> next){
    List<Store.Customer> clients=store.customers("");
    if(clients.isEmpty()){customerForm(id->next.accept(store.customer(id)));return;}
    String[] labels=new String[clients.size()];
    for(int i=0;i<clients.size();i++)labels[i]=clients.get(i).name()+" • "+clients.get(i).phone;
    new AlertDialog.Builder(this).setTitle("انتخاب مشتری")
      .setItems(labels,(dialog,index)->next.accept(clients.get(index)))
      .setNeutralButton("＋ مشتری جدید",(dialog,index)->customerForm(id->next.accept(store.customer(id))))
      .setNegativeButton("انصراف",null).show();
  }
  private void entryForm(Store.Customer customer,String kind){
    boolean debt=LedgerLogic.DEBT.equals(kind);
    LinearLayout form=vertical();form.setPadding(d(18),d(8),d(18),0);
    form.addView(text("مشتری: "+customer.name(),14,true,accent));gap(form,9);
    LinearLayout items=field("اجناس","مثلاً برنج، شیر، نان",InputType.TYPE_CLASS_TEXT);
    if(debt){form.addView(items);gap(form,11);}
    LinearLayout amount=field("مبلغ (تومان)","مثلاً ۲۵۰٬۰۰۰",InputType.TYPE_CLASS_NUMBER);
    form.addView(amount);
    gap(form,5);
    TextView wordsPreview=text("مبلغ را وارد کنید تا به حروف نمایش داده شود.",13,false,accent);
    form.addView(wordsPreview);
    EditText amountInput=edit(amount);
    amountInput.addTextChangedListener(new TextWatcher(){
      boolean formatting=false;
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){}
      public void afterTextChanged(Editable editable){
        if(formatting)return;
        String raw=editable.toString();
        int caret=Math.max(0,amountInput.getSelectionStart()),digitsBefore=0;
        for(int i=0;i<Math.min(caret,raw.length());i++)
          if(Character.digit(raw.charAt(i),10)>=0)digitsBefore++;
        String grouped=Money.groupDigits(raw);
        if(!grouped.equals(raw)){
          formatting=true;amountInput.setText(grouped);
          int pos=0,passed=0;
          while(pos<grouped.length()&&passed<digitsBefore){
            if(Character.digit(grouped.charAt(pos),10)>=0)passed++;
            pos++;
          }
          amountInput.setSelection(Math.min(pos,grouped.length()));
          formatting=false;
        }
        if(grouped.isEmpty()){wordsPreview.setText("مبلغ را وارد کنید.");return;}
        try{
          if(grouped.equals("۰"))wordsPreview.setText("به حروف: صفر تومان");
          else wordsPreview.setText("به حروف: "+Money.inWords(Money.parse(grouped)));
        }catch(Exception e){wordsPreview.setText("مبلغ نامعتبر یا خارج از محدوده است.");}
      }
    });
    RadioGroup methods=new RadioGroup(this);methods.setOrientation(LinearLayout.VERTICAL);
    methods.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    String[] paymentMethods={"نقدی","کارتخوان","کارت‌به‌کارت"};
    if(!debt){
      gap(form,12);form.addView(text("روش دریافت وجه",13,true,muted));
      for(int i=0;i<paymentMethods.length;i++){
        RadioButton rb=new RadioButton(this);rb.setText(paymentMethods[i]);rb.setTextColor(fg);rb.setTextSize(fs(14));
        rb.setId(1000+i);methods.addView(rb);if(i==0)rb.setChecked(true);
      }
      form.addView(methods);
    }
    gap(form,12);
    final long[] selectedTime={System.currentTimeMillis()};
    final TextView dateLabel=text("تاریخ و ساعت ثبت: "+Shamsi.dateTime(selectedTime[0]),13,true,accent);
    form.addView(dateLabel);gap(form,6);
    action(form,"📅 تغییر تاریخ شمسی",false,()->selectDay("تاریخ تراکنش",selectedTime[0],start->{
      Calendar previous=Shamsi.calendar();previous.setTimeInMillis(selectedTime[0]);
      Calendar updated=Shamsi.calendar();updated.setTimeInMillis(start);
      updated.set(Calendar.HOUR_OF_DAY,previous.get(Calendar.HOUR_OF_DAY));
      updated.set(Calendar.MINUTE,previous.get(Calendar.MINUTE));
      updated.set(Calendar.SECOND,previous.get(Calendar.SECOND));
      selectedTime[0]=updated.getTimeInMillis();
      dateLabel.setText("تاریخ و ساعت ثبت: "+Shamsi.dateTime(selectedTime[0]));
    }));
    action(form,"◷ تغییر ساعت",false,()->{
      Calendar cal=Shamsi.calendar();cal.setTimeInMillis(selectedTime[0]);
      new TimePickerDialog(this,(view,hour,minute)->{
        Calendar changed=Shamsi.calendar();changed.setTimeInMillis(selectedTime[0]);
        changed.set(Calendar.HOUR_OF_DAY,hour);changed.set(Calendar.MINUTE,minute);
        changed.set(Calendar.SECOND,0);changed.set(Calendar.MILLISECOND,0);
        selectedTime[0]=changed.getTimeInMillis();
        dateLabel.setText("تاریخ و ساعت ثبت: "+Shamsi.dateTime(selectedTime[0]));
      },cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show();
    });
    AlertDialog dialog=new AlertDialog.Builder(this)
      .setTitle(debt?"ثبت نسیه":"ثبت دریافت وجه").setView(form)
      .setNegativeButton("انصراف",null).setPositiveButton("ثبت تراکنش",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
      try{
        long n=Money.parse(edit(amount).getText().toString());
        String method=debt?"":paymentMethods[methods.getCheckedRadioButtonId()-1000];
        store.addMovement(customer.id,kind,debt?edit(items).getText().toString():"",method,n,selectedTime[0]);
        dialog.dismiss();customerId=customer.id;page=DETAIL;draw();notice("تراکنش ثبت شد");
      }catch(Exception e){notice(e.getMessage()==null?"ثبت ناموفق بود":e.getMessage());}
    }));
    dialog.show();
  }
  private void chooseSms(Store.Customer customer){
    String[] modes={"خلاصه صورتحساب بازه","ریز حساب بازه (تمام تراکنش‌ها)"};
    new AlertDialog.Builder(this).setTitle("نوع پیامک")
      .setItems(modes,(dialog,which)->{
        try{
          Store.Statement s=store.statement(customer.id,from(),until());
          String sms=StatementSms.compose(s,shop(),occupation(),which==1);
          new AlertDialog.Builder(this).setTitle("پیش‌نمایش پیامک")
            .setMessage(sms+"\n\nپیامک طولانی ممکن است در چند بخش ارسال شود.")
            .setNegativeButton("انصراف",null)
            .setPositiveButton("باز کردن پیامک",(di,idx)->openSms(customer.phone,sms)).show();
        }catch(Exception e){error(e);}
      }).setNegativeButton("انصراف",null).show();
  }
  private void openSms(String phone,String message){
    try{
      String recipient=phone;
      for(int i=0;i<10;i++){
        recipient=recipient.replace((char)('۰'+i),(char)('0'+i))
          .replace((char)('٠'+i),(char)('0'+i));
      }
      Intent send=new Intent(Intent.ACTION_SENDTO);
      send.setData(Uri.parse("smsto:"+Uri.encode(recipient)));
      send.putExtra("sms_body",message);
      startActivity(send);
    }catch(ActivityNotFoundException e){notice("برنامه پیامک روی گوشی پیدا نشد");}
  }
  private void chooseCustomRange(){
    long current=customFrom!=null?customFrom:System.currentTimeMillis();
    selectDay("روز شروع بازه",current,start->{
      long end=customUntil!=null?customUntil-1:System.currentTimeMillis();
      selectDay("روز پایان بازه",end,last->{
        if(last<start){notice("پایان نباید قبل از شروع باشد");return;}
        customFrom=start;customUntil=Shamsi.nextDay(last);draw();
      });
    });
  }
  private void selectDay(String label,long date,Consumer<Long> done){
    Calendar cal=Shamsi.calendar();cal.setTimeInMillis(date);
    LinearLayout wrap=horizontal();wrap.setPadding(d(12),d(10),d(12),0);
    NumberPicker year=new NumberPicker(this),month=new NumberPicker(this),day=new NumberPicker(this);
    year.setMinValue(1300);year.setMaxValue(1500);year.setValue(cal.get(Calendar.YEAR));
    month.setMinValue(1);month.setMaxValue(12);month.setValue(cal.get(Calendar.MONTH)+1);
    day.setMinValue(1);day.setMaxValue(31);day.setValue(cal.get(Calendar.DAY_OF_MONTH));
    for(NumberPicker p:new NumberPicker[]{day,month,year})
      wrap.addView(p,new LinearLayout.LayoutParams(0,d(160),1));
    Runnable clamp=()->{
      Calendar c=Shamsi.calendar();c.clear();
      c.set(Calendar.YEAR,year.getValue());c.set(Calendar.MONTH,month.getValue()-1);c.set(Calendar.DAY_OF_MONTH,1);
      int max=c.getActualMaximum(Calendar.DAY_OF_MONTH);
      if(day.getValue()>max)day.setValue(max);
      day.setMaxValue(max);
    };
    year.setOnValueChangedListener((p,a,b)->clamp.run());
    month.setOnValueChangedListener((p,a,b)->clamp.run());clamp.run();
    new AlertDialog.Builder(this).setTitle(label).setView(wrap)
      .setNegativeButton("انصراف",null)
      .setPositiveButton("انتخاب",(dia,index)->{
        try{done.accept(Shamsi.dayStart(year.getValue(),month.getValue(),day.getValue()));}
        catch(Exception e){notice("تاریخ نامعتبر است");}
      }).show();
  }
  private void settings(){
    title("تنظیمات مغازه");
    LinearLayout c=card();
    c.addView(text("نام مغازه",12,false,muted));gap(c,4);
    c.addView(text(shop(),18,true,fg));gap(c,12);
    c.addView(text("عنوان شغل",12,false,muted));gap(c,4);
    c.addView(text(occupation(),18,true,fg));gap(c,14);
    action(c,"ویرایش اطلاعات مغازه",false,this::editShop);place(body,c,18);
    title("شخصی‌سازی ظاهر");
    clickRow(body,"حالت تاریک / روشن","هماهنگ با گوشی ("+(dark?"تاریک":"روشن")+")",
      ()->notice("حالت از تنظیمات گوشی خوانده می‌شود"));
    clickRow(body,"رنگ پوسته",ACCENT_NAMES[prefs.getInt("theme",0)],this::chooseTheme);
    LinearLayout sizeCard=card();
    TextView fontLabel=text("اندازه فونت: "+fontStep()+" از ۱۰ پله افزایش",14,true,fg);
    sizeCard.addView(fontLabel);gap(sizeCard,7);
    SeekBar fontSlider=new SeekBar(this);fontSlider.setMax(10);
    fontSlider.setProgress(fontStep());sizeCard.addView(fontSlider,lp(-1,50));
    TextView sample=text("پیش‌نمایش نوشته فارسی ۱۲۳٬۴۵۶",14,false,muted);
    sizeCard.addView(sample);
    fontSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar seek,int progress,boolean fromUser){
        fontLabel.setText("اندازه فونت: "+progress+" از ۱۰ پله افزایش");
        sample.setTextSize(14+progress);
      }
      public void onStartTrackingTouch(SeekBar seek){}
      public void onStopTrackingTouch(SeekBar seek){
        prefs.edit().putInt("fontStep",seek.getProgress()).apply();draw();
      }
    });
    place(body,sizeCard,12);
    gap(body,12);title("پشتیبان‌گیری محلی");
    action(body,"↓ ذخیره بکاپ JSON",true,this::saveBackup);
    action(body,"↑ بازیابی بکاپ JSON",false,this::selectRestore);
    body.addView(text("بکاپ بدون رمزگذاری است و نام و شماره مشتری‌ها در فایل قابل خواندن‌اند. بازیابی همه داده‌های موجود را جایگزین می‌کند.",12,false,muted));
    gap(body,22);body.addView(text("نسخه ۱.۰ • آفلاین • بدون مجوز ارسال مستقیم پیامک",12,false,muted));
  }
  private void editShop(){
    LinearLayout f=vertical();f.setPadding(d(16),d(8),d(16),0);
    LinearLayout name=field("نام مغازه","نام مغازه",InputType.TYPE_CLASS_TEXT);
    LinearLayout work=field("عنوان شغل","عنوان شغل",InputType.TYPE_CLASS_TEXT);
    edit(name).setText(shop());edit(work).setText(occupation());
    f.addView(name);gap(f,12);f.addView(work);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("اطلاعات مغازه")
      .setView(f).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
      String n=edit(name).getText().toString().trim(),o=edit(work).getText().toString().trim();
      if(n.isEmpty()||o.isEmpty()||n.length()>120||o.length()>120){notice("نام یا شغل نامعتبر است");return;}
      prefs.edit().putString("shopName",n).putString("occupation",o).apply();
      dialog.dismiss();draw();
    }));
    dialog.show();
  }
  private void chooseTheme(){
    new AlertDialog.Builder(this).setTitle("رنگ پوسته")
      .setSingleChoiceItems(ACCENT_NAMES,prefs.getInt("theme",0),(dialog,which)->{
        prefs.edit().putInt("theme",which).apply();dialog.dismiss();draw();
      }).setNegativeButton("انصراف",null).show();
  }
  private void saveBackup(){
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE,
      "DaftarNesieh_"+Shamsi.day(System.currentTimeMillis()).replace('/','-')+".json");
    startActivityForResult(intent,BACKUP_CREATE);
  }
  private void selectRestore(){
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");
    startActivityForResult(intent,BACKUP_OPEN);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(result!=RESULT_OK||data==null||data.getData()==null)return;
    Uri uri=data.getData();
    if(request==BACKUP_CREATE){
      try(OutputStream out=getContentResolver().openOutputStream(uri)){
        if(out==null)throw new IllegalStateException("فایل قابل نوشتن نیست");
        byte[] json=store.exportData(shop(),occupation()).toString(2).getBytes(StandardCharsets.UTF_8);
        out.write(json);out.flush();notice("بکاپ ذخیره شد");
      }catch(Exception e){error(e);}
    }else if(request==BACKUP_OPEN){
      try(InputStream in=getContentResolver().openInputStream(uri)){
        if(in==null)throw new IllegalStateException("فایل باز نشد");
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] block=new byte[8192];int n;
        while((n=in.read(block))!=-1){
          if(output.size()+n>30_000_000)throw new IllegalArgumentException("بکاپ بیش از ۳۰ مگابایت است");
          output.write(block,0,n);
        }
        JSONObject json=new JSONObject(output.toString("UTF-8"));
        if(!"DaftarNesieh".equals(json.optString("format"))||(json.optInt("schemaVersion")!=1&&json.optInt("schemaVersion")!=2))
          throw new IllegalArgumentException("فرمت بکاپ معتبر نیست");
        new AlertDialog.Builder(this).setTitle("تأیید بازیابی")
          .setMessage("اطلاعات فعلی کاملاً جایگزین می‌شود. بهتر است ابتدا از داده‌های فعلی بکاپ بگیرید.")
          .setNegativeButton("انصراف",null)
          .setPositiveButton("بازیابی",(dia,index)->{
            try{
              store.restoreData(json);
              prefs.edit().putString("shopName",json.getString("shopName"))
                .putString("occupation",json.getString("occupation")).apply();
              customerId=-1;clearCustom();period=0;page=DASHBOARD;draw();notice("بازیابی انجام شد");
            }catch(Exception e){error(e);}
          }).show();
      }catch(Exception e){error(e);}
    }
  }
  private void notice(String s){Toast.makeText(this,s==null?"خطا رخ داد":s,Toast.LENGTH_LONG).show();}
  private void error(Exception e){notice("خطا: "+e.getMessage());}
}
