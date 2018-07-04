package co.polarr.demo.autoenhance.utils;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class EGLHelper {

    public EGL10 mEgl;
    public EGLDisplay mEglDisplay;
    public EGLConfig mEglConfig;
    public EGLSurface mEglSurface;
    public EGLContext mEglContext;
    public GL10 mGL;

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public static final int SURFACE_PBUFFER = 1;
    public static final int SURFACE_PIM = 2;
    public static final int SURFACE_WINDOW = 3;

    private int surfaceType = SURFACE_PBUFFER;
    private Object surface_native_obj;

    private int red = 8;
    private int green = 8;
    private int blue = 8;
    private int alpha = 8;
    private int depth = 0;
    private int renderType = 4;
    private int bufferType = EGL10.EGL_SINGLE_BUFFER;
    private EGLContext shareContext = EGL10.EGL_NO_CONTEXT;


    public void config(int red, int green, int blue, int alpha, int depth, int renderType) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.depth = depth;
        this.renderType = renderType;
    }

    public void setSurfaceType(int type, Object... obj) {
        this.surfaceType = type;
        if (obj != null) {
            this.surface_native_obj = obj[0];
        }
    }

    public int eglInit() {
        return eglInit(100, 100);
    }

    public int eglInit(int width, int height) {
        int[] attributes = new int[]{
                EGL10.EGL_RED_SIZE, red,
                EGL10.EGL_GREEN_SIZE, green,
                EGL10.EGL_BLUE_SIZE, blue,
                EGL10.EGL_ALPHA_SIZE, alpha,
                EGL10.EGL_DEPTH_SIZE, depth,
                EGL10.EGL_RENDERABLE_TYPE, renderType,
                EGL10.EGL_NONE};

        mEgl = (EGL10) EGLContext.getEGL();
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        int[] version = new int[2];
        mEgl.eglInitialize(mEglDisplay, version);

        int[] configNum = new int[1];
        mEgl.eglChooseConfig(mEglDisplay, attributes, null, 0, configNum);
        if (configNum[0] == 0) {
            return -1;
        }
        EGLConfig[] c = new EGLConfig[configNum[0]];
        mEgl.eglChooseConfig(mEglDisplay, attributes, c, configNum[0], configNum);
        mEglConfig = c[0];
        int[] surAttr = new int[]{
                EGL10.EGL_WIDTH, width,
                EGL10.EGL_HEIGHT, height,
                EGL10.EGL_NONE
        };
        mEglSurface = createSurface(surAttr);
        int[] contextAttr = new int[]{
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, shareContext, contextAttr);
        makeCurrent();

        return mEgl.eglGetError();
    }

    public void makeCurrent() {
        mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
        mGL = (GL10) mEglContext.getGL();
    }

    public void destroy() {
        mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
        mEgl.eglDestroyContext(mEglDisplay, mEglContext);
        mEgl.eglTerminate(mEglDisplay);
    }

    private EGLSurface createSurface(int[] attr) {
        switch (surfaceType) {
            case SURFACE_WINDOW:
                return mEgl.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface_native_obj, attr);
            case SURFACE_PIM:
                return mEgl.eglCreatePixmapSurface(mEglDisplay, mEglConfig, surface_native_obj, attr);
            default:
                return mEgl.eglCreatePbufferSurface(mEglDisplay, mEglConfig, attr);
        }
    }

    public void reCreateSurface(int width, int height) {
        mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        mEgl.eglDestroySurface(mEglDisplay, mEglSurface);


        int[] surAttr = new int[]{
                EGL10.EGL_WIDTH, width,
                EGL10.EGL_HEIGHT, height,
                EGL10.EGL_NONE
        };
        mEglSurface = createSurface(surAttr);

        makeCurrent();
    }

    public static ByteBuffer readTexture(int texId, int width, int height) {
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
}
