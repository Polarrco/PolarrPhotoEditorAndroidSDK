package co.polarr.demo.autoenhance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBtnClick(View view) {
        switch (view.getId()) {
            case R.id.camera_btn:
                startActivity(new Intent(this, CameraActivity.class));

                break;
            case R.id.photo_btn: {
                startActivity(new Intent(this, PhotoDemoActivity.class));

                break;
            }
        }
    }
}
