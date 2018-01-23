package co.polarr.polarrrenderdemo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.polarr.qrcode.QRUtils;
import co.polarr.renderer.FilterPackageUtil;
import co.polarr.renderer.PolarrRender;
import co.polarr.renderer.PolarrRenderThread;
import co.polarr.renderer.entities.Adjustment;
import co.polarr.renderer.entities.BrushItem;
import co.polarr.renderer.entities.FilterItem;
import co.polarr.renderer.entities.FilterPackage;
import co.polarr.renderer.entities.MagicEraserHistoryItem;
import co.polarr.renderer.entities.MagicEraserPath;
import co.polarr.renderer.utils.QRCodeUtil;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMPORT_PHOTO = 1;
    private static final int REQUEST_IMPORT_QR_PHOTO = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int ACTIVITY_RESULT_QR_SCANNER = 3;

    //    private static final int POINT_BRUSH_MOSIC = 1;
//    private static final int POINT_BRUSH_BLUR = 2;
    private static final int POINT_BRUSH_PAINT = 3;
    private static final int POINT_MAGIC_ERASER = 4;

    private static final int TOUCH_FPS = 30;
    private String brushType;
    private int currentPointState;
    private List<PointF> currentPoints;

    private BrushItem paintBrushItem;
    private int paintState = 0; // 0:idle, 1:paint, 3:mageic eraser

    private AppCompatSeekBar seekbar;
    private TextView labelTv;

    /**
     * Render View
     */
    private DemoView renderView;
    private RelativeLayout renderRl;
    /**
     * adjustment container
     */
    private View sliderCon;
    /**
     * save adjustment values
     */
    private Map<String, Object> localStateMap = new HashMap<>();
    private Map<String, Object> faceStates = new HashMap<>();
    private FilterItem mCurrentFilter;

    private List<FilterItem> mFilters;
    private PolarrRenderThread polarrRenderThread;
    private long lasUpdateTime;
    private Adjustment currentMask;
    //    private BrushItem currentBrushItem;
    private int inputWidth;
    private int inputHeight;
    private List<MagicEraserHistoryItem> historyItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init render view
        renderRl = (RelativeLayout) findViewById(R.id.render_rl);
        renderView = (DemoView) findViewById(R.id.render_view);
        renderView.setAlpha(0);

        sliderCon = findViewById(R.id.slider);
        sliderCon.setVisibility(View.INVISIBLE);

        labelTv = (TextView) findViewById(R.id.label_tv);
        seekbar = (AppCompatSeekBar) findViewById(R.id.seekbar);

        polarrRenderThread = new PolarrRenderThread(getResources());
        polarrRenderThread.start();


        currentPointState = 0;
        currentPoints = new ArrayList<>();
        renderView.setClickable(true);
        renderView.setOnTouchListener(demoViewTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        hideAll();
        switch (item.getItemId()) {
            case R.id.navigation_import_image:
                importImage();
                break;
            case R.id.navigation_import_demo:
                importImageDemo();
                break;
            case R.id.navigation_reset:
                // for benchmark memories
                /*
                for(int i = 0; i < 10; i ++) {
                    renderView.releaseRender();
                    renderView.postInit();
                    importImageDemo();
                }
                */
                reset();
                break;
            case R.id.navigation_qr_scan:
                showQRScan();
                break;
            case R.id.navigation_import_qr:
                importQrImage();
                break;
            case R.id.navigation_bitmap:
                final ImageView demoIV = (ImageView) findViewById(R.id.demo_iv);
                demoIV.setImageBitmap(null);
//                if (!checkAndRequirePermission(0)) {
//                    break;
//                }
                if (mFilters == null) {
                    List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
                    mFilters = new ArrayList<>();
                    for (FilterPackage filterPackage : packages) {
                        mFilters.addAll(filterPackage.filters);
                    }
                }

                Bitmap imageBm = BitmapFactory.decodeResource(getResources(), R.mipmap.demo_1);
                float targetWidth = 1080;
                float scale = targetWidth / imageBm.getWidth();

                final Bitmap bmp = Bitmap.createScaledBitmap(imageBm, (int) (imageBm.getWidth() * scale), (int) (imageBm.getHeight() * scale), true);
                imageBm.recycle();

                List<String> filterIds = new ArrayList<>();
                for (int i = 0; i < mFilters.size(); i++) {
                    filterIds.add(mFilters.get(i).id);
                }
//                filterIds.add(mFilters.get(5).id);

                List<Map<String, Object>> filterStates = new ArrayList<>();
//                for (int i = 0; i < mFilters.size(); i++) {
//                    filterIds.add(mFilters.get(i).id);
//                }
                filterStates.add(mFilters.get(5).state);

                BenchmarkUtil.TimeStart("renderBitmap");

            {
                List<Bitmap> bitmapList = PolarrRender.renderBitmaps(getResources(), bmp, filterIds);
                bmp.recycle();

                for (int i = 0; i < bitmapList.size(); i++) {
                    if (i != bitmapList.size() - 1) {
                        bitmapList.get(i).recycle();
                    }
                }

                final Bitmap displayBitmap = bitmapList.get(bitmapList.size() - 1);

                demoIV.setImageBitmap(displayBitmap);
            }


//            polarrRenderThread.renderBitmap(bmp, filterIds, new RenderCallback() {
//                @Override
//                public void onRenderBitmap(List<Bitmap> bitmapList) {
//                    BenchmarkUtil.TimeEnd("renderBitmap");
//                    bmp.recycle();
//
////                        for (int i = 0; i < bitmapList.size(); i++) {
////                            File storageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PolarrDebug");
////                            if (!storageDirectory.exists()) {
////                                storageDirectory.mkdir();
////                            }
////                            File outFile = new File(storageDirectory, "g_" + loopCount + "_filter_" + (i + 1) + ".jpg");
////                            if (outFile.exists()) {
////                                outFile.delete();
////                            }
////                            try {
////                                outFile.createNewFile();
////                                FileOutputStream filterOutputStream = new FileOutputStream(outFile);
////                                bitmapList.get(i).compress(Bitmap.CompressFormat.JPEG, 90, filterOutputStream);
////
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }
////                        }
//
//                    for (int i = 0; i < bitmapList.size(); i++) {
//                        if (i != bitmapList.size() - 1) {
//                            bitmapList.get(i).recycle();
//                        }
//                    }
//
//                    final Bitmap displayBitmap = bitmapList.get(bitmapList.size() - 1);
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            demoIV.setImageBitmap(displayBitmap);
//                        }
//                    });
//
//                    loopCount++;
//                }
//            });

            break;
        }

        return true;
    }

    private void startBrush(int brushState) {
        currentPointState = brushState;
        currentPoints.clear();
        updateBrush(null);
        if (currentPointState == POINT_BRUSH_PAINT) {
            setBrushPaint(brushType);
        }
    }

    private void endTouch() {
        if (currentPointState == POINT_MAGIC_ERASER) {
            magicEraserPath(null);
            magicErase(currentPoints);
        }

        currentPoints.clear();
        if (currentPointState == POINT_BRUSH_PAINT) {
            if (paintState == 1) {
                paintState = 0;
                paintBrushItem = null;

                List<PointF> points = new ArrayList<>();
                points.addAll(currentPoints);
                currentPoints.clear();
                renderView.brushAddPoints(points);
                renderView.brushFinish();

                // no need call
//                int lastBrushId = renderView.getBrushTexture();
//                renderView.setBrushTexture(lastBrushId);
            }
        }
    }

    private void stopBrush() {
        currentPointState = 0;
    }

    private void updateBrush(PointF point) {
        switch (currentPointState) {
            case POINT_BRUSH_PAINT:
                if (point != null) {
                    if (paintState == 0) {
                        setBrushPaint(brushType);
                    }
                }
                break;
            case POINT_MAGIC_ERASER:
                magicEraserPath(currentPoints);
                break;
        }
        lazyUpdate(TOUCH_FPS);
    }

    private void lazyUpdate(int fps) {
        long time = System.currentTimeMillis();
        if (time - lasUpdateTime > (1000f / fps)) {
            if (currentPointState == POINT_BRUSH_PAINT) {
                if (paintBrushItem != null) {
                    List<PointF> points = new ArrayList<>();
                    points.addAll(currentPoints);
                    currentPoints.clear();
                    renderView.brushAddPoints(points);
                }
            } else {
                renderView.updateStates(localStateMap);
            }

            lasUpdateTime = System.currentTimeMillis();
        }
    }

    private View.OnTouchListener demoViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (currentPointState <= 0) {
                return false;
            } else {
                PointF touchPoint = new PointF(event.getX() / v.getWidth(), event.getY() / v.getHeight());
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        currentPoints.add(touchPoint);
                        updateBrush(touchPoint);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        endTouch();
                        break;
                }
                return true;
            }
        }
    };

    public void btnClicked(View view) {
        hideAll();
        switch (view.getId()) {
            case R.id.tv_desc:
                importImageDemo();
                break;
            case R.id.btn_addjustment:
                showList();
                break;
            case R.id.btn_auto: {
                sliderCon.setVisibility(View.VISIBLE);
                final String label = "Auto enhance";
                renderView.autoEnhance(localStateMap, 0.5f);
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;
                        renderView.autoEnhance(localStateMap, adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(50);
            }

            break;
            case R.id.btn_auto_face:
                renderView.autoEnhanceFace0(localStateMap);
                break;
            case R.id.btn_auto_all: {
                sliderCon.setVisibility(View.VISIBLE);
                final String label = "Auto enhance";
                renderView.autoEnhanceAll(localStateMap, 0.5f);
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;
                        renderView.autoEnhanceAll(localStateMap, adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(50);
            }

            break;
            case R.id.btn_add_radial:
                setRadialMask();
                break;
            case R.id.btn_add_gradient:
                setGradientMask();
                break;
            case R.id.btn_paint_brush: {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                final CharSequence items[] = {
                        "meitu_1", "meitu_2", "meitu_3", "meitu_4", "eraser"
                };
                adb.setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        switch (n) {
                            case 0:
                                brushType = "stroke_5";
                                startBrush(POINT_BRUSH_PAINT);
                                break;
                            case 1:
                                brushType = "mosaic";
                                startBrush(POINT_BRUSH_PAINT);

                                break;
                            case 2:
                                brushType = "blur";
                                startBrush(POINT_BRUSH_PAINT);

                                break;
                            case 3:
                                brushType = "stroke_6";
                                startBrush(POINT_BRUSH_PAINT);
                                break;
                            case 4:
                                brushType = "eraser";
                                startBrush(POINT_BRUSH_PAINT);
                                break;
                        }
                        renderView.setPaintMode(true);
                        dialog.dismiss();
                    }

                });
                adb.setNegativeButton("Cancel", null);
                adb.setTitle("Choose a paint:");
                adb.show();
            }

            break;
            case R.id.btn_filters:
                showFilters();
                break;
            case R.id.btn_eraser: {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                final CharSequence items[] = {
                        "init", "add path", "undo", "redo", "reset", "histories"
                };
                adb.setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        switch (n) {
                            case 0:
                                renderView.initMagicEraser();
                                break;
                            case 1:
                                currentPointState = POINT_MAGIC_ERASER;
                                break;
                            case 2:
                                renderView.undoMagicEraser();
                                break;
                            case 3:
                                renderView.redoMagicEraser();
                                break;
                            case 4:
                                renderView.resetMagicEraser();
                                break;
                            case 5:
                                for (MagicEraserHistoryItem historyItem : historyItems) {
                                    renderView.renderMagicEraserHistory(historyItem);
                                }
                                break;
                        }
                        dialog.dismiss();
                    }

                });
                adb.setNegativeButton("Cancel", null);
                adb.setTitle("Choose a function of magic eraser:");
                adb.show();
            }
            break;
        }
    }

    @Override
    protected void onDestroy() {
        polarrRenderThread.interrupt();
        renderView.releaseRender();
        super.onDestroy();
    }

    private boolean checkAndRequirePermission(int permissionRequestId) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    permissionRequestId);

            return false;
        }

        return true;
    }

    private void importImageDemo() {
        final Bitmap imageBm = scaledBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.demo_1), renderRl.getWidth(), renderRl.getHeight());
