package net.vinnen.trackerjacket;

import android.opengl.Matrix;

/**
 * Created by Julius on 25.06.2018.
 */

public class ArmSegment {
    public ArmSegment(float posX, float posY, float posZ, float x, float y, float z, float rotX, float rotY, float rotZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
    }

    public float posX;
    public float posY;
    public float posZ;
    public float x;
    public float y;
    public float z;
    public float rotX;
    public float rotY;
    public float rotZ;

    public float vecX = 0;
    public float vecY = y;
    public float vecZ = 0;

    public float endX = 0;
    public float endY = 0;
    public float endZ = 0;

    public void getEndpoint(){
        //Reset
        vecX = 0;
        vecY = y;
        vecZ = 0;

        //RotZ
        vecX = (float) (Math.cos(Math.toRadians(rotZ) * vecX) - (Math.sin(Math.toRadians(rotZ))) * vecY);
        vecY = (float) (-Math.sin(Math.toRadians(rotZ) * vecX) + (Math.cos(Math.toRadians(rotZ))) * vecY);
        vecZ = 1 * vecZ;

        //RotX
        vecX = 1 * vecX;
        vecY = (float) (Math.cos(Math.toRadians(rotX) * vecY) - (Math.sin(Math.toRadians(rotX))) * vecZ);
        vecZ = (float) (Math.sin(Math.toRadians(rotX) * vecY) + (Math.cos(Math.toRadians(rotX))) * vecZ);

        //RotY
        vecX = (float) (Math.cos(Math.toRadians(rotY) * vecX) + (Math.sin(Math.toRadians(rotY))) * vecZ);
        vecY = 1 * vecY;
        vecZ = (float) (-Math.sin(Math.toRadians(rotY) * vecX) + (Math.cos(Math.toRadians(rotY))) * vecZ);



        endX = posX;// + vecX;
        endY = posY;// + vecY;
        endZ = posZ;// + vecZ;
    }
}
