package co.polarr.demo.autoenhance;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import co.polarr.renderer.PolarrRender;
import co.polarr.renderer.filters.Basic;

/**
 * Created by Colin on 2017/11/18.
 * Camera Demo
 */

public class CameraRenderView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final int DESIGN_WIDTH = 1080;
    private static final int DESIGN_HEIGHT = 1920;

    private Context mContext;

    /**
     * Camera and SurfaceTexture
     */
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    private final OESTexture mCameraTexture = new OESTexture();
    private PolarrRender polarrRender = new PolarrRender();
    private int mWidth, mHeight;
    private boolean updateTexture = false;
    private float[] mOrientationM = new float[16];

    private String debugFPSStr;
    private String debugRenderStr;

    private OnDebugInterface onDebugInterface;


    private long[] frameList = new long[100];
    private long lastFrameTime;

    {
        Arrays.fill(frameList, -1);
    }

    private int frameIndex = 0;

    private long[] renderDuringList = new long[100];

    {
        Arrays.fill(renderDuringList, -1);
    }

    private int renderDuringIndex = 0;


    public CameraRenderView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CameraRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(CameraRenderView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        updateTexture = true;
        requestRender();
    }

    @Override
    public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
        polarrRender.initRender(getResources(), DESIGN_WIDTH, DESIGN_HEIGHT, false, PolarrRender.EXTERNAL_OES);
        polarrRender.enableRealTimeAutoEnhancement(true);
    }

    @Override
    public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
        // force set the surface size
        width = DESIGN_WIDTH;
        height = DESIGN_HEIGHT;
        mWidth = width;
        mHeight = height;

        long startTime = System.currentTimeMillis();
        // update if size is changed
//        polarrRender.updateSize(mWidth, mHeight);
        Log.d("updateSize", (System.currentTimeMillis() - startTime) + "ms");
        Log.d("ver", PolarrRender.Version());
        //generate camera texture------------------------
        mCameraTexture.init();
        polarrRender.setInputTexture(mCameraTexture.getTextureId());

        //set up surfacetexture------------------
        SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
        mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        if (oldSurfaceTexture != null) {
            oldSurfaceTexture.release();
        }

        //set camera para-----------------------------------
        int camera_width = 0;
        int camera_height = 0;

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        mCamera = Camera.open();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        if (psize.size() > 0) {
            int i;
            for (i = 0; i < psize.size(); i++) {
                if (psize.get(i).width < width || psize.get(i).height < height)
                    break;
            }
            if (i > 0)
                i--;
            param.setPreviewSize(psize.get(i).width, psize.get(i).height);

            camera_width = psize.get(i).width;
            camera_height = psize.get(i).height;

        }

        //get the camera orientation and display dimension------------
        if (mContext.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            Matrix.setRotateM(mOrientationM, 0, 90.0f, 0f, 0f, 1f);
        } else {
            Matrix.setRotateM(mOrientationM, 0, 0.0f, 0f, 0f, 1f);
        }
        Matrix.scaleM(mOrientationM, 0, 1, -1, 1);
        param.setPictureFormat(PixelFormat.JPEG);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        //start camera-----------------------------------------
        mCamera.setParameters(param);
        mCamera.startPreview();

        //start render---------------------
        requestRender();
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        if (lastFrameTime != 0) {
            frameList[frameIndex++] = (System.currentTimeMillis() - lastFrameTime);
            frameIndex %= frameList.length;
        }
        lastFrameTime = System.currentTimeMillis();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (mSurfaceTexture == null) {
            return;
        }
        //render the texture to FBO if new frame is available
        if (updateTexture) {
            mSurfaceTexture.updateTexImage();
            updateTexture = false;
        }

        long startTime = System.currentTimeMillis();
        polarrRender.updateInputTexture();
        polarrRender.drawFrame();

        GLES20.glViewport(0, 0, mWidth, mHeight);
        // demo draw screen
        Basic filter = Basic.getInstance(getResources());
        filter.setInputTextureId(polarrRender.getOutputId());
        filter.setNeedClear(false);

        // update the matrix for camera orientation
        Matrix.setIdentityM(filter.getMatrix(), 0);
        Matrix.multiplyMM(filter.getMatrix(), 0, filter.getMatrix(), 0, mOrientationM, 0);

        filter.draw();

        GLES20.glFinish();

        renderDuringList[renderDuringIndex++] = (System.currentTimeMillis() - startTime);
        renderDuringIndex %= renderDuringList.length;

        if (frameIndex % 60 == 0) {
            printDuring();
        }
    }

    public void autoEnhancement(final boolean isEnable) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.fastAutoEnhancement(isEnable);
            }
        });
    }

    private void printDuring() {
        int total = 0;
        for (long during : frameList) {
            if (during == -1) {
                return;
            }
            total += during;
        }
        total /= frameList.length;
        debugFPSStr = String.format(Locale.ENGLISH, "FRAME: %d ms. %.2f fps", total, 1000f / total);
        Log.d("Frame_TIME", debugFPSStr);

        total = 0;
        for (long during : renderDuringList) {
            if (during == -1) {
                return;
            }
            total += during;
        }
        total /= renderDuringList.length;
        debugRenderStr = String.format(Locale.ENGLISH, "RENDER: %d ms. %.2f fps", total, 1000f / total);
        Log.d("RENDER_TIME", debugRenderStr);

        if (onDebugInterface != null) {
            onDebugInterface.onDebugLog(debugFPSStr + "\n" + debugRenderStr);
        }
    }

    public void onDestroy() {
        updateTexture = false;
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }

        mCamera = null;
    }

    public void setOnDebugInterface(OnDebugInterface onDebugInterface) {
        this.onDebugInterface = onDebugInterface;
    }

    public void takePhoto(final OnCaptureCallback onCaptureCallback) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(readTexture(polarrRender.getOutputId(), mWidth, mHeight));

                onCaptureCallback.onPhoto(bitmap);
            }
        });
    }

    private static ByteBuffer readTexture(int texId, int width, int height) {
        int channels = 4;
        ByteBuffer ib = ByteBuffer.allocate(width * height * channels);
        int[] fFrame = new int[1];
        GLES20.glGenFramebuffers(1, fFrame, 0);
        bindFrameTexture(fFrame[0], texId);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        unBindFrameBuffer();
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        return ib;
    }

    private static void bindFrameTexture(int frameBufferId, int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    private static void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public interface OnCaptureCallback {
        void onPhoto(Bitmap bitmap);
    }

    public interface OnDebugInterface {
        void onDebugLog(String logStr);
    }
}