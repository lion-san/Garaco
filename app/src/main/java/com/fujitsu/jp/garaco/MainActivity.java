package com.fujitsu.jp.garaco;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    // = 0 の部分は、適当な値に変更してください（とりあえず試すには問題ないですが）
    private static final int REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ボタンの押した動作
        Button button = (Button) findViewById(R.id.talk);

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
                //test code
                //Getリクエストの送信 for Garako
                /*SendHttpRequest http = new SendHttpRequest();
                String data = http.sendRequestToGarako("おはよう");
                WebView webView;
                webView = (WebView) findViewById(R.id.webView);
                //webView.loadUrl(url);
                webView.loadData(data, "text/html", null);*/
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

            //Getリクエストの送信 for Garako
            SendHttpRequest http = new SendHttpRequest();
            String json_org = http.sendRequestToGarako(resultsString);
            // トーストを使って結果を表示
            Toast.makeText(this, json_org, Toast.LENGTH_LONG).show();

            WebView webView;

            webView = (WebView) findViewById(R.id.webView);
            //webView.loadUrl(url);
            //webView.loadData(data, "text/html", null);
            webView.loadDataWithBaseURL(null, json_org, "text/html", "UTF-8", null);


            //----------------------------------
            //-- JSONの振り分け処理
            //----------------------------------
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
                            executeAction( event.getJSONArray("actions"));
                        }
                    }
                    else{//部分一致の場合
                        if(resultsString.indexOf(param) != -1){
                            //処理の実行
                            executeAction( event.getJSONArray("actions"));
                        }

                    }

                }

            } catch (JSONException e) {

            }

            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 処理の実行
     * @param actions
     */
    private void executeAction( JSONArray actions ) throws JSONException{


        for(int i = 0; i < actions.length(); i++){
            JSONObject action = actions.getJSONObject(i);

        }
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
}
