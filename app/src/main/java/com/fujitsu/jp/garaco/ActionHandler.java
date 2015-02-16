package com.fujitsu.jp.garaco;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by clotcr_22 on 2015/02/16.
 */
public class ActionHandler {

    private Activity activity;
    private TextToSpeech tts;

    /**
     * 処理の実行
     * @param actions
     */
    protected void executeAction( Activity act, JSONArray actions ) throws JSONException {

        //インスタンス変数にセット
        activity = act;

        for(int i = 0; i < actions.length(); i++){
            JSONObject action = actions.getJSONObject(i);

            Toast.makeText(activity, action.getString("action"), Toast.LENGTH_SHORT).show();
            Toast.makeText(activity, action.getString("param"), Toast.LENGTH_SHORT).show();

            //actionに基づき動作
            exec(action.getString("action"),  action.getString("param"));

        }
    }

    private void exec( String action, String param){

        switch ( action ){

            case "talk":
                doTalk( param );
                break;

            case "camera":
                break;

            case "light":
                //doFlash();
                break;

            case "wait":
                break;

            default:
                break;
        }


    }

    private void doWait( String param ){

    }

    private void doTalk( String param){

        tts.speak(param, TextToSpeech.QUEUE_FLUSH, null);

    }

    private void doFlash(){

        Camera camera;

        //カメラデバイス取得
        camera = Camera.open();
        //カメラデバイス動作開始
        camera.startPreview();

        //パラメータ取得
        Camera.Parameters params = camera.getParameters();
        //フラッシュモードを点灯に設定
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        //パラメータ設定
        camera.setParameters(params);
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
}
