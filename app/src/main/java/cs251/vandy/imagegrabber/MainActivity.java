package cs251.vandy.imagegrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;


/**
 * The main UI for downloading an image
 */
public class MainActivity extends Activity {

    /**
     * Identifier for a messenger in the service intent
     */
    public static final String MESSENGER_OBJECT = "messenger";

    /**
     * Identifier for the downloadHandler message
     */
    public static final int MESSAGE_IDENTIFIER = 1;

    /**
     * The button that downloads the image through a thread
     */
    private Button mThreadButton;

    /**
     * The button that downloads the image through a service
     */
    private Button mServiceButton;

    /**
     * The keyboard where the image location is entered
     */
    private EditText mAddrText;

    /**
     * The TableLayout element that formats the image display
     */
    private TableLayout mLayout;

    /**
     * The handler for the ui
     */
    private mainHandler uiHandler;

    /**
     * The handler for the background thread
     */
    private DownloadImageHandler downloadHandler;

    /**
     * Private class to create the ui Handler
     */
    private class mainHandler extends Handler {

        /**
         * mainHandler()
         * Constructor for the Handler
         * @param looper the looper associated with the thread
         */
        public mainHandler(Looper looper) {
            super(looper);
        }

        /**
         * handleMessage()
         * Takes the necessary data from the msg and calls display using the data
         * @param msg the incoming message
         */
        @Override
        public void handleMessage(Message msg) {
            display((Intent) msg.obj);
        }
    }

    /**
     * onCreate()
     * Instantiates the UI of the main UI
     * @param savedInstanceState used if the activity needs to be recreated without
     *                           losing prior information
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddrText = (EditText) findViewById(R.id.imageAddress);
        mThreadButton = (Button) findViewById(R.id.threadButton);
        mServiceButton = (Button) findViewById(R.id.serviceButton);
        mLayout = (TableLayout) findViewById(R.id.mainLayout);

        uiHandler = new mainHandler(getMainLooper());

        final HandlerThread download = new HandlerThread("Image Downloader");
        download.start();

        downloadHandler = new DownloadImageHandler(download.getLooper(), MainActivity.this);

        /**
         * the "Run" button to download in a thread
         */
        mThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threadButtonClick();
            }
        });

        /**
         * the "Run" button to download via a service
         */
        mServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceButtonClick();
            }
        });
    }

    /**
     * threadButtonClick()
     * Puts the String form of the image address and a Messenger associated with the ui
     * in a message and sends it to the downloadHandler
     */
    private void threadButtonClick() {

        int permissionCheck = this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            this.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        Message msg = downloadHandler.obtainMessage(MESSAGE_IDENTIFIER, getText());
        msg.replyTo = new Messenger(uiHandler);
        downloadHandler.sendMessage(msg);
    }

    /**
     * serviceButtonClick()
     * Starts the sequence that downloads the image via a service
     */
    private void serviceButtonClick() {

        int permissionCheck = this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            this.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        startService(makeServiceIntent(getText()));
    }

    /**
     * display()
     * Creates a row in mLayout containing the image's name, download time, and
     * a button that, upon being clicked, opens the gallery application to display
     * the image
     * @param data an Intent containing the image location, name, and download time
     */
    private void display(final Intent data) {

        final Uri mLoc = data.getData();

        if(mLoc != null) {

            double time = data.getLongExtra(DownloadImageHandler.DOWNLOAD_TIME, 1) / 1000.0;
            String imgName = data.getStringExtra(DownloadImageHandler.IMAGE_NAME);

            LayoutInflater inflater = getLayoutInflater();
            TableRow tr = (TableRow) inflater.inflate(R.layout.table_row, null, false);

            TextView nameView = (TextView) tr.findViewById(R.id.image_name);
            TextView timeView = (TextView) tr.findViewById(R.id.image_time);
            Button button = (Button) tr.findViewById(R.id.showButton);

            nameView.setText(imgName);
            timeView.setText(String.valueOf(time) + "s");
            button.setText("Show");

            mLayout.addView(tr);

            /**
             * the "Show" button
             */
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(makeGalleryIntent(mLoc));
                }
            });

        } else {
            Toast mToast = Toast.makeText(this, "Invalid URL", Toast.LENGTH_LONG);
            mToast.show();
        }
    }

    /**
     * makeGalleryIntent()
     * factory method for creating the Intent to open the gallery application
     * @param mLoc the absolute file path to the image
     * @return an Intent with data containing the absolute file path to the image
     */
    private Intent makeGalleryIntent(Uri mLoc) {
        Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
        galleryIntent.setDataAndType(mLoc, "image/*");
        return galleryIntent;
    }

    /**
     * makeServiceIntent()
     * factory method for creating the Intent to start a service which will download
     * the image
     * @param address the URL of the image in String form
     * @return an Intent with data containing the URL of the image in String form
     *         and a Messenger bound to the ui thread
     */
    private Intent makeServiceIntent (String address) {
        Intent serviceIntent = new Intent(this, DownloadIntentService.class);
        serviceIntent.setData(Uri.parse(address));
        serviceIntent.putExtra(MESSENGER_OBJECT, new Messenger(uiHandler));
        return serviceIntent;
    }

    /**
     * getText()
     * converts the sequence entered by the user via the EditText object
     * into a String
     * @return whatever the user entered in String form
     */
    private String getText() {
        return mAddrText.getText().toString();
    }
}