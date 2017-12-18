package com.example.alizey.testing;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity implements BluetoothStateCallback{

    private static final String device = "HC-05";
    private static final UUID BT_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");

    private TextView connectionState;
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothSocket bluetoothSocket;
    private static ConnectTask connectTask;
    private static WriterThread writerThread;
    public static String left = null;
    public static String right = null;

    //Button send;
    //EditText ed;
   // String sendingSerial;
    TextView tv3;
    TextView tvleft;
    TextView tvright;
    Button emergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //connectionState = (TextView) findViewById(R.id.textView3);
        //send = (Button) findViewById(R.id.button2);
        //ed = (EditText) findViewById(R.id.editText2);
        connectionState = (TextView) findViewById(R.id.textView);
        tvleft = (TextView) findViewById(R.id.textView_strength_left);
        tvright = (TextView) findViewById(R.id.textView_strength_right);
        tv3 = (TextView) findViewById(R.id.textView3);
        emergency = (Button) findViewById(R.id.button);

        emergency.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               sendCommand("115001");
           }
        });

        JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                left = steering(angle, strength);
                if("115".equals(left)){
                    sendCommand("115600");
                }
                else{
                    finalTarzan();
                }
                tvleft.setText(steering(angle, strength));



            }
        });

        JoystickView joystickRight = (JoystickView) findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                right = speed(angle, strength);
                if("001".equals(right)){
                    sendCommand("600001");
                }
                else{
                    finalTarzan();
                }
                tvright.setText(speed(angle, strength));

            }
        });




        MainActivity.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // register these receivers, we need them for setting up connections automatically
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        // try to connect
        MainActivity.connectTask = new ConnectTask();
        MainActivity.connectTask.execute();
    }

    private void finalTarzan() {

        //
        if (left == null) {
            sendCommand("600" + right);
            tv3.setText("---" + right);
            left = null;
            right = null;
        }
        else if(right == null){
            sendCommand(left + "600");
            tv3.setText(left + "---");
            left = null;
        }
        else{
            sendCommand(left + right);
            tv3.setText(left + right);
            left = null;
            right = null;
        }

    }

    private String steering(int ang, int str){

        String ans = "";
        float temp = 115;

        if(ang == 0 && str == 0){
            return "115";
        }
        else{
            if(ang == 180){
                temp = (float) ((((float)str/100)*65) + 115);
                ans = ans + (int)temp;
                //return ans;
            }
            else if(ang == 0){
                temp = (float) (115 - (((float)str/100)*65));

                if(temp < 100){
                    ans = "0" + (int)temp;
                    //return ans;
                }
                else {
                    ans = ans + (int)temp;
                    //return ans;
                }

            }
        }

        return ans;
    }

    private String speed(int ang, int str){

        float temp = 1;
        String ans = "";

        if(ang == 0 && str == 0){
            return "001";
        }
        else{
            if(ang == 90){
                temp = (float) ((((float)str/100)*155) + 100);
                ans = ans + (int)temp;
            }
            else if(ang == 270){
                temp = (float) ((((float)str/100)*155) + 255);
                if (temp == 255)
                    temp = 256;
                ans = ans + (int)temp;
            }
        }

        if(temp == 1)
            return "001";

        return ans;
    }



    //Sends a command to the writerThread.
    private void sendCommand(String command) {
        if (MainActivity.writerThread != null) {
            MainActivity.this.connectionState.setText("trying to send command \"" + command + "\"");
            MainActivity.writerThread.queueSend(command.getBytes());
        } else {
            MainActivity.this.connectionState.setText("could not send command \"" + command + "\" because there is no socket connection");
        }
    }


    //When the writer head fails to write, this method gets called
    @Override
    public void onWriteFailure(final String e) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText("failure: " + e + ", reconnecting...");
                if (MainActivity.connectTask != null && connectTask.getStatus() == AsyncTask.Status.RUNNING) {
                    MainActivity.connectTask.cancel(true);
                }
                MainActivity.connectTask = new ConnectTask();
                MainActivity.connectTask.execute();
            }
        });

    }

    //When the writerhead writes succesfully to the socket, this method gets called.
    @Override
    public void onWriteSuccess(final String command) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText("successfully sent command: \"" + command + "\"");
            }
        });
    }

    private void restartWriterThread() {
        if (MainActivity.writerThread != null) {
            MainActivity.writerThread.interrupt();
            MainActivity.writerThread.setRunning(false);
            MainActivity.writerThread = null;
        }
        MainActivity.writerThread = new WriterThread(MainActivity.this, MainActivity.bluetoothSocket);
        MainActivity.writerThread.start();
    }

    class ConnectTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            connectionState.setText("trying to connect to " + MainActivity.device + "...");
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                // we need to enable the bluetooth first in order to make this app working
                if (!bluetoothAdapter.isEnabled()) {
                    publishProgress("bluetooth was not enabled, enabling...");
                    bluetoothAdapter.enable();
                    // turning on bluetooth takes some time
                    while (!bluetoothAdapter.isEnabled()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    publishProgress("bluetooth turned on");
                }
                // here we are going to check if the device was paired in android, if not the user will be prompt to do so.
                String address = null;
                for (BluetoothDevice d : bluetoothAdapter.getBondedDevices()) {
                    if (MainActivity.device.equals(d.getName())) {
                        address = d.getAddress();
                        break;
                    }
                }
                if (address == null) {
                    return MainActivity.device + " was never paired. Please pair first using Android.";
                }
                // we have a mac address, now we try to open a socket
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                publishProgress("creating socket...");
                MainActivity.bluetoothSocket = device.createRfcommSocketToServiceRecord(MainActivity.BT_UUID);
                publishProgress("canceling discovery...");
                // we cancel discovery for other devices, since it will speed up the connection
                MainActivity.bluetoothAdapter.cancelDiscovery();
                publishProgress("trying to connect to " + device + " with address " + address);
                // try to connect to the bluetooth device, if unsuccessful, an exception will be thrown
                MainActivity.bluetoothSocket.connect();
                // start the writerThread
                restartWriterThread();
                return "connected, writer thread is running";
            } catch (IOException e) {
                try {
                    // try to close the socket, since we can have only one
                    MainActivity.bluetoothSocket.close();
                } catch (IOException e1) {
                    return "failure due " + e.getMessage() + ", closing socket did not work.";
                }
                return "failure due " + e.getMessage();
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    // the bluetooth was turned off, so we stop any running connection tasks
                    if (MainActivity.connectTask != null && connectTask.getStatus() == AsyncTask.Status.RUNNING) {
                        MainActivity.connectTask.cancel(true);
                    }
                    connectionState.setText("bluetooth was turned off, restarting...");
                    // enable the bluetooth again, and wait till it is turned on
                    bluetoothAdapter.enable();
                    while (!bluetoothAdapter.isEnabled()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    connectionState.setText("bluetooth turned on");
                    // try to connect again with the device
                    MainActivity.connectTask = new ConnectTask();
                    MainActivity.connectTask.execute();
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // this event is useful if the user has paired the device for the first time
                if (MainActivity.connectTask == null || MainActivity.connectTask.getStatus() == AsyncTask.Status.FINISHED) {
                    connectionState.setText("connected with bluetooth device, reconnecting...");
                    // reconnect since the app is doing nothing at this moment
                    MainActivity.connectTask = new ConnectTask();
                    MainActivity.connectTask.execute();
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // if the connection gets lost, we have to reconnect again
                connectionState.setText("connection lost, reconnecting...");
                if (MainActivity.connectTask != null && connectTask.getStatus() == AsyncTask.Status.RUNNING) {
                    MainActivity.connectTask.cancel(true);
                }
                MainActivity.connectTask = new ConnectTask();
                MainActivity.connectTask.execute();
            }
        }
    };



}
