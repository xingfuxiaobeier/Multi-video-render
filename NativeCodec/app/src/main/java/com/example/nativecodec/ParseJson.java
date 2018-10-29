package com.example.nativecodec;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by dhb on 2018/6/15.
 */

public class ParseJson {

    private static int width = 544;
    private static int height = 968;

    /**
     * 读取assert目录中文件
     * @param name
     * @return
     */
    public static String readAssertFilte(String name) {
        StringBuffer  buffer = new StringBuffer();
        Context context = Application.getContext();
        InputStream is = context.getClass().getClassLoader().getResourceAsStream("assets/"+name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        BufferedReader reader = null;
//        try {
//            reader = new BufferedReader(new FileReader(name));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        String tmp = "";
        try {
            while ((tmp = reader.readLine()) != null) {
                buffer.append(tmp);
                buffer.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    /**
     * 根据定点坐标你渲染纹理（存在问题：画不同的三角形时形变程度不同）
     * @param json
     * @param index
     * @return
     */
    public static float[] createVertexMatrix(String json, int index) {
        JSONArray array = JSON.parseArray(json);
        float[] pixF = new float[12];
        float[] pixFOut = new float[12];
        JSONObject object = (JSONObject) array.get(index % array.size());
        boolean d = object.getBoolean("d");
        int t = object.getInteger("t");
        String c = object.getString("c");
        JSONArray jsonArray = object.getJSONArray("c");
        Object[] pix = jsonArray.toArray();
        int m = 0;
        float w = -1;
        float h = -1;
        float cP = -1;

        for (int j = 0; j < pix.length; j++) {
            if (j % 2 == 0) {
                w = Float.parseFloat(pix[j].toString());
                w = (float) (w - 0.5 * width) / (float) (0.5 * width);
                cP = w;
            } else if (j % 2 == 1) {
                h = Float.parseFloat(pix[j].toString());
                h = (float) (h - 0.5 * height) / (float) (0.5 * height);
                cP = h;
            }
            pixF[m] = cP / (float) 1;
            if (j % 2 == 1) {
                m++;
                pixF[m] = 0;
            }
            m++;
        }

        //set matrix value directly
        pixFOut[0] = pixF[6];
        pixFOut[1] = -pixF[7];
        pixFOut[2] = -pixF[8];
        pixFOut[3] = pixF[9];
        pixFOut[4] = -pixF[10];
        pixFOut[5] = -pixF[11];
        pixFOut[6] = pixF[0];
        pixFOut[7] = -pixF[1];
        pixFOut[8] = -pixF[2];
        pixFOut[9] = pixF[3];
        pixFOut[10] = -pixF[4];
        pixFOut[11] = -pixF[5];

//        pixFOut = pixF;

//            System.out.println("d : " + d);
//            System.out.println("t : " + t);
        System.out.println("json array size : " + array.size());
//            int j = -1;
//            System.out.print("current maxtix : {");
//            for (j = 0; j < pixF.length - 1; j++) {
//                System.out.print(pixF[j] + ",");
//            }
//            if (j == (pixF.length - 1)) {
//                System.out.println(pixF[j]);
//            }
//            System.out.print("}");
        return pixFOut;
    }

    /**
     * 投影矫正的方式生成定点坐标系
     * @param pix
     * @param pixFOut
     */
    private static void createVertexMatrix2(Object[] pix, float[] pixFOut) {

        float bottomLeft_x = Float.parseFloat(pix[0].toString());
        float bottomLeft_y = Float.parseFloat(pix[1].toString());
        float bottomRight_x = Float.parseFloat(pix[2].toString());
        float bottomRight_y = Float.parseFloat(pix[3].toString());
        float topLeft_x = Float.parseFloat(pix[4].toString());
        float topLeft_y = Float.parseFloat(pix[5].toString());
        float topRight_x = Float.parseFloat(pix[6].toString());
        float topRight_y = Float.parseFloat(pix[7].toString());

        pixFOut[0] = bottomLeft_x;
        pixFOut[1] = height - bottomLeft_y;
        pixFOut[2] = bottomRight_x;
        pixFOut[3] = height - bottomRight_y;
        pixFOut[6] = topLeft_x;
        pixFOut[7] = height - topLeft_y;
        pixFOut[4] = topRight_x;
        pixFOut[5] = height - topRight_y;

//        pixFOut[0] = topLeft_x;
//        pixFOut[1] = topLeft_y;
//        pixFOut[2] = topRight_x;
//        pixFOut[3] = topRight_y;
//        pixFOut[4] = bottomRight_x;
//        pixFOut[5] = bottomRight_y;
//        pixFOut[6] = bottomLeft_x;
//        pixFOut[7] = bottomLeft_y;


    }

    /**
     * 与上边生成顶点坐标系配套方法，投影矫正生成纹理坐标系
     * @param pix
     * @param pixFOut
     */
    private static void createFragmentMatrix(Object[] pix, float[] pixFOut) {

        //json中的数据对应数组p位置
        //pix[0],pix[1] -- p[0]
        //pix[2],pix[3] -- p[1]
        //pix[4],pix[5] -- p[3]
        //pix[6],pix[7] -- p[2]
        System.arraycopy(GLUtil.baseFragmentData, 0, pixFOut, 0, GLUtil.baseFragmentData.length);

        float q[] = new float[4];
//
        calcQn(
                Float.parseFloat(pix[0].toString()), height - Float.parseFloat(pix[1].toString()),
                Float.parseFloat(pix[2].toString()), height - Float.parseFloat(pix[3].toString()),
                Float.parseFloat(pix[6].toString()), height - Float.parseFloat(pix[7].toString()),
                Float.parseFloat(pix[4].toString()), height - Float.parseFloat(pix[5].toString()),
                q);

//        calcQn(
//                305.681f, 388.135f,
//                537.711f, 388.133f,
//                537.713f, 786.702f,
//                305.681f, 786.703f,
////                284.801f, 390.686f,
////                508.487f, 345.963f,
////                591.18f, 714.742f,
////                382.394f, 782.887f,
//                q);


        for (int i = 0;i < pixFOut.length; i++) {
            int in = i / 3;
            pixFOut[i] *= q[in];

        }

//        printArray(pixFOut, "before return");

    }

    /**
     * 创建顶点坐标系和纹理坐标系
     * @param pix
     * @param vertex
     * @param fragment
     */
    public static void createVFMatrix(
            Object[] pix,
            float[] vertex,
            float[] fragment) {

        createVertexMatrix2(pix, vertex);
        createFragmentMatrix(pix, fragment);
    }

    /**
     * 读取json存入内存
     * @param json
     * @return
     */
    public static Param readJsonOut(String json) {
        JSONArray array = JSON.parseArray(json);
        int size = array.size();
        Param param = new Param();
        Object[][] out = new Object[size][];
        for (int i = 0; i < size; i++) {
            JSONObject object = (JSONObject) array.get(i);
//            boolean d = object.getBoolean("d");
//            int t = object.getInteger("t");
//            String c = object.getString("c");
            JSONArray jsonArray = object.getJSONArray("c");
            Object[] pix = jsonArray.toArray();
            out[i] = pix;
        }
        param.setSize(size);
        param.setValue(out);
        return param;

    }

    public static class Param {
        private int size;
        private Object[][] value;

        public Param() {
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public Object[][] getValue() {
            return value;
        }

        public void setValue(Object[][] value) {
            this.value = value;
        }
    }

    /**
     * 获取纹理坐标中四个点
     * @param pix
     * @param leftBottom
     * @param rightTop
     * @param leftTop
     * @param rightBottom
     */
    public static void getVertextPointFromJson(
            Object[] pix,
            float[] leftBottom,
            float[] rightTop,
            float[] leftTop,
            float[]rightBottom) {

        //reverse
        leftBottom[0] = Float.parseFloat(pix[0].toString()) / width;
        leftBottom[1] =Float.parseFloat(pix[1].toString()) / height;
        rightBottom[0] = Float.parseFloat(pix[2].toString()) / width;
        rightBottom[1] = Float.parseFloat(pix[3].toString()) / height;
        leftTop[0] = Float.parseFloat(pix[4].toString()) / width;
        leftTop[1] = Float.parseFloat(pix[5].toString()) / height;
        rightTop[0] = Float.parseFloat(pix[6].toString()) / width;
        rightTop[1] = Float.parseFloat(pix[7].toString()) / height;

        //fixed
//        leftBottom[0] = Float.parseFloat(pix[4].toString()) / width;
//        leftBottom[1] =Float.parseFloat(pix[5].toString()) / height;
//        rightBottom[0] = Float.parseFloat(pix[6].toString()) / width;
//        rightBottom[1] = Float.parseFloat(pix[7].toString()) / height;
//        leftTop[0] = Float.parseFloat(pix[0].toString()) / width;
//        leftTop[1] = Float.parseFloat(pix[1].toString()) / height;
//        rightTop[0] = Float.parseFloat(pix[2].toString()) / width;
//        rightTop[1] = Float.parseFloat(pix[3].toString()) / height;

    }

    /**
     * 打印
     * @param pixFOut
     * @param mes
     */
    public static void printArray(float[] pixFOut, String mes) {
        System.out.print("\n");
        int j = -1;
        System.out.print(mes + " : current maxtix : {");
        for (j = 0; j < pixFOut.length - 1; j++) {
            System.out.print(pixFOut[j] + ",");
        }
        if (j == (pixFOut.length - 1)) {
            System.out.print(pixFOut[j]);
        }
        System.out.print("}");
    }


    /**
     * 投影矫正方式生成所需的向量
     * @param p0x
     * @param p0y
     * @param p1x
     * @param p1y
     * @param p2x
     * @param p2y
     * @param p3x
     * @param p3y
     * @param q
     * @return
     */
    private static float[] calcQn(
            float p0x,
            float p0y,
            float p1x,
            float p1y,
            float p2x,
            float p2y,
            float p3x,
            float p3y,
            float[]q)
    {
        float ax = p2x - p0x;
        float ay = p2y - p0y;
        float bx = p3x - p1x;
        float by = p3y - p1y;

        float cross = ax * by - ay * bx;
        if (cross != 0) {
            float cy = p0y - p1y;
            float cx = p0x - p1x;

            float s = (ax * cy - ay * cx) / cross;

            if (s > 0 && s < 1) {

                float t = (bx * cy - by * cx) / cross;

                if (t > 0 && t < 1) {

                    q[0] = 1 / (1 - t);
                    q[1] = 1 / (1 - s);
                    q[2] = 1 / t;
                    q[3] = 1 / s;

                    // you can now pass (u * q, v * q, q) to OpenGL
                }
            }
        }
        return q;
    }

    /**
     * 同上 生成矩阵
     * @param bottomLeftX
     * @param bottomLeftY
     * @param bottomRightX
     * @param bottomRightY
     * @param topRightX
     * @param topRightY
     * @param topLeftX
     * @param topLeftY
     * @param attributesData
     */
    public static void drawNonAffine(
            float bottomLeftX,
            float bottomLeftY,
            float bottomRightX,
            float bottomRightY,
            float topRightX,
            float topRightY,
            float topLeftX,
            float topLeftY,
            float[] attributesData) {
        float ax = topRightX - bottomLeftX;
        float ay = topRightY - bottomLeftY;
        float bx = topLeftX - bottomRightX;
        float by = topLeftY - bottomRightY;

        float cross = ax * by - ay * bx;

        if (cross != 0) {
            float cy = bottomLeftY - bottomRightY;
            float cx = bottomLeftX - bottomRightX;

            float s = (ax * cy - ay * cx) / cross;

            if (s > 0 && s < 1) {
                float t = (bx * cy - by * cx) / cross;

                if (t > 0 && t < 1) {
                    //uv coordinates for texture
                    float u0 = 0; // texture bottom left u
                    float v0 = 0; // texture bottom left v
                    float u2 = 1; // texture top right u
                    float v2 = 1; // texture top right v

                    int bufferIndex = 0;

                    float q0 = 1 / (1 - t);
                    float q1 = 1 / (1 - s);
                    float q2 = 1 / t;
                    float q3 = 1 / s;

                    attributesData[bufferIndex++] = bottomLeftX;
                    attributesData[bufferIndex++] = bottomLeftY;
                    attributesData[bufferIndex++] = u0 * q0;
                    attributesData[bufferIndex++] = v2 * q0;
                    attributesData[bufferIndex++] = q0;

                    attributesData[bufferIndex++] = bottomRightX;
                    attributesData[bufferIndex++] = bottomRightY;
                    attributesData[bufferIndex++] = u2 * q1;
                    attributesData[bufferIndex++] = v2 * q1;
                    attributesData[bufferIndex++] = q1;

                    attributesData[bufferIndex++] = topRightX;
                    attributesData[bufferIndex++] = topRightY;
                    attributesData[bufferIndex++] = u2 * q2;
                    attributesData[bufferIndex++] = v0 * q2;
                    attributesData[bufferIndex++] = q2;

                    attributesData[bufferIndex++] = topLeftX;
                    attributesData[bufferIndex++] = topLeftY;
                    attributesData[bufferIndex++] = u0 * q3;
                    attributesData[bufferIndex++] = v0 * q3;
                    attributesData[bufferIndex++] = q3;

                }
            }
        }

    }

    public static void main(String[] args) {
//        String test = readAssertFilte("/Users/mtime/AndroidStudioProjects/NativeCodec/app/src/main/assets/data3.json");
//        createVertexMatrix(test, 0);
        int[][] test = new int[2][4];
        test[0][0] = 0;
        test[0][1] = 1;
        test[0][2] = 2;
        test[0][3] = 3;
        test[1][0] = 4;
        test[1][1] = 5;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print(test[i][j]);
            }
            System.out.print("\n");
        }
    }
}
