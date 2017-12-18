package com.example.alizey.testing;

/**
 * Created by Alizey on 18-12-2017.
 */

interface BluetoothStateCallback {

    void onWriteFailure(String e);

    void onWriteSuccess(String command);
}
