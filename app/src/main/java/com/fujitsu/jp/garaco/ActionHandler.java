package com.fujitsu.jp.garaco;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by clotcr_22 on 2015/02/16.
 */
public class ActionHandler {

    private Activity activity;
    private Context context;
    private TextToSpeech tts;
    private Camera mCam;
    private WebView web;

    private Boolean face_ditect = false;

    /**
     * コンストラクタ
     */
    public ActionHandler(Activity activity) {
        this.activity = activity;
    }

    synchronized protected void analyzeJson( String resultsString, String json_org ){

        Boolean flg = false;

        try{

            JSONArray jsons = new JSONArray(json_org);

            web.loadUrl(StaticParams.ACTION_ANIMATION);
            web.reload();

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
                        this.executeAction(this.getActivity(), event.getJSONArray("actions"));
                        flg = true;
                    }
                }
                else if((param.equals(StaticParams.FACE_DETECT) && (face_ditect))){//顔検知の場合
                    //処理の実行
                    this.executeAction(this.getActivity(), event.getJSONArray("actions"));
                    flg = true;
                }
                else{//部分一致の場合
                    if(resultsString.indexOf(param) != -1){
                        //処理の実行
                        this.executeAction(this.getActivity(), event.getJSONArray("actions"));
                        flg = true;
                    }

                }
            }

            if( !flg ) {
                Toast.makeText(activity, "何も該当しませんでした。", Toast.LENGTH_SHORT).show();
                doTalk(resultsString +"が理解できませんでした。意味を教えてください。");
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Network Busy!", Toast.LENGTH_SHORT).show();
            return;
        }
    }


    /**
     * 処理の実行
     * @param actions
     */
    protected void executeAction( Activity act, JSONArray actions ) throws JSONException {

        //インスタンス変数にセット
        activity = act;

        for(int i = 0; i < actions.length(); i++){
            JSONObject action = actions.getJSONObject(i);

            //Toast.makeText(activity, action.getString("action"), Toast.LENGTH_SHORT).show();
            //Toast.makeText(activity, action.getString("param"), Toast.LENGTH_SHORT).show();

            //actionに基づき動作
            exec(action.getString("action"),  action.getString("param"));

        }

        //web.loadUrl(StaticParams.STOP_ANIMATION);
    }

    private void exec( String action, String param){

        switch ( action ){

            case "talk":
                doTalk(param);
                break;

            case "camera":
                doCamera();
                break;

            case "light":
                doFlash();
                break;

            case "wait":
                doWait( param );
                break;

            case "application":
                doApplication(param);
                break;

            default:
                break;
        }


    }

    /**
     *
     * @param param
     */
    synchronized private void doWait( String param ){

        try {
            Thread.sleep( Integer.parseInt(param) * 1000 );
            Toast.makeText(activity, param+"秒待ちます", Toast.LENGTH_LONG).show();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param param
     */
    synchronized private void doTalk( String param){

        tts.speak(param, TextToSpeech.QUEUE_ADD, null);
        Toast.makeText(activity, param, Toast.LENGTH_SHORT).show();

    }

    synchronized private void doCamera( ){

        try {
            // 画像取得
            mCam.takePicture(null, null, mPicJpgListener);
        }
            catch (Exception e){
                e.printStackTrace();
            }
    }

    /**
     *
     */
    synchronized private void doFlash(){

        generateNotification("");
    }


    synchronized private void doApplication( String param ){

        PackageManager pm = activity.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(param);

        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "対象のアプリがありません", Toast.LENGTH_SHORT).show();
        }


    }


    /**
     *
     * @param param
     */
    private void generateNotification(String param) {

        //システムトレイに通知するアイコン
       // int icon = R.drawable.ic_stat_gcm;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(0, "", when);
        //String title = context.getString(R.string.app_name);

              //ステータスバーをクリックした時に立ち上がるアクティビティ
        //Intent notificationIntent = new Intent(context, OfferDisplayActivity.class);

        /*notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
            Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent =
            PendingIntent.getActivity(context, 0, notificationIntent, 0);*/

        //notification.setLatestEventInfo(context, title, message, intent);
                //通知の種類　音 バイブにしている時は鳴らない　
        //notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE ;
        notification.flags =  Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL ;
        notification.ledOnMS = 3000;
        notification.ledOffMS = 1000;
        notification.ledARGB = Color.BLUE;

        NotificationManager notificationManager =
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

        doWait("3");
        notificationManager.cancel( 0 );
    }

    /**
     * JPEG データ生成完了時のコールバック
     */
    private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
        synchronized  public void onPictureTaken(byte[] data, Camera camera) {
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
            mCam.startPreview();

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


    /**
     *
     */
    protected void cameraDestroy() {

        if (mCam != null) {
            mCam.release();
            mCam = null;
        }
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public TextToSpeech getTts() {
        return tts;
    }

    public void setTts(TextToSpeech tts) {
        this.tts = tts;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Camera getmCam() {
        return mCam;
    }

    public void setmCam(Camera mCam) {
        this.mCam = mCam;
    }

    public Boolean getFace_ditect() {
        return face_ditect;
    }

    public void setFace_ditect(Boolean face_ditect) {
        this.face_ditect = face_ditect;
    }

    public WebView getWeb() {
        return web;
    }

    public void setWeb(WebView web) {
        this.web = web;
    }
}
