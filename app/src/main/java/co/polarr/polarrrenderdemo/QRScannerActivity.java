package co.polarr.polarrrenderdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import co.polarr.qrcode.QRCodeView;
import co.polarr.qrcode.QRView;


public class QRScannerActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private QRView qrView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        qrView = (QRView) findViewById(R.id.qRView);
        qrView.setDelegate(new QRCodeView.Delegate() {
            @Override
            public void onScanQRCodeSuccess(String result) {
                Intent data = new Intent();
                data.putExtra("value", result);

                if (getParent() == null) {
                    setResult(Activity.RESULT_OK, data);
                } else {
                    getParent().setResult(Activity.RESULT_OK, data);
                }
                finish();
            }

            @Override
            public void onScanQRCodeOpenCameraError() {

            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);

            return;
        }


        startCamera();
    }

    private void startCamera() {
        qrView.startCamera();
        qrView.startSpotAndShowRect();
    }

    @Override
    protected void onDestroy() {
        qrView.stopSpotAndHiddenRect();

        qrView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();

                return;
            }
        }

        setResult(AppCompatActivity.RESULT_CANCELED);
        finish();
    }
}
