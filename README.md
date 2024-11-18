# Code Explaination

### **1. Package and Imports**
```java
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
```

**Explanation**:
- **Package Declaration**: Groups the code into a specific namespace, `com.example.ttf20`.
- **Imports**: Brings in necessary classes for:
  - Camera control (`CameraManager`, `CameraAccessException`).
  - UI components (`Button`, `EditText`, `Toast`).
  - Permission handling (`Manifest`, `ActivityCompat`, `ContextCompat`).
  - Multithreading (`Handler`, `Looper`).


### **2. Class and Variables**
```java
public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final String TAG = "TextToFlash";
    private EditText inputText;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean stopTransmission = false;
```

**Explanation**:
- **`MainActivity`**: The main entry point of the app, extending `AppCompatActivity` for compatibility.
- **Constants**:
  - `CAMERA_REQUEST_CODE`: Used to track camera permission requests.
  - `TAG`: A string identifier for logging.
- **Variables**:
  - `inputText`: Reference to a UI input field.
  - `cameraManager`: Manages camera functionality.
  - `cameraId`: Stores the ID of the device's camera.
  - `stopTransmission`: A flag to control message transmission.


### **3. `onCreate()` Method**
```java
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
            text += " ";
            String binaryMessage = stringToBinary(text);
            String finalMessage = "111111111111" + binaryMessage + "000000000000";
            stopTransmission = false;
            new Thread(() -> transmitBinaryString(finalMessage)).start();
        } else {
            Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
        }
    });

    btnStop.setOnClickListener(v -> stopTransmission = true);
}
```

**Explanation**:
- **UI Setup**: Links XML UI components to Java variables using `findViewById`.
- **Camera Setup**:
  - Initializes `cameraManager` to control the flashlight.
  - Fetches the first camera ID.
- **Permissions**:
  - Checks if the app has permission to use the camera; if not, requests it.
- **Button Actions**:
  - **Transmit Button (`btnTransmit`)**:
    - Converts input text into a binary string.
    - Adds start and stop markers (`111...` and `000...`) to the message.
    - Resets the stop flag and starts a new thread to transmit the binary string using the flashlight.
  - **Stop Button (`btnStop`)**:
    - Sets the `stopTransmission` flag to halt transmission.


### **4. Text-to-Binary Conversion**
```java
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
```

**Explanation**:
- **`stringToBinary`**:
  - Converts each character into an 8-bit binary representation.
  - Appends a parity bit (to check for transmission errors).
- **`calculateParity`**:
  - Calculates a parity bit based on the binary string, ensuring an even number of `1`s.


### **5. Transmitting the Binary String**
```java
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
```

**Explanation**:
- **Binary Transmission**:
  - Iterates through each bit in the binary string.
  - Controls the flashlight to represent `1` (on) and `0` (off).
  - Halts if `stopTransmission` is true.
- **Timing**:
  - Delays 100ms between each bit for readability.


### **6. Flashlight Control**
```java
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
```

**Explanation**:
- **`flashLightOn`**: Turns the flashlight on using the camera manager.
- **`flashLightOff`**: Turns the flashlight off.
- Handles potential exceptions to ensure app stability.

---

### **7. Permissions Result**
```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == CAMERA_REQUEST_CODE) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission is required for flash control", Toast.LENGTH_SHORT).show();
        }
    }
}
```

**Explanation**:
- Checks the result of the camera permission request.
- Displays a message if the permission is denied.

---
