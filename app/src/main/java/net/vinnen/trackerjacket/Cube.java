package net.vinnen.trackerjacket;

import android.opengl.GLES20;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * Created by Julius on 20.06.2018.
 */

public class Cube {
    private final int mProgram;

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;


    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float cubeCoords[];

    static float topLeft[] = {-0.5f,  0.5f, 0.5f};
    static float bottomLeft[] = {-0.5f, -0.5f, 0.5f};
    static float bottomRight[] = {0.5f, -0.5f, 0.5f};
    static float topRight[] = {0.5f,  0.5f, 0.5f};
    static float topLeftBack[] = {-0.5f,  0.5f, -0.5f};
    static float bottomLeftBack[] = {-0.5f, -0.5f, -0.5f};
    static float bottomRightBack[] = {0.5f, -0.5f, -0.5f};
    static float topRightBack[] = {0.5f,  0.5f, -0.5f};

    float edges[][] ={topLeft, bottomLeft, bottomRight, topRight, topLeftBack, bottomLeftBack, bottomRightBack, topRightBack};


    private short drawOrder[] = { 0, 1, 2, 0, 2, 3,//Vorne
    3, 2, 6, 6, 7, 3, //Rechts
    0, 5, 1, 5, 0, 4,//Links
    5, 4, 6, 6, 4, 7, //Hinten
    1, 5, 2, 2, 5, 6, //Unten
    0, 3, 4, 3, 7, 4//Oben
    }; // order to draw vertices

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 1f, 0f, 0f, 0.50f };

    public Cube() {
        ArrayList<Float> l = new ArrayList<Float>();
        for (short s: drawOrder) {
            for (int i = 0; i < edges[s].length; i++) {
                l.add(edges[s][i]);
            }
        }
        cubeCoords = new float[l.size()];
        int x = 0;
        for (Float f: l) {
            cubeCoords[x++] = (f != null ? f : Float.NaN);
        }
        vertexCount = cubeCoords.length / COORDS_PER_VERTEX;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                cubeCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(cubeCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        int vertexShader = RendererThread.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = RendererThread.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);
        Log.d("tria", GLES20.glGetShaderInfoLog(vertexShader));

    }

    private int mPositionHandle;
    private int mColorHandle;

    private final int vertexCount;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);


        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    // Use to access and set the view transformation
    private int mMVPMatrixHandle;


}
