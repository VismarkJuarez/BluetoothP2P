package com.example.bluetooth_p2p;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button bluetoothOnButton, bluetoothOffButton, enableBuetoothDiscoverabilityButton, discoverDevicesButton, listenforNewMessagesButton;
    Button send;
    TextView msg_box;
    EditText writeMsg;
    BluetoothAdapter myBluetoothAdapter;
    Intent btEnablingIntent;
    int requestCodeForEnable;

    ListView listView;
    BluetoothDevice[] btArray;

    SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    int REQUEST_ENABLE_BLUETOOTH = 1;

    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID = UUID.fromString("0704bf52-0931-4a15-aaff-e662f6632d7d");

    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothOnButton = (Button)findViewById(R.id.bluetoothOn);
        bluetoothOffButton = (Button)findViewById(R.id.bluetoothOff);
        discoverDevicesButton = (Button)findViewById(R.id.discoverDevicesButton);
        enableBuetoothDiscoverabilityButton = (Button)findViewById(R.id.enableBluetoothDiscoverability);
        listenforNewMessagesButton = (Button)findViewById(R.id.listenForNewMessages);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestCodeForEnable = 1; //any request code > 0 is fine (can also be replaced by `REQUEST_ENABLE_BLUETOOTH`
        listView = (ListView)findViewById(R.id.listView);

        send = (Button)findViewById(R.id.sendButton);
        msg_box = (TextView)findViewById(R.id.messageBoxTextView);
        writeMsg = (EditText)findViewById(R.id.writeMsgEditText);

        setupOnClickListeners();
    }

    private void setupOnClickListeners() {

        bluetoothOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothAdapter.isEnabled()) {
                    myBluetoothAdapter.disable();
                }
            }
        });

        bluetoothOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //First, check to see if device supports bluetooth connectivity
                if(myBluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth is not supported, bruh", Toast.LENGTH_LONG).show();
                } else {
                    if(!myBluetoothAdapter.isEnabled()) {
                        startActivityForResult(btEnablingIntent, requestCodeForEnable);
                    }
                }
            }
        });

        discoverDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt = myBluetoothAdapter.getBondedDevices();
                String[] strings = new String[bt.size()];

                //TODO investigate instantiation of this array
                btArray = new BluetoothDevice[bt.size()];

                int index = 0;

                if(bt.size() > 0) {
                    for(BluetoothDevice device : bt) {
                        strings[index] = device.getName();
                        btArray[index] = device;
                        index++;
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        listenforNewMessagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start(); //will begin automatically listening for any messages
            }
        });

        enableBuetoothDiscoverabilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
                startActivity(intent);
            }
        });


        //setting up onClick logic for the listView. This will be triggered
        //when an item in the listview gets clicked on:
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass clientClass = new ClientClass(btArray[position]);
                clientClass.start();
                Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(writeMsg.getText());
                sendReceive.write(string.getBytes());
            }
        });

    }


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    Toast.makeText(getApplicationContext(), "Listening.", Toast.LENGTH_SHORT).show();
                    break;
                case STATE_CONNECTING:
                    Toast.makeText(getApplicationContext(), "Connecting.", Toast.LENGTH_SHORT).show();
                    break;
                case STATE_CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connected.", Toast.LENGTH_SHORT).show();
                    break;
                case STATE_CONNECTION_FAILED:
                    Toast.makeText(getApplicationContext(), "Connection Failed.", Toast.LENGTH_SHORT).show();
                    break;
                case STATE_MESSAGE_RECEIVED:
                    //Consume the received message and display it on the screen
                    byte[] readBuffer = (byte[])msg.obj;
                    String tempMsg = new String(readBuffer, 0, msg.arg1);
                    msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == requestCodeForEnable) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluethooth is enabled, brah.", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth enabling was cancelled.", Toast.LENGTH_LONG).show();
            }
        }
    }

    //TODO: Should be moved to a separate file.
    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            try {
                serverSocket = myBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket socket = null;
            while(socket == null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                //if connection was established
                if(socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    //send and receive logic:
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }

    //TODO: Should be moved to a separate file.
    private class ClientClass extends Thread {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1) {
            device = device1;
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            //always listen for a new message
            while(true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
