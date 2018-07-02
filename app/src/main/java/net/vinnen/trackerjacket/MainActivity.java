package net.vinnen.trackerjacket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.Policy;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    public static final String TAG = "ConnectThread";

    public ArmSegment uLA = new ArmSegment(0.15f,1.6f,-0.1f,0.065f,0.3f,0.065f,0,0,-90);
    public ArmSegment uRA = new ArmSegment(-0.15f,1.6f,-0.1f,0.065f,0.3f,0.065f ,0, 0, 0);

    public ArmSegment lLA = new ArmSegment(0,0,0,0.065f,0.3f,0.065f,0,0,0);
    public ArmSegment lRA = new ArmSegment(0,0,0,0.065f,0.3f,0.065f,0,0,0);

    private TextureView mTextureView;
    Camera mCamera;
    Canvas pic;

    ConnectThread connectThread;
    RendererThread rendererThread;
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

        mTextureView = findViewById(R.id.textuV);

        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            private float firstDown = 0f;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    firstDown = event.getAxisValue(0);
                    return true;
                }
                rendererThread.rota = (event.getAxisValue(0)-firstDown)/2;
                //Log.d(TAG,"Scroll: " + (event.getAxisValue(0)-firstDown));
                return true;
            }
        });

        mTextureView.setSurfaceTextureListener(this);

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

        valuesToDisplay[16] = "Body:";
        valuesToDisplay[12] = "UpLeft:";
        valuesToDisplay[8] = "LowLeft";
        valuesToDisplay[4] = "UpRight:";
        valuesToDisplay[0] = "LowRight:";

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
                values[i] = Double.parseDouble(valuesToDisplay[i]);
                //valuesToDisplay[i] = String.valueOf(values[i]).split(",")[0];
            }
            if(i%4 == 0){
                tv[i].setText(valuesToDisplay[i]);
            }else {
                tv[i].setText(String.valueOf(values[i]).split("\\.")[0]);//valuesToDisplay[i]);
            }
        }

        lRA.rotX = (float)values[2];
        lRA.rotY = (float) (360 - (values[1] - valuesOffset[1]));
        lRA.rotZ = (float) (180 - (values[3]+90));
        //Log.d(TAG, "Val: " + (values[1] - valuesOffset[1]));

        uRA.rotX = (float)values[6];
        uRA.rotY = (float) (360 - (values[5] - valuesOffset[5]));
        uRA.rotZ = (float) (180 - (values[7]+90));

        lLA.rotX = (float)values[10];
        lLA.rotY = (float) (180 - (values[9] - valuesOffset[9]));
        lLA.rotZ = (float) (180 - (values[11]+90));

        uLA.rotX = (float)values[14];
        uLA.rotY = (float) (180 - (values[13] - valuesOffset[13]));
        uLA.rotZ = (float) (180 - (values[15]+90));
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

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {


        rendererThread = new RendererThread(surface, this);
        rendererThread.start();

        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(rendererThread.mProjectionMatrix, 0, -ratio, ratio, -1, 1, 0.8f, 3);

        /*
        mCamera = Camera.open();

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        //pic = mTextureView.lockCanvas();
        //pic.drawCircle(50,50,50, green);
        //mTextureView.unlockCanvasAndPost(pic);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {


    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        rendererThread.isStopped = true;
        //mCamera.stopPreview();
        //mCamera.release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //pic.drawCircle(0,0,1, green);
        //mTextureView.unlockCanvasAndPost(pic);
    }
}
