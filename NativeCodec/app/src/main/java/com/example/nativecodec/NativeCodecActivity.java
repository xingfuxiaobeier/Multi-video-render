/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nativecodec;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

import cn.ismartv.player.IsmartvPlayer;

public class NativeCodecActivity extends Activity implements OnClickListener, SurfaceHolder.Callback, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    static {
        System.loadLibrary("native-codec-jni");
        System.loadLibrary("ijkffmpeg");

    }

    private static final String TAG = "NativeCodecActivity";

    private String mSourceString = "/storage/emulated/0/aweidasheng/asset/lmzg/asset2.mp4";//"/storage/emulated/0/aweidasheng/asset/ybbz/asset1.mp4"/*"/storage/emulated/0/test.mp4"*/;
    private String maskSource = "/storage/emulated/0/aweidasheng/asset/lmzg/asset1.mp4";//"/storage/emulated/0/aweidasheng/asset/ybbz/asset0.mp4";

    private SurfaceView mSurfaceView1;
    private SurfaceHolder mSurfaceHolder1;
    private VideoSink mSelectedVideoSink;
    private VideoSink mNativeCodecPlayerVideoSink;

    private SurfaceHolderVideoSink mSurfaceHolder1VideoSink;
    private GLViewVideoSink mGLView1VideoSink;
    private GLViewVideoSink mGLView1VideoSink2;

    private boolean mCreated = false;
    private boolean mIsPlaying = false;

    private IsmartvPlayer mIsmartvPlayer;
    private IsmartvPlayer player2;
    private IsmartvPlayer ffplayer;
    private boolean softDecode = true;

    private MyGLSurfaceView mGLView1;
    private ImageGLSurfaceView mGLView2;

    private RadioButton mRadio1;
    private RadioButton mRadio2;
    private RadioButton mRadio3;
    private Button startBtn;
    private Button rewindBtn;
    private Button frameBtn;

    private TextView durationTextView;
    private SeekBar progressbar;

    private int currentSeekPosition;

    private float height[] = {1, 0.5f, 0.5f};

    //add
    private static final int LOAD_PICTURE_SUCCSESS = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_PICTURE_SUCCSESS:
                    //
                    setBlendPic((Bitmap) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private void setBlendPic(Bitmap bitmap) {
        if (mGLView1 != null) {
            mGLView1.setBlendPic(bitmap);
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mIsmartvPlayer = new IsmartvPlayer();
        if (!softDecode) {
            player2 = new IsmartvPlayer();
        }
        ffplayer = new IsmartvPlayer();

        mGLView1 = (MyGLSurfaceView) findViewById(R.id.glsurfaceview1);
        mGLView2 = (ImageGLSurfaceView) findViewById(R.id.glsurfaceview2);

        mGLView1.setGetFramePlayer(ffplayer);
        mGLView2.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        mGLView1.requestRender();

        // set up the Surface 1 video sink
        mSurfaceView1 = (SurfaceView) findViewById(R.id.surfaceview1);
        mSurfaceHolder1 = mSurfaceView1.getHolder();
        mSurfaceHolder1.addCallback(this);

        // initialize content source spinner
//        Spinner sourceSpinner = (Spinner) findViewById(R.id.source_spinner);
//        ArrayAdapter<CharSequence> sourceAdapter = ArrayAdapter.createFromResource(
//                this, R.array.source_array, android.R.layout.simple_spinner_item);
//        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        sourceSpinner.setAdapter(sourceAdapter);
//        sourceSpinner.setOnItemSelectedListener(this);

        mRadio1 = (RadioButton) findViewById(R.id.radio1);
        mRadio2 = (RadioButton) findViewById(R.id.radio2);
        mRadio3 = (RadioButton) findViewById(R.id.radio3);

        mRadio1.setOnCheckedChangeListener(this);
        mRadio1.toggle();
        mRadio2.setOnCheckedChangeListener(this);
        mRadio3.setOnCheckedChangeListener(this);
        // the surfaces themselves are easier targets than the radio buttons
        mSurfaceView1.setOnClickListener(this);
        mGLView1.setOnClickListener(this);

        // native MediaPlayer videoStart/pause
        startBtn = ((Button) findViewById(R.id.start_native));
        startBtn.setOnClickListener(this);

        // native MediaPlayer rewind
        rewindBtn = ((Button) findViewById(R.id.rewind_native));
        rewindBtn.setOnClickListener(this);

        //get a frame
        frameBtn = ((Button) findViewById(R.id.get_frame));
        frameBtn.setOnClickListener(this);

//        durationTextView = (TextView) findViewById(R.id.duration);

//        progressbar = (SeekBar) findViewById(R.id.progressbar);
//        progressbar.setOnSeekBarChangeListener(this);


    }

    private void readAssertFile(final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap blendPic = GLUtil.getImageFromAssetsFile(getApplicationContext(), name);
                //handler
                Message message = Message.obtain();
                message.what = LOAD_PICTURE_SUCCSESS;
                message.obj = blendPic;
                handler.sendMessage(message);
            }
        }).start();
    }

    void switchSurface(int checkIndex) {
        if (checkIndex == 0 || checkIndex == 1) {
            if (mCreated && mNativeCodecPlayerVideoSink != mSelectedVideoSink) {
                mIsmartvPlayer._stop();
                mCreated = false;
                mSelectedVideoSink.useAsSinkForNative();
                mNativeCodecPlayerVideoSink = mSelectedVideoSink;
                if (mSourceString != null) {
                    Log.i("@@@", "recreating player");
                    mCreated = mIsmartvPlayer._prepare(mSourceString);
                    Log.d(TAG, "duration: " + mIsmartvPlayer._getDuration());
//                durationTextView.setText(Util.formatTime(mIsmartvPlayer._getDuration()));
                    mIsPlaying = false;
                }

                if (!softDecode) {
                    //player2
                    if (mSelectedVideoSink instanceof GLViewVideoSink) {
                        Log.i(TAG, "switchSurface: when glsurface, shutting down player2 and reprepare it");
                        player2._stop();
                        ((GLViewVideoSink) mSelectedVideoSink).useAsSinkForNative2();
                        player2._prepare(maskSource);
                    }
                }
            }

        } else if (checkIndex == 2) {
            //ffplayer prepare2
            if (ffplayer != null) {
                ffplayer._prepare2(maskSource);
            }

        }
    }

    /**
     * Called when the activity is about to be paused.
     */
    @Override
    protected void onPause() {
        mIsPlaying = false;
        mIsmartvPlayer._setPlayWhenReady(false);
        mGLView1.onPause();
        mGLView2.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRadio2.isChecked()) {
            mGLView1.onResume();
        }
        if (mRadio3.isChecked()) {
            mGLView2.onResume();
        }
    }

    /**
     * Called when the activity is about to be destroyed.
     */
    @Override
    protected void onDestroy() {
        mIsmartvPlayer._stop();
        if (!softDecode) {
            player2._stop();
        }
        mCreated = false;
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.surfaceview1:
                mRadio1.toggle();
                break;
            case R.id.glsurfaceview1:
                mRadio2.toggle();
                break;
            case R.id.glsurfaceview2:
                mRadio3.toggle();
            case R.id.start_native:
                videoStart();
                break;
            case R.id.rewind_native:
                videoRewind();
                break;
            case R.id.get_frame:
                getFrameInfo();
                break;
        }
    }

    private void getFrameInfo () {
        StringBuffer buffer_y = new StringBuffer();
        StringBuffer buffer_u = new StringBuffer();
        StringBuffer buffer_v = new StringBuffer();
        if (ffplayer != null) {
            IsmartvPlayer.FrameData frameData = ffplayer._getVideoFrameBySort(null, null, 0);
            ArrayList<IsmartvPlayer.Data> datas = frameData.getDatas();

//            int[] y = new int[datas.get(0).getLinesize()];
//            int[] u = new int[datas.get(1).getLinesize()];
//            int[] v = new int[datas.get(2).getLinesize()];
            if (datas != null) {
                for (int i = 0; i < datas.size(); i++) {
                    Log.i(TAG, "queue --- queue read frame info"
                            + ", format : " + datas.get(i).getFormat()
                            + ", linesize(" + i + ") : "+ datas.get(i).getLinesize()
                            + ", width : " + datas.get(i).getWidth()
                            + ", height : " + datas.get(i).getHeight());
//                            + ", buffer : " + new String(datas.get(i).getData()));
                }
//                for (int j = 0; j < datas.get(0).getLinesize() * datas.get(0).getHeight() * height[0]; j ++) {
//                    byte cur = datas.get(0).getData()[j];
//                    buffer_y.append(String.format("%x ", cur));
//                }
//                Log.i(TAG, "queue --- queue read frame info, buffer y : " + buffer_y.toString());
//
//                for (int j = 0; j < datas.get(1).getLinesize() * datas.get(1).getHeight() * height[1]; j ++) {
//                    byte cur = datas.get(1).getData()[j];
//                    buffer_u.append(String.format("%x ", cur));
//                }
//                Log.i(TAG, "queue --- queue read frame info, buffer u : " + buffer_u.toString());
//
                for (int j = 0; j < datas.get(2).getLinesize(); j ++) {
                    byte cur = datas.get(2).getData()[j];
                    buffer_v.append(String.format("%x ", cur));
                }
                Log.i(TAG, "queue --- queue read frame info, buffer v : " + buffer_v.toString());


            }
            mGLView2.setRenderFrame(frameData);
        } else {
            Bitmap blendPic = GLUtil.getImageFromAssetsFile(getApplicationContext(), "asset2.png");
            mGLView2.setRenderFrame2(blendPic);
        }
    }

    private void videoStart() {
        if (!mCreated) {
            if (mNativeCodecPlayerVideoSink == null) {
                if (mSelectedVideoSink == null) {
                    return;
                }
                mSelectedVideoSink.useAsSinkForNative();
                mNativeCodecPlayerVideoSink = mSelectedVideoSink;
            }
            if (mSourceString != null) {
                mCreated = mIsmartvPlayer._prepare(mSourceString);

                if (!softDecode) {
                    //当前是glsurfaceview
                    if (mSelectedVideoSink instanceof GLViewVideoSink) {
                        //set surface
                        ((GLViewVideoSink) mSelectedVideoSink).useAsSinkForNative2();
                        //prepare
                        player2._prepare(maskSource);
                    }
                }
//                durationTextView.setText(Util.formatTime(mIsmartvPlayer._getDuration()));
            }
        }
        if (mCreated) {
            mIsPlaying = !mIsPlaying;
            mIsmartvPlayer._setPlayWhenReady(mIsPlaying);
            if (!softDecode) {
                if (mSelectedVideoSink instanceof GLViewVideoSink) {
                    player2._setPlayWhenReady(mIsPlaying);
                }
            }
        }
    }

    private void videoRewind() {
        if (mNativeCodecPlayerVideoSink != null) {
            mIsmartvPlayer._rewind();
            if (!softDecode) {
                player2._rewind();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged format=" + format + ", width=" + width + ", height=" + height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");
        if (mRadio1.isChecked()) {
            mIsmartvPlayer._setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//        mSourceString = parent.getItemAtPosition(pos).toString();
//        Log.v(TAG, "onItemSelected " + mSourceString);
    }

    @Override
    public void onNothingSelected(AdapterView parent) {
//        Log.v(TAG, "onNothingSelected");
//        mSourceString = null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i("@@@@", "oncheckedchanged");
        if (buttonView == mRadio1 && isChecked) {
            mRadio2.setChecked(false);
            mRadio3.setChecked(false);
        }
        if (buttonView == mRadio2 && isChecked) {
            mRadio1.setChecked(false);
            mRadio3.setChecked(false);
        }
        if (buttonView == mRadio3 && isChecked) {
            mRadio1.setChecked(false);
            mRadio2.setChecked(false);
        }
        int checkIndex = -1;
        if (isChecked) {
            if (mRadio1.isChecked()) {
                checkIndex = 0;
                if (mSurfaceHolder1VideoSink == null) {
                    mSurfaceHolder1VideoSink = new SurfaceHolderVideoSink(mSurfaceHolder1);
                }
                mSelectedVideoSink = mSurfaceHolder1VideoSink;
                mGLView1.onPause();
                Log.i("@@@@", "dhb test, radio 1.");
            } else if (mRadio2.isChecked()){
                checkIndex = 1;
                //read assert file
                readAssertFile("asset3.png");
//                readAssertFile("test.jpg");
                mGLView1.onResume();
                if (mGLView1VideoSink == null) {
                    mGLView1VideoSink = new GLViewVideoSink(mGLView1);
                }
                mSelectedVideoSink = mGLView1VideoSink;
                Log.i("@@@@", "dhb test, radio 2.");

            } else if (mRadio3.isChecked()) {
                checkIndex = 2;

                Log.i("@@@@", "dhb test, radio 3.");

            }
            switchSurface(checkIndex);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.d(TAG, "progress: " + progress);
        currentSeekPosition = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStartTrackingTouch");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStopTrackingTouch");
        long positionMs = currentSeekPosition * mIsmartvPlayer._getDuration() / 100;
        mIsmartvPlayer._seekTo(positionMs);
    }

    // VideoSink abstracts out the difference between Surface and SurfaceTexture
    // aka SurfaceHolder and GLSurfaceView
    static abstract class VideoSink {

        abstract void setFixedSize(int width, int height);

        abstract void useAsSinkForNative();

    }

    class SurfaceHolderVideoSink extends VideoSink {

        private final SurfaceHolder mSurfaceHolder;

        SurfaceHolderVideoSink(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        @Override
        void setFixedSize(int width, int height) {
            mSurfaceHolder.setFixedSize(width, height);
        }

        @Override
        void useAsSinkForNative() {
            Surface s = mSurfaceHolder.getSurface();
            Log.i("@@@", "setting surface " + s);
            mIsmartvPlayer._setSurface(s);
        }

        public Surface getGLSurface() {
            Surface s = mSurfaceHolder.getSurface();
            return s;
        }

    }

    class GLViewVideoSink extends VideoSink {

        private final MyGLSurfaceView mMyGLSurfaceView;

        GLViewVideoSink(MyGLSurfaceView myGLSurfaceView) {
            mMyGLSurfaceView = myGLSurfaceView;
        }

        @Override
        void setFixedSize(int width, int height) {
        }

        @Override
        void useAsSinkForNative() {
            SurfaceTexture st = mMyGLSurfaceView.getSurfaceTexture();
            Surface s = new Surface(st);
            mIsmartvPlayer._setSurface(s);
            s.release();
        }

        public void useAsSinkForNative2() {
            SurfaceTexture st = mMyGLSurfaceView.getSurfaceTexture2();
            Surface s = new Surface(st);
            player2._setSurface(s);
            s.release();
        }

        public Surface getGLSurface() {
            SurfaceTexture st = mMyGLSurfaceView.getSurfaceTexture();
            Surface s = new Surface(st);
            return s;
        }

    }

}
