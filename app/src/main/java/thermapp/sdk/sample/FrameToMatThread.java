package thermapp.sdk.sample;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by fuele on 11/3/2016.
 */

public class FrameToMatThread implements Runnable {
        private int[] frame;
        Handler myHandler;
        public FrameToMatThread(int[] frame, final ImageView imageBlock) {
            this.frame = frame;
            myHandler = new Handler(Looper.getMainLooper()) {
                ImageView iv=imageBlock;
                @Override
                public void handleMessage(Message inputMessage) {
                    Bitmap bmp = (Bitmap) inputMessage.obj;
                    iv.setImageBitmap(bmp);
                }
            };
        }
        @Override
    public synchronized void run() {
            Mat mat = new Mat(384, 288, CvType.CV_8UC1);
            int index=0, max=4000, min=2000;
             for (int c = 0; c<288; c++)
                    for (int r = 0; r<384; r++)
                         mat.put(r, c, (frame[index++]-min)*255/(max-min));

            Bitmap bmp = Bitmap.createBitmap(288,384, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);

            Message completeMessage =
                    myHandler.obtainMessage(0, bmp);
            completeMessage.sendToTarget();
        }


    public void start() {
    }
}
