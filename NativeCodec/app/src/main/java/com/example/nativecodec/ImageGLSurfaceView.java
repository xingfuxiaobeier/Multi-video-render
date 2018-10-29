package com.example.nativecodec;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.ismartv.player.IsmartvPlayer;

/**
 * Created by dhb on 2018/7/9.
 */

public class ImageGLSurfaceView extends GLSurfaceView {

    private static String TAG = "ImageGLSurfaceView";
    private ImageRender render;
    public ImageGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public ImageGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        render = new ImageRender();
        setRenderer(render);
    }

    public void setRenderFrame(IsmartvPlayer.FrameData data) {
        if (render != null) {
            render.setFrameData(data);
            requestRender();
        }
    }

    public void setRenderFrame2(Bitmap pic) {
        if (render != null) {
            render.setFrameData2(pic);
            requestRender();
        }
    }
}

class ImageRender implements GLSurfaceView.Renderer {


    private static String TAG = "ImageRender";
    private String externalYuvVertextShader;
    private String externalYuvFragmentShader;
    //mask program
    private int maskProgram;
    private int maskPositionHandler;
    private int maskTextureHandler;
    private int maskYHandler;
    private int maskUHandler;
    private int maskVHandler;
    private int maskColorConversionHandler;

    private int yTexture = 0;
    private int uTexture = 0;
    private int vTexture = 0;
    boolean newFrame = false;
    int frameWidth = -1;
    int frameHeight = -1;
    private ByteBuffer yBuffer = null;
    private ByteBuffer uBuffer = null;
    private ByteBuffer vBuffer = null;
    private byte[] yBytes;
    private byte[] uBytes;
    private byte[] vBytes;

    //rgb
    private Bitmap bitmap;
    private int rgbaTextureId;
    private boolean yuvRenderTag = true;

