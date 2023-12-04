package com.hzx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aispeech.DUILiteConfig;
import com.aispeech.DUILiteSDK;
import com.hzx.speechtool.R;
import com.hzx.ui.CloudAsrActivity;
import com.hzx.util.AIPermissionRequest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "MainActivity";
    private Button btn_localAsr,btn_cloudAsr;
    private AIPermissionRequest mPermissionRequest;
    private static boolean haveAuth = false;

    Toast mToast;

    private AIPermissionRequest.PermissionGrant mPermissionGrant = new AIPermissionRequest.PermissionGrant() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case AIPermissionRequest.CODE_READ_PHONE_STATE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_READ_PHONE_STATE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_RECORD_AUDIO:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_RECORD_AUDIO", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case AIPermissionRequest.CODE_READ_CONTACTS:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_READ_CONTACTS", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.currentThread().setName("t-lite-main");
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        //实例化动态权限申请类
        mPermissionRequest = new AIPermissionRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestMulti();//所有权限一同申请
            com.aispeech.common.Log.d(TAG, "request all needed　permissions");
        }

        initView();

    }

    public void initView(){
        btn_localAsr = findViewById(R.id.btn_localAsr);
        btn_cloudAsr = findViewById(R.id.btn_cloudAsr);
        

        btn_localAsr.setOnClickListener(this);
        btn_cloudAsr.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        if (v == btn_localAsr) {
            
        } else if (v == btn_cloudAsr) {
            Log.i("TAG", "onClick: " + "cloud");
            Intent intent = new Intent(MainActivity.this, CloudAsrActivity.class);
            startActivity(intent);
        }

    }


    public void auth(View view){
        // 产品认证需设置 apiKey, productId, productKey, productSecret
        DUILiteConfig config = new DUILiteConfig(
                "dfa688367feadfa688367fea656d8386",
                "279621476",
                "7fc0f18fd606c4f3916ab5d9d7ecbe83",
                "20bf043850f71959ea69e5e5cafd0923",null,true);

        //设置授权连接超时时长，默认5000ms
        config.setAuthTimeout(5000);
        // 自定义设置授权文件的保存路径,需要确保该路径事先存在
        config.setDeviceProfileDirPath("/sdcard/speech");
        //设置SDK录音模式
        // 单麦 单麦Echo 双麦 线性4麦 环形4麦 环形6麦  使用 Spinner 选择的
        config.setAudioRecorderType(DUILiteConfig.TYPE_COMMON_MIC);//使用单麦
        config.setExtraParameter("DEVICE_ID", "666666666");

        //输出SDK logcat日志，同时保存日志文件在/sdcard/duilite/DUILite_SDK.log，须在init之前调用.
        DUILiteSDK.openLog(this.getApplication(), "/sdcard/duilite/DUILite_SDK.log");

        String core_version = DUILiteSDK.getCoreVersion();//获取内核版本号
        com.aispeech.common.Log.d(TAG, "core version is: " + core_version);

        boolean isAuthorized = DUILiteSDK.isAuthorized(getApplicationContext());//查询授权状态，DUILiteSDK.init之后随时可以调
        com.aispeech.common.Log.d(TAG, "DUILite SDK is isAuthorized ？ " + isAuthorized);

        DUILiteSDK.init(getApplicationContext(),config, new DUILiteSDK.InitListener() {
            @Override
            public void success() {
                Log.i(TAG, "auth success: ");
                haveAuth = true;
                showTip("授权成功！");
            }

            @Override
            public void error(String errorCode, String errorInfo) {
                Log.i(TAG, "error: " + "errorCode is " + errorCode + ",errorInfo is " + errorInfo);
                haveAuth = false;
                showTip("授权失败！");
            }
        });
    }

    public void requestMulti() {
        mPermissionRequest.requestMultiPermissions(this, mPermissionGrant);
    }

    public void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }
}