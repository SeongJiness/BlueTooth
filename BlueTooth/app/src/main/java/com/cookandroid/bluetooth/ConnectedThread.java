package com.cookandroid.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private static final String TAG = "ArduinoSensorData"; // TAG 변수를 정의

    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    private volatile boolean receivingData = false;

    public void startReceiving() {
        receivingData = true;
    }

    public void stopReceiving() {
        receivingData = false;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if (bytes != 0) {
                    buffer = new byte[1024];
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
        while (receivingData) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if (bytes != 0) {
                    buffer = new byte[1024];
                    SystemClock.sleep(100); // pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                    // Parse JSON data and process the sensor value
                    String jsonData = new String(buffer, 0, bytes);
                    processData(jsonData);
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes();           //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }

    private void processData(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            int sensorValue = jsonObject.getInt("sensorValue");

            // 센서 값을 사용하여 필요한 작업 수행
            // 예시로 로그를 출력합니다.
            String logMessage = "Received sensor value: " + sensorValue;
            Log.d(TAG, logMessage);
            // 여기서부터 센서 값을 활용하여 필요한 작업을 수행할 수 있습니다.
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data: " + e.getMessage());
        }
    }
}
