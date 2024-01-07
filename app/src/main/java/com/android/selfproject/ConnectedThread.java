package com.android.selfproject;

import android.bluetooth.BluetoothSocket;
<<<<<<< HEAD
=======
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
>>>>>>> 46dd2bf (센서값수정/낙상알고리즘 추가)
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
<<<<<<< HEAD
=======
import java.nio.charset.StandardCharsets;
>>>>>>> 46dd2bf (센서값수정/낙상알고리즘 추가)

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

<<<<<<< HEAD
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
=======
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
>>>>>>> 46dd2bf (센서값수정/낙상알고리즘 추가)
    }

    @Override
    public void run() {
<<<<<<< HEAD
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
=======
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
>>>>>>> 46dd2bf (센서값수정/낙상알고리즘 추가)
            }
        }
    }

<<<<<<< HEAD
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
=======
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

>>>>>>> 46dd2bf (센서값수정/낙상알고리즘 추가)
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data: " + e.getMessage());
        }
    }
<<<<<<< HEAD
}
=======

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
>>>>>>> 46dd2bf (센서값수정/낙상알고리즘 추가)
