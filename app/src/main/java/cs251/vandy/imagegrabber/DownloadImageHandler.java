package cs251.vandy.imagegrabber;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Downloads an image via a thread using handleMessage()
 * handleMessage() will be called when this Handler receives a message
 */
public class DownloadImageHandler extends Handler {

    /**
     * Identifier for the image name in the reply Intent
     */
    public static final String IMAGE_NAME = "name";

    /**
     * Identifier for the time taken to download in the reply Intent
     */
    public static final String DOWNLOAD_TIME = "time";

    /**
     * The context of the thread
     */
    private WeakReference<Context> mContext;

    /**
     * DownloadImageHandler()
     * Constructor for the Handler
     * @param looper this thread's looper
     * @param context this thread's context
     */
    public DownloadImageHandler(Looper looper, Context context) {
        super(looper);
        mContext = new WeakReference<Context>(context);
    }

    /**
     * handleMessage()
     * Called when this Handler receives a message
     * Will download an image via a thread and store the absolute location of the
     * image URI, the image name, and the time taken to download in an Intent which
     * will be sent back to the ui thread to handle there
     * @param msg the message sent to this Handler containing the String address of
     *            the image
     */
    @Override
    public void handleMessage(Message msg) {

        String address = (String) msg.obj;
        String imgName = new File(address).getName();

        long before = System.currentTimeMillis();
        Uri imgLoc = DownloadUtils.downloadImage(mContext.get(), Uri.parse(address));
        long downTime = System.currentTimeMillis() - before;

        Intent replyData = makeReplyIntent(imgLoc, imgName, downTime);

        Message replyMsg = Message.obtain();
        replyMsg.obj = replyData;

        try {
            msg.replyTo.send(replyMsg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * makeReplyIntent()
     * factory method for creating a reply Intent to the ui thread
     * this Intent contains the image's absolute location, the image's name, and the
     * time taken to download the image
     * @param imgLoc the image's absolute location
     * @param imgName the image's name
     * @param downTime the time taken to download the image
     * @return an Intent containing the image's absolute location, name, and time taken to
     *         download
     */
    public static Intent makeReplyIntent(Uri imgLoc, String imgName, long downTime) {
        Intent replyIntent = new Intent();
        replyIntent.setData(imgLoc);
        replyIntent.putExtra(IMAGE_NAME, imgName);
        replyIntent.putExtra(DOWNLOAD_TIME, downTime);
        return replyIntent;
    }
}
