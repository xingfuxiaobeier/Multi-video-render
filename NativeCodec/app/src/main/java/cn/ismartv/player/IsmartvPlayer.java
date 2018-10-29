package cn.ismartv.player;

import android.view.Surface;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by huibin on 09/08/2017.
 */

public class IsmartvPlayer {

    private long objId;
    private final CopyOnWriteArraySet listeners;

    public IsmartvPlayer() {
        listeners = new CopyOnWriteArraySet();
        _init();
    }

    public native void _init();

    public native boolean _prepare(String source);

    public native boolean _prepare2(String source);

    public native void _setSurface(Surface surface);

    public native long _getDuration();

    public native long _getCurrentPosition();

    public native void _seekTo(long positionMs);

    public native void _setPlayWhenReady(boolean playWhenReady);

    public native void _rewind();

    public native void _stop();
    public native void _stop2();


    public native FrameData _getVideoFrameBySort(String source, Surface surface, int index);

    public static class Data {
        private byte[] data;
        private int linesize;
        private int format;
        private int width;
        private int height;

        public Data() {
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public int getLinesize() {
            return linesize;
        }

        public void setLinesize(int linesize) {
            this.linesize = linesize;
        }

        public int getFormat() {
            return format;
        }

        public void setFormat(int format) {
            this.format = format;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }
    public static class FrameData {
        private ArrayList<Data> datas;

        public FrameData() {
        }

        public ArrayList<Data> getDatas() {
            return datas;
        }

        public void setDatas(ArrayList<Data> datas) {
            this.datas = datas;
        }
    }


//
//    public native void seekTo();
//
//    public native void stop();
//
//    public native void release();

//    public void addListener(EventListener listener) {
//        listeners.add(listener);
//    }
//
//    public void removeListener(EventListener listener) {
//        listeners.remove(listener);
//    }
//
//    interface EventListener {
//
//        void onPlayerError(Exception error);
//
//    }

}
