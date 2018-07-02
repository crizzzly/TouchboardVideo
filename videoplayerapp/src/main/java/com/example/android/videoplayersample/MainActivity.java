package com.example.android.videoplayersample;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.example.chrissi.touchboardvideo.USB_PERMISSION";
    Button startButton, stopButton; //,sendButton, clearButton;
    TextView textView;
    //EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    String[] uris = new String[3];
    int actualVideo = 0;
    boolean deviceConnected = false;



    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                tvAppend(textView, data);



            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ACTION_USB_PERMISSION)) {
                boolean granted =
                        Objects.requireNonNull(intent.getExtras()).getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            //setUiEnabled(true); //Enable Buttons in UI
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback); //
                            tvAppend(textView, "Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (Objects.requireNonNull(intent.getAction()).equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);
            }
        }
    };


    //checks if external strorage is writable
    public boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)){
            return true;
        }
        return false;
    }

    //checks if external storage is available to at least read
    public boolean isExternalStorageReadable(){
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //create logDirectories & file
        if(isExternalStorageWritable()){
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder");
            File logDirectory = new File(appDirectory+"/log");
            File logFile = new File(logDirectory, "logcat" + System.currentTimeMillis() + ".txt");

            //create appFolder
            if(!appDirectory.exists()){
                appDirectory.mkdir();
            }

            //create logFolder
            if(!logDirectory.exists()){
                logDirectory.mkdir();
            }

            //clear previous logcat and then write the new one to file
            try{
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile + " *:S MainActivity:D VideoActivity:D" );
            }catch (IOException e){
                e.printStackTrace();
            }
        }



        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton =findViewById(R.id.buttonStart);
        stopButton =  findViewById(R.id.buttonStop);
        textView =  findViewById(R.id.textView);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        uris[0]= "asset:///video_1.mp4";
        uris[1]= "asset:///video_2.mp4";
        uris[2]= "asset:///video_3.mp4";

    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        //sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341 || deviceVID == 0x2a6e)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                    deviceConnected = true;
                } else {
                    connection = null;
                    device = null;
                    deviceConnected = false;
                }

                if (!keep){
                    //return connected;
                    break;
                }
            }
        }
    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView, "\nSerial Connection Closed! \n");

    }

    private void startVideo(CharSequence seq){
        CharSequence ftext = seq;
        tvAppend(textView, "\nstarting Video Playback"+ftext.toString()+"!");

        int number = Integer.parseInt(ftext.toString());
        //tvAppend(textView, "\n"+uris[number]+"\n");
        tvAppend(textView, "number to start: "+number);
        if (number < uris.length ||
                number == 10 ||
                number == 11){
                tvAppend(textView, "\ncreated Intent\n");

                if (number == 10) {
                    actualVideo++;
                    actualVideo %= uris.length;
                } else if (number == 9) {
                    actualVideo++;
                    actualVideo %= uris.length;
                } else actualVideo = number;
            tvAppend(textView, "starting video activity: "+number);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(MainActivity.this, VideoActivity.class);

                    intent.putExtra("URI", uris[actualVideo]);
                    startActivity(intent);
                }
            });



        }


        /*
        else if(number == 11){
            intent.putExtra("URI", "null");
            startActivity(intent);
        }
        */



    }


    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);

            }
        });

        if(ftext.toString().length() < 5) {

            startVideo(ftext);
        }
    }


}
