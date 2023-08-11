package com.cookandroid.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    private final static int REQUEST_ENABLE_BT = 1;

    TextView textStatus;
    Button btnPaired, btnSearch, btnSend, receiveButton;
    ListView listView;

    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    BluetoothSocket btSocket;
    ConnectedThread connectedThread;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 블루투스 SPP 프로파일 UUID
    private static final String BT_ADDRESS = "XX:XX:XX:XX:XX:XX"; // 아두이노 블루투스 주소

    private static final String TAG = "ArduinoSensorData";

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceAddress = device.getAddress();
                String deviceName = device.getName();

                if (deviceName != null && !deviceAddressArray.contains(deviceAddress)) {
                    btArrayAdapter.add(deviceName);
                    deviceAddressArray.add(deviceAddress);
                    btArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        BluetoothSocket socket = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } else {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get permission
        String[] permissionList = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN // 스캔 권한 요청을 위해 추가
        };

        ActivityCompat.requestPermissions(this, permissionList, 1);

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            // Device does not support Bluetooth
            Log.d("MainActivity", "Device does not support Bluetooth.");
            finish(); // Close the app
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initialize views
        textStatus = findViewById(R.id.text_status);
        btnPaired = findViewById(R.id.btn_paired);
        btnSearch = findViewById(R.id.btn_search);
        btnSend = findViewById(R.id.btn_send);
        receiveButton = findViewById(R.id.btn_receive);
        listView = findViewById(R.id.listview);

        // Initialize ListView
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        // Register the ACTION_FOUND receiver.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        listView.setOnItemClickListener(new myOnItemClickListener());

        // 시리얼 통신으로 받은 JSON 데이터를 가정하여 아래와 같이 처리합니다.
        String jsonData = ""; // 시리얼 통신으로 받은 JSON 데이터를 저장
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            int sensorValue = jsonObject.getInt("sensorValue");

            // 센서 값 사용 예시
            Log.d(TAG, "Received sensor value: " + sensorValue);
            // 여기서부터 센서 값을 활용하여 필요한 작업을 수행할 수 있습니다.
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data: " + e.getMessage());
        }
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receiveDataFromArduino();
            }
        });
    }

    // Handle button click event to search for paired devices
    public void onClickButtonPaired(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인되었을 때 처리
                executePairedDeviceSearch();
            } else {
                // 권한 요청
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
            }
        } else {
            // 마시멜로우 이전 버전에서는 권한 체크 없이 처리
            executePairedDeviceSearch();
        }
    }

    private void executePairedDeviceSearch() {
        btArrayAdapter.clear();
        if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
            deviceAddressArray.clear();
        }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
            }
        } else {
            Log.d("MainActivity", "No paired devices found.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(discoveryReceiver);
    }
    // 디바이스 검색 버튼 클릭 핸들러
    public void onClickButtonSearch(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                // 기존 스캔 중지
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }

                // 새로운 검색을 시작하기 전에 기존 목록을 지웁니다.
                btArrayAdapter.clear();
                deviceAddressArray.clear();

                // ACTION_FOUND 리시버 등록을 제거합니다.
                unregisterReceiver(discoveryReceiver);

                // ACTION_FOUND 리시버 다시 등록
                IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(discoveryReceiver, discoveryFilter);

                // 검색 시작
                startDeviceDiscovery();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 3);
            }
        } else {
            // 기존 스캔 중지
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }

            // 새로운 검색을 시작하기 전에 기존 목록을 지웁니다.
            btArrayAdapter.clear();
            deviceAddressArray.clear();

            // ACTION_FOUND 리시버 등록을 제거합니다.
            unregisterReceiver(discoveryReceiver);

            // ACTION_FOUND 리시버 다시 등록
            IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(discoveryReceiver, discoveryFilter);

            // 검색 시작
            startDeviceDiscovery();
        }
    }
    private void startDeviceDiscovery() {
        // 권한이 허용된 경우에만 스캔 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 허용되지 않은 경우
            Toast.makeText(this, "Bluetooth Admin permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이미 스캔 중인 경우 중지
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
    }

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

            textStatus.setText("try...");

            final String name = btArrayAdapter.getItem(position); // get name
            final String address = deviceAddressArray.get(position); // get address
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device);
                btSocket.connect();
            } catch (IOException e) {
                flag = false;
                textStatus.setText("connection failed!");
                e.printStackTrace();
            }

            if (flag) {
                textStatus.setText("connected to " + name);
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
            }

        }
    }

    private void receiveDataFromArduino() {
        if (connectedThread != null) {
            // If the connection is already established, start receiving data
            connectedThread.startReceiving();
        } else {
            // If the connection is not established, create a new connection and start receiving data
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }

            if (deviceAddressArray.size() > 0) {
                textStatus.setText("try...");

                final String name = btArrayAdapter.getItem(0); // get name of the first device in the list (you can modify this as needed)
                final String address = deviceAddressArray.get(0); // get address of the first device in the list (you can modify this as needed)
                boolean flag = true;

                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                // create & connect socket
                try {
                    btSocket = createBluetoothSocket(device);
                    btSocket.connect();
                } catch (IOException e) {
                    flag = false;
                    textStatus.setText("connection failed!");
                    e.printStackTrace();
                }

                if (flag) {
                    textStatus.setText("connected to " + name);
                    connectedThread = new ConnectedThread(btSocket);
                    connectedThread.startReceiving();
                }
            } else {
                // No devices in the list, handle this case accordingly (e.g., show a message or take any action)
            }
        }
    }

    public void onClickButtonSend(View view){
        if(connectedThread!=null){ connectedThread.write("a"); }
    }

}







