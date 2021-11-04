package com.example.dcardtry;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dcardtry.SQLconnect.MysqlCon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class ArticlePageActivity extends AppCompatActivity {
    ///
    List<GetDcardInfo> dcardList;
    List<String> chartValue;
    Integer neg, neu, pos;
    RecyclerView mRecyclerView;
    MyAdapter adapter;

    boolean success = false;
    MysqlCon con;
    RecyclerView.LayoutManager mLayoutManager;

    ListView lv;

    List<String> selected = new ArrayList<String>();
///


    //private static final String DCARD_URL = "http://172.26.8.81:13306/GetData4.php";
    private DrawerLayout drawerLayout;
    String Name, Job, Account, Password;//接收帳號相關資料
    TextView DM_Tilte;//側邊選單標題 : 姓名+職稱


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_page);

        mRecyclerView = findViewById(R.id.ArticlePage_RecyclerView);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        con = new MysqlCon();

        dcardList = new ArrayList<>();





        //設定隱藏標題
        getSupportActionBar().hide();
        //設定隱藏狀態
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);

        SyncData orderData = new SyncData();
        orderData.execute("");
        /*recycleview教學
        Article_Summary.setLayoutManager(new LinearLayoutManager(this));
        Article_Summary.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new MyAdapter(mData);
        Article_Summary.setAdapter(adapter);*/

        //AP_LoadDcard();

        //取得傳遞過來的資料
        Intent intent = this.getIntent();
        Name = intent.getStringExtra("name");
        Job = intent.getStringExtra("job");
        Account = intent.getStringExtra("account");
        Password = intent.getStringExtra("password");

        //加上側邊選單姓名、職稱
        DM_Tilte = findViewById(R.id.drawer_menu_title);
        DM_Tilte.setText("\t" + Name + "\n" + Job + "\t\t 您好");


    }
   /* public void AP_LoadDcard() {

        int i =0;
        new Thread(() -> {


            String[]da;
            MysqlCon getdata = new MysqlCon();
            // 讀取資料
            String data = getdata.getdcarddata();
            Log.v("OK",data);
            da=data.split("nex1");
            // selected.add(data);
            for(int j=0;j<da.length;j++) {
                selected.add(da[j]);
            }


            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    com.example.dcardtry.ArticlePage.this,
                    android.R.layout.simple_list_item_1,
                    (List<String>) selected);
            arrayAdapter.notifyDataSetChanged();
            lv.post(() -> lv.setAdapter(arrayAdapter));



        }).start();
    }*/


    private class SyncData extends AsyncTask<String, String, String> {

        String msg = "Internet/DB_Credentials/Windows_FireWall_TurnOn ERROR, See Android Monitor in the bottom for details!";
        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ArticlePageActivity.this, "Synchronising", "RecycleView Loading, Please Wait...", true);
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
            Toast.makeText(ArticlePageActivity.this, "" + msg, Toast.LENGTH_LONG).show();
            if (success == false) {

            } else {
                try {
                    adapter = new MyAdapter(getApplicationContext(), dcardList);
                    mRecyclerView.setAdapter(adapter);
                } catch (Exception e) {

                }
            }
        }
    }

    private void AP_LoadDcard() {


    }

    public void geturl(int i) {


        new Thread(() -> {
            String v = "";
            String[] urllist;
            MysqlCon geturl = new MysqlCon();
            // 讀取資料
            final String data = geturl.getdcarddata();
            Log.v("OK", v);


        }).start();
    }

    //側邊選單code Strat
    public void ClickMenu(View view) {
        openDrawer(drawerLayout);
    }

    public void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public void ClickCancle(View view) {
        closeDrawer(drawerLayout);
    }

    public static void closeDrawer(DrawerLayout drawerLayout) {
        //check condition
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            //when drawer is open
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void ClickHome(View view) {
        //Restart activity_home_page.xml
        redirectActivity(this, HomePage.class);
    }

    public void ClickArticle(View view) {
        //Redirect(重定向) activity to articlePage
        closeDrawer(drawerLayout);
    }

    public void ClickChart(View view) {
        //Redirect(重定向) activity to chartPage
        //redirectActivity(this,);
    }

    public void ClickAccountInfo(View view) {
        //Redirect(重定向) activity to accountPage(帳號管理頁面)
        redirectActivity(this, UserChangePassword.class);
    }

    public void ClickLogout(View view) {
        //回到登入頁面
        logout(this);
    }

    public void redirectActivity(Activity activity, Class aClass) {
        //導到其他頁面
        //Initialize intent
        Intent intent = new Intent(activity, aClass);
        intent.putExtra("name", Name);
        intent.putExtra("job", Job);
        intent.putExtra("account", Account);
        intent.putExtra("password", Password);
        //set flag
        //intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        //start activity
        activity.startActivity(intent);
    }

    public static void logout(Activity activity) {
        //Initialize alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        //set title
        builder.setTitle("登出提醒");
        //set message
        builder.setMessage("確定要登出嗎?");
        //Positive yes button
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Finish activity
                activity.finishAffinity();
                //回到登入頁面
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
            }
        });
        //Negative no button
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Dismiss dialog
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeDrawer(drawerLayout);
    }

}


