package krot.sample.com.hypebroadcastingvideo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.hypelabs.hype.Error;
import com.hypelabs.hype.Hype;
import com.hypelabs.hype.Instance;
import com.hypelabs.hype.Message;
import com.hypelabs.hype.MessageInfo;
import com.hypelabs.hype.MessageObserver;
import com.hypelabs.hype.NetworkObserver;
import com.hypelabs.hype.StateObserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements StateObserver, NetworkObserver, MessageObserver {

    private static final int PICK_FILE_REQUEST = 109;
    public static final int PERMISSION_CODE = 110;
    private static final int REQUEST_PERMISSION_SETTING = 120;

    private List<Instance> instanceList;
    private SimpleExoPlayer player;

    /**
     * Views
     **/

    @BindView(R.id.tv_device_count)
    TextView tvDeviceCount;

    @BindView(R.id.video_message)
    SimpleExoPlayerView videoMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        videoMessage.setVisibility(View.INVISIBLE);

        String[] permission = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!hasPermission(this, permission)) {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_CODE);
        } else {
            initializeHypeService();
            Hype.start();
        }

    }

    private void initializePlayer() {

        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(),
                new DefaultLoadControl());

        videoMessage.setPlayer(player);

    }

    private void initializeHypeService() {
        Hype.setContext(this);

        // Generate an app identifier in the HypeLabs dashboard (https://hypelabs.io/apps/),
        // by creating a new app. Copy the given identifier here.

        Hype.addStateObserver(this);
        Hype.addNetworkObserver(this);
        Hype.addMessageObserver(this);

        Hype.setAppIdentifier("f0441ff3");
    }


    private boolean hasPermission(Context context, String ... permissionQueue) {
        if (context != null && permissionQueue != null) {
            for (String permission : permissionQueue) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        initializeHypeService();
                        Hype.start();
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (shouldShowRationale) {
                            //show the reason why user must grant STORAGE permission
                            //show dialog
                            new AlertDialog.Builder(this).setTitle("Permission Denied").setMessage(R.string.permission_rationale).setPositiveButton("RE-TRY", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
                                }
                            }).setNegativeButton("I'M SURE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();

                        } else {
                            //never ask again
                            //close dialog and do nothing
                            new AlertDialog.Builder(this)
                                    .setTitle("Grant permission")
                                    .setMessage(R.string.app_setting_permission)
                                    .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent appSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            appSettingIntent.setData(uri);
                                            startActivityForResult(appSettingIntent, REQUEST_PERMISSION_SETTING);
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    }).show();
                        }
                    }
                }
                break;
        }
    }

    /**
     * MESSAGE
     **/
    @Override
    public void onHypeMessageReceived(Message message, Instance instance) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "received", Toast.LENGTH_SHORT).show();
            }
        });

        Log.i("WTF", "received: messsage = " + message.getData());
    }

    @Override
    public void onHypeMessageFailedSending(MessageInfo messageInfo, Instance instance, Error error) {

    }

    @Override
    public void onHypeMessageSent(MessageInfo messageInfo, Instance instance, float v, boolean b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "message sent", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onHypeMessageDelivered(MessageInfo messageInfo, Instance instance, float v, boolean b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "message delivered", Toast.LENGTH_SHORT).show();
            }
        });

    }


    /**
     * NETWORK
     **/
    @Override
    public void onHypeInstanceFound(Instance instance) {
        Log.i("WTF", "Found: instance = " + instance.getStringIdentifier());
        Hype.resolve(instance);
    }

    @Override
    public void onHypeInstanceLost(Instance instance, Error error) {
        Log.i("WTF", "Lost: instance = " + instance.getStringIdentifier());
        if (instanceList != null) {
            instanceList.remove(instance);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDeviceCount.setText("[" + instanceList.size() + "]");
            }
        });
    }

    @Override
    public void onHypeInstanceResolved(Instance instance) {
        Log.i("WTF", "Resolved: instance = " + instance.getStringIdentifier());
        if (instanceList != null) {
            instanceList.add(instance);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDeviceCount.setText("[" + instanceList.size() + "]");
            }
        });
    }

    @Override
    public void onHypeInstanceFailResolving(Instance instance, Error error) {
        Log.i("WTF", "Fail Resolving: instance = " + instance);
    }


    /**
     * STATE
     **/
    @Override
    public void onHypeStart() {
        initializePlayer();
        instanceList = new ArrayList<>();
        Log.i("WTF", "onHypeStart");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Enabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onHypeStop(Error error) {
        Log.i("WTF", "onHypeStop");
        if (instanceList != null) {
            instanceList.clear();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDeviceCount.setText("[0]");
                Toast.makeText(MainActivity.this, "Disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onHypeFailedStarting(final Error error) {
        Log.i("WTF", "onHypeFailedStarting");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Error code = " + error.getCode() + " - description = " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onHypeReady() {

    }

    @Override
    public void onHypeStateChange() {

    }

    @Override
    public String onHypeRequestAccessToken(int i) {
        Log.i("WTF", "onHypeRequestAccessToken");
        return "064e04e5ab0669db7eaa5561eb8dde";
    }


    @OnClick(R.id.btn_start)
    public void doStartHype() {
        Hype.start();
    }

    @OnClick(R.id.btn_stop)
    public void doStopHype() {
        Hype.stop();
    }

    @OnClick(R.id.btn_choose_video)
    public void doPickVideo() {
        if (instanceList.size() > 0) {
            Intent chooseImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(Intent.createChooser(chooseImageIntent, "Choose an app below: "), PICK_FILE_REQUEST);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE_REQUEST) {

            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri filePath = data.getData();
                Log.i("WTF", "data[1] = " + filePath + " - fileName = " + filePath.getLastPathSegment());
                String path = getPath(filePath);
                Log.i("WTF", "path = " + path);

                byte[] message = new byte[0];
                try {
                    message = convert(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.i("WTF", "message = " + message);

                //send message
                for (int i = 0; i < instanceList.size(); i++) {
                    Hype.send(message, instanceList.get(i));
                }


            }
        }
    }






    public void saveFile(byte[] data) {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Hype");

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = new File(dir, "test.mp4");

        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public String getPath(Uri uri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }


    public byte[] getVideoMessage(String picturePath) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(picturePath));
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }


    public byte[] convert(String path) throws IOException {

        FileInputStream fis = new FileInputStream(path);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];

        for (int readNum; (readNum = fis.read(b)) != -1;) {
            bos.write(b, 0, readNum);
        }

        byte[] bytes = bos.toByteArray();

        return bytes;
    }
}
