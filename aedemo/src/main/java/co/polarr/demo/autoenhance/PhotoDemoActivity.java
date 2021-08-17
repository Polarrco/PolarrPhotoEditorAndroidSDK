package co.polarr.demo.autoenhance;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.Locale;

import co.polarr.demo.autoenhance.utils.EGLHelper;
import co.polarr.renderer.PolarrRender;

public class PhotoDemoActivity extends AppCompatActivity {
    private static final int DESIGN_WIDTH = 1080;
    private static final int DESIGN_HEIGHT = 1920;
    private static final int REQUEST_IMPORT_PHOTO = 1;

    private PolarrRender render = null;
    private EGLHelper eglHelper;
    private Handler renderHandler;

    private ImageView src_iv;
    private ImageView target_iv;
    private Bitmap input;
    private boolean keepOutput = false;
    private TextView debug_tv;

    private static final int[] demoRids = {R.mipmap.demo, R.mipmap.demo2};
    private int currentDemoIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        src_iv = findViewById(R.id.src_iv);
        target_iv = findViewById(R.id.target_iv);
        debug_tv = findViewById(R.id.debug_tv);

        initPolarrRender();
    }

    @Override
    protected void onDestroy() {
        releasePolarrRender();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        keepOutput = false;
        ((ToggleButton) findViewById(R.id.autoenhance_tb)).setChecked(false);
        super.onPause();
    }

    private void initPolarrRender() {
        HandlerThread renderThread = new HandlerThread("PolarrRenderThread");
        renderThread.start();
        renderHandler = new Handler(renderThread.getLooper());

        renderHandler.post(new Runnable() {
            @Override
            public void run() {
                eglHelper = new EGLHelper();
                eglHelper.eglInit();

                render = new PolarrRender();
                render.initRender(getResources(), DESIGN_WIDTH, DESIGN_HEIGHT, false, PolarrRender.TEXTURE_2D);
                render.enableRealTimeAutoEnhancement(true);
                render.createInputTexture();

            }
        });
    }

    private void importBm(final Bitmap input) {
        renderHandler.post(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, render.getTextureId());
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, input, 0);
                render.updateInputTexture();
                Log.d("BENCHMARK", "input: " + (System.currentTimeMillis() - start) + "ms");
            }
        });
    }

    private void renderOutput() {
        renderHandler.post(new Runnable() {
            @Override
            public void run() {
                if (input == null) {
                    return;
                }
                while (keepOutput) {
                    long start = System.currentTimeMillis();
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, render.getTextureId());
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, input, 0);
                    Log.d("BENCHMARK", "input: " + (System.currentTimeMillis() - start) + "ms");
                    start = System.currentTimeMillis();
                    render.updateInputTexture();
                    Log.d("BENCHMARK", "update input: " + (System.currentTimeMillis() - start) + "ms");

                    start = System.currentTimeMillis();
                    render.drawFrame();
                    GLES20.glFinish();
                    final long renderTime = (System.currentTimeMillis() - start);
                    Log.d("BENCHMARK", "draw frame: " + renderTime + "ms");

                    start = System.currentTimeMillis();
                    final Bitmap bitmap = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(EGLHelper.readTexture(render.getOutputId(), input.getWidth(), input.getHeight()));
                    Log.d("BENCHMARK", "output: " + (System.currentTimeMillis() - start) + "ms");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            target_iv.setImageBitmap(bitmap);
                            debug_tv.setText(String.format(Locale.ENGLISH, "Render fps:%.2f", 1000f / renderTime));
                        }
                    });
                }
            }
        });
    }

    private void releasePolarrRender() {
        renderHandler.post(new Runnable() {
            @Override
            public void run() {
                render.release();
                eglHelper.destroy();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_IMPORT_PHOTO == requestCode) {
            if (data != null) {
                Uri uri = data.getData();
                Bitmap imageBm = decodeBitmapFromUri(PhotoDemoActivity.this, uri);
                if (imageBm != null) {
                    input = imageBm;

                    importBm(input);
                    src_iv.setImageBitmap(input);
                }
            }
        }
    }

    private static Bitmap decodeBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap decodedBm = BitmapFactory.decodeStream(inputStream);
            Bitmap formatedBm = Bitmap.createScaledBitmap(decodedBm, DESIGN_WIDTH, DESIGN_HEIGHT, false);
            if (decodedBm != formatedBm) {
                decodedBm.recycle();
            }

            return formatedBm;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onBtnClick(View view) {
        switch (view.getId()) {
            case R.id.import_btn:
                int rid = demoRids[currentDemoIndex++];
                currentDemoIndex %= demoRids.length;

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), rid);
                input = Bitmap.createScaledBitmap(bitmap, DESIGN_WIDTH, DESIGN_HEIGHT, false);
                if (bitmap != input) {
                    bitmap.recycle();
                }
                importBm(input);
                src_iv.setImageBitmap(input);

                break;
            case R.id.import_album_btn:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMPORT_PHOTO);
                break;
            case R.id.autoenhance_tb: {
                boolean isEnable = ((ToggleButton) view).isChecked();
                if (!keepOutput) {
                    keepOutput = isEnable;
                    renderOutput();
                } else {
                    keepOutput = isEnable;
                }

                break;
            }
            case R.id.ae_tb: {
                final boolean isEnable = ((ToggleButton) view).isChecked();

                final boolean needRestart = keepOutput;
                if (needRestart) {
                    keepOutput = false;
                }
                renderHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        render.fastAutoEnhancement(isEnable);
                        if (needRestart) {
                            keepOutput = true;
                            renderOutput();
                        }
                    }
                });
                break;
            }
        }
    }
}
