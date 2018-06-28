package net.vinnen.trackerjacket;

import android.opengl.Matrix;

/**
 * Created by Julius on 25.06.2018.
 */

public class ArmSegment {
    public ArmSegment(float posX, float posY, float posZ, float dimX, float dimY, float dimZ, float rotX, float rotY, float rotZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.dimX = dimX;
        this.dimY = dimY;
        this.dimZ = dimZ;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
    }

    public float posX;
    public float posY;
    public float posZ;
    public float dimX;
    public float dimY;
    public float dimZ;
    public float rotX;
    public float rotY;
    public float rotZ;

    public float vecX = 0;
    public float vecY = dimY;
    public float vecZ = 0;

    public float endX = 0;
    public float endY = 0;
    public float endZ = 0;

    public void getEndpoint(){
        //Reset
        vecX = 0;
        vecY = dimY;
        vecZ = 0;

        /*
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
*/

        //Roll des Vektors ist egal
        //rotateVector(vecX,vecY, vecZ, "X", rotX);

        rotateVector(vecX,vecY, vecZ, "Z", rotZ);
        rotateVector(vecX,vecY, vecZ, "Y", rotY);

        endX = posX + vecX;
        endY = posY + vecY;
        endZ = posZ + vecZ;
    }

    public void rotateVector(double x, double y, double z, String axis, double deg){
        double u, v, w;
        u=0;v=0;w=0;
        if(axis.equals("X")){
            u=1;v=0;w=0;
        } else if(axis.equals("Y")){
            u=0;v=1;w=0;
        } else if(axis.equals("Z")){
            u=0;v=0;w=1;
        }
        vecX =(float)( u*(u*x + v*y + w*z)*(1d - Math.cos(Math.toRadians(deg)))
                + x*Math.cos(Math.toRadians(deg))
                + (-w*y + v*z)*Math.sin(Math.toRadians(deg)));
        vecY = (float)( v*(u*x + v*y + w*z)*(1d - Math.cos(Math.toRadians(deg)))
                + y*Math.cos(Math.toRadians(deg))
                + (w*x - u*z)*Math.sin(Math.toRadians(deg)));
        vecZ =  (float)( w*(u*x + v*y + w*z)*(1d - Math.cos(Math.toRadians(deg)))
                + z*Math.cos(Math.toRadians(deg))
                + (-v*x + u*y)*Math.sin(Math.toRadians(deg)));
    }
}
