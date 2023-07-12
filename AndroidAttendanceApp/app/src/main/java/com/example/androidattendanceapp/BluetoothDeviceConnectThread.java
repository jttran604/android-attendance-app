/* Contributions
Jacob Medel: Refactoring with DataTransfer class
Justin Tran:
Wasif Reaz: Comments
Esteban Kim: Establishing Bluetooth Connection, Comments
 */
package com.example.androidattendanceapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

//connection thread for newly and previously connected bluetooth devices
public class BluetoothDeviceConnectThread extends Thread {
    private UUID mUUID = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
    private BluetoothAdapter bluetoothAdapter;
    private MainActivity mainActivity;
    private BluetoothSocket bluetoothSocket;

    @SuppressLint("MissingPermission")

    //Connect thread to maintain connections with previously paired devices
    public BluetoothDeviceConnectThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice bluetoothDevice, MainActivity mainActivity, Boolean previouslyPaired) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.mainActivity = mainActivity;
        BluetoothSocket temporarySocket = null;

        try {
            if (previouslyPaired) { temporarySocket = bluetoothDevice.createRfcommSocketToServiceRecord(mUUID); }
            else { temporarySocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(mUUID); }
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread.BluetoothDeviceConnectThread: Socket Failed to Connect with Error: ", e);
            return;
        }

        bluetoothSocket = temporarySocket;
    }

    @SuppressLint("MissingPermission")
    //Open bluetooth discovery and connection with close clause
    public void run() {
        bluetoothAdapter.cancelDiscovery();

        try {
            bluetoothSocket.connect();
        } catch (IOException connectException) {
            bluetoothAdapter.startDiscovery();
            try {
                bluetoothSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "ConnectThread.run: Unable to Close Socket with Error: ", closeException);
            }
            return;
        }

        Log.i(TAG, "Connected to: " + bluetoothSocket.getRemoteDevice().getName());
        mainActivity.startDataTransfer(bluetoothSocket);
    }
    //Cancel bluetooth connection
    @SuppressLint("MissingPermission")
    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread.cancel: Unable to Close Socket with Error: ", e);
        }
    }
}
