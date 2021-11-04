package com.example.dcardtry;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.dcardtry.SQLconnect.MysqlCon;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomePage extends AppCompatActivity {
    List<GetDcardInfo> dcardList;
    List<String> chartValue;
    Integer neg, neu, pos;
    RecyclerView mRecyclerView;
    MyAdapter adapter1;

    boolean success = false;
    MysqlCon con;
    RecyclerView.LayoutManager mLayoutManager;




    LinearLayout dotsLayout;
    SliderAdapter adapter;
    ArticleSummaryAdapter AS_Adapter;
    ViewPager2 pager2;
    private static final String DCARD_URL = "http://172.26.8.81:13306/GetData5.php"
            ,SCORE_URL = "http://172.26.8.81:13306/Amount_Score.php"
            ,DATE_URL = "http://172.26.8.81:13306/Amount_Date.php";
    int list[],bannerpic[];
    TextView[] dots;
    String bannertxt[];
    RecyclerView Article_Summary;

    private DrawerLayout drawerLayout;
    Timer BannerTimer;
    String Name,Job,Account,Password;//接收登入頁面傳過來的資料
    TextView DM_Tilte;//側邊選單標題 : 姓名+職稱
    TextView MSTitle,MSAccount,MSAverage,MSKey;//本月統計用





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        con = new MysqlCon();
        //設定隱藏標題
        getSupportActionBar().hide();
        //設定隱藏狀態
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        //取得傳遞過來的資料
        Intent intent = this.getIntent();
        Name = intent.getStringExtra("name");
        Job = intent.getStringExtra( "job" );
        Account = intent.getStringExtra( "account" );
        Password = intent.getStringExtra("password");

        //加上側邊選單姓名、職稱
        DM_Tilte=findViewById( R.id.drawer_menu_title );
        DM_Tilte.setText( "\t"+Name+"\n"+Job+"\t\t 您好" );

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        dotsLayout=findViewById(R.id.dots_container);
        pager2=findViewById(R.id.view_pager2);

        //Banner中底部Title
        bannertxt=new String[]{"即時掌握社群訊息","精準分析找出關鍵問題","視覺化呈現大量資料","","",""};

        //Banner中圖片
        bannerpic = new int[]{R.drawable.banner_pic8,
                R.drawable.banner_pic7,
                R.drawable.banner_pic2,
                R.drawable.banner_pic3,
                R.drawable.banner_pic3,
                R.drawable.banner_pic3
        };

        adapter =new SliderAdapter(list,bannertxt,bannerpic);
        pager2.setAdapter(adapter);
        pager2.setCurrentItem(99);

        dots=new TextView[6];
        dotsIndicator();

        mRecyclerView = findViewById(R.id.Article_Summary_RecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        dcardList = new ArrayList<>();

     //   loadDcard();
        SyncData orderData = new SyncData();
        orderData.execute("");
        monthStatic(); //本月概覽

        pager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectedIndicator(position);
                super.onPageSelected(position);
            }
        });

        BannerTimer = new Timer(true);//此處造成模擬器一開始執行時，第一頁會馬上跳掉。不會等設定時間
        TimerTask timerTask;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                pager2.setCurrentItem(pager2.getCurrentItem()+1);
                if(pager2.getCurrentItem()==(Integer.MAX_VALUE)-2)
                //防止自動輪播到最後一張時無法繼續輪播下去
                {
                    pager2.setCurrentItem(pager2.getCurrentItem()%5);
                }
            }
        };
        BannerTimer.schedule(timerTask, 0,20000);

    }


    //本月概覽 開始
    private void monthStatic(){
      MSTitle=findViewById( R.id.month_static_title );
      MSAccount=findViewById( R.id.articleAmount );
      MSAverage=findViewById( R.id.averagePoint );
      MSKey=findViewById( R.id.keyword_Match_Amount );

      int year,month;

      Calendar getmonth=Calendar.getInstance();
      getmonth.setTime( new Date() );
      year=getmonth.get(Calendar.YEAR);
      //month=getmonth.get(Calendar.MONTH)+1;
      month=2;

      MSTitle.setText( "本月概覽 ( "+year+" 年 "+month+" 月 )");

        //本月概覽 - 文章數 開始
        new Thread(() -> {
            MysqlCon getamount = new MysqlCon();
            // 讀取資料
            final int count = getamount.HomeAmount(year,month);
            String v=Integer.toString( count );
            Log.v("OK",v);
            MSAccount.post(() -> MSAccount.setText(v));
        }).start();
        //本月概覽 - 文章數 結束

        //本月概覽 - 平均情緒分析 開始
        new Thread(() -> {
            MysqlCon getscore = new MysqlCon();
            // 讀取資料
            final float AvgScore = getscore.ScoreAnalysis(year,month);
            String v=Float.toString( AvgScore );
            Log.v("OK",v);
            MSAverage.post(() -> MSAverage.setText(v));
        }).start();
        //本月概覽 - 平均情緒分析 結束

        //本月概覽 - 關鍵詞文章數 開始
        new Thread(() -> {
            MysqlCon getkey = new MysqlCon();
            // 讀取資料
            final int keywordcount = getkey.KeywordCount(year,month);
            String v=Integer.toString( keywordcount );
            Log.v("OK",v);
            MSKey.post(() -> MSKey.setText(v));
        }).start();
        //本月概覽 - 關鍵詞文章數 結束

    }
    //本月概覽 結束

    private void dotsIndicator() {
        for(int i=0;i<dots.length;i++){
            dots[i]=new TextView(this);
            dots[i].setText(Html.fromHtml("&#9679;"));
            dots[i].setTextSize(18);
            dotsLayout.addView(dots[i]);//設定主圖背景顏色
        }
        //dots[0].setTextColor(getResources().getColor(R.color.gray));//line:61-64暫時解決初始畫面時Banner下方圓點看不到的問題
        for(int i=0;i<=5;i++){
            dots[i].setTextColor(getResources().getColor(R.color.gray));
        }
    }

    private void selectedIndicator(int position) {
        for(int i=0;i<dots.length;i++){
            if(i==position%6){
                dots[i].setTextColor(getResources().getColor(R.color.black));
            }
            else{
                dots[i].setTextColor(getResources().getColor(R.color.gray));
            }
        }
    }

    public void BannerBntToRight(View view){pager2.setCurrentItem(pager2.getCurrentItem()+1);}
    public void BannerBntToLeft(View view){pager2.setCurrentItem(pager2.getCurrentItem()-1);}

  /*  private void loadDcard(){
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, DCARD_URL, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject dcardObject = response.getJSONObject(i);
                        GetDcardInfo dcard = new GetDcardInfo();
                        dcard.setSascore(dcardObject.getString("SA_Score"));
                        dcard.setSaclass(dcardObject.getString("SA_Class"));
                        dcard.setTitle(dcardObject.getString("Title"));
                        dcard.setDate(dcardObject.getString("CreatedAt"));
                        dcard.setContent(dcardObject.getString("Content"));
                        dcard.setId(dcardObject.getString("Id"));
                        dcard.setLv1(dcardObject.getString("KeywordLevel1"));
                        dcard.setLv2(dcardObject.getString("KeywordLevel2"));
                        dcard.setLv3(dcardObject.getString("KeywordLevel3"));
                        dcardList.add(dcard);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Article_Summary.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                AS_Adapter = new ArticleSummaryAdapter(getApplicationContext(), dcardList,5);
                Article_Summary.setAdapter(AS_Adapter);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonArrayRequest);

    }
*/

    private class SyncData extends AsyncTask<String, String, String> {

        String msg = "Internet/DB_Credentials/Windows_FireWall_TurnOn ERROR, See Android Monitor in the bottom for details!";
        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(HomePage.this, "Synchronising", "RecycleView Loading, Please Wait...", true);
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                Connection conn = con.CONN();
                if (conn == null) {
                    success = false;
                } else {
                    String query = "SELECT dcard_rawdata.Id, dcard_rawdata.Title, dcard_rawdata.CreatedAt, dcard_rawdata.Content, nlp_analysis.SA_Score, nlp_analysis.SA_Class, comparison.Level, comparison.KeywordLevel1, comparison.KeywordLevel2, comparison.KeywordLevel3 FROM dcard_rawdata JOIN nlp_analysis ON dcard_rawdata.Id = nlp_analysis.Id JOIN comparison ON comparison.Id = nlp_analysis.Id WHERE dcard_rawdata.Id = nlp_analysis.Id ORDER BY  dcard_rawdata.Id DESC";
                    Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery(query);
                    if (rs != null) {
                        while (rs.next()) {
                            try {
                                GetDcardInfo dcard = new GetDcardInfo();
                                dcard.setSascore(rs.getString("SA_Score"));
                                dcard.setSaclass(rs.getString("SA_Class"));
                                dcard.setTitle(rs.getString("Title"));
                                dcard.setDate(rs.getString("CreatedAt"));
                                dcard.setContent(rs.getString("Content"));
                                dcard.setId(rs.getString("Id"));
                                dcard.setLv1(rs.getString("KeywordLevel1"));
                                dcard.setLv2(rs.getString("KeywordLevel2"));
                                dcard.setLv3(rs.getString("KeywordLevel3"));
                                dcardList.add(dcard);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        msg = "FOUND";
                        success = true;
                    } else {
                        msg = "NO DATA FOUND!";
                        success = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Writer writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                msg = writer.toString();
                success = false;
            }
            return msg;
        }

        protected void onPostExecute(String msg) {
            progress.dismiss();
            Toast.makeText(HomePage.this, "" + msg, Toast.LENGTH_LONG).show();
            if (success == false) {

            } else {
                try {

                    AS_Adapter = new ArticleSummaryAdapter(getApplicationContext(), dcardList,5);
                    mRecyclerView.setAdapter(AS_Adapter);
                } catch (Exception e) {

                }
            }
        }
    }

  /*  private void loadDcard(){
        HttpsTrustManager.allowAllSSL();
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, DCARD_URL, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject dcardObject = response.getJSONObject(i);
                        GetDcardInfo dcard = new GetDcardInfo();
                        dcard.setTitle(dcardObject.getString("Title"));
                        dcard.setDate(dcardObject.getString("Date"));
                        dcard.setContent(dcardObject.getString("Content"));
                        dcardList.add(dcard);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Article_Summary.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                AS_Adapter = new  ArticleSummaryAdapter(getApplicationContext(), dcardList);
                Article_Summary.setAdapter(AS_Adapter);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonArrayRequest);

    }*/

    //側邊選單code Strat
    public void ClickMenu(View view){
        openDrawer(drawerLayout);
    }

    public void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer( GravityCompat.START );
    }

    public void ClickCancle(View view){
        closeDrawer(drawerLayout);
    }

    public static void closeDrawer(DrawerLayout drawerLayout) {
        //check condition
        if(drawerLayout.isDrawerOpen( GravityCompat.START )){
            //when drawer is open
            drawerLayout.closeDrawer( GravityCompat.START );
        }
    }

    public void ClickHome(View view){
        //Restart activity_home_page.xml
        closeDrawer(drawerLayout);
    }

    public void ClickArticle(View view){
        //Redirect(重定向) activity to articlePage
        redirectActivity(this, ArticlePageActivity.class);
    }

    public void ClickChart(View view){
        //Redirect(重定向) activity to chartPage
        //redirectActivity(this,);
    }

    public void ClickAccountInfo(View view){
        //Redirect(重定向) activity to accountPage(帳號管理頁面)
        redirectActivity(this,UserChangePassword.class);
    }

    public void ClickLogout(View view){
        //回到登入頁面
        logout(this);
    }

    public void redirectActivity(Activity activity,Class aClass){
         //導到其他頁面
         //Initialize intent
         Intent intent=new Intent(activity,aClass);
         intent.putExtra( "name",Name );
         intent.putExtra( "job",Job );
         intent.putExtra( "account",Account );
         intent.putExtra( "password",Password );
         //start activity
         activity.startActivity(intent);
    }

    public static void logout(Activity activity){
        //Initialize alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder( activity );
        //set title
        builder.setTitle( "登出提醒" );
        //set message
        builder.setMessage( "確定要登出嗎?" );
        //Positive yes button
        builder.setPositiveButton( "是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Finish activity
                activity.finishAffinity();
                //回到登入頁面
                Intent intent=new Intent(activity,MainActivity.class);
                activity.startActivity( intent );
            }
        } );
        //Negative no button
        builder.setNegativeButton( "否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Dismiss dialog
                dialog.dismiss();
            }
        } );
        builder.show();
    }

    @Override
    protected void onPause(){
       super.onPause();
       closeDrawer(drawerLayout);
    }
    //側邊選單code End
}