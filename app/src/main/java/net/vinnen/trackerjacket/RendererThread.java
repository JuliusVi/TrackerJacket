package net.vinnen.trackerjacket;


import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static javax.microedition.khronos.egl.EGL10.EGL_ALPHA_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_BLUE_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_DEPTH_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_GREEN_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_RENDERABLE_TYPE;
import static javax.microedition.khronos.egl.EGL10.EGL_STENCIL_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_SUCCESS;

/**
 * Created by Julius on 20.06.2018.
 */

public class RendererThread extends Thread {
    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    public final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    boolean isStopped = false;
    private SurfaceTexture surface;
    private Cube triangle;

    public RendererThread(SurfaceTexture surface){
        this.surface = surface;
    }

    int[] config = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 0,
            EGL_STENCIL_SIZE, 0,
            EGL_NONE
    };

    public EGLConfig chooseEglConfig(EGL10 egl, EGLDisplay eglDisplay){
        int[] configsCount = {0};
        EGLConfig[] configs = new EGLConfig[1];
        egl.eglChooseConfig(eglDisplay, config, configs, 1, configsCount);
        return configs[0];
    }

    @Override
    public void run() {
        super.run();
        EGL10 egl = (EGL10)EGLContext.getEGL();
        EGLDisplay eglDisplay = egl.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(eglDisplay, new int[]{0,0});   // getting OpenGL ES 2
        EGLConfig eglConfig = chooseEglConfig(egl, eglDisplay);
        EGLContext eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, new int[]{EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE});
        EGLSurface eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null);


        float colorVelocity = 0.01f;
        float color = 0f;

        double rota = 0d; //Winkel
        float xFin = 0f;
        float yFin = 0f;
        egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

        this.triangle = new Cube();
        while (!isStopped && egl.eglGetError() == EGL_SUCCESS) {
            egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
            if (color > 1 || color < 0) colorVelocity *= -1;
            color += colorVelocity;
            rota += 0.01;
            rota = rota % 360;

            xFin = (float)(2 * Math.cos(rota) - 2 * Math.sin(rota));
            yFin = (float)(2 * Math.sin(rota) + 2 * Math.cos(rota));

            GLES20.glClearColor(color / 2, color, color, 0.10f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Set the camera position (View matrix)
            Matrix.setLookAtM(mViewMatrix, 0, xFin, 4, yFin, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

            // Calculate the projection and view transformation
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            Matrix.translateM(mMVPMatrix,0,1,0,1);
            // Draw shape
            triangle.draw(mMVPMatrix);

            Matrix.translateM(mMVPMatrix,0,-1,0,-1);
            // Draw shape
            triangle.draw(mMVPMatrix);

            Matrix.translateM(mMVPMatrix,0,-1,0,-1);
            // Draw shape
            triangle.draw(mMVPMatrix);

            Matrix.translateM(mMVPMatrix,0,2,0,0);
            // Draw shape
            triangle.draw(mMVPMatrix);

            Matrix.translateM(mMVPMatrix,0,-2,0,2);
            Matrix.rotateM(mMVPMatrix,0,45,0,1,0);
            Matrix.rotateM(mMVPMatrix,0,90,1,0,0);
            Matrix.scaleM(mMVPMatrix,0,1 ,2 ,1 );
            // Draw shape
            triangle.draw(mMVPMatrix);

            egl.eglSwapBuffers(eglDisplay, eglSurface);

            try {
                Thread.sleep((long)(1f / 60f * 1000f)); // in real life this sleep is more complicated
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        surface.release();
        egl.eglDestroyContext(eglDisplay, eglContext);
        egl.eglDestroySurface(eglDisplay, eglSurface);
    }

    public static int loadShader(int type, String shaderCode){

        int[] compiled = new int[1];
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS, compiled, 0);
        if(compiled[0]==0){
            Log.d("rend", GLES20.glGetShaderInfoLog(shader));
        }

        return shader;
    }
}