    private FloatBuffer mVertices;
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int VERTICES_DATA_POS_OFFSET = 0;
    private static final int VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.0f, 0.0f,
            1.0f, -1.0f, 0, 1.0f, 0.0f,
            -1.0f,  1.0f, 0, 0.0f, 1.0f,
            1.0f,  1.0f, 0 , 1.0f, 1.0f,
    };

    private float[] height = {1,  0.5f, 0.5f};
    public ImageRender() {
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        int[] textures = new int[3];
        GLES20.glGenTextures(3, textures, 0);
        yTexture = textures[0];
        uTexture = textures[1];
        vTexture = textures[2];

        //-----------------------------------------------------------------------//
        //mask video program
        //-----------------------------------------------------------------------//

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        /* Set up shaders and handles to their variables */
        externalYuvVertextShader = ParseJson.readAssertFilte("yuvvertextshader");
        if (yuvRenderTag) {
            externalYuvFragmentShader = ParseJson.readAssertFilte("yuvfragmentshader");
        } else {
            externalYuvFragmentShader = ParseJson.readAssertFilte("rgbfragmentshader");
        }
        maskProgram = GLUtil.createProgram(externalYuvVertextShader, externalYuvFragmentShader);
        if (maskProgram == 0) {
            return;
        }

        maskPositionHandler = GLES20.glGetAttribLocation(maskProgram, "position");
        GLUtil.checkGlError("glGetAttribLocation position");
        if (maskPositionHandler == -1) {
            throw new RuntimeException("Could not get attrib location for position");
        }
        maskTextureHandler = GLES20.glGetAttribLocation(maskProgram, "coordinate");
        GLUtil.checkGlError("glGetAttribLocation coordinate");
        if (maskTextureHandler == -1) {
            throw new RuntimeException("Could not get attrib location for coordinate");
        }

        maskYHandler = GLES20.glGetUniformLocation(maskProgram, "inputImageTexture");
        GLUtil.checkGlError("glGetUniformLocation inputImageTexture");
        if (maskYHandler == -1) {
            throw new RuntimeException("Could not get uniform location for inputImageTexture");
        }
//
        if (yuvRenderTag) {
            maskUHandler = GLES20.glGetUniformLocation(maskProgram, "inputImageTexture2");
            GLUtil.checkGlError("glGetUniformLocation inputImageTexture2");
            if (maskUHandler == -1) {
                throw new RuntimeException("Could not get uniform location for inputImageTexture2");
            }

            maskVHandler = GLES20.glGetUniformLocation(maskProgram, "inputImageTexture3");
            GLUtil.checkGlError("glGetUniformLocation inputImageTexture3");
            if (maskVHandler == -1) {
                throw new RuntimeException("Could not get uniform location for inputImageTexture3");
            }

            maskColorConversionHandler = GLES20.glGetUniformLocation(maskProgram, "um3_ColorConversion");
            GLUtil.checkGlError("glGetUniformLocation um3_ColorConversion");
            if (maskColorConversionHandler == -1) {
                throw new RuntimeException("Could not get uniform location for um3_ColorConversion");
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

            Log.i(TAG, "dhb test, on draw frame begin ... s");

        GLES20.glUseProgram(maskProgram);
        GLUtil.checkGlError("glUseProgram mProgram");
            //fbo offscreen render origin external texture mode to texture2D mode
        if (yuvRenderTag) {
            onDrawPrepare();
            GLES20.glUniformMatrix3fv(maskColorConversionHandler, 1, false, GLUtil.g_bt709, 0);

        } else {
            onDrawPrepare2();
        }

            mVertices.position(VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maskPositionHandler, 3, GLES20.GL_FLOAT, false,
                    VERTICES_DATA_STRIDE_BYTES, mVertices);
            GLUtil.checkGlError("glVertexAttribPointer maskPositionHandler");
            GLES20.glEnableVertexAttribArray(maskPositionHandler);
            GLUtil.checkGlError("glEnableVertexAttribArray maskPositionHandler");

            mVertices.position(VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maskTextureHandler, 3, GLES20.GL_FLOAT, false,
                    VERTICES_DATA_STRIDE_BYTES, mVertices);
            GLUtil.checkGlError("glVertexAttribPointer maskTextureHandler");
            GLES20.glEnableVertexAttribArray(maskTextureHandler);
            GLUtil.checkGlError("glEnableVertexAttribArray maskTextureHandler");

            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(0f, 0f, 0f, 0f);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            //unbind
            GLES20.glDisableVertexAttribArray(maskPositionHandler);
            GLES20.glDisableVertexAttribArray(maskTextureHandler);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        Log.i(TAG, "dhb test, on draw frame end ... s");

    }

    private void onDrawPrepare() {

        if (newFrame) {
            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, new frame arrives begin ... ");
            Log.i(TAG, "dhb test, real draw get frame, frame width : " + frameWidth + ", frame height : " + frameHeight);

            //y
            if (yBuffer == null && uBuffer == null && vBuffer == null) {
                yBuffer = ByteBuffer.allocate(frameWidth * frameHeight);
                uBuffer = ByteBuffer.allocate(frameWidth * frameHeight / 4);
                vBuffer = ByteBuffer.allocate(frameWidth * frameHeight / 4);
            }

            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame 1 ... ");
            yBuffer.put(yBytes).position(0);
            GLUtil.loadTexture2(yTexture, frameWidth, frameHeight, yBuffer);
            Log.i(TAG, "dhb test, real draw get frame, texture y id : " + yTexture);
            GLUtil.checkGlError("load texture y.");

            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, 2 ... ");
            uBuffer.put(uBytes).position(0);
            GLUtil.loadTexture2(uTexture, frameWidth / 2, frameHeight / 2, uBuffer);
            Log.i(TAG, "dhb test, real draw get frame, texture u id : " + uTexture);

            GLUtil.checkGlError("load texture u.");

            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, 3 ... ");
            vBuffer.put(vBytes).position(0);
            GLUtil.loadTexture2(vTexture, frameWidth / 2, frameHeight / 2, vBuffer);
            Log.i(TAG, "dhb test, real draw get frame, texture v id : " + vTexture);

            GLUtil.checkGlError("load texture v.");
            newFrame = false;
            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, 4 ... ");

        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexture);
        GLES20.glUniform1i(maskYHandler, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTexture);
        GLES20.glUniform1i(maskUHandler, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTexture);
        GLES20.glUniform1i(maskVHandler, 2);
    }

    private void onDrawPrepare2() {
        if (bitmap != null) {
            Log.i(TAG, "dhb test, on draw prepare2 begin ... ");
            rgbaTextureId = GLUtil.loadTexture(bitmap);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, rgbaTextureId);
            GLES20.glUniform1i(maskYHandler, 0);
        }
    }

    //set data
    public void setFrameData(IsmartvPlayer.FrameData data) {
//        printFrameData(data);
        frameWidth = data.getDatas().get(0).getWidth();
        frameHeight = data.getDatas().get(0).getHeight();
        if (yBytes == null && uBytes == null && vBytes == null) {
            yBytes = new byte[frameWidth * frameHeight];
            uBytes = new byte[frameWidth * frameHeight / 4];
            vBytes = new byte[frameWidth * frameHeight / 4];
        }
        System.arraycopy(data.getDatas().get(0).getData(), 0, yBytes, 0, yBytes.length);
        System.arraycopy(data.getDatas().get(1).getData(), 0, uBytes, 0, uBytes.length);
        System.arraycopy(data.getDatas().get(2).getData(), 0, vBytes, 0, vBytes.length);

        newFrame = true;

        Log.i(TAG, "dhb test, print byte length, y = " + yBytes.length + ", u = " + uBytes.length + ", v = " + vBytes.length);

    }

    private void printFrameData (IsmartvPlayer.FrameData data) {
        int size_y = (int) (data.getDatas().get(0).getLinesize());
        int size_u = (int) (data.getDatas().get(1).getLinesize());
        int size_v = (int) (data.getDatas().get(2).getLinesize());

        System.out.println("\nframe data y length : " + size_y);
        for (int i = 0; i < size_y; i++) {
            System.out.print(data.getDatas().get(0).getData()[i]);
            System.out.print(" ");
        }

        System.out.println("\nframe data u length : " + size_u);
        for (int i = 0; i < size_u; i++) {
            System.out.print(data.getDatas().get(1).getData()[i]);
            System.out.print(" ");
        }

        System.out.println("\nframe data v length : " + size_v);
        for (int i = 0; i < size_v; i++) {
            System.out.print(data.getDatas().get(2).getData()[i]);
            System.out.print(" ");
        }
    }

    public void setFrameData2(Bitmap pic) {
        bitmap = pic;
    }

}
