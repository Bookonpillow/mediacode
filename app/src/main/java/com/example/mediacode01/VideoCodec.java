package com.example.mediacode01;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.HandlerThread;
import android.provider.MediaStore;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class VideoCodec {
    private Handler mHandler;
    private MediaMuxer mMuxer;
    private MediaCodec mediaCodec;
    private MediaCodec audiomediacodec;
    private int videoTrack;
    private int audioTrack;
    private boolean isRecording;
    public void startRecoding(String path,int width,int height,int degress){
        try{
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            //色彩空间
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            //码率
            format.setInteger(MediaFormat.KEY_BIT_RATE,500_000);
            //帧率fps
            format.setInteger(MediaFormat.KEY_FRAME_RATE,20);
            //关键帧间隔
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);

            mediaCodec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

            mediaCodec.start();
            //混合器 音频+视频 MP4
            mMuxer=new MediaMuxer(path,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    );
            mMuxer.setOrientationHint(degress);
        }catch (IllegalStateException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        HandlerThread thread=new HandlerThread("videoCodec");
        thread.start();
        mHandler= new Handler(thread.getLooper());
        isRecording=true;
    }
    public boolean isRecording(){return isRecording;}

    public void stop() {
        isRecording = false;
        mMuxer.stop();
        mMuxer.release();
    }
    public void queueEncode(byte[] buffer){
        if(!isRecording){
            Log.e("xxx","没开始录制");
            return;
        }
        Log.e("xxx","开始录制");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                     //立即得到有效输入缓冲区
                int index=mediaCodec.dequeueInputBuffer(0);
                if(index>=0){
                    ByteBuffer inputBuffer=mediaCodec.getInputBuffer(index);
                    inputBuffer.clear();
                    inputBuffer.put(buffer,0,buffer.length);
                    //填充数据后再加入队列
                    mediaCodec.queueInputBuffer(index,0,buffer.length,System.nanoTime()/1000,0);
                }
                while (true){
                    //获取输出缓冲区（编码后的数据从缓冲区获取）
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
                    //稍后重试
                    if(encoderStatus== MediaCodec.INFO_TRY_AGAIN_LATER){
                        break;
                    }else if(encoderStatus==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                        //输出格式发生变化 第一次总会调用，所以在这里开启混合器
                        MediaFormat newFormat=mediaCodec.getOutputFormat();
                        videoTrack=mMuxer.addTrack(newFormat);
                        mMuxer.start();
                    }else if(encoderStatus==MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                        //可以忽略
                    }else{
                        //正常则encoderStatus 获取缓冲区下标
                        ByteBuffer encodedData=mediaCodec.getOutputBuffer(encoderStatus);
                        //如果当前的buffer是配置信息，不管它，不用写出去
                        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0){
                            bufferInfo.size=0;
                        }
                        if(bufferInfo.size!=0){
                            //设置从哪里开始读数据（读出来就是编码后的数据）
                            encodedData.position(bufferInfo.offset);
                            //设置能读数据的总长度
                            encodedData.limit(bufferInfo.offset+bufferInfo.size);
                            //写出为MP4
                            mMuxer.writeSampleData(videoTrack,encodedData,bufferInfo);
                        }
                        //释放这个缓冲区，后续可以存放新的编码后的数据
                         mediaCodec.releaseOutputBuffer(encoderStatus,false);
                    }
                }
            }
        });
    }

}
