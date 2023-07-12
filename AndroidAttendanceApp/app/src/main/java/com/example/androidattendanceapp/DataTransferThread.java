/* Contributions
Jacob Medel: Refactoring to store ID's, Inital class skeleton
Justin Tran:
Wasif Reaz: Data Transfer Thread, Comments
Esteban Kim: Bluetooth Data Read/Write Functionality
 */
package com.example.androidattendanceapp;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

//Establishes thread for IO streams for reading and writing ID list
public class DataTransferThread extends Thread {
    private static final String TAG = "DATA_TRANSFER_THREAD";
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private List<String> idList;
    private byte[] mmBuffer;
    private MutableLiveData<Integer> receivedListOfIDs;
    private MutableLiveData<Boolean> enableSubmitButton;

    int totalAttendanceCounter = 0;

    //Creates IO streams for data transfer
    public DataTransferThread(BluetoothSocket socket, List<String> idList, MutableLiveData<Integer> receivedListOfIDs, MutableLiveData<Boolean> enableSubmitButton) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.idList = idList;
        this.mmBuffer = new byte[1024];
        this.receivedListOfIDs = receivedListOfIDs;
        this.enableSubmitButton = enableSubmitButton;

        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    //reads the student id's and appends on run
    public void run() {
        int numBytes;

        // Needed to read initial set of data
		String allListID = "*ID*";
		byte[] byteArray = allListID.getBytes();
		write(byteArray);

        while (true) {
            try {
                numBytes = mmInStream.read(mmBuffer);

                String received = new String(mmBuffer, 0, numBytes);

                if (received.trim().length() > 0 && !received.contains("*")) {
                    idList.add(received.trim());
                    totalAttendanceCounter++;
                    receivedListOfIDs.postValue(totalAttendanceCounter);
                    enableSubmitButton.postValue(true);
                }
            }
            catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    // Call From Main
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        }
        catch (Exception e) {
            Log.e(TAG, "Error: " , e);
        }
    }

    // Call From Main
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }

}
