package com.fujitsu.jp.garaco;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements TextToSpeech.OnInitListener {

    // = 0 の部分は、適当な値に変更してください（とりあえず試すには問題ないですが）
    private static final int REQUEST_CODE = 0;

    private TextToSpeech tts;
    private Context context;

    private ProgressDialog progressBar;

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            String ready = "準備OKです";
            //tts.speak(ready, TextToSpeech.QUEUE_FLUSH, null);
            Toast.makeText(this, ready, Toast.LENGTH_SHORT).show();
        } else {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Contextの取得
        context = getApplicationContext();

        //ボタンの押した動作
        Button button = (Button) findViewById(R.id.talk);

        //テストの押した動作
        Button send = (Button) findViewById(R.id.send);

        //TTSの初期化
        tts = new TextToSpeech(context, this);

        //ぐるぐる
        progressBar = new ProgressDialog(this);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setMessage("処理を実行中しています");
        progressBar.setCancelable(true);



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    // インテント作成
                    Intent intent = new Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // ACTION_WEB_SEARCH
                    intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "Let's say!"); // お好きな文字に変更できます

                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);// 取得する結果の数

                    // インテント発行
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // このインテントに応答できるアクティビティがインストールされていない場合
                    Toast.makeText(MainActivity.this,
                            "ActivityNotFoundException", Toast.LENGTH_LONG).show();
                }
            }
        });

        //ボタン用
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//テストの押した動作
                EditText txt = (EditText) findViewById(R.id.txt1);
                SpannableStringBuilder sb = (SpannableStringBuilder)txt.getText();
                String str = sb.toString();
                executeRobot( str );
            }
        });
    }

    // アクティビティ終了時に呼び出される
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 自分が投げたインテントであれば応答する
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            String resultsString = "";

            // 結果文字列リスト
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            /*for (int i = 0; i< results.size(); i++) {
                // ここでは、文字列が複数あった場合に結合しています
                resultsString += results.get(i);
            }*/
            resultsString = results.get(0);

            // トーストを使って結果を表示
            Toast.makeText(this, resultsString, Toast.LENGTH_LONG).show();

            //会話から実行
            executeRobot( resultsString );


            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * executeRobot
     */
    private void executeRobot( String resultsString ){

        //表示
        progressBar.show();

        //Getリクエストの送信 for Garako
        //SendHttpRequest http = new SendHttpRequest();
        //String json_org = http.sendRequestToGarako(resultsString);


        // サブスレッドで実行するタスクを作成
        MyAsyncTask task = new MyAsyncTask() {
            @Override
            protected String doInBackground(String... params) {
                String resultsString = params[0];
                try {
                    // Twitter フォロー実行
                    SendHttpRequest http = new SendHttpRequest();
                    String json_org = http.sendRequestToGarako(resultsString);

                    this.setParam( resultsString );

                    progressBar.dismiss();//消去


                    return json_org;
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String json_org) {

                // トーストを使って結果を表示
                //Toast.makeText(this.getActivity(), json_org, Toast.LENGTH_SHORT).show();

                //WebView webView = (WebView) findViewById(R.id.webView);
                //webView.loadUrl(url);
                //webView.loadData(data, "text/html", null);
                //webView.loadDataWithBaseURL(null, json_org, "text/html", "UTF-8", null);

                String resultsString = this.getParam();

                ActionHandler act = new ActionHandler();
                act.setTts(this.getTts());
                act.setContext( context );

                //----------------------------------
                //-- JSONの振り分け処理
                //----------------------------------
                boolean flg = false;
                try {
                    JSONArray  jsons = new JSONArray(json_org);

                    for (int i = 0; i < jsons.length(); i++) {
                        // 予報情報を取得
                        JSONObject event = jsons.getJSONObject(i);
                        // Event
                        String e = event.getString("event");
                        // Operator
                        String operator = event.getString("operator");
                        // 条件
                        String param = event.getString("param");


                        //条件の検査
                        if( operator.equals("==")){//完全一致の場合

                            if(resultsString.equals( param )){
                                //処理の実行
                                act.executeAction(this.getActivity(), event.getJSONArray("actions"));
                                flg = true;
                            }
                        }
                        else{//部分一致の場合
                            if(resultsString.indexOf(param) != -1){
                                //処理の実行
                                act.executeAction(this.getActivity(), event.getJSONArray("actions"));
                                flg = true;
                            }

                        }

                    }

                    if( !flg )
                        Toast.makeText(this.getActivity(), "何も該当しませんでした。", Toast.LENGTH_SHORT).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        };
        task.execute( resultsString );
        task.setActivity(this);
        task.setTts( this.tts );

     }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.shutdown();
    }
}
