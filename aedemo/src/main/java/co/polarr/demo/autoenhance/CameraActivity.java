package co.polarr.demo.autoenhance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;


    private CameraRenderView mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);
        final TextView debug_tv = findViewById(R.id.debug_tv);
        mRenderer = findViewById(R.id.renderer_view);

        if (!checkAndRequirePermission(REQUEST_CAMERA_PERMISSION)) {
            mRenderer.setVisibility(View.INVISIBLE);
        }

//        findViewById(R.id.btn_photo).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (checkAndRequirePermission(REQUEST_STORAGE_PERMISSION)) {
//                    takePhoto();
//                }
//            }
//        });

        mRenderer.setOnDebugInterface(new CameraRenderView.OnDebugInterface() {
            @Override
            public void onDebugLog(final String logStr) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        debug_tv.setText(logStr);
                    }
                });
            }
        });
    }

    public void onBtnClick(View view) {
        switch (view.getId()) {
            case R.id.ae_tb:
                mRenderer.autoEnhancement(((ToggleButton) view).isChecked());
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onPause() {
        super.onPause();
        mRenderer.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

    private void takePhoto() {
        final File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "polarr_demo");
        if (!folder.exists()) {
            folder.mkdir();
        }

        mRenderer.takePhoto(new CameraRenderView.OnCaptureCallback() {
            @Override
            public void onPhoto(Bitmap bitmap) {
                String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                final File outputFile = new File(folder.getPath(), fileName + "_" + "EXPORT" + ".jpg");
                try {
                    OutputStream fileOS = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOS);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Rescanning the icon_library/gallery so it catches up with our own changes
                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            mediaScanIntent.setData(Uri.fromFile(outputFile));
                            sendBroadcast(mediaScanIntent);

                            Toast.makeText(CameraActivity.this, "Saved:" + outputFile.getPath(), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                bitmap.recycle();
            }
        });
    }

    private boolean checkAndRequirePermission(int permissionRequestId) {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        switch (permissionRequestId) {
            case REQUEST_CAMERA_PERMISSION:
                permission = Manifest.permission.CAMERA;
                break;
            case REQUEST_STORAGE_PERMISSION:
                permission = Manifest.permission.READ_EXTERNAL_STORAGE;
                break;
        }
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    permissionRequestId);

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mRenderer.setVisibility(View.VISIBLE);
            }
        } else if ((requestCode == REQUEST_STORAGE_PERMISSION) && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        }
    }

}