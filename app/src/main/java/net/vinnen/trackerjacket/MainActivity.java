package net.vinnen.trackerjacket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "ConnectThread";

    ConnectThread connectThread;
    String targetName = "TrackerJacket";

    ConstraintLayout[] cl = new ConstraintLayout[5];
    TextView tv[] = new TextView[20];

    public String[] valuesToDisplay = new String[20];
    public double[] values = new double[20];
    public double[] valuesOffset = new double[20];
    public String ctrlString = "r";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button sendTime = (Button)findViewById(R.id.sendTimeBtn);
        sendTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectThread.sendString("t" + System.currentTimeMillis() + "s");
            }
        });

        cl[0] = (ConstraintLayout) findViewById(R.id.body);
        cl[1] = (ConstraintLayout) findViewById(R.id.upperLeftArm);
        cl[2] = (ConstraintLayout) findViewById(R.id.lowerLeftArm);
        cl[3] = (ConstraintLayout) findViewById(R.id.upperRightArm);
        cl[4] = (ConstraintLayout) findViewById(R.id.lowerRightArm);

        for (int i = 0; i < 5; i++) {
            tv[i*4] = cl[i].findViewById(R.id.Label);
            tv[i*4+1] = cl[i].findViewById(R.id.yaw);
            tv[i*4+2] = cl[i].findViewById(R.id.pitch);
            tv[i*4+3] = cl[i].findViewById(R.id.roll);
        }

        valuesToDisplay[0] = "Body:";
        valuesToDisplay[4] = "UpLeft:";
        valuesToDisplay[8] = "LowLeft";
        valuesToDisplay[12] = "UpRight:";
        valuesToDisplay[16] = "LowRight:";

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(getApplicationContext(),"Bluetooth not Supported on this Device", Toast.LENGTH_LONG).show();
            return;
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice targetDevice = null;

        Log.d(TAG, "Host Device Name: " + targetName);

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals(targetName)){
                    targetDevice = device;
                }
            }
        }
        if(targetDevice==null){
            Log.e(TAG, "No target found");
            return;
        }
        connectThread = new ConnectThread(targetDevice, this);
        connectThread.start();
    }

    public void updateDisplay(){
        for (int i = 0; i < valuesToDisplay.length; i++) {
            if(i%4 != 0){
                values[i] = Double.parseDouble(valuesToDisplay[i])-valuesOffset[i];
                //valuesToDisplay[i] = String.valueOf(values[i]).split(",")[0];
            }
            tv[i].setText(String.valueOf(values[i]).split("\\.")[0]);//valuesToDisplay[i]);
        }
    }

    public void calibrateJacket(View v){
        for (int i = 0; i < valuesOffset.length; i++) {
            if(i%4 != 0){
                valuesOffset[i] = values[i];
            }
        }
        connectThread.sendString("C");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connectThread.sendString("c");
        Toast.makeText(this, "Calibration done", Toast.LENGTH_SHORT).show();
    }
}
