package com.example.mediacode01;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.util.List;

public class CameraHelper implements Camera.PreviewCallback {
    public static final String TAG="CameraHelper";
    private int width;
    private int height;
    private int mCameraId;
    private Camera mCamera;
    private byte[] buffer;
    private Camera.PreviewCallback mPreviewCallback;
    private SurfaceTexture mSurfaceTexture;

    public CameraHelper(int width,int height){
        this.width=width;
        this.height=height;
        mCameraId= Camera.CameraInfo.CAMERA_FACING_BACK;
    }
    public void switchCamera(){
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraId=Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else{
            mCameraId=Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview(mSurfaceTexture);
    }
    public int  getmCameraId(){
        return mCameraId;
    }
    public void stopPreview(){
        if(mCamera!=null){
            //预览数据回调接口
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放摄像头
            mCamera.release();
            mCamera=null;
        }
    }
    public void startPreview(SurfaceTexture surfaceTexture){
        stopPreview();
        try {
            mSurfaceTexture = surfaceTexture;
            //获取camera属性
            mCamera = Camera.open(mCameraId);
            //配置cammera的属性
            Camera.Parameters parameters = mCamera.getParameters();
            //设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            boolean isSupportSize = false;
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                if (supportedPreviewSize.width == width && supportedPreviewSize.height == height) {
                    isSupportSize = true;
                    break;
                }
            }
            if (!isSupportSize) {
                Camera.Size size=supportedPreviewSizes.get(0);
                width=size.width;
                height=size.height;
            }
            //这里是摄像头的宽高
            parameters.setPreviewSize(width,height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setDisplayOrientation(90);
            //设置摄像头 图像传感器的角度、方向
            mCamera.setParameters(parameters);
            buffer=new byte[width*height*3/2];
            i420=new byte[width*height*3/2];
            //数据缓存区
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            //设置预览画面
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    public int getWidth(){return width;}
    public int getHeight(){return height;}

    public void setPreviewCallback(Camera.PreviewCallback previewCallback){
        mPreviewCallback=previewCallback;
    }
    byte[] i420;
    private void nv21ToI420(byte[] data){
        //y数据
        System.arraycopy(data,0,i420,0,width*height);
        int index=width*height;
        for(int i=width*height;i<data.length;i+=2){
            i420[index++]=data[i+1];
        }
        for(int i=width*height;i<data.length;i+=2){
            i420[index++]=data[i];
        }
    }

@Override
public void onPreviewFrame(byte[] data, Camera camera) {
    if(null!=mPreviewCallback) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Allocation bmData = renderScriptNV21ToRGBA888(MyApplication.getContext(), width, height, data);
        bmData.copyTo(bitmap);
        mPreviewCallback.onPreviewFrame(data,camera);
    }
    camera.addCallbackBuffer(buffer);

}

    public Allocation renderScriptNV21ToRGBA888(Context context, int width, int height, byte[] nv21) {

        RenderScript rs = RenderScript.create(context);

        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);

        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);

        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);

        yuvToRgbIntrinsic.forEach(out);

        return out;
    }
}
