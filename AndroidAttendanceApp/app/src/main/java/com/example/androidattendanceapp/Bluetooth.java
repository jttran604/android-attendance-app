/* Contributions
Jacob Medel: Minor bug fixing, Inital class skeleton and packaging
Justin Tran:
Wasif Reaz: Comments
Esteban Kim: Permission Request, Bluetooth Enablement, Discovery, and Connection Functionality, Comments
 */
package com.example.androidattendanceapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//detects bluetooth activity across devices and establishes connections with permission
public class Bluetooth extends Activity {
    private final int REQUEST_PERMISSION_BLUETOOTH = 1;
    private int currentIndex = 0;
    private BluetoothAdapter bluetoothAdapter;
    private MainActivity mainActivity;
    private Set<String> previouslyCheckedBluetoothDevices;
    private List<BluetoothDevice> discoveredDevices;
    private Set<BluetoothDeviceConnectThread> bluetoothDeviceConnectThreads;

    @SuppressLint("MissingPermission")
    //opens broadcastreceiver for app for finding bluetooth devices
    public final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String bluetoothDeviceName = bluetoothDevice.getName();
                String bluetoothDeviceHardwareAddress = bluetoothDevice.getAddress();

                if (!previouslyCheckedBluetoothDevices.contains(bluetoothDeviceHardwareAddress)) {
                    previouslyCheckedBluetoothDevices.add(bluetoothDeviceHardwareAddress);
                    discoveredDevices.add(bluetoothDevice);
                    startConnectOnDiscoveredDevices();
                }
            }
        }
    };
    /*
     Checks if new Bluetooth devices have been discovered and starts the connection
     */
    public Bluetooth(MainActivity mainActivity) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainActivity = mainActivity;
        previouslyCheckedBluetoothDevices = new HashSet<>();
        discoveredDevices = new ArrayList<>();
        bluetoothDeviceConnectThreads = new HashSet<>();
    }

    /*
     * Checks the Bluetooth status of the device and prompts user to enable bluetooth if disabled
     *
     * @param mainActivity Activity used to exit in non Bluetooth capable devices
     * @param bluetoothActivityResultLauncher Activity used to launch bluetooth enablement request
     */
    @SuppressLint("MissingPermission")
    public void startBluetoothConnection(MainActivity mainActivity, ActivityResultLauncher<Intent> bluetoothActivityResultLauncher) {
        // Bluetooth Not Supported
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth.startBluetoothConnection: Bluetooth Capability Not Supported");
            mainActivity.finish();
        }
        // Bluetooth Not Enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothActivityResultLauncher.launch(enableBTIntent);
        }
        // Bluetooth Previously Enabled
        else {
            Log.d(TAG, "Bluetooth.startBluetoothConnection: Bluetooth Enabled");
        }

        bluetoothAdapter.startDiscovery();
        findPairedDevices();
    }

    /*
    * Checks permissions necessary for Bluetooth connection are given
    *
    * @param mainActivity Activity used to display Bluetooth permission requests
     */
    public void checkBluetoothPermissions(MainActivity mainActivity) {
        if (mainActivity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                mainActivity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                mainActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                mainActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mainActivity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_BLUETOOTH);
        }
    }

    /*
    * Checks previously paired bluetooth device
     */
    @SuppressLint("MissingPermission")
    public void findPairedDevices() {
        Set<BluetoothDevice> previouslyPairedBluetoothDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bluetoothDevice : previouslyPairedBluetoothDevices) {
            String bluetoothDeviceName = bluetoothDevice.getName();
            String bluetoothDeviceHardwareAddress = bluetoothDevice.getAddress();

            if (!previouslyCheckedBluetoothDevices.contains(bluetoothDeviceHardwareAddress)) {
                previouslyCheckedBluetoothDevices.add(bluetoothDeviceHardwareAddress);

                BluetoothDeviceConnectThread bluetoothDeviceConnectThread = new BluetoothDeviceConnectThread(bluetoothAdapter, bluetoothDevice, mainActivity,true);
                bluetoothDeviceConnectThreads.add(bluetoothDeviceConnectThread);
                bluetoothDeviceConnectThread.start();
            }
        }
    }

    /*
    * Checks if new Bluetooth devices have been discovered and starts the connection
     */
    public void startConnectOnDiscoveredDevices() {
        if (currentIndex < discoveredDevices.size()) {
            BluetoothDevice bluetoothDevice = discoveredDevices.get(currentIndex);
            currentIndex++;

            BluetoothDeviceConnectThread bluetoothDeviceConnectThread = new BluetoothDeviceConnectThread(bluetoothAdapter, bluetoothDevice, mainActivity,false);
            bluetoothDeviceConnectThreads.add(bluetoothDeviceConnectThread);
            bluetoothDeviceConnectThread.start();
        }
    }

    /*
    * Closes all paired devices
     */
    @SuppressLint("MissingPermission")
    public void closeEverything() {
        for (BluetoothDeviceConnectThread pairedDeviceThread : bluetoothDeviceConnectThreads) {
            pairedDeviceThread.cancel();
        }
    }
}
