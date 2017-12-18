package com.example.alizey.testing;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Alizey on 18-12-2017.
 */

public class WriterThread extends Thread {

    private LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private MainActivity mainActivity;
    private volatile boolean isRunning = true;
    private BluetoothSocket bluetoothSocket;

    WriterThread(MainActivity mainActivity, BluetoothSocket bluetoothSocket) {
        this.mainActivity = mainActivity;
        this.bluetoothSocket = bluetoothSocket;

    }

    @Override
    public void run() {
        try {
            // get the output stream from the bluetooth device, so we can send data on it
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            // this while is looping, but don't worry, we use a nice queue
            // if there is no data to send, the thread will be blocked
            while (isRunning()) {
                try {
                    // get data from the queue, and write it
                    byte[] data = queue.take();
                    outputStream.write(data);
                    // there are no exceptions so the writing was successful
                    this.mainActivity.onWriteSuccess(new String(data));
                } catch (InterruptedException e) {
                    // something went wrong, so notify the activity
                    this.mainActivity.onWriteFailure("thread was interrupted");
                    break;
                }
            }
        } catch (IOException e) {
            // something went wrong, so notify the activity
            this.mainActivity.onWriteFailure("thread caught IOException: " + e.getMessage());
        } finally {
            // the thread stopped, let's try to close the socket
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                this.mainActivity.onWriteFailure("could not close socket");
            }
        }
    }

    // get the running state of the thread
    private boolean isRunning() {
        return isRunning;
    }

    //Used to stop the running thread. Once stopped, you can't run the same thread again.
    void setRunning(boolean running) {
        isRunning = running;
    }

    // adding some data to the queue
    // the data to be sent to the socket.
    void queueSend(byte[] data) {
        queue.add(data);
    }

}
