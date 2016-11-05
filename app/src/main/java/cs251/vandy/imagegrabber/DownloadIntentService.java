package cs251.vandy.imagegrabber;

        import android.app.IntentService;
        import android.content.Intent;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.Message;
        import android.os.Messenger;
        import android.os.RemoteException;

        import java.io.File;

/**
 * Downloads an image via an IntentService
 */
public class DownloadIntentService extends IntentService {

    /**
     * DownloadIntentService()
     * Constructor for the class
     */
    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    /**
     * onHandleIntent()
     * Does the work of the service, is called when the service runs
     * This downloads the image while keeping track of how long it took to download and
     * puts the image's absolute location, its name, and how long it took to download in
     * an Intent which will be sent back to the ui Handler to then display
     * @param data an Intent containing the address of the image in String form
     */
    @Override
    public void onHandleIntent(Intent data) {

        Uri address = data.getData();
        String imgName = new File("" + address).getName();

        long before = System.currentTimeMillis();
        Uri imgLoc = DownloadUtils.downloadImage(getApplicationContext(), address);
        long downTime = System.currentTimeMillis() - before;

        Intent replyData = DownloadImageHandler.makeReplyIntent(imgLoc, imgName, downTime);

        Message replyMsg = Message.obtain();
        replyMsg.obj = replyData;

        Bundle bundle = data.getExtras();
        Messenger replyMessenger = (Messenger) bundle.get(MainActivity.MESSENGER_OBJECT);

        try {
            replyMessenger.send(replyMsg);
        } catch (RemoteException e) {
            e.printStackTrace();;
        }
    }
}

