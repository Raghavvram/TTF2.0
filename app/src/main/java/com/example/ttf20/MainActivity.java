package com.example.ttf20;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final String TAG = "TextToFlash";
    private EditText inputText;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean stopTransmission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.inputText);
        Button btnTransmit = findViewById(R.id.btnTransmit);
        Button btnStop = findViewById(R.id.btnStop);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException: ", e);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        btnTransmit.setOnClickListener(v -> {
            String text = inputText.getText().toString();
            if (!text.isEmpty()) {
                text += " "; // Add space at the end
                String binaryMessage = stringToBinary(text);
                String finalMessage = "111111111111" + binaryMessage + "000000000000";
                stopTransmission = false; // Reset stop flag
                new Thread(() -> transmitBinaryString(finalMessage)).start();
            } else {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> stopTransmission = true);
    }

    private String stringToBinary(String input) {
        StringBuilder result = new StringBuilder();
        for (char character : input.toCharArray()) {
            String binaryString = String.format("%8s", Integer.toBinaryString(character)).replaceAll(" ", "0");
            int parityBit = calculateParity(binaryString);
            result.append(binaryString).append(parityBit);
        }
        return result.toString();
    }

    private int calculateParity(String binaryString) {
        int parity = 0;
        for (char bit : binaryString.toCharArray()) {
            if (bit == '1') {
                parity ^= 1;
            }
        }
        return parity;
    }

    private void transmitBinaryString(String binaryString) {
        Handler handler = new Handler(Looper.getMainLooper());
        for (char bit : binaryString.toCharArray()) {
            if (stopTransmission) {
                flashLightOff();
                return;
            }
            handler.post(() -> {
                if (bit == '1') {
                    flashLightOn();
                } else {
                    flashLightOff();
                }
            });
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException: ", e);
            }
        }
        flashLightOff();
    }

    private void flashLightOn() {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, true);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException: ", e);
        }
    }

    private void flashLightOff() {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException: ", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission is required for flash control", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
