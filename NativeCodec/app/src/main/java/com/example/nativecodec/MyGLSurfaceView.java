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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.ismartv.player.IsmartvPlayer;

public class MyGLSurfaceView extends GLSurfaceView {

    MyRenderer mRenderer;

    public MyGLSurfaceView(Context context) {
        this(context, null);
    }

    public MyGLSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        mRenderer = new MyRenderer(this);
        setRenderer(mRenderer);
        Log.i("@@@", "setrenderer");
    }

    @Override
    public void onPause() {
        mRenderer.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRenderer.getSurfaceTexture();
    }

    public SurfaceTexture getSurfaceTexture2() {
        return  mRenderer.getSurfaceTexture2();
    }

    public void setBlendPic(Bitmap pic) {
        mRenderer.setBlendPic(pic);
    }

    public void setGetFramePlayer(IsmartvPlayer player) {
        if (player != null) {
            mRenderer.setPlayer(player);
        }
    }
}

class MyRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public MyRenderer(GLSurfaceView view) {

        glSurfaceView = view;
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        testVertices = ByteBuffer.allocateDirect(GLUtil.testVerticeDate.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        testVertices.put(GLUtil.testVerticeDate).position(0);

        testFragment = ByteBuffer.allocateDirect(GLUtil.TEXTURE_NO_ROTATION.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        testFragment.put(GLUtil.TEXTURE_NO_ROTATION).position(0);

        noRotationFragment = ByteBuffer.allocateDirect(GLUtil.TEXTURE_NO_ROTATION.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        noRotationFragment.put(GLUtil.TEXTURE_NO_ROTATION).position(0);

//        attributeBuffer = ByteBuffer.allocateDirect(5 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        attributeBuffer.put(attributesData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);

        indicesBuffer = ByteBuffer.allocateDirect(6 * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        indicesData = new short[] { 0, 1, 2, 2, 3, 0 };
        indicesBuffer.position(0);
        indicesBuffer.put(indicesData);

    }
    public void onPause() {
    }

    public void onResume() {
        mLastTime = SystemClock.elapsedRealtimeNanos();
    }

    private int count = 0;
    @Override
    public void onDrawFrame(GL10 glUnused) {
        Log.i(TAG, "dhb test, on draw frame begin ... ");
        synchronized(this) {

            while (!runnableQueue.isEmpty()) {
                Runnable current = runnableQueue.poll();
                current.run();
            }

            if (updateSurface) {
                count++;
                mSurface.updateTexImage();
//                mSurface2.updateTexImage();
                mSurface.getTransformMatrix(mSTMatrix);

                //get mask frame
                try {
                    if (true) {
                        Log.i(TAG, "dhb test, real draw get frame begin : " + count);
                        IsmartvPlayer.FrameData frameData = player._getVideoFrameBySort(null, null, 0);
                        frameWidth = frameData.getDatas().get(0).getWidth();
                        frameHeight = frameData.getDatas().get(0).getHeight();
                        yBytes = frameData.getDatas().get(0).getData();
                        uBytes = frameData.getDatas().get(1).getData();
                        vBytes = frameData.getDatas().get(2).getData();
                        newFrame = true;
                        Log.i(TAG, "dhb test, real draw get frame end : " + count + ", width : " + frameWidth + ", height : " + frameHeight);
                    }
                    updateSurface = false;
                } catch (Exception e) {
                    Log.e(TAG, "get video frame error, " + e);
                }
            }
        }

        try {

            long before = System.currentTimeMillis();
            offscreenRenderOriVideo(fboOri2DID, mTextureID);
            long current = System.currentTimeMillis();
            Log.i(TAG, "offscreen render background frame current frames cost time : " + (current - before) + " ms");
            before = current;
//        offscreenRenderOriVideo(fboMaskID, maskTextureID);
            offscreenRenderMaskVideoFrame();
            current = System.currentTimeMillis();
            Log.i(TAG, "offscreen render mask frame current frames cost time : " + (current - before) + " ms");
            before = current;
            offscreenRender3DModel(fboID);
            current = System.currentTimeMillis();
            Log.i(TAG, "offscreen render 3D model frame current frames cost time : " + (current - before) + " ms");
//            offscreenRender3DModel(fbo1ID);

            before = current;
            renderBlending();
            current = System.currentTimeMillis();
            Log.i(TAG, "render all in one frame cost time : " + (current - before) + " ms");
            Log.i(TAG, "render all current frames total cost time : " + (current - before) + " ms");
        } catch (Exception e) {
            Log.e(TAG, "dhb test, on draw frame error, " + e);
        }


    }

    private void renderBlending() {
        //surface blending render videos
        GLES20.glUseProgram(origProgram);
        GLUtil.checkGlError("glUseProgram origProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboOriTexture2DID);
        GLES20.glUniform1i(uniformOriTextureHandle, 4);

        mVertices.position(VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(origPositionHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLUtil.checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(origPositionHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray maPositionHandle");

        mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(origTextureHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLUtil.checkGlError("glVertexAttribPointer origTextureHandle");
        GLES20.glEnableVertexAttribArray(origTextureHandle);

        mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(blendingTextureHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLUtil.checkGlError("glVertexAttribPointer blendingTextureHandle");
        GLES20.glEnableVertexAttribArray(blendingTextureHandle);

        mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maskTextureHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLUtil.checkGlError("glVertexAttribPointer maskTextureHandle");
        GLES20.glEnableVertexAttribArray(maskTextureHandle);

        //3d render
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureID);
        GLES20.glUniform1i(uniformBlendingTextureHandle, 3);
        GLUtil.checkGlError("glEnableVertexAttribArray maTextureHandle");

//        //mask render
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboMaskTextureID);
        GLES20.glUniform1i(uniformMaskTextureHandle, 5);
        GLUtil.checkGlError("glEnableVertexAttribArray maTextureHandle");

        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    }

    private void offscreenRenderOriVideo(int fboID, int origTextureId) {

        //fbo offscreen render origin external texture mode to texture2D mode
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboID);
        GLES20.glUseProgram(nTranProgram);
        GLUtil.checkGlError("glUseProgram mPrograme");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, origTextureId);

        mVertices.position(VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLUtil.checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray maPositionHandle");

//        testVertices.position(0);
//        GLES20.glVertexAttribPointer(nTranPositionHandle, 3, GLES20.GL_FLOAT, false,
//                12, testVertices);
//        checkGlError("glVertexAttribPointer maPosition");
//        GLES20.glEnableVertexAttribArray(nTranPositionHandle);
//        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(nTranTextureHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLUtil.checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(nTranTextureHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray maTextureHandle");

        oriSTMatrix = mSTMatrix;
//        GLU.gluProject();
//        Matrix.translateM(oriSTMatrix, 0, 0, 0, 0);
//        Matrix.rotateM(oriSTMatrix, 0, 30, 0, 1, 0);
//        Matrix.scaleM(oriSTMatrix, 0, 0, 0, 0);

        GLES20.glUniformMatrix4fv(nTranSTMatrixHandle, 1, false, oriSTMatrix, 0);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glClearColor(0.643f, 0.776f, 0.223f, 1.0f);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        //unbind
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void offscreenRenderMaskVideoFrame() {
        {

            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame begin, new frame tag : " + newFrame);
            //fbo offscreen render origin external texture mode to texture2D mode
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMaskID);
            GLES20.glUseProgram(maskProgram);
            GLUtil.checkGlError("glUseProgram mProgram");

            if (newFrame) {
                Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, new frame arrives begin ... ");

                //y
                if (yBuffer == null && uBuffer == null && vBuffer == null) {
                    yBuffer = ByteBuffer.allocate(frameWidth * frameHeight);
                    uBuffer = ByteBuffer.allocate(frameWidth * frameHeight / 4);
                    vBuffer = ByteBuffer.allocate(frameWidth * frameHeight / 4);
                }

                Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame 1 ... ");
                yBuffer.put(yBytes).position(0);
                GLUtil.loadTexture2(yTexture, frameWidth, frameHeight, yBuffer);

                Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, 2 ... ");
                uBuffer.put(uBytes).position(0);
                GLUtil.loadTexture2(uTexture, frameWidth / 2, frameHeight / 2, uBuffer);

                Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame, 3 ... ");
                vBuffer.put(vBytes).position(0);
                GLUtil.loadTexture2(vTexture, frameWidth / 2, frameHeight / 2, vBuffer);
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

            GLES20.glUniformMatrix3fv(maskColorConversionHandler, 1, false, GLUtil.g_bt709, 0);

            mVertices.position(VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maskPositionHandler, 3, GLES20.GL_FLOAT, false,
                    VERTICES_DATA_STRIDE_BYTES, mVertices);
            GLUtil.checkGlError("glVertexAttribPointer maskPositionHandler");
            GLES20.glEnableVertexAttribArray(maskPositionHandler);
            GLUtil.checkGlError("glEnableVertexAttribArray maskPositionHandler");

//            mVertices.position(VERTICES_DATA_UV_OFFSET);
//            GLES20.glVertexAttribPointer(maskTextureHandler, 3, GLES20.GL_FLOAT, false,
//                    VERTICES_DATA_STRIDE_BYTES, mVertices);
//            GLUtil.checkGlError("glVertexAttribPointer maskTextureHandler");
//            GLES20.glEnableVertexAttribArray(maskTextureHandler);
//            GLUtil.checkGlError("glEnableVertexAttribArray maskTextureHandler");
            noRotationFragment.position(0);
            GLES20.glVertexAttribPointer(maskTextureHandler, 3, GLES20.GL_FLOAT, false,
                    12, noRotationFragment);
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
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            Log.i(TAG, "dhb test, real draw get frame, offscreen render mask video frame end ... ");

        }
    }

    private void offscreenRender3DModel(int fbo) {
        //fbo offscreen render 3D model
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glUseProgram(mProgram);
        GLUtil.checkGlError("glUseProgram mPrograme");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, fboTextureID);
        Log.i(TAG, "blendPicTexture = " + blendPicTexture);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blendPicTexture);

        testVertices.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                8, testVertices);
        GLUtil.checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray maPositionHandle");


        testFragment.position(0);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                12, testFragment);
        GLUtil.checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray maTextureHandle");


        if (renderKey == 0) {
            //world
            GLES20.glUniformMatrix3fv(uWorldHandle, 1, false, world, 0);
        } else {

            GLES20.glUniform2fv(leftBottom, 1, leftBottomf, 0);
            GLES20.glUniform2fv(rightTop, 1, rightTopf, 0);
            GLES20.glUniform2fv(leftTop, 1, leftTopf, 0);
            GLES20.glUniform2fv(rightBottom, 1, rightBottomf, 0);
            ParseJson.printArray(leftBottomf, "dhb test vertex : leftBottom");
            ParseJson.printArray(rightTopf, "dhb test vertex : rightTop");
            ParseJson.printArray(leftTopf, "dhb test vertex : leftTop");
            ParseJson.printArray(rightBottomf, "dhb test vertex : rightBottom");
        }

        long now = SystemClock.elapsedRealtimeNanos();
        mRunTime += (now - mLastTime);
        mLastTime = now;
        double d = ((double)mRunTime) / 1000000000;
        Matrix.setIdentityM(mMMatrix, 0);
        if (fbo == fboID) {
//            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        } else {
//            Matrix.rotateM(mMMatrix, 0, 30, (float)Math.sin(d), (float)Math.cos(d), 0);
//            float test = (float)(Math.sin(d));
//            if (test < 0) {
//                test *= 3;
//            }
//            Matrix.translateM(mMMatrix, 0, (float)Math.sin(d), (float)Math.cos(d), test);
////        Matrix.scaleM(mMMatrix, 0, (float)Math.sin(d), 0, 0);
//            Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
//            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
//
//            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
//            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        }

        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glClearColor(0.643f, 0.776f, 0.223f, 1.0f);
        GLES20.glClearColor(0f, 0f, 0f, 0f);

        if (renderKey == 1) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        } else {
            indicesBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indicesBuffer);
        }
        //unbind
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
//        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        this.viewWidth = width;
        this.viewHeight = height;
        GLES20.glViewport(0, 0, width, height);
//        GLES20.glDepthRangef()
        mRatio = (float) width / height;
        Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 1, 8);

        //fbo
        int frameBuffer[] = new int[4];
//        GLES11Ext.glGenFramebuffersOES();
        GLES20.glGenFramebuffers(4, frameBuffer, 0);
        fboID = frameBuffer[0];
        fboOri2DID = frameBuffer[1];
        fbo1ID = frameBuffer[2];
        fboMaskID = frameBuffer[3];
        GLUtil.checkGlError("glDrawArrays");

        //external texture id
        mTextureID = GLUtil.genTexture(width, height);
        maskTextureID = GLUtil.genTexture(width, height);
//        fboTextureID = genTexture2(width, height);

        //texture2D id
        int[] fboTexture = new int[7];
        GLES20.glGenTextures(7, fboTexture, 0);
        fboTextureID = fboTexture[0];
        fboOriTexture2DID = fboTexture[1];
        fboTexture1ID = fboTexture[2];
        fboMaskTextureID = fboTexture[3];

        yTexture = fboTexture[4];
        uTexture = fboTexture[5];
        vTexture = fboTexture[6];

        //bind framebuffer and texture id
        GLUtil.bindFrameBuffer(fboTextureID, fboID, width, height);
        GLUtil.bindFrameBuffer(fboOriTexture2DID, fboOri2DID, width, height);
        GLUtil.bindFrameBuffer(fboTexture1ID, fbo1ID, width, height);
        GLUtil.bindFrameBuffer(fboMaskTextureID, fboMaskID, width, height);

        /*
         * Create the SurfaceTexture that will feed this textureID, and pass it to the camera
         */

        mSurface = new SurfaceTexture(mTextureID);
        mSurface.setOnFrameAvailableListener(this);

        mSurface2 = new SurfaceTexture(maskTextureID);

        world = new float[] {
                2f / width, 0, 0,
                0, 2f / height, 0,
                -1f, -1f, 1
        };

        //
//        GLUtil.loadTexture()
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.

        /* Set up alpha blending and an Android background color */

        //-----------------------------------------------------------------------//
        //blend pic program
        //-----------------------------------------------------------------------//


        if (renderKey == 0) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            mProgram = GLUtil.createProgram(blendVertexShader, blendFragmentShader);
            if (mProgram == 0) {
                return;
            }

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            GLUtil.checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            GLUtil.checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

//        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
//        checkGlError("glGetUniformLocation uMVPMatrix");
//        if (muMVPMatrixHandle == -1) {
//            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
//        }

//        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
//        checkGlError("glGetUniformLocation uSTMatrix");
//        if (muSTMatrixHandle == -1) {
//            throw new RuntimeException("Could not get attrib location for uSTMatrix");
//        }

            uWorldHandle = GLES20.glGetUniformLocation(mProgram, "u_World");
            GLUtil.checkGlError("glGetUniformLocation u_World");
            if (uWorldHandle == -1) {
                throw new RuntimeException("Could not get attrib location for u_World");
            }
        } else {

            //read fragmentShader
            externalFragmentShader = ParseJson.readAssertFilte("fragmentShader");
            Log.i(TAG, "external fragment shader string : " + externalFragmentShader);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            mProgram = GLUtil.createProgram(blendVertexShader2, externalFragmentShader);
            if (mProgram == 0) {
                return;
            }

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            GLUtil.checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            GLUtil.checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }


            //纹理4个点坐标
            leftBottom = GLES20.glGetUniformLocation(mProgram, "leftBottom");
            GLUtil.checkGlError("glGetUniformLocation leftBottom");
            if (leftBottom == -1) {
                throw new RuntimeException("Could not get attrib location for leftBottom");
            }

            rightTop = GLES20.glGetUniformLocation(mProgram, "rightTop");
            GLUtil.checkGlError("glGetUniformLocation rightTop");
            if (rightTop == -1) {
                throw new RuntimeException("Could not get attrib location for rightTop");
            }

            leftTop = GLES20.glGetUniformLocation(mProgram, "leftTop");
            GLUtil.checkGlError("glGetUniformLocation leftTop");
            if (leftTop == -1) {
                throw new RuntimeException("Could not get attrib location for leftTop");
            }

            rightBottom = GLES20.glGetUniformLocation(mProgram, "rightBottom");
            GLUtil.checkGlError("glGetUniformLocation rightBottom");
            if (rightBottom == -1) {
                throw new RuntimeException("Could not get attrib location for rightBottom");
            }

        }


