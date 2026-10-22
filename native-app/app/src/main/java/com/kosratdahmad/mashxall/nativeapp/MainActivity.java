package com.kosratdahmad.mashxall.nativeapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android Views implementation. All content is local; no WebView or network required. */
public class MainActivity extends Activity implements SensorEventListener {
    private static final int GREEN=Color.rgb(16,60,48), GREEN2=Color.rgb(29,85,68);
    private static final int GOLD=Color.rgb(215,183,91), CREAM=Color.rgb(246,243,234);
    private static final int INK=Color.rgb(35,45,40), MUTED=Color.rgb(109,119,111);
    private static final int BORDER=Color.rgb(228,224,211), WHITE=Color.WHITE;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SQLiteDatabase db;
    private SharedPreferences prefs;
    private String lang="ckb", city="Sulaymaniyah", page="home", activeTrack="";
    private int cityId=77382, tasbih=0;
    private LinearLayout root, body, nav;
    private ScrollView scroll;
    private TextView cityButton, qiblaArrow, qiblaAngle;
    private SensorManager sensors;
    private Sensor rotationSensor;
    private boolean sensorOn=false;
    private double qiblaBearing=0, azimuth=0;
    private MediaPlayer player;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(GREEN);
        prefs=getSharedPreferences("mashxall_native",MODE_PRIVATE);
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
        if(sensors!=null) rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        lang=prefs.getString("language","ckb");
        if(!lang.equals("ckb")&&!lang.equals("ar")&&!lang.equals("en")) lang="ckb";
        cityId=prefs.getInt("city_id",77382);
        city=prefs.getString("city_name","Sulaymaniyah");
        tasbih=prefs.getInt("tasbih_count",0);
        buildShell();
        loading();
        io.execute(()->{
            try {
                SQLiteDatabase opened=new DatabaseHelper(this).open();
                runOnUiThread(()->{
                    db=opened;
                    if(!hasPrayerData(cityId)){cityId=77382;city="Sulaymaniyah";}
                    city=cityName(cityId,city);
                    prefs.edit().putInt("city_id",cityId).putString("city_name",city).apply();
                    updateCityButton();
                    home();
                });
            } catch(Exception e) { runOnUiThread(()->databaseError(e)); }
        });
    }

    @Override protected void onDestroy(){
        unregisterCompass(); stopAudio(); io.shutdownNow();
        if(db!=null&&db.isOpen())db.close();
        super.onDestroy();
    }
    @Override protected void onPause(){unregisterCompass();super.onPause();}
    @Override protected void onResume(){super.onResume();if(page.equals("qibla"))registerCompass();}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    @Override public void onSensorChanged(SensorEvent event){
        if(event.sensor.getType()!=Sensor.TYPE_ROTATION_VECTOR)return;
        float[] matrix=new float[9],orientation=new float[3];
        SensorManager.getRotationMatrixFromVector(matrix,event.values);
        SensorManager.getOrientation(matrix,orientation);
        azimuth=(Math.toDegrees(orientation[0])+360)%360;
        if(qiblaArrow!=null&&page.equals("qibla"))qiblaArrow.setRotation((float)norm(qiblaBearing-azimuth));
        if(qiblaAngle!=null&&page.equals("qibla"))qiblaAngle.setText(String.format(Locale.US,"%.0f°",norm(qiblaBearing-azimuth)));
    }
    private void registerCompass(){if(sensors!=null&&rotationSensor!=null&&!sensorOn){sensors.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_UI);sensorOn=true;}}
    private void unregisterCompass(){if(sensors!=null&&sensorOn){sensors.unregisterListener(this);sensorOn=false;}}
    private void stopAudio(){if(player!=null){try{player.stop();}catch(Exception ignored){} player.release();player=null;}activeTrack="";}

    private void buildShell(){
        root=col();root.setBackgroundColor(CREAM);root.setLayoutDirection(isEn()?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(18),dp(16),dp(18),dp(16));header.setBackgroundColor(GREEN);
        LinearLayout branding=col();
        TextView name=txt("مەشخەڵ",25,WHITE,true);
        TextView tagline=txt(tr("یارمەتییەکی ڕۆژانە بۆ ژیانی ئیمانی","رفيقك اليومي للعبادة والذكر","Your daily worship companion"),11,0xFFDCE8DF,false);
        branding.addView(name);branding.addView(tagline);
        header.addView(branding,new LinearLayout.LayoutParams(0,-2,1));
        cityButton=txt("",13,WHITE,true);cityButton.setGravity(Gravity.CENTER);cityButton.setPadding(dp(10),dp(11),dp(10),dp(11));
        cityButton.setBackground(round(GREEN2,15,0x553A8067));cityButton.setOnClickListener(v->cityPicker());
        header.addView(cityButton);
        root.addView(header);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        body=col();body.setPadding(dp(16),dp(16),dp(16),dp(22));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=row();nav.setBackgroundColor(WHITE);nav.setPadding(dp(4),dp(6),dp(4),dp(6));nav.setElevation(dp(10));
        root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);refreshNav();updateCityButton();
    }
    private void loading(){
        body.removeAllViews();LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setPadding(dp(10),dp(70),dp(10),dp(65));
        TextView moon=txt("☾",56,GOLD,true);moon.setGravity(Gravity.CENTER);c.addView(moon);
        TextView n=txt("مەشخەڵ",27,GREEN,true);n.setGravity(Gravity.CENTER);c.addView(n,margin(0,8,0,6));
        TextView t=txt(tr("ناوەڕۆکی ناوخۆیی ئامادە دەکرێت…","جارٍ تجهيز المحتوى المحلي…","Preparing offline content…"),13,MUTED,false);t.setGravity(Gravity.CENTER);c.addView(t);
        body.addView(c);
    }
    private void databaseError(Exception e){
        body.removeAllViews();title(tr("کێشە لە داتاکان","تعذّر فتح البيانات","Could not open local data"));
        para(tr("داتابەیسی ناوخۆیی نەخوێندرایەوە. تکایە بەرنامەکە دووبارە بکەرەوە.","تعذرت قراءة قاعدة البيانات المحلية. أعد فتح التطبيق.","The offline database could not be opened."));
        para(e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));
    }
    private void refreshNav(){
        if(nav==null)return;
        if(page.equals("qibla"))registerCompass();else unregisterCompass();
        nav.removeAllViews();
        addNav("home","⌂",tr("سەرەکی","الرئيسية","Home"));
        addNav("azkar","۞",tr("زیکرەکان","الأذكار","Azkar"));
        addNav("names","99",tr("ناوەکان","الأسماء","Names"));
        addNav("settings","⚙",tr("ڕێکخستن","الإعدادات","Settings"));
    }
    private void addNav(String key,String icon,String label){
        boolean chosen=page.equals(key)||(page.equals("reader")&&key.equals("azkar"))||
            ((page.equals("audio")||page.equals("qibla")||page.equals("tasbih"))&&key.equals("home"));
        LinearLayout item=col();item.setGravity(Gravity.CENTER);item.setPadding(dp(3),dp(3),dp(3),dp(3));
        TextView symbol=txt(icon,21,chosen?GREEN:MUTED,true);symbol.setGravity(Gravity.CENTER);
        TextView word=txt(label,10,chosen?GREEN:MUTED,chosen);word.setGravity(Gravity.CENTER);
        item.addView(symbol);item.addView(word);
        item.setOnClickListener(v->{if(key.equals("home"))home();else if(key.equals("azkar"))categories();else if(key.equals("names"))names();else settings();});
        nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
    }

    private void home(){
        page="home";refreshNav();body.removeAllViews();
        section(tr("بەخێربێیت","أهلاً بك","Welcome"),today());
        LinearLayout card=card();card.setBackground(round(GREEN,24,0));
        card.addView(txt(tr("کاتەکانی نوێژ","مواقيت الصلاة","Prayer times"),19,WHITE,true));
        card.addView(txt("⌖  "+cityLabel(),13,0xFFDCE8DF,false),margin(0,4,0,10));
        Map<String,String> times=prayerTimes();
        if(times.isEmpty())card.addView(txt(tr("کاتەکانی ئەم بەروارە نییە","لا توجد مواقيت محفوظة لهذا التاريخ","No saved times for today"),13,WHITE,false));
        else{
            String[] keys={"fajr","sunrise","dhuhr","asr","maghrib","isha"},labels=prayerLabels();
            for(int r=0;r<3;r++){
                LinearLayout rr=row();
                for(int j=0;j<2;j++){
                    int i=r*2+j;LinearLayout tile=col();tile.setPadding(dp(11),dp(10),dp(11),dp(10));
                    tile.setBackground(round(0xFF174C3D,14,0x553A8067));
                    tile.addView(txt(labels[i],12,0xFFDCE8DF,false));
                    tile.addView(txt(times.get(keys[i]),18,WHITE,true),margin(0,4,0,0));
                    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
                    p.setMargins(j==0?0:dp(6),0,j==0?dp(6):0,0);rr.addView(tile,p);
                }card.addView(rr,margin(0,r==0?0:dp(7),0,0));
            }
        }
        body.addView(card,margin(0,10,0,0));
        section(tr("خزمەتگوزارییەکان","الخدمات","Explore"),tr("ناوەڕۆکی ناوخۆیی","محتوى محفوظ محلياً","Offline content"));
        feature("۞",tr("زیکر و دوعاکان","الأذكار والأدعية","Azkar & Duas"),tr("بابەت و وەرگێڕان","أقسام وترجمات","Topics and translations"),this::categories);
        feature("99",tr("ناوە پیرۆزەکان","أسماء الله الحسنى","Allah’s Names"),tr("٩٩ ناو لەگەڵ ماناکان","الأسماء ومعانيها","99 names and meanings"),this::names);
        feature("●",tr("تەسبیح","التسبيح","Tasbih"),tr("ژمێرەری زیکر","عداد الذكر","Dhikr counter"),this::tasbihPage);
        feature("♫",tr("دەنگ و بانگ","الأصوات والأذان","Audio & Adhan"),tr("دەنگە ڕەسەنەکان","التسجيلات المحلية","Bundled audio"),this::audioPage);
        feature("☾",tr("قیبلە","اتجاه القبلة","Qibla"),tr("کۆمپاس و ئاراستە","البوصلة والاتجاه","Compass guidance"),this::qiblaPage);
        LinearLayout note=card();note.setBackground(round(0xFFF0EAD8,18,0));
        note.addView(txt(tr("بێ ئینتەرنێت","دون اتصال بالإنترنت","Works offline"),15,GREEN,true));
        note.addView(txt(tr("کاتەکان، زیکر، ناوەکان و دەنگەکان لەناو بەرنامەدان.","المواقيت والأذكار والأسماء والتسجيلات محفوظة داخل التطبيق.","Prayer times, Azkar, names and audio are bundled locally."),12,MUTED,false),margin(0,5,0,0));
        body.addView(note,margin(0,8,0,0));
    }
    private void feature(String icon,String title,String subtitle,Runnable action){
        LinearLayout c=card();c.setOrientation(HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);
        TextView ico=txt(icon,24,Color.rgb(158,123,34),true);ico.setGravity(Gravity.CENTER);ico.setMinWidth(dp(39));c.addView(ico);
        LinearLayout words=col();words.setPadding(dp(10),0,dp(4),0);
        words.addView(txt(title,15,GREEN,true));words.addView(txt(subtitle,11,MUTED,false),margin(0,4,0,0));
        c.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView go=txt(isEn()? "›":"‹",24,MUTED,false);c.addView(go);
        c.setOnClickListener(v->action.run());body.addView(c,margin(0,7,0,0));
    }

    private void categories(){
        page="azkar";refreshNav();body.removeAllViews();title(tr("زیکر و دوعاکان","الأذكار والأدعية","Azkar & Duas"));
        para(tr("بابەتێک هەڵبژێرە بۆ بینینی زیکرەکانی.","اختر موضوعاً لعرض الأذكار الخاصة به.","Choose a topic to browse."));
        for(Item x:items("SELECT c._id,COALESCE((SELECT category_name FROM azkar_category_translation t WHERE t.category_id=c._id AND t.language=? LIMIT 1),(SELECT category_name FROM azkar_category_translation t WHERE t.category_id=c._id AND t.language='ar' LIMIT 1),'') FROM azkar_category c ORDER BY c._id",new String[]{lang}))
            listCard("۞",x.name,tr("بینینی زیکرەکان","عرض الأذكار","Browse"),()->chapters(x.id,x.name));
    }
    private void chapters(int category,String title){
        page="reader";refreshNav();body.removeAllViews();backTitle(title,this::categories);
        List<Item> list=items("SELECT c._id,COALESCE((SELECT chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=c._id AND t.language=? LIMIT 1),(SELECT chapter_name FROM azkar_chapter_translation t WHERE t.chapter_id=c._id AND t.language='ar' LIMIT 1),'') FROM azkar_chapter c WHERE c.category_id=? ORDER BY c._id",new String[]{lang,String.valueOf(category)});
        if(list.isEmpty())para(tr("بابەت بەردەست نییە","لا توجد فصول","No chapters found"));
        for(Item x:list)listCard("›",x.name,tr("خوێندنەوە","قراءة","Read"),()->readDhikr(category,x.id,title,x.name));
    }
    private void readDhikr(int category,int chapter,String categoryTitle,String chapterTitle){
        page="reader";refreshNav();body.removeAllViews();backTitle(chapterTitle,()->chapters(category,categoryTitle));
        String sql="SELECT i._id,i.item,COALESCE((SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=i._id AND t.language=? LIMIT 1),(SELECT t.item_translation FROM azkar_item_translation t WHERE t.item_id=i._id AND t.language='ar' LIMIT 1),''),COALESCE((SELECT rt.reference FROM azkar_reference r JOIN azkar_reference_translation rt ON rt.reference_id=r._id WHERE r.item_id=i._id AND rt.language=? LIMIT 1),(SELECT rt.reference FROM azkar_reference r JOIN azkar_reference_translation rt ON rt.reference_id=r._id WHERE r.item_id=i._id AND rt.language='ar' LIMIT 1),'') FROM azkar_item i WHERE i.chapter_id=? ORDER BY i._id";
        int n=1;
        try(Cursor c=db.rawQuery(sql,new String[]{lang,lang,String.valueOf(chapter)})){
            while(c.moveToNext()){
                String arabic=safe(c.getString(1)),translation=safe(c.getString(2)),source=safe(c.getString(3));
                LinearLayout box=card();LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);
                TextView number=txt(String.format(Locale.US,"%02d",n++),12,Color.rgb(158,123,34),true);
                number.setPadding(dp(10),dp(8),dp(10),dp(8));number.setBackground(round(0xFFF5EEDB,12,0));top.addView(number);
                TextView copy=txt(tr("کۆپی","نسخ","Copy"),12,GREEN,true);copy.setPadding(dp(12),dp(8),dp(12),dp(8));copy.setBackground(round(0xFFEAF0E9,12,0));copy.setOnClickListener(v->copy(arabic));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.weight=1;cp.gravity=Gravity.LEFT;top.addView(copy,cp);box.addView(top);
                TextView a=txt(arabic,22,INK,false);a.setTextDirection(View.TEXT_DIRECTION_RTL);a.setLineSpacing(dp(4),1.1f);box.addView(a,margin(0,14,0,0));
                if(!translation.isEmpty()){TextView t=txt(translation,15,GREEN2,false);t.setLineSpacing(dp(4),1.1f);box.addView(t,margin(0,12,0,0));}
                if(!source.isEmpty())box.addView(txt(source,11,MUTED,false),margin(0,12,0,0));
                body.addView(box,margin(0,10,0,0));
            }
        }catch(Exception e){para(tr("نەتوانرا زیکرەکان بخوێنرێنەوە","تعذرت قراءة الأذكار","Could not read Azkar"));}
    }
    private void names(){
        page="names";refreshNav();body.removeAllViews();title(tr("ناوە پیرۆزەکانی خودا","أسماء الله الحسنى","The Beautiful Names of Allah"));
        para(tr("٩٩ ناو لەگەڵ مانا و ڕوونکردنەوەکانیان.","تسعة وتسعون اسماً مع معانيها.","99 names with their meanings."));
        String sql="SELECT n._id,n.name,COALESCE((SELECT t.name FROM name_translation t WHERE t.name_id=n._id AND t.language=? LIMIT 1),(SELECT t.name FROM name_translation t WHERE t.name_id=n._id AND t.language='ar' LIMIT 1),'') FROM name n ORDER BY n._id";
        try(Cursor c=db.rawQuery(sql,new String[]{lang})){
            while(c.moveToNext()){
                LinearLayout box=card();box.setOrientation(HORIZONTAL);box.setGravity(Gravity.CENTER_VERTICAL);
                TextView num=txt(String.valueOf(c.getInt(0)),12,Color.rgb(158,123,34),true);num.setGravity(Gravity.CENTER);num.setPadding(dp(9),dp(8),dp(9),dp(8));num.setBackground(round(0xFFF5EEDB,30,0));box.addView(num);
                LinearLayout detail=col();detail.setPadding(dp(11),0,dp(4),0);
                detail.addView(txt(safe(c.getString(1)),21,GREEN,true));detail.addView(txt(safe(c.getString(2)),12,MUTED,false),margin(0,4,0,0));
                box.addView(detail,new LinearLayout.LayoutParams(0,-2,1));body.addView(box,margin(0,6,0,0));
            }
        }catch(Exception ignored){}
    }

    private void audioPage(){
        page="audio";refreshNav();body.removeAllViews();title(tr("دەنگ و بانگ","الأصوات والأذان","Audio & Adhan"));
        para(tr("دەنگەکان لە ناو بەرنامەکەدا پارێزراون؛ ئینتەرنێت پێویست نییە.","التسجيلات محفوظة داخل التطبيق ولا تحتاج إلى الإنترنت.","Audio is bundled locally; no internet required."));
        String[][] tracks={
          {"abdulbasit","بانگ — عەبدولباسط","الأذان — عبد الباسط","Adhan — Abdul Basit"},
          {"abdulmajid","بانگ — عەبدولمەجید","الأذان — عبد المجيد","Adhan — Abdul Majid"},
          {"hijaz","بانگ — حجاز","أذان الحجاز","Hijaz Adhan"},
          {"madina","بانگی مەدینە","أذان المدينة","Madinah Adhan"},
          {"madina_fajr","بانگی بەیانی — مەدینە","أذان الفجر — المدينة","Fajr Adhan — Madinah"},
          {"majid","بانگ — ماجید","الأذان — ماجد","Adhan — Majid"},
          {"mecca","بانگی مەککە","أذان مكة","Makkah Adhan"},
          {"mecca_short","بانگی کورت — مەککە","أذان مكة المختصر","Short Makkah Adhan"},
          {"mansur","بانگ — مەنسوور","الأذان — منصور","Adhan — Mansur"},
          {"mshari_fajr","بانگی بەیانی — مشاری","أذان الفجر — مشاري","Fajr Adhan — Mishary"},
          {"names99","گوێگرتن لە ناوە پیرۆزەکان","استماع أسماء الله الحسنى","Listen to Allah’s 99 Names"},
          {"naqshbandi","بانگ — نەقشبەندی","الأذان — نقشبندي","Adhan — Naqshbandi"},
          {"nasr","بانگ — نەسر","الأذان — نصر","Adhan — Nasr"},
          {"peshawa","بانگ — پێشەوا","الأذان — بيشوا","Adhan — Peshawa"},
          {"rahid","بانگ — ڕاهید","الأذان — راهيد","Adhan — Rahid"},
          {"ramazan","دەنگی ڕەمەزان","صوت رمضان","Ramadan audio"}
        };
        for(String[] t:tracks){
            boolean playing=t[0].equals(activeTrack)&&player!=null&&player.isPlaying();
            String title=isCkb()?t[1]:isAr()?t[2]:t[3];
            String status=t[0].equals(activeTrack)&&player!=null?(playing?tr("لە پەخشکردندایە","يعمل الآن","Playing now"):tr("وەستاوە","متوقف مؤقتاً","Paused")):tr("کرتە بکە بۆ پەخشکردن","اضغط للتشغيل","Tap to play");
            listCard(playing?"Ⅱ":"▶",title,status,()->toggleAudio(t[0]));
        }
        if(player!=null){Button stop=button(tr("وەستاندنی دەنگ","إيقاف الصوت","Stop audio"),false);stop.setOnClickListener(v->{stopAudio();audioPage();});body.addView(stop,margin(0,12,0,0));}
    }
    private void toggleAudio(String key){
        if(player!=null&&key.equals(activeTrack)){if(player.isPlaying())player.pause();else player.start();audioPage();return;}
        stopAudio();int id=getResources().getIdentifier(key,"raw",getPackageName());
        if(id==0){Toast.makeText(this,tr("فایلی دەنگ نەدۆزرایەوە","الملف الصوتي غير موجود","Audio file not found"),Toast.LENGTH_LONG).show();return;}
        try{
            player=MediaPlayer.create(this,id);if(player==null)throw new IllegalStateException("MediaPlayer cannot decode audio");
            activeTrack=key;player.setOnCompletionListener(mp->{stopAudio();if(page.equals("audio"))audioPage();});player.start();audioPage();
        }catch(Exception e){stopAudio();Toast.makeText(this,tr("پەخشکردن سەرکەوتوو نەبوو","تعذر تشغيل الصوت","Could not play audio"),Toast.LENGTH_LONG).show();}
    }

    private void qiblaPage(){
        page="qibla";refreshNav();body.removeAllViews();title(tr("ئاراستەی قیبلە","اتجاه القبلة","Qibla direction"));
        para(tr("ئاراستەکە بەپێی شوێنی هەڵبژێردراوە. کاتێک سنسۆری کۆمپاس هەبێت نیشاندەرەکە لەگەڵ جووڵەی ئامێر دەسوڕێتەوە.","يُحسب الاتجاه بحسب المدينة المختارة، وتتحرك الإشارة مع البوصلة عند توفر المستشعر.","Calculated for the selected city. With a compass sensor, the pointer follows your device."));
        qiblaBearing=bearing();LinearLayout card=col();card.setGravity(Gravity.CENTER);card.setPadding(dp(18),dp(28),dp(18),dp(26));card.setBackground(round(GREEN,26,0));
        TextView icon=txt("🕋",42,WHITE,true);icon.setGravity(Gravity.CENTER);card.addView(icon);
        FrameLayout dial=new FrameLayout(this);dial.setBackground(round(0xFF174C3D,140,0xFF4E816D));
        LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(220),dp(220));dl.setMargins(0,dp(14),0,dp(12));
        TextView north=txt("N",16,GOLD,true);north.setGravity(Gravity.CENTER);FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(35),Gravity.TOP|Gravity.CENTER_HORIZONTAL);dial.addView(north,np);
        qiblaArrow=txt("↑",69,GOLD,true);qiblaArrow.setGravity(Gravity.CENTER);dial.addView(qiblaArrow,new FrameLayout.LayoutParams(-1,-1));
        card.addView(dial,dl);qiblaAngle=txt(String.format(Locale.US,"%.0f°",qiblaBearing),25,WHITE,true);qiblaAngle.setGravity(Gravity.CENTER);card.addView(qiblaAngle);
        TextView hint=txt(tr("گۆشە لە ئاراستەی ئامێر","زاوية الاتجاه بحسب البوصلة","Relative direction"),12,0xFFDCE8DF,false);hint.setGravity(Gravity.CENTER);card.addView(hint,margin(0,4,0,0));
        body.addView(card,margin(0,10,0,0));
        para(tr("شار: ","المدينة: ","City: ")+cityLabel());
        if(rotationSensor==null){para(tr("سنسۆری کۆمپاس لە ئامێرەکەدا نەدۆزرایەوە؛ گۆشەی شارەکە نیشان دەدرێت.","لا يوجد مستشعر بوصلة؛ تظهر زاوية المدينة المحسوبة.","No compass sensor detected; city bearing is still available."));qiblaArrow.setRotation((float)qiblaBearing);}
        else{registerCompass();qiblaArrow.setRotation((float)norm(qiblaBearing-azimuth));}
        Button c=button(tr("گۆڕینی شار","تغيير المدينة","Change city"),false);c.setOnClickListener(v->cityPicker());body.addView(c,margin(0,10,0,0));
    }
    private double bearing(){
        double[] p=coordinates();
        double a=Math.toRadians(p[0]), b=Math.toRadians(p[1]), kLat=Math.toRadians(21.4225), kLon=Math.toRadians(39.8262), dl=kLon-b;
        return norm(Math.toDegrees(Math.atan2(Math.sin(dl)*Math.cos(kLat),Math.cos(a)*Math.sin(kLat)-Math.sin(a)*Math.cos(kLat)*Math.cos(dl))));
    }
    private double[] coordinates(){
        if(db!=null)try(Cursor c=db.rawQuery("SELECT latitude,longitude FROM location WHERE _id=? LIMIT 1",new String[]{String.valueOf(cityId)})){if(c.moveToFirst())return new double[]{c.getDouble(0),c.getDouble(1)};}catch(Exception ignored){}
        return new double[]{35.5578,45.4233};
    }
    private double norm(double x){return (x%360+360)%360;}

    private void tasbihPage(){
        page="tasbih";refreshNav();body.removeAllViews();title(tr("ژمێرەری تەسبیح","عداد التسبيح","Tasbih counter"));
        para(tr("کرتە لە کارت بکە بۆ زیادکردنی ژمارە.","اضغط على الدائرة لزيادة العدد.","Tap the counter to add one."));
        LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setPadding(dp(18),dp(36),dp(18),dp(36));c.setBackground(round(GREEN,28,0));
        TextView phrase=txt("سُبْحَانَ اللَّهِ",26,WHITE,true);phrase.setGravity(Gravity.CENTER);c.addView(phrase);
        TextView count=txt(String.valueOf(tasbih),65,GOLD,true);count.setGravity(Gravity.CENTER);c.addView(count,margin(0,12,0,10));
        TextView hint=txt(tr("کرتە بکە بۆ زیادکردن","اضغط للزيادة","Tap to count"),13,0xFFDCE8DF,false);hint.setGravity(Gravity.CENTER);c.addView(hint);
        c.setOnClickListener(v->{tasbih++;prefs.edit().putInt("tasbih_count",tasbih).apply();tasbihPage();});body.addView(c,margin(0,16,0,0));
        Button reset=button(tr("سفرکردنەوە","تصفير العداد","Reset"),false);reset.setOnClickListener(v->{tasbih=0;prefs.edit().putInt("tasbih_count",0).apply();tasbihPage();});body.addView(reset,margin(0,12,0,0));
    }

    private void settings(){
        page="settings";refreshNav();body.removeAllViews();title(tr("ڕێکخستن","الإعدادات","Settings"));
        para(tr("زمان و شوێنی کاتەکانی نوێژ دیاری بکە.","اختر اللغة والمدينة المستخدمة في المواقيت.","Choose the app language and prayer-time city."));
        listCard("文",tr("زمانی بەرنامە","لغة التطبيق","App language"),languageLabel(),this::languagePicker);
        listCard("⌖",tr("شار","المدينة","City"),cityLabel(),this::cityPicker);
        listCard("♫",tr("دەنگ و بانگ","الأصوات والأذان","Audio & Adhan"),tr("دەنگە ناوخۆییەکان","التسجيلات المحلية","Bundled recordings"),this::audioPage);
        listCard("☾",tr("قیبلە","اتجاه القبلة","Qibla"),tr("کۆمپاس و ئاراستە","البوصلة والاتجاه","Compass-guided direction"),this::qiblaPage);
        listCard("▣",tr("کارکردنی ناوخۆیی","العمل دون إنترنت","Offline operation"),tr("داتاکان لە ئامێرەکەتن","البيانات محفوظة على جهازك","Data is stored on your device"),()->new AlertDialog.Builder(this).setTitle(tr("ناوەڕۆکی ناوخۆیی","المحتوى المحلي","Offline content")).setMessage(tr("بەرنامەکە پێویستی بە ئینتەرنێت نییە.","التطبيق لا يحتاج إلى الإنترنت.","The app does not require internet access.")).setPositiveButton(tr("باشە","حسناً","OK"),null).show());
        TextView footer=txt("مەشخەڵ  •  Native Android 1.0",12,MUTED,false);footer.setGravity(Gravity.CENTER);body.addView(footer,margin(0,25,0,0));
    }

    private void languagePicker(){
        String[] labels={"کوردیی سۆرانی","العربية","English"},codes={"ckb","ar","en"};
        int selected=lang.equals("ar")?1:lang.equals("en")?2:0;
        new AlertDialog.Builder(this).setTitle(tr("زمان","اللغة","Language")).setSingleChoiceItems(labels,selected,(d,w)->{
            lang=codes[w];prefs.edit().putString("language",lang).apply();d.dismiss();
            if(root!=null)root.setLayoutDirection(isEn()?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);
            updateCityButton();if(page.equals("names"))names();else if(page.equals("azkar"))categories();else if(page.equals("settings"))settings();else home();
        }).setNegativeButton(tr("داخستن","إلغاء","Cancel"),null).show();
    }
    private void cityPicker(){
        if(db==null)return;
        List<Item> cities=items("SELECT l._id,l.name FROM location l JOIN country c ON c._id=l.country_id JOIN prayer_time p ON p.location_id=l._id GROUP BY l._id,l.name,c.code ORDER BY CASE WHEN c.code='IQ' THEN 0 ELSE 1 END,l.name",new String[]{});
        if(cities.isEmpty())return;
        LinearLayout holder=col();holder.setPadding(dp(16),dp(8),dp(16),dp(6));
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setHint(tr("گەڕان بە ناوی شار...","ابحث باسم المدينة...","Search cities..."));input.setBackground(round(WHITE,13,BORDER));holder.addView(input);
        ListView lv=new ListView(this);lv.setDividerHeight(dp(1));List<Item> filtered=new ArrayList<>(cities);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,cityLabels(filtered));lv.setAdapter(adapter);holder.addView(lv,new LinearLayout.LayoutParams(-1,dp(390)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("هەڵبژاردنی شار","اختر المدينة","Choose city")).setView(holder).setNegativeButton(tr("داخستن","إلغاء","Cancel"),null).create();
        lv.setOnItemClickListener((p,v,pos,id)->{
            if(pos<0||pos>=filtered.size())return;Item pick=filtered.get(pos);cityId=pick.id;city=pick.name;
            prefs.edit().putInt("city_id",cityId).putString("city_name",city).apply();updateCityButton();
            String old=page;dialog.dismiss();if(old.equals("settings"))settings();else if(old.equals("qibla"))qiblaPage();else home();
        });
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int n,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                String q=s.toString().toLowerCase(Locale.ROOT).trim();filtered.clear();
                for(Item x:cities)if(x.name.toLowerCase(Locale.ROOT).contains(q)||cityLabelFor(x.name).toLowerCase(Locale.ROOT).contains(q))filtered.add(x);
                adapter.clear();adapter.addAll(cityLabels(filtered));adapter.notifyDataSetChanged();
            }public void afterTextChanged(Editable e){}
        });
        dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.92f),ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private List<String> cityLabels(List<Item> list){List<String> a=new ArrayList<>();for(Item x:list)a.add(cityLabelFor(x.name));return a;}
    private String cityLabelFor(String s){
        if(isCkb()){
            if(s.equalsIgnoreCase("Sulaymaniyah"))return "سلێمانی";
            if(s.equalsIgnoreCase("Erbil"))return "هەولێر";
            if(s.equalsIgnoreCase("Kirkuk"))return "کەرکووک";
            if(s.equalsIgnoreCase("Duhok"))return "دهۆک";
            if(s.equalsIgnoreCase("Halabja"))return "هەڵەبجە";
            if(s.equalsIgnoreCase("Baghdad"))return "بەغدا";
            if(s.equalsIgnoreCase("Mosul"))return "مووسڵ";
            if(s.equalsIgnoreCase("Basrah"))return "بەسرە";
        }
        if(isAr()){
            if(s.equalsIgnoreCase("Sulaymaniyah"))return "السليمانية";
            if(s.equalsIgnoreCase("Erbil"))return "أربيل";
            if(s.equalsIgnoreCase("Kirkuk"))return "كركوك";
            if(s.equalsIgnoreCase("Duhok"))return "دهوك";
            if(s.equalsIgnoreCase("Halabja"))return "حلبجة";
            if(s.equalsIgnoreCase("Baghdad"))return "بغداد";
            if(s.equalsIgnoreCase("Mosul"))return "الموصل";
            if(s.equalsIgnoreCase("Basrah"))return "البصرة";
        }return s;
    }
    private String cityLabel(){return cityLabelFor(city);}
    private String languageLabel(){return tr("کوردیی سۆرانی","العربية","English");}
    private void updateCityButton(){if(cityButton!=null)cityButton.setText("⌖  "+cityLabel());}

    private List<Item> items(String sql,String[] args){
        List<Item> out=new ArrayList<>();if(db==null)return out;
        try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext())out.add(new Item(c.getInt(0),safe(c.getString(1))));}catch(Exception ignored){}
        return out;
    }
    private boolean hasPrayerData(int id){try(Cursor c=db.rawQuery("SELECT 1 FROM prayer_time WHERE location_id=? LIMIT 1",new String[]{String.valueOf(id)})){return c.moveToFirst();}catch(Exception e){return false;}}
    private String cityName(int id,String fallback){try(Cursor c=db.rawQuery("SELECT name FROM location WHERE _id=? LIMIT 1",new String[]{String.valueOf(id)})){if(c.moveToFirst())return c.getString(0);}catch(Exception ignored){}return fallback;}
    private Map<String,String> prayerTimes(){
        Map<String,String> m=new HashMap<>();if(db==null)return m;
        String date=new SimpleDateFormat("MM-dd",Locale.US).format(new Date());
        try(Cursor c=db.rawQuery("SELECT fajr,sunrise,dhuhr,asr,maghrib,isha FROM prayer_time WHERE location_id=? AND date=? LIMIT 1",new String[]{String.valueOf(cityId),date})){
            if(c.moveToFirst()){m.put("fajr",c.getString(0));m.put("sunrise",c.getString(1));m.put("dhuhr",c.getString(2));m.put("asr",c.getString(3));m.put("maghrib",c.getString(4));m.put("isha",c.getString(5));}
        }catch(Exception ignored){}return m;
    }
    private String[] prayerLabels(){return new String[]{tr("بەیانی","الفجر","Fajr"),tr("هەڵاتنی خۆر","الشروق","Sunrise"),tr("نیوەڕۆ","الظهر","Dhuhr"),tr("عەسر","العصر","Asr"),tr("مەغریب","المغرب","Maghrib"),tr("عیشا","العشاء","Isha")};}
    private String today(){Locale l=isCkb()?new Locale("ckb"):isAr()?new Locale("ar"):Locale.ENGLISH;return DateFormat.getDateInstance(DateFormat.MEDIUM,l).format(new Date());}
    private boolean isCkb(){return lang.equals("ckb");}private boolean isAr(){return lang.equals("ar");}private boolean isEn(){return lang.equals("en");}
    private String tr(String c,String a,String e){return isCkb()?c:isAr()?a:e;}
    private String safe(String s){return s==null?"":s;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(VERTICAL);l.setLayoutDirection(isEn()?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(HORIZONTAL);l.setLayoutDirection(isEn()?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);return l;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s==null?"":s);t.setTextSize(size);t.setTextColor(color);t.setGravity(isEn()?Gravity.LEFT:Gravity.RIGHT);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setIncludeFontPadding(true);return t;}
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout card(){LinearLayout l=col();l.setPadding(dp(15),dp(15),dp(15),dp(15));l.setBackground(round(WHITE,20,BORDER));return l;}
    private LinearLayout.LayoutParams margin(int left,int top,int right,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(left),dp(top),dp(right),dp(bottom));return p;}
    private void title(String s){body.addView(txt(s,22,GREEN,true),margin(0,0,0,8));}
    private void section(String s,String sub){body.addView(txt(s,23,GREEN,true),margin(0,0,0,2));if(sub!=null&&!sub.isEmpty())body.addView(txt(sub,12,MUTED,false),margin(0,0,0,5));}
    private void para(String s){TextView t=txt(s,13,MUTED,false);t.setLineSpacing(dp(3),1.1f);body.addView(t,margin(0,0,0,10));}
    private void backTitle(String s,Runnable action){
        LinearLayout r=row();TextView b=txt(tr("‹ گەڕانەوە","‹ رجوع","‹ Back"),13,GREEN,true);b.setPadding(dp(11),dp(9),dp(11),dp(9));b.setBackground(round(0xFFE9EEE7,13,0));b.setOnClickListener(v->action.run());r.addView(b);
        TextView t=txt(s,18,GREEN,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(10),0,0,0);r.addView(t,p);body.addView(r,margin(0,0,0,8));
    }
    private void listCard(String icon,String title,String sub,Runnable action){
        LinearLayout l=row();l.setGravity(Gravity.CENTER_VERTICAL);l.setPadding(dp(14),dp(13),dp(14),dp(13));l.setBackground(round(WHITE,18,BORDER));
        TextView i=txt(icon,23,Color.rgb(158,123,34),true);i.setGravity(Gravity.CENTER);i.setMinWidth(dp(35));l.addView(i);
        LinearLayout words=col();words.setPadding(dp(10),0,dp(5),0);words.addView(txt(title,15,GREEN,true));words.addView(txt(sub,11,MUTED,false),margin(0,4,0,0));l.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        l.addView(txt(isEn()?"›":"‹",23,MUTED,false));l.setOnClickListener(v->action.run());body.addView(l,margin(0,6,0,0));
    }
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(primary?WHITE:GREEN);b.setBackground(round(primary?GREEN:0xFFE8EDE6,15,0));return b;}
    private void copy(String s){ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cb!=null)cb.setPrimaryClip(ClipData.newPlainText("Dhikr",s));Toast.makeText(this,tr("کۆپی کرا","تم النسخ","Copied"),Toast.LENGTH_SHORT).show();}

    private static final class Item{final int id;final String name;Item(int id,String name){this.id=id;this.name=name;}}
}
