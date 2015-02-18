package com.fujitsu.jp.garaco;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.view.WindowManager.LayoutParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends ActionBarActivity implements TextToSpeech.OnInitListener {

    // = 0 の部分は、適当な値に変更してください（とりあえず試すには問題ないですが）
    private static final int REQUEST_CODE = 0;

    private TextToSpeech tts;
    private Context context;

    private ProgressDialog progressBar;

    private ActionHandler act;

    private MyAsyncTask task;
    private String res = null;

    private Date lasttime = null;



    /** カメラのハードウェアを操作する {@link Camera} クラスです。 */
    private Camera mCamera;
    /** カメラのプレビューを表示する {@link SurfaceView} です。 */
    private SurfaceView mView;
    private CameraOverlayView mCameraOverlayView;

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //String ready = "準備OKです";
            //tts.speak(ready, TextToSpeech.QUEUE_FLUSH, null);
            //Toast.makeText(this, ready, Toast.LENGTH_SHORT).show();
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

        //カメラ
        mView = (SurfaceView) findViewById(R.id.surfaceView);
        //mView = new SurfaceView(this);
        //setContentView(mView);//ここでActivityに設定してしまうので、SurfaceViewのみに設定する
        //SurfaceHolder holder = mView.getHolder();
        //holder.addCallback(surfaceHolderCallback);
        //holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //顔検知用重畳ビュー
        mCameraOverlayView = new CameraOverlayView(this);
        addContentView(mCameraOverlayView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));


        //Robotプログラムスタート
        initRobot();


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


    private void initRobot ( ){
        // サブスレッドで実行するタスクを作成
        task = new MyAsyncTask() {
            @Override
            protected String doInBackground(String... params) {
                String resultsString = params[0];
                try {
                    // Twitter フォロー実行
                    SendHttpRequest http = new SendHttpRequest();
                    String json_org = http.sendRequestToGarako("");

                    res = json_org;//インスタンス変数にＪＳＯＮ(命令セット)をセット

                    this.setParam(resultsString);

                    progressBar.dismiss();//消去

                    return json_org;
                } catch (Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();

                }
                return null;
            }

            @Override
            protected void onPostExecute(String json_org) {
                String ready = "ロボナイゼーション。イニシャライズド";
                tts.speak(ready, TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(this.getActivity(), ready, Toast.LENGTH_SHORT).show();

            }
        };

        try {

            //アクションハンドラの生成
            act = new ActionHandler(this);

            task.setActivity(this);
            task.setTts(this.tts);
            act.setmCam(mCamera);
            act.setContext(context);

            //非同期処理開始
            task.execute("");
        }catch( Exception e){
            e.printStackTrace();
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
        task = new MyAsyncTask() {
            @Override
            protected String doInBackground(String... params) {
                String resultsString = params[0];
                try {

                    if(res == null) {

                        // Twitter フォロー実行
                        SendHttpRequest http = new SendHttpRequest();
                        String json_org = http.sendRequestToGarako(resultsString);
                        res = json_org;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();

                }
                progressBar.dismiss();//消去
                this.setParam(resultsString);
                return res;
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


                act.setTts(this.getTts());
                //act.setContext(context);

                //----------------------------------
                //-- JSONの振り分け処理
                //----------------------------------

                act.analyzeJson(resultsString, json_org);

            }
        };

        task.setActivity(this);
        task.setTts( this.tts );
        //アクションハンドラの生成
        //act = new ActionHandler( this );
        act.setContext(context);
        act.setmCam( mCamera );

        task.execute( resultsString );
     }


//--------------------------------------------------------------------------------
//--- Camera -----------------------------------------------------------------------------
//--------------------------------------------------------------------------------

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //コールバック関数をセット
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(surfaceHolderCallback);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /** カメラのコールバックです。 */
    private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            try {
                // 生成されたとき
                mCamera = Camera.open(1);

                // リスナをセット  // 顔検出の開始
                mCamera.setFaceDetectionListener(faceDetectionListener);
                mCamera.startFaceDetection();

                // プレビューをセットする
                mCamera.setPreviewDisplay(holder);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {
            @Override
            public void onFaceDetection(Face[] faces, Camera camera) {
                Log.d("onFaceDetection", "顔検出数:" + faces.length);
                // View に渡す
                //mCameraOverlayView.setFaces(faces);


                if(faces.length > 0){

                    lasttime = null;

                    if (!act.getFace_ditect()) {

                        act.setFace_ditect(true);
                        //tts.speak("侵入者を検知しました", TextToSpeech.QUEUE_FLUSH, null);
                        // 画像取得
                        //mCamera.takePicture(null, null, mPicJpgListener);
                        executeRobot(StaticParams.FACE_DETECT);

                        lasttime = null;
                    }
                }
                else{
                    if(lasttime == null )
                        lasttime = new Date();

                    //検知ゼロが指定ミリ秒以上続くまで、処理しない
                    else if ( ((new Date()).getTime() - lasttime.getTime() > 10000)){
                        act.setFace_ditect(false);//フラグをもどす
                        if( lasttime != null){
                        long a = (( new Date()).getTime() - lasttime.getTime());
                        Log.d("#######################", "時間（ミリ秒）"+ a);}
                    }
                }

            }
        };

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            // 変更されたとき
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = previewSizes.get(0);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            //parameters.setPreviewSize(640, 480);
            // width, heightを変更する
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // 破棄されたとき
            mCamera.release();
            mCamera = null;
        }

    };

    /**
     * JPEG データ生成完了時のコールバック
     */
    private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null) {
                return;
            }

            String saveDir = Environment.getExternalStorageDirectory().getPath() + "/garaco";

            // SD カードフォルダを取得
            File file = new File(saveDir);

            // フォルダ作成
            if (!file.exists()) {
                if (!file.mkdir()) {
                    Log.e("Debug", "Make Dir Error");
                }
            }

            // 画像保存パス
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String imgPath = saveDir + "/" + sf.format(cal.getTime()) + ".jpg";

            // ファイル保存
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(imgPath, true);
                fos.write(data);
                fos.close();

                // アンドロイドのデータベースへ登録
                // (登録しないとギャラリーなどにすぐに反映されないため)
                registAndroidDB(imgPath);

            } catch (Exception e) {
                Log.e("Debug", e.getMessage());
            }

            fos = null;

            // takePicture するとプレビューが停止するので、再度プレビュースタート
            mCamera.startPreview();

            // mIsTake = false;
        }
    };

    /**
     * アンドロイドのデータベースへ画像のパスを登録
     * @param path 登録するパス
     */
    private void registAndroidDB(String path) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }


//--------------------------------------------------------------------------------
//--------------------------------------------------------------------------------
//--------------------------------------------------------------------------------


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
        //mCamera.release();
    }
}
