package com.android.selfproject;

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
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;


public class BlueTooth extends AppCompatActivity {

    BluetoothAdapter btAdapter;


    TextView textStatus;
    Button btnSearch, btnBluetoothOn, btnBluetoothOff;
    ListView listView;

    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    BluetoothSocket btSocket;
    ConnectedThread connectedThread;

    private final static int REQUEST_ENABLE_BT = 1;
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
        setContentView(R.layout.blue_tooth_layout);


        ImageButton ImageButton = findViewById(R.id.backButton);
       ImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
          public void onClick(View v) {
                Intent intent = new Intent(); // 인텐트 객체 생성하고
                intent.putExtra("name", "suzin"); // 인텐트 객체에 데이터 넣기
                setResult(RESULT_OK, intent); // 응답 보내기
                finish(); // 현재 액티비티 없애기
            }
        });

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

        // 뷰 ID값 찾아오기
        textStatus = findViewById(R.id.text_status);
        btnBluetoothOn = findViewById(R.id.btnBluetoothOn);
        btnBluetoothOff = findViewById(R.id.btnBluetoothOff);
        btnSearch = findViewById(R.id.btn_search);
        listView = findViewById(R.id.listview);

        //블루투스 ON 버튼 리스너
        btnBluetoothOn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOn();
            }
        });

        //블루투스 OFF 버튼 리스너
        btnBluetoothOff.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });

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
    }

    void bluetoothOn() {
        if(btAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        }
        else {
            if (btAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, REQUEST_ENABLE_BT);
            }
        }
    }

    void bluetoothOff() {
        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {
                // 사용자에게 Bluetooth를 비활성화할 것인지 묻는 대화상자를 띄우지 않고
                // 토스트 메시지를 표시한 후, 일정 시간 후에 시스템 설정으로 이동합니다.
                Toast.makeText(getApplicationContext(), "Bluetooth를 비활성화하려면 시스템 설정에서 꺼야합니다...", Toast.LENGTH_SHORT).show();

                // 일정 시간(예: 2초) 후에 시스템 설정으로 이동
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 시스템 설정으로 이동
                        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                    }
                }, 1000); // 2000 밀리초 = 2초
            } else {
                Log.d("Bluetooth", "Bluetooth 상태: 이미 비활성화됨");
            }
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
                receiveDataFromArduino(); // 연결 수립 전에 데이터 수신 시작
                connectedThread.startReceiving(); // 추가: 연결이 수립되었을 때 데이터 수신을 시작함
            }
        }
    }

    private void receiveDataFromArduino() {
        Log.d(TAG, "receiveDataFromArduino method called");
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


}
