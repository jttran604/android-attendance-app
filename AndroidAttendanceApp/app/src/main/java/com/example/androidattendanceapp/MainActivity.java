/* Contributions
Jacob Medel: Initial class skeleton and onCreate
Justin Tran:
Wasif Reaz: Data Transfer, Attendance Counter, Comments
Esteban Kim: Submit Button UI, Functionality of Attendance Counter, Comments
 */
package com.example.androidattendanceapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

//main activity for app including scan submission, and incrementing respective attendance referencing id list
public class MainActivity extends AppCompatActivity {
    int attendanceCounter = 0;
    TextView attendanceCounterTextView;
    EditText cometIdEditText;
    Button submitButton;
    Bluetooth bluetooth;
    DataTransferThread dt;
    List<String> idList;
    MutableLiveData<Integer> receivedListOfIDs;
    MutableLiveData<Boolean> enableSubmitButton;

    ActivityResultLauncher<Intent> bTActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "bTActivityResultLauncher.onActivityResult: Bluetooth Enabled Successfully");
                    }
                }
            });

    //enables bluetooth and pulls list of IDs for comparison from output stream and submission for scanner
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receivedListOfIDs = new MutableLiveData<>(0);
        enableSubmitButton = new MutableLiveData<>(false);

        idList = new ArrayList<>();

        bluetooth = new Bluetooth(this);
        bluetooth.checkBluetoothPermissions(this);

        submitButton = (Button) findViewById(R.id.submitButton);

        /*
        TextView that shows total successfully signed in student
         */
        attendanceCounterTextView = (TextView) findViewById(R.id.attendanceCounterTextView);
        receivedListOfIDs.observe(MainActivity.this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer totalAttendance) {
                attendanceCounterTextView.setText("Total Attendance: " + attendanceCounter + " / " + totalAttendance);
            }
        });

        enableSubmitButton.observe(MainActivity.this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enableSubmitButton) {
                submitButton.setEnabled(enableSubmitButton);
            }
        });

        /*
        EditText that displays scanned CometID
         */
        cometIdEditText = (EditText) findViewById(R.id.cometIdEditText);
        cometIdEditText.setText("");

        // Listens for when Newline is inputted and calls submitID
        cometIdEditText.addTextChangedListener(new TextWatcher() {

            //Before and after functions required; goes unused
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {}

            //Checks If Last Input is a newline character; if so submit
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (start + before <= charSequence.length()) {
                    char lastInput = charSequence.charAt(start + before);
                    if (lastInput == '\n') {
                        submitUserId(cometIdEditText);
                    }
                }
            }
        });

        cometIdEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                }
                return false;
            }

        });

        bluetooth.startBluetoothConnection(this, bTActivityResultLauncher);

        IntentFilter bluetoothIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetooth.bluetoothBroadcastReceiver, bluetoothIntentFilter);
    }

    //creates data transfer thread
    public void startDataTransfer(BluetoothSocket mainSocket) {
        dt = new DataTransferThread(mainSocket, idList, receivedListOfIDs, enableSubmitButton);
        dt.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.closeEverything();
        unregisterReceiver(bluetooth.bluetoothBroadcastReceiver);
        dt.cancel();
    }

    /*
    * Increments total successfully signed in students within class section
     */
    public void incrementAttendance() {
        attendanceCounter++;
        attendanceCounterTextView.setText("Total Attendance: " + String.valueOf(attendanceCounter) + " / " + String.valueOf(idList.size()));
    }

    /*
    * Submits scanned CometID after successfully confirming scanned CometID exists within database
    *
    * @param view Vies used to display success or failure of submitted userID
     */
    public void submitUserId(View view) {

        //Displays message depending on whether scanning failed or succeeded
        String inputValue = cometIdEditText.getText().toString();

        boolean successfulLogin = false;

        for (int idIndex = 0; idIndex < idList.size(); idIndex++) {
            if (inputValue.contains(idList.get(idIndex))) {
                dt.write(inputValue.getBytes());
                incrementAttendance();
                successfulLogin = true;
                break;
            }
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        //Detects and displays success of scan
        if (successfulLogin) {
            Snackbar.make(view, "Scan Success", Snackbar.LENGTH_LONG).show();
        }
        else {
            Snackbar.make(view, "Scan Failure. See Professor", Snackbar.LENGTH_LONG).show();
        }

        cometIdEditText.getText().clear();
    }
}
