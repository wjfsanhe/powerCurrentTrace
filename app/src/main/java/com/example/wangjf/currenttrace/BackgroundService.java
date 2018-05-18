package com.example.wangjf.currenttrace;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackgroundService extends Service {
    public final String TAG = "BackgroundService";

    private final IBinder mBinder = new Binder();
    private boolean mScreenOnOff = true;
    private final String mCurrentNode= "/sys/class/power_supply/battery/current_now";
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {

                // It is time to bump the value!
                case 1: {
                    try {
                        if (msg.arg1 == 1) {
                            Thread.sleep(2000);
                            readFile(mCurrentNode, "on");

                        } else if (msg.arg1 ==0) {
                            Thread.sleep(2000);
                            readFile(mCurrentNode, "off");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    //sys_path 为节点映射到的实际路径
    public  String readFile(String sys_path, String onOff) {
        String prop = "waiting";// 默认值
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(sys_path));
            prop = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, " ***ERROR*** Here is what I know: " + e.getMessage());
        } finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.w(TAG, "readFile cmd from"+sys_path + "data"+" -> prop = "+prop);
        saveCurrent(prop, onOff);
        return prop;
    }
    private void saveCurrent(String currentVal , String onOff) {
        Date nowTime = new Date(System.currentTimeMillis());
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String dateTime = sdFormatter.format(nowTime);
        String line = dateTime + " : " + currentVal + "  [" +onOff + "]" + "\r\n";
        Log.w(TAG, line);
        writeToSDCardFile("current", "trace.txt",line
                , "UTF-8", true);
    }
    public  boolean isFileExist(String director) {
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + director);
        return file.exists();
    }

    public  boolean createFile(String director) {
        if (isFileExist(director)) {
            return true;
        } else {
            File file = new File(Environment.getExternalStorageDirectory()
                    + File.separator + director);
            if (!file.mkdirs()) {
                return false;
            }
            return true;
        }
    }
    public  File writeToSDCardFile(String directory, String fileName,
                                   String content, String encoding, boolean isAppend) {
        // mobile SD card path +path
        File file = null;
        OutputStream os = null;
        try {
            if (!createFile(directory)) {
                return file;
            }
            file = new File(Environment.getExternalStorageDirectory()
                    + File.separator + directory + File.separator + fileName);
            os = new FileOutputStream(file, isAppend);
            if (encoding.equals("")) {
                os.write(content.getBytes());
            } else {
                os.write(content.getBytes(encoding));
            }
            os.flush();
        } catch (IOException e) {
            Log.e("FileUtil", "writeToSDCardFile:" + e.getMessage());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
    private Runnable delayedTask = new Runnable() {
        @Override
        public void run() {
            readFile(mCurrentNode, mScreenOnOff ? "on" : "off");
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String prop = "waiting";

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mHandler.sendMessage(mHandler.obtainMessage(1,1,0));

            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOnOff = false;
                mHandler.sendMessage(mHandler.obtainMessage(1,0,0));
            } else {
                Log.w(TAG, "Registered for but not handling action " + action);
            }
        }
    };
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate be called");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // Register for broadcasts of interest.
        registerReceiver(mBroadcastReceiver, filter, null, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand be called");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