//        final Bitmap imageBm = BitmapFactory.decodeResource(getResources(), R.mipmap.demo_large);
        inputWidth = imageBm.getWidth();
        inputHeight = imageBm.getHeight();
        new Thread() {
            @Override
            public void run() {
                float perfectSize = 500f;
                float minScale = Math.min(perfectSize / imageBm.getWidth(), perfectSize / imageBm.getHeight());
                minScale = Math.min(minScale, 1f);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBm, (int) (minScale * imageBm.getWidth()), (int) (minScale * imageBm.getHeight()), true);
                FaceUtil.InitFaceUtil(MainActivity.this);
                Map<String, Object> faces = FaceUtil.DetectFace(scaledBitmap);
                FaceUtil.Release();
                scaledBitmap.recycle();

                faceStates = faces;
                localStateMap.putAll(faceStates);

                renderView.updateStates(localStateMap);
            }
        }.start();

        renderView.importImage(imageBm);
        renderView.setAlpha(1);
        updateRenderLayout(imageBm.getWidth(), imageBm.getHeight());
    }

    private void importImage() {
        findViewById(R.id.tv_desc).setVisibility(View.GONE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_PHOTO);
    }

    private void importQrImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_QR_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_IMPORT_PHOTO == requestCode) {
            if (data != null) {
                final Uri uri = data.getData();
                renderView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap imageBm = scaledBitmap(decodeBitmapFromUri(MainActivity.this, uri), renderRl.getWidth(), renderRl.getHeight());
//                        final Bitmap imageBm = decodeBitmapFromUri(MainActivity.this, uri);
                        inputWidth = imageBm.getWidth();
                        inputHeight = imageBm.getHeight();
                        new Thread() {
                            @Override
                            public void run() {
                                float perfectSize = 500f;
                                float minScale = Math.min(perfectSize / imageBm.getWidth(), perfectSize / imageBm.getHeight());
                                minScale = Math.min(minScale, 1f);
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBm, (int) (minScale * imageBm.getWidth()), (int) (minScale * imageBm.getHeight()), true);
                                FaceUtil.InitFaceUtil(MainActivity.this);
                                Map<String, Object> faces = FaceUtil.DetectFace(scaledBitmap);
                                FaceUtil.Release();
                                scaledBitmap.recycle();

                                faceStates = faces;
                                localStateMap.putAll(faceStates);

                                renderView.updateStates(localStateMap);
                            }
                        }.start();

                        renderView.importImage(imageBm);
                        renderView.setAlpha(1);

                        updateRenderLayout(imageBm.getWidth(), imageBm.getHeight());
                    }
                }, 1000);
            }
        } else if (REQUEST_IMPORT_QR_PHOTO == requestCode && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                File file = new File(uri.toString());


                final String qrCodeData = QRUtils.decodeImageQRCode(this, file.getPath());
                if (qrCodeData != null) {

                    new Thread() {
                        @Override
                        public void run() {
                            String statesString = QRCodeUtil.requestQRJson(qrCodeData);
                            updateQrStates(statesString);
                        }
                    }.start();
                }
            }
        } else if (ACTIVITY_RESULT_QR_SCANNER == requestCode && resultCode == RESULT_OK) {
            if (data == null || data.getStringExtra("value") == null) {
                return;
            }
            final String urlString = data.getStringExtra("value");
            new Thread() {
                @Override
                public void run() {
                    String statesString = QRCodeUtil.requestQRJson(urlString);
                    updateQrStates(statesString);
                }
            }.start();
        }
    }

    private void updateRenderLayout(int width, int height) {
        int viewWidth = renderRl.getWidth();
        int viewHeight = renderRl.getHeight();

        float scale = Math.min((float) viewWidth / width, (float) viewHeight / height);
        width *= scale;
        height *= scale;

        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) renderView.getLayoutParams();
        rlp.width = width;
        rlp.height = height;
        renderView.setLayoutParams(rlp);
    }

    private static Bitmap decodeBitmapFromUri(Context context, Uri uri) { //, int viewWidth, int viewHight
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap decodedBm = BitmapFactory.decodeStream(inputStream);
            Bitmap formatedBm = decodedBm.copy(Bitmap.Config.ARGB_8888, false);
            decodedBm.recycle();

            return formatedBm;//scaledBitmap(formatedBm, viewWidth, viewHight);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap scaledBitmap(Bitmap formatedBm, int width, int height) {
        int w = formatedBm.getWidth();
        int h = formatedBm.getHeight();
        Matrix matrix = new Matrix();
        float scalew = (float) width / w;
        float scaleh = (float) height / h;
        float scale = scalew < scaleh ? scalew : scaleh;
        matrix.postScale(scale, scale);
        Bitmap bmp = Bitmap.createBitmap(formatedBm, 0, 0, w, h, matrix, true);
        if (!formatedBm.equals(bmp) && !formatedBm.isRecycled()) {
            formatedBm.recycle();
        }

        return bmp;
    }

    private void showQRScan() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivityForResult(intent, ACTIVITY_RESULT_QR_SCANNER);
    }

    private void updateQrStates(String statesString) {
        // reset to default
        renderView.updateStatesWithJson(statesString);
    }

    private void reset() {
        localMasks.clear();
        localStateMap.clear();
        localStateMap.putAll(faceStates);
        FaceUtil.ResetFaceStates(faceStates);

        renderView.updateStates(localStateMap);
    }

    @Override
    public void finish() {
        releaseRender();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.finish();
    }

    private void releaseRender() {
        renderView.releaseRender();
    }

    private void magicErase(List<PointF> points) {
        MagicEraserPath path = new MagicEraserPath();
        path.points = new ArrayList<>();
        path.points.addAll(points);
        path.radius = 0.031f * inputWidth;

        renderView.renderMagicEraser(path, historyItems);
    }

    private void magicEraserPath(List<PointF> points) {
        if (points != null) {
            MagicEraserPath path = new MagicEraserPath();
            path.points = new ArrayList<>();
            path.points.addAll(points);
            path.radius = 0.031f * inputWidth;

            renderView.renderMagicEraserPathOverlay(path);
        } else {
            renderView.renderMagicEraserPathOverlay(null);
        }
    }

    private void setRadialMask() {
        Adjustment radialMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = radialMask.adjustments;

        maskAdjustment.blur = 0.5f; // (0f, +1.5f)
        maskAdjustment.exposure = 0.5f; // (-1f, +1f)
        maskAdjustment.gamma = 0; // (-1f, +1f)
        maskAdjustment.temperature = 0.5f; // (-1f, +1f)
        maskAdjustment.tint = 0; // (-1f, +1f)
        maskAdjustment.saturation = 0; // (-1f, +1f)
        maskAdjustment.vibrance = 0; // (-1f, +1f)
        maskAdjustment.contrast = 0.3f; // (-1f, +1f)
        maskAdjustment.highlights = 0; // (-1f, +1f)
        maskAdjustment.shadows = -0.8f; // (-1f, +1f)
        maskAdjustment.clarity = 1f; // (-1f, +1f)
        maskAdjustment.mosaic_size = 0.2f; // (0, +1f)
        maskAdjustment.mosaic_pattern = "square";// "square","hexagon","dot","triangle","diamond",
        maskAdjustment.shadows_hue = 0; // (0, +1f)
        maskAdjustment.shadows_saturation = 0; // (0, +1f)
        maskAdjustment.dehaze = -0.2f; // (-1f, +1f)

        radialMask.type = "radial";
        radialMask.position = new float[]{0f, 0f}; // (-0.5f, +0.5f) from center of photo
        radialMask.size = new float[]{0.608f, 0.45f}; // (0f, +1f) width, height
        radialMask.feather = 0.1f;  // (0, +1f)
        radialMask.invert = true;

        radialMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(radialMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateStates(localStateMap);
    }

    private void setGradientMask() {
        Adjustment gradientMask = new Adjustment();
        co.polarr.renderer.entities.Context.LocalState maskAdjustment = gradientMask.adjustments;

        maskAdjustment.blur = 0f; // (0f, +1.5f)
        maskAdjustment.exposure = -0.4f; // (-1f, +1f)
        maskAdjustment.gamma = 0; // (-1f, +1f)
        maskAdjustment.temperature = 0f; // (-1f, +1f)
        maskAdjustment.tint = 0; // (-1f, +1f)
        maskAdjustment.saturation = -1; // (-1f, +1f)
        maskAdjustment.vibrance = 0; // (-1f, +1f)
        maskAdjustment.contrast = -0.9f; // (-1f, +1f)
        maskAdjustment.highlights = 0; // (-1f, +1f)
        maskAdjustment.shadows = 0f; // (-1f, +1f)
        maskAdjustment.clarity = -1f; // (-1f, +1f)
        maskAdjustment.mosaic_size = 0f; // (0, +1f)
        maskAdjustment.shadows_hue = 0.6f; // (0, +1f)
        maskAdjustment.shadows_saturation = 0.5f; // (0, +1f)
        maskAdjustment.dehaze = 0f; // (-1f, +1f)

        gradientMask.type = "gradient";
        gradientMask.startPoint = new float[]{0.12f, -0.36f}; // (-0.5f, +0.5f) from center
        gradientMask.endPoint = new float[]{-0.096f, 0.26f}; // (-0.5f, +0.5f) from center
        gradientMask.reflect = true;
        gradientMask.invert = false;

        gradientMask.disabled = false;

        List<Adjustment> localMasks = new ArrayList<>();
        localMasks.add(gradientMask);
        localStateMap.put("local_adjustments", localMasks);

        renderView.updateStates(localStateMap);
    }

    List<Adjustment> localMasks = new ArrayList<>();

    private void setBrushPaint(String paintType) {
        BrushItem brushItem = new BrushItem();

        if (paintType.equals("stroke_5")) {
            brushItem.flow = 0.90f;     // A:0.85 B:0.90 C:0.75
            brushItem.size = 0.40f;     // A:0.25 B:0.40 C:0.80
            brushItem.randomize = 0.8f; // A:0.80 B:0.80 C:0.80
            brushItem.spacing = 0.45f;  // A:0.85 B:0.45 C:0.45
            brushItem.hardness = 1f;
        } else if (paintType.equals("mosaic")) {
            brushItem.size = 0.25f;     // A:0.14 B:0.25 C:0.5
            brushItem.spacing = 0.5f;
            brushItem.flow = 1f;
            brushItem.randomize = 0f;
            brushItem.hardness = 0.5f;
        } else if (paintType.equals("blur")) {
            brushItem.size = 0.25f;     // A:0.14 B:0.25 C:0.5
            brushItem.spacing = 0.5f;
            brushItem.flow = 1f;
            brushItem.randomize = 0f;
            brushItem.hardness = 0.5f;

        } else if (paintType.equals("stroke_6")) {
            brushItem.flow = 0.70f;     // A:0.70 B:0.70 C:0.70
            brushItem.size = 0.40f;     // A:0.30 B:0.56 C:0.80
            brushItem.randomize = 0.8f; // A:0.50 B:0.50 C:0.50
            brushItem.spacing = 0.45f;  // A:0.85 B:0.45 C:0.45
            brushItem.hardness = 1f;
        } else {
            brushItem.size = 0.25f;     // A:0.14 B:0.25 C:0.5
            brushItem.spacing = 0.5f;
            brushItem.flow = 1f;
            brushItem.randomize = 0f;
            brushItem.hardness = 0.5f;
        }
        brushItem.texture = paintType; // "stroke_5","stroke_6","mosaic","blur"

        paintState = 1;
        paintBrushItem = brushItem;

        renderView.brushStart(paintBrushItem);
    }

    private void showFilters() {
        if (mFilters == null) {
            List<FilterPackage> packages = FilterPackageUtil.GetAllFilters(getResources());
            mFilters = new ArrayList<>();
            for (FilterPackage filterPackage : packages) {
                mFilters.addAll(filterPackage.filters);
            }
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[mFilters.size()];
        for (int i = 0; i < mFilters.size(); i++) {
            items[i] = mFilters.get(i).filterName("zh");
        }
        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                FilterItem filterItem = mFilters.get(n);
                mCurrentFilter = filterItem;

                localStateMap.clear();
                localStateMap.putAll(faceStates);

                renderView.updateStates(mCurrentFilter.state);

                final String label = "Filter:" + filterItem.filterName("zh");
                labelTv.setText(label);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f;

                        if (mCurrentFilter != null) {
                            Map<String, Object> interpolateStates = FilterPackageUtil.GetRefStates(mCurrentFilter.state, adjustmentValue);

                            localStateMap.clear();
                            localStateMap.putAll(faceStates);
                            localStateMap.putAll(interpolateStates);

                            renderView.updateStates(interpolateStates);
                        }

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                seekbar.setProgress(50);

                dialog.dismiss();
            }

        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Choose a filter:");
        adb.show();
    }

    private void showList() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final CharSequence items[] = new CharSequence[]{
                "exposure",
                "contrast",
                "saturation",
                "distortion_horizontal",
                "distortion_vertical",
                "fringing",
                "color_denoise",
                "luminance_denoise",
                "dehaze",
                "diffuse",
                "temperature",
                "tint",
                "gamma",
                "highlights",
                "shadows",
                "whites",
                "blacks",
                "clarity",
                "vibrance",
                "highlights_hue",
                "highlights_saturation",
                "shadows_hue",
                "shadows_saturation",
                "balance",
                "sharpen",
                "hue_red",
                "hue_orange",
                "hue_yellow",
                "hue_green",
                "hue_aqua",
                "hue_blue",
                "hue_purple",
                "hue_magenta",
                "saturation_red",
                "saturation_orange",
                "saturation_yellow",
                "saturation_green",
                "saturation_aqua",
                "saturation_blue",
                "saturation_purple",
                "saturation_magenta",
                "luminance_red",
                "luminance_orange",
                "luminance_yellow",
                "luminance_green",
                "luminance_aqua",
                "luminance_blue",
                "luminance_purple",
                "luminance_magenta",
                "grain_amount",
                "grain_size",
                "mosaic_square",
                "mosaic_hexagon",
                "mosaic_dot",
                "mosaic_triangle",
                "mosaic_diamond",
        };

        adb.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int n) {
                sliderCon.setVisibility(View.VISIBLE);
                CharSequence label = items[n];
                if (label.toString().startsWith("mosaic_")) {
                    String type = label.toString().substring("mosaic_".length());
                    localStateMap.put("mosaic_pattern", type);
                    renderView.updateStates(localStateMap);

                    label = "mosaic_size";
                    Toast.makeText(MainActivity.this, "Mosaic type: " + type + ", try to adjust 'mosaic_size'", Toast.LENGTH_LONG).show();
                }

                labelTv.setText(label);
                final CharSequence finalLabel = label;
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float adjustmentValue = (float) progress / 100f * 2f - 1f;
                        localStateMap.put(finalLabel.toString(), adjustmentValue);

                        labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", finalLabel, adjustmentValue));

                        renderView.updateStates(localStateMap);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                if (localStateMap.containsKey(label.toString())) {
                    float adjustmentValue = (float) localStateMap.get(label.toString());
                    seekbar.setProgress((int) ((adjustmentValue + 1) / 2 * 100));
                    labelTv.setText(String.format(Locale.ENGLISH, "%s: %.2f", label, adjustmentValue));
                } else {
                    seekbar.setProgress(50);
                }

                dialog.dismiss();
            }

        });
        adb.setNegativeButton("Cancel", null);
        adb.setTitle("Choose a type:");
        adb.show();
    }

    private void hideList() {
    }

    private void hideAll() {
        renderView.setPaintMode(false);
        currentMask = null;
        findViewById(R.id.tv_desc).setVisibility(View.GONE);
        stopBrush();
        sliderCon.setVisibility(View.INVISIBLE);

        hideList();
    }
}