package com.example.mediacode01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediacode01.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Camera.PreviewCallback{

    private static final int CAMERA_PERMISSION_CODE = 1;
    private static final int AUDIO_PERMISSION_CODE = 2;
    private static final int WRITE_PERMISSION_CODE = 3;

    // Used to load the 'mediacode01' library on application startup.
    static {
        System.loadLibrary("mediacode01");
    }

    private ActivityMainBinding binding;


    private CameraHelper mCameraHelper;
    private VideoCodec videoCodec;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        check();
        mCameraHelper=new CameraHelper(640,480);
        mCameraHelper.setPreviewCallback(this);

        TextureView textureView=findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mCameraHelper.startPreview(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                mCameraHelper.stopPreview();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
        videoCodec=new VideoCodec();
        findViewById(R.id.btn_record).setOnClickListener(this);

    }
    public void check(){
        // 检查摄像头权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，向用户请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

// 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，向用户请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_CODE);
        }
        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，向用户请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, WRITE_PERMISSION_CODE);
        }
        if (ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 4);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了摄像头权限，可以执行相应的操作
            } else {
                // 用户拒绝了摄像头权限，可能需要显示一个提示或采取其他措施
            }
        }
        if (requestCode == AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了录音权限，可以执行相应的操作
            } else {
                // 用户拒绝了录音权限，可能需要显示一个提示或采取其他措施
            }
        }

        if (requestCode == WRITE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了录音权限，可以执行相应的操作
            } else {
                // 用户拒绝了录音权限，可能需要显示一个提示或采取其他措施
            }
        }
        if (requestCode == 4) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了录音权限，可以执行相应的操作
            } else {
                // 用户拒绝了录音权限，可能需要显示一个提示或采取其他措施
            }
        }
    }
    @Override
    protected void onDestroy(){super.onDestroy();}


    public native String stringFromJNI();

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
         videoCodec.queueEncode(data);
    }

    @Override
    public void onClick(View v) {
       Button button=(Button) v;
       if(videoCodec.isRecording()){
           button.setText("开始录制");
           videoCodec.stop();
       }else{
           button.setText("停止录制");
           videoCodec.startRecoding("/sdcard/DCIM/video.MP4",mCameraHelper.getWidth(),
           mCameraHelper.getHeight(),90);
       }
    }
}