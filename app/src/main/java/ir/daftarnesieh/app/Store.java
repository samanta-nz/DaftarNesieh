package ir.daftarnesieh.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** All customer and financial records remain in a local SQLite database. */
public final class Store extends SQLiteOpenHelper {
  public static final class Customer {
    public long id, balance;
    public String first,last,phone;
    public String name(){return first+" "+last;}
  }
  public static final class Movement {
    public long id,customerId,amount,timestamp;
    public String kind,items,method;
  }
  public static final class Statement {
    public Customer customer;
    public long from,to;
    public final List<Movement> movements=new ArrayList<>();
    public LedgerLogic.Report report;
  }
  public static final class Stats {public long debts,receipts,collected,receivable;}
  public Store(Context context){super(context,"daftar.db",null,1);}
  @Override public void onConfigure(SQLiteDatabase db){db.setForeignKeyConstraintsEnabled(true);}
  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT,first TEXT NOT NULL,last TEXT NOT NULL,phone TEXT NOT NULL,created_at INTEGER NOT NULL)");
    db.execSQL("CREATE TABLE movements (id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,kind TEXT NOT NULL CHECK(kind IN ('DEBT','PAYMENT')),items TEXT NOT NULL DEFAULT '',method TEXT NOT NULL DEFAULT '',amount INTEGER NOT NULL CHECK(amount>0),timestamp INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX idx_movements_customer_time ON movements(customer_id,timestamp,id)");
    db.execSQL("CREATE INDEX idx_movements_time ON movements(timestamp)");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("Migration required");}
  private static String clean(String text,int max){
    String s=text==null?"":text.trim();
    if(s.length()>max)throw new IllegalArgumentException("متن بیش از حد طولانی است");
    return s;
  }
  public long addCustomer(String first,String last,String phone){
    first=clean(first,80);last=clean(last,80);phone=clean(phone,25);
    if(first.isEmpty()||last.isEmpty())throw new IllegalArgumentException("نام و نام خانوادگی اجباری است");
    if(phone.replaceAll("[^0-9۰-۹٠-٩]","").length()<7)throw new IllegalArgumentException("شماره تماس معتبر وارد کنید");
    ContentValues v=new ContentValues();
    v.put("first",first);v.put("last",last);v.put("phone",phone);v.put("created_at",System.currentTimeMillis());
    return getWritableDatabase().insertOrThrow("customers",null,v);
  }
  public long addMovement(long customerId,String kind,String items,String method,long amount){
    LedgerLogic.signed(kind,amount);
    items=clean(items,1000);method=clean(method,30);
    if(LedgerLogic.DEBT.equals(kind)&&items.isEmpty())throw new IllegalArgumentException("نام اجناس الزامی است");
    if(LedgerLogic.PAYMENT.equals(kind)&&
        !(method.equals("نقدی")||method.equals("کارتخوان")||method.equals("کارت‌به‌کارت")))
      throw new IllegalArgumentException("روش پرداخت نامعتبر است");
    ContentValues v=new ContentValues();
    v.put("customer_id",customerId);v.put("kind",kind);v.put("items",items);
    v.put("method",method);v.put("amount",amount);v.put("timestamp",System.currentTimeMillis());
    return getWritableDatabase().insertOrThrow("movements",null,v);
  }
  public List<Customer> customers(String search){
    List<Customer> out=new ArrayList<>();
    String sql="SELECT c.id,c.first,c.last,c.phone,COALESCE(SUM(CASE WHEN m.kind='DEBT' THEN m.amount ELSE -m.amount END),0) balance "+
      "FROM customers c LEFT JOIN movements m ON m.customer_id=c.id";
    String[] args=null;
    if(search!=null&&!search.trim().isEmpty()){
      sql+=" WHERE (c.first LIKE ? ESCAPE '\\' OR c.last LIKE ? ESCAPE '\\' OR c.phone LIKE ? ESCAPE '\\')";
      String safe=search.trim().replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
      args=new String[]{"%"+safe+"%","%"+safe+"%","%"+safe+"%"};
    }
    sql+=" GROUP BY c.id ORDER BY c.last COLLATE NOCASE,c.first COLLATE NOCASE";
    try(Cursor c=getReadableDatabase().rawQuery(sql,args)){
      while(c.moveToNext()){
        Customer x=new Customer();x.id=c.getLong(0);x.first=c.getString(1);
        x.last=c.getString(2);x.phone=c.getString(3);x.balance=c.getLong(4);out.add(x);
      }
    }
    return out;
  }
  public Customer customer(long id){
    String sql="SELECT c.id,c.first,c.last,c.phone,COALESCE((SELECT SUM(CASE WHEN kind='DEBT' THEN amount ELSE -amount END) FROM movements WHERE customer_id=c.id),0) FROM customers c WHERE c.id=?";
    try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(id)})){
      if(!c.moveToFirst())return null;
      Customer x=new Customer();x.id=c.getLong(0);x.first=c.getString(1);
      x.last=c.getString(2);x.phone=c.getString(3);x.balance=c.getLong(4);return x;
    }
  }
  private static Movement movement(Cursor c){
    Movement e=new Movement();e.id=c.getLong(0);e.customerId=c.getLong(1);e.kind=c.getString(2);
    e.items=c.getString(3);e.method=c.getString(4);e.amount=c.getLong(5);e.timestamp=c.getLong(6);return e;
  }
  public Statement statement(long customerId,long from,long until){
    if(until<=from)throw new IllegalArgumentException("بازه نامعتبر است");
    Statement s=new Statement();s.customer=customer(customerId);
    if(s.customer==null)throw new IllegalArgumentException("مشتری یافت نشد");
    s.from=from;s.to=until;
    List<LedgerLogic.Entry> entries=new ArrayList<>();
    String sql="SELECT id,customer_id,kind,items,method,amount,timestamp FROM movements WHERE customer_id=? AND timestamp<? ORDER BY timestamp ASC,id ASC";
    try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(customerId),String.valueOf(until)})){
      while(c.moveToNext()){
        Movement e=movement(c);
        entries.add(new LedgerLogic.Entry(e.timestamp,e.kind,e.amount));
        if(e.timestamp>=from)s.movements.add(e);
      }
    }
    s.report=LedgerLogic.report(entries,from,until);return s;
  }
  public Stats stats(long from,long until){
    Stats s=new Stats();
    long currentCustomer=-1,running=0;
    String sql="SELECT id,customer_id,kind,items,method,amount,timestamp FROM movements WHERE timestamp<? ORDER BY customer_id ASC,timestamp ASC,id ASC";
    try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(until)})){
      while(c.moveToNext()){
        Movement e=movement(c);
        if(e.customerId!=currentCustomer){
          if(currentCustomer!=-1)s.receivable=LedgerLogic.sum(s.receivable,LedgerLogic.collectible(running));
          currentCustomer=e.customerId;running=0;
        }
        if(e.timestamp>=from){
          if(LedgerLogic.DEBT.equals(e.kind))s.debts=LedgerLogic.sum(s.debts,e.amount);
          else {
            s.receipts=LedgerLogic.sum(s.receipts,e.amount);
            s.collected=LedgerLogic.sum(s.collected,LedgerLogic.appliedPayment(running,e.amount));
          }
        }
        running=LedgerLogic.sum(running,LedgerLogic.signed(e.kind,e.amount));
      }
    }
    if(currentCustomer!=-1)s.receivable=LedgerLogic.sum(s.receivable,LedgerLogic.collectible(running));
    return s;
  }
  public JSONObject exportData(String shop,String occupation) throws Exception {
    JSONObject j=new JSONObject();j.put("format","DaftarNesieh");j.put("schemaVersion",1);
    j.put("exportedAt",System.currentTimeMillis());j.put("shopName",shop);j.put("occupation",occupation);
    JSONArray customers=new JSONArray(),movements=new JSONArray();
    try(Cursor c=getReadableDatabase().rawQuery("SELECT id,first,last,phone,created_at FROM customers ORDER BY id",null)){
      while(c.moveToNext()){
        JSONObject x=new JSONObject();x.put("id",c.getLong(0));x.put("first",c.getString(1));x.put("last",c.getString(2));
        x.put("phone",c.getString(3));x.put("created_at",c.getLong(4));customers.put(x);
      }
    }
    try(Cursor c=getReadableDatabase().rawQuery("SELECT id,customer_id,kind,items,method,amount,timestamp FROM movements ORDER BY id",null)){
      while(c.moveToNext()){
        JSONObject x=new JSONObject();x.put("id",c.getLong(0));x.put("customer_id",c.getLong(1));x.put("kind",c.getString(2));
        x.put("items",c.getString(3));x.put("method",c.getString(4));x.put("amount",c.getLong(5));
        x.put("timestamp",c.getLong(6));movements.put(x);
      }
    }
    j.put("customers",customers);j.put("movements",movements);return j;
  }
  public void restoreData(JSONObject backup) throws Exception {
    if(!"DaftarNesieh".equals(backup.optString("format"))||backup.optInt("schemaVersion")!=1)
      throw new IllegalArgumentException("فرمت فایل بکاپ نامعتبر است");
    JSONArray customers=backup.getJSONArray("customers"),movements=backup.getJSONArray("movements");
    if(customers.length()>100000||movements.length()>200000)throw new IllegalArgumentException("بکاپ بیش از حد بزرگ است");
    if(clean(backup.getString("shopName"),120).isEmpty()||clean(backup.getString("occupation"),120).isEmpty())
      throw new IllegalArgumentException("نام مغازه یا شغل در بکاپ موجود نیست");
    SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
    try{
      db.delete("movements",null,null);db.delete("customers",null,null);
      for(int i=0;i<customers.length();i++){
        JSONObject x=customers.getJSONObject(i);
        long id=x.getLong("id"),created=x.getLong("created_at");
        if(id<=0||created<0)throw new IllegalArgumentException("شناسه مشتری نامعتبر");
        String first=clean(x.getString("first"),80),last=clean(x.getString("last"),80),phone=clean(x.getString("phone"),25);
        if(first.isEmpty()||last.isEmpty()||phone.isEmpty())throw new IllegalArgumentException("مشخصات مشتری ناقص است");
        ContentValues v=new ContentValues();v.put("id",id);v.put("first",first);v.put("last",last);
        v.put("phone",phone);v.put("created_at",created);db.insertOrThrow("customers",null,v);
      }
      for(int i=0;i<movements.length();i++){
        JSONObject x=movements.getJSONObject(i);
        long id=x.getLong("id"),customerId=x.getLong("customer_id"),amount=x.getLong("amount"),timestamp=x.getLong("timestamp");
        if(id<=0||customerId<=0||timestamp<0)throw new IllegalArgumentException("شناسه یا تاریخ تراکنش نامعتبر");
        String kind=x.getString("kind"),items=clean(x.getString("items"),1000),method=clean(x.getString("method"),30);
        LedgerLogic.signed(kind,amount);
        if(LedgerLogic.DEBT.equals(kind)&&items.isEmpty())throw new IllegalArgumentException("شرح نسیه ناقص است");
        if(LedgerLogic.PAYMENT.equals(kind)&&
          !(method.equals("نقدی")||method.equals("کارتخوان")||method.equals("کارت‌به‌کارت")))
          throw new IllegalArgumentException("روش پرداخت نامعتبر است");
        ContentValues v=new ContentValues();v.put("id",id);v.put("customer_id",customerId);v.put("kind",kind);
        v.put("items",items);v.put("method",method);v.put("amount",amount);v.put("timestamp",timestamp);
        db.insertOrThrow("movements",null,v);
      }
      db.setTransactionSuccessful();
    }finally{db.endTransaction();}
  }
}
