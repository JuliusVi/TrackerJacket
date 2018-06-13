package net.vinnen.trackerjacket;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Julius on 13.06.2018.
 */

public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String TAG = "ConnectThread";

    private MainActivity context;
    char x = 'y';

    public ConnectThread(BluetoothDevice device, MainActivity context) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.context = context;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            UUID tmpo = device.getUuids()[0].getUuid();
            tmp = device.createRfcommSocketToServiceRecord(tmpo);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        //mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.e(TAG,"Could not establish connection",connectException);
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
        Log.d(TAG, "Connection sucessfull");

        // The connection attempt succeeded.

        OutputStream outSt = null;
        InputStream inSt = null;
        try {
            outSt = mmSocket.getOutputStream();
            inSt = mmSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStreamReader inRead = new InputStreamReader(inSt);
        BufferedReader bRead = new BufferedReader(inRead);

        Log.d(TAG,"running");

        String line = "";
        int nextSens=0;

        try {
            while (!(line = bRead.readLine()).startsWith("S")) {
                final String[] parts = line.split(",");
                int multi = Integer.parseInt(parts[0]);
                //Log.d(TAG, line);
                for (int i = 2; i < 5; i++) {
                    int glob = multi * 4 + (i - 1);
                    Log.d(TAG, glob + "" + glob);
                    context.valuesToDisplay[glob] = parts[i];
                }
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        context.updateDisplay();
                    }
                });
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        try {
            outSt.close();
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"End of transmission");


    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