        //-----------------------------------------------------------------------//
        //original video program
        //-----------------------------------------------------------------------//

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        /* Set up shaders and handles to their variables */
        nTranProgram = GLUtil.createProgram(oriVertexShader, mFragmentShader);
        if (nTranProgram == 0) {
            return;
        }

        nTranPositionHandle = GLES20.glGetAttribLocation(nTranProgram, "aPosition");
        GLUtil.checkGlError("glGetAttribLocation aPosition");
        if (nTranPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        nTranTextureHandle = GLES20.glGetAttribLocation(nTranProgram, "aTextureCoord");
        GLUtil.checkGlError("glGetAttribLocation aTextureCoord");
        if (nTranTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        nTranSTMatrixHandle = GLES20.glGetUniformLocation(nTranProgram, "uSTMatrix");
        GLUtil.checkGlError("glGetUniformLocation uSTMatrix");
        if (nTranSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        //-----------------------------------------------------------------------//
        //mask video program
        //-----------------------------------------------------------------------//

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        /* Set up shaders and handles to their variables */
        externalYuvVertextShader = ParseJson.readAssertFilte("yuvvertextshader");
        externalYuvFragmentShader = ParseJson.readAssertFilte("yuvfragmentshader");
        maskProgram = GLUtil.createProgram(externalYuvVertextShader, externalYuvFragmentShader);
        if (maskProgram == 0) {
            return;
        }

        maskPositionHandler = GLES20.glGetAttribLocation(maskProgram, "position");
        GLUtil.checkGlError("glGetAttribLocation position");
        if (nTranPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for position");
        }
        maskTextureHandler = GLES20.glGetAttribLocation(maskProgram, "coordinate");
        GLUtil.checkGlError("glGetAttribLocation coordinate");
        if (nTranTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for coordinate");
        }

        maskYHandler = GLES20.glGetUniformLocation(maskProgram, "inputImageTexture");
        GLUtil.checkGlError("glGetUniformLocation inputImageTexture");
        if (maskYHandler == -1) {
            throw new RuntimeException("Could not get uniform location for inputImageTexture");
        }

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


        //-----------------------------------------------------------------------//
        //3 texture all in program
        //-----------------------------------------------------------------------//

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        GLES20.glClearColor(0.643f, 0.776f, 0.223f, 1.0f);

        /* Set up shaders and handles to their variables */
        origProgram = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (origProgram == 0) {
            return;
        }

        origPositionHandle = GLES20.glGetAttribLocation(origProgram, "position");
        GLUtil.checkGlError("glGetAttribLocation position");
        if (origPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for position");
        }
        origTextureHandle = GLES20.glGetAttribLocation(origProgram, "inputTextureCoordinate");
        GLUtil.checkGlError("glGetAttribLocation inputTextureCoordinate");
        if (origTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for inputTextureCoordinate");
        }

        blendingTextureHandle = GLES20.glGetAttribLocation(origProgram, "inputTextureCoordinate2");
        GLUtil.checkGlError("glGetAttribLocation inputTextureCoordinate2");
        if (blendingTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for inputTextureCoordinate2");
        }


        maskTextureHandle = GLES20.glGetAttribLocation(origProgram, "inputTextureCoordinate3");
        GLUtil.checkGlError("glGetAttribLocation inputTextureCoordinate3");
        if (maskTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for inputTextureCoordinate3");
        }

        uniformOriTextureHandle = GLES20.glGetUniformLocation(origProgram, "inputImageTexture");
        GLUtil.checkGlError("glGetAttribLocation inputImageTexture");
        if (uniformOriTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for inputImageTexture");
        }

        uniformBlendingTextureHandle = GLES20.glGetUniformLocation(origProgram, "inputImageTexture2");
        GLUtil.checkGlError("glGetAttribLocation inputImageTexture2");
        if (uniformBlendingTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for inputImageTexture2");
        }

        uniformMaskTextureHandle = GLES20.glGetUniformLocation(origProgram, "inputImageTexture3");
        GLUtil.checkGlError("glGetAttribLocation inputImageTexture3");
        if (uniformBlendingTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for inputImageTexture3");
        }

        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 4f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        synchronized(this) {
            updateSurface = false;
        }

        //read json file
        String json = ParseJson.readAssertFilte("data3.json");
        jsonParam = ParseJson.readJsonOut(json);


        //start time
//        startTime = System.currentTimeMillis();

    }

    private int frameCount = -1;
    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        /* For simplicity, SurfaceTexture calls here when it has new
         * data available.  Call may come in from some random thread,
         * so let's be safe and use synchronize. No OpenGL calls can be done here.
         */

        frameCount++;

        long beginTime = System.currentTimeMillis();
        if (frameCount <= jsonParam.getSize()) {
            Object[] param = jsonParam.getValue()[frameCount];
            if (renderKey == 0) {
                ParseJson.createVFMatrix(param, GLUtil.testVerticeDate, GLUtil.testFragmentData);
                testVertices.put(GLUtil.testVerticeDate).position(0);
                testFragment.put(GLUtil.testFragmentData).position(0);
            } else {
                ParseJson.getVertextPointFromJson(param, leftBottomf, rightTopf, leftTopf, rightBottomf);
            }
        }

        long endTime = System.currentTimeMillis();
        updateSurface = true;
        Log.i(TAG, "onFrameAvailable, create current frame matrix cost time : " + (endTime - beginTime) + "ms");



//        double sleepTime = (double) 1 / (double) fps;
//        Log.i(TAG, "onFrameAvailable " + surface.getTimestamp() + ", sleep time : " + sleepTime);
//        glSurfaceView.requestRender();
//        try {
//            Thread.sleep((long) (sleepTime * 1000));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int VERTICES_DATA_POS_OFFSET = 0;
    private static final int VERTICES_DATA_UV_OFFSET = 3;
//    private final float[] mVerticesData = {
//        // X, Y, Z, U, V
//        -1.25f, -1.0f, 0, 0.f, 0.f,
//         1.25f, -1.0f, 0, 1.f, 0.f,
//        -1.25f,  1.0f, 0, 0.f, 1.f,
//         1.25f,  1.0f, 0, 1.f, 1.f,
//    };
    private final float[] mVerticesData = {
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0, 0.0f, 0.0f,
        1.0f, -1.0f, 0, 1.0f, 0.0f,
        -1.0f,  1.0f, 0, 0.0f, 1.0f,
        1.0f,  1.0f, 0 , 1.0f, 1.0f,
    };

    private float[] attributesData = new float[4 * 5];
    private short[] indicesData;


    private FloatBuffer mVertices;
    private FloatBuffer testVertices;
    private FloatBuffer testFragment;
    private FloatBuffer attributeBuffer;
    private ShortBuffer indicesBuffer;

    private FloatBuffer noRotationFragment;

    private final String mVertexShader =


        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uSTMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec4 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
        "}\n";

    //注：uSTMatrix常用于不同角度的旋转0，90，180，270
    private final String oriVertexShader =
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";

    private final String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "   void main() {\n" +
            "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    private final String blendVertexShader =
            "precision mediump float;\n" +
//            "uniform mat4 uSTMatrix;\n" +
            "uniform mat3 u_World;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec3 vTextureCoord;\n" +
            "void main() {\n" +
            "  vec3 xyz = u_World * vec3(aPosition.xy, 1);\n" +
            "  gl_Position = vec4(xyz.xy, 0, 1);\n" +
            "  vTextureCoord = vec3(aTextureCoord.xyz);\n" +
            "}\n";

    private final String blendVertexShader2 =
            "precision mediump float;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = vec4(aPosition.xy, 0, 1);\n" +
                    "  vTextureCoord = vec2(aTextureCoord.xy);\n" +
                    "}\n";

    private final String blendFragmentShader =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec3 vTextureCoord;\n" +
        " uniform sampler2D sTexture;\n" +
        "   void main() {\n" +
        "  gl_FragColor = texture2D(sTexture, vTextureCoord.xy / vTextureCoord.z);\n" +
        "}\n";


    //normal shader
    private final String VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            "attribute vec4 inputTextureCoordinate3;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            "varying vec2 textureCoordinate3;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "    textureCoordinate3 = inputTextureCoordinate3.xy;\n" +
            "}";

    public final String ADD_BLEND_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            "\n" +
            " uniform samplerExternalOES inputImageTexture;\n" +
            " uniform samplerExternalOES inputImageTexture2;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "   lowp vec4 base = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   lowp vec4 overlay = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "   gl_FragColor = base + overlay;\n" +
            " }";

    public final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " varying highp vec2 textureCoordinate3;\n" +
            "\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " uniform sampler2D inputImageTexture3;\n" +

                    " \n" +
            " void main()\n" +
            " {\n" +
            "   lowp vec4 base = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   lowp vec4 overlay = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "   lowp vec4 mask = texture2D(inputImageTexture3, textureCoordinate3);\n" +
            "   if (mask.r == 1.0 && mask.g == 1.0 && mask.b == 1.0)\n" +
            "   {\n" +
            "       gl_FragColor = base;\n" +
            "   }\n" +
            "   else  {\n" +
            "       gl_FragColor = overlay;\n" +
            "   }\n" +
//              " if (mask.a >= 0.0 && base.a >= 0.0 && overlay.a >=0.0)\n" +
//              " {\n" +
////                    "       gl_FragColor = mix(base, overlay, overlay.a);\n" +
//
//                    "       gl_FragColor =  mask;\n" +
//              " }\n" +
            " }";


    private ParseJson.Param jsonParam = null;
    private String externalFragmentShader = "";
    private String externalYuvVertextShader;
    private String externalYuvFragmentShader;
    private float[] mMVPMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private float[] oriSTMatrix = new float[16];
    private float[] world = new float[9];

    //width and height
    private int viewWidth;
    private int viewHeight;

    //transform program
    private int mProgram;
    private int mTextureID;
    private int maskTextureID;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int uWorldHandle;

    private int leftBottom;
    private int rightTop;
    private int leftTop;
    private int rightBottom;

    private float[] leftBottomf = new float[2];
    private float[] rightTopf = new float[2];
    private float[] leftTopf = new float[2];
    private float[] rightBottomf = new float[2];

    //not transform program
    private int nTranProgram;
    private int nTranSTMatrixHandle;
    private int nTranPositionHandle;
    private int nTranTextureHandle;
    private int fboOriTexture2DID;
    private int fboOri2DID;

    //mask program
    private int maskProgram;
    private int maskPositionHandler;
    private int maskTextureHandler;
    private int maskYHandler;
    private int maskUHandler;
    private int maskVHandler;
    private int maskColorConversionHandler;

    //ori program
    private int origProgram;
    private int fboTextureID;
    private int fboID;
    private int origPositionHandle;
    private int origTextureHandle;
    private int blendingTextureHandle;
    private int maskTextureHandle;
    private int uniformOriTextureHandle;
    private int uniformBlendingTextureHandle;
    private int uniformMaskTextureHandle;
    private int uniformBlendingTexture1Handle;

    //fbos
    private int fbo1ID;
    private int fboMaskID;
    private int fboTexture1ID;
    private int fboMaskTextureID;

    //mask yuv texture ids
    private int yTexture = 0;
    private int uTexture = 0;
    private int vTexture = 0;
    boolean newFrame = false;
    int frameWidth = -1;
    int frameHeight = -1;
    private ByteBuffer yBuffer;
    private ByteBuffer uBuffer;
    private ByteBuffer vBuffer;
    private byte[] yBytes;
    private byte[] uBytes;
    private byte[] vBytes;

    private float mRatio = 1.0f;
    private SurfaceTexture mSurface;
    private SurfaceTexture mSurface2;
    private boolean updateSurface = false;
    private long mLastTime = -1;
    private long mRunTime = 0;

    //fps
    private int fps = 15;
    private long startTime = -1;


    //render method : 0 - project fixed 1 - fragment shader
    private int renderKey = 0;

    //set get every frame player
    private IsmartvPlayer player;

    //blend pic thread
    private Queue<Runnable> runnableQueue = new LinkedBlockingDeque<>();
    private static final Object object = new Object();
    private int blendPicTexture = -1;

    private static final String TAG = "MyRenderer";
    private GLSurfaceView glSurfaceView;

    public void setPlayer(IsmartvPlayer player) {
        this.player = player;
    }

    // Magic key
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    public SurfaceTexture getSurfaceTexture() {
        return mSurface;
    }

    public SurfaceTexture getSurfaceTexture2() {
        return mSurface2;
    }

    public void setBlendPic(final Bitmap pic) {
        if (pic != null) {
            Runnable loadTexture = new Runnable() {
                @Override
                public void run() {
                    blendPicTexture = GLUtil.loadTexture(pic);
                }
            };
            runOnGLThread(loadTexture);
        }
    }

    private void runOnGLThread(Runnable runnable) {
        synchronized (object) {
            runnableQueue.add(runnable);
        }
    }
}
