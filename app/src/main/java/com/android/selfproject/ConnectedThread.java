package com.android.selfproject;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private static final String TAG = "ArduinoSensorData";
    private volatile boolean receivingData = false;

    // External components to be set from outside
    private Handler handler;
    private StringBuilder receivedDataBuilder;
    private Runnable onDataReceivedCallback;

    private Context context;

    public ConnectedThread(BluetoothSocket socket, Context context, Handler handler) {
        mmSocket = socket;
        this.context = context;
        this.handler = handler;


        try {
            mmInStream = socket.getInputStream();
            mmOutStream = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException("Error getting input/output stream", e);
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setOnDataReceivedCallback(Runnable onDataReceivedCallback) {
        this.onDataReceivedCallback = onDataReceivedCallback;
    }

    public void startReceiving() {
        receivingData = true;
        super.start();
    }

    @Override
    public void run() {
        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        receivedDataBuilder = new StringBuilder();

        while (receivingData && !Thread.currentThread().isInterrupted()) {
            try {
                int byteAvailable = mmInStream.available();
                if (byteAvailable > 0) {
                    byte[] bytes = new byte[byteAvailable];
                    mmInStream.read(bytes);

                    for (int i = 0; i < byteAvailable; i++) {
                        byte tempByte = bytes[i];
                        if (tempByte == '\n') {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String text = new String(encodedBytes, StandardCharsets.US_ASCII);

                            readBufferPosition = 0;

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 센서값을 JSON으로 변환
                                        processReceivedData(text);
                                    }
                                });

                        } else {
                            readBuffer[readBufferPosition++] = tempByte;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                // 1초마다 받아옴
                SystemClock.sleep(1000);
            } catch (Exception e) {
                // InterruptedException 처리
                e.printStackTrace();
                // 혹은 다른 처리를 수행할 수 있습니다.
            }
        }
    }

    private void processReceivedData(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);

            // 가속도 데이터 추출
            double accX = jsonObject.getDouble("acc_x");
            double accY = jsonObject.getDouble("acc_y");
            double accZ = jsonObject.getDouble("acc_z");

            // 가속도 크기 계산
            double accelerationMagnitude = Math.sqrt(accX * accX + accY * accY + accZ * accZ);

            // 낙상 감지 임계값 설정 (조절이 필요할 수 있음)
            double fallThreshold = 40.0;

            String logMessage = "Received sensor values: accX=" + accX + ", accY=" + accY + ", accZ=" + accZ;
            Log.d(TAG, logMessage);

            // 낙상 감지 알고리즘
            if (accelerationMagnitude > fallThreshold) {
                // 낙상 감지됨
                Log.d(TAG, "Fall Detected!");
                //Intent intent = new Intent(context, CountdownTimer.class);
                //context.startActivity(intent);
            }

            // 센서 값을 사용하여 필요한 작업 수행

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data: " + e.getMessage());
        }
    }

    public void write(String input) {
        byte[] bytes = input.getBytes();
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error writing data to OutputStream", e);
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing BluetoothSocket", e);
        }
    }
}