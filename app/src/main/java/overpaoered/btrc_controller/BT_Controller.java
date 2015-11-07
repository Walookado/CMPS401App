package overpaoered.btrc_controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

public class BT_Controller extends Activity implements OnClickListener {

    TextView Result;

    private static final String TAG = "bluetooth1";

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static String address2 = "20:15:04:30:64:35";
    private static String address = "94:39:E5:46:A5:2D";
    private static String logtag = "BTRC_Controller";
    private String dataToSend;

    Handler handler = new Handler();
    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt__controller);


        //Button Stuff
        ImageButton buttonUp = (ImageButton) findViewById(R.id.UpButton);
        buttonUp.setOnClickListener(this);

        ImageButton buttonDown = (ImageButton) findViewById(R.id.DownButton);
        buttonDown.setOnClickListener(this);

        ImageButton buttonUpRight = (ImageButton) findViewById(R.id.UpRightButton);
        buttonUpRight.setOnClickListener(this);

        ImageButton buttonUpLeft = (ImageButton) findViewById(R.id.UpLeftButton);
        buttonUpLeft.setOnClickListener(this);

        ImageButton buttonDownLeft = (ImageButton) findViewById(R.id.DownLeftButton);
        buttonDownLeft.setOnClickListener(this);

        ImageButton buttonDownRight = (ImageButton) findViewById(R.id.DownRightButton);
        buttonDownRight.setOnClickListener(this);

        ImageButton buttonFire = (ImageButton) findViewById(R.id.fireLaser);
        buttonFire.setOnClickListener(this);

        Button buttonBluetooth = (Button) findViewById(R.id.BTConnect);
        buttonBluetooth.setOnClickListener(this);

        //Bluetooth Stuff

        //btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

    }

    @Override
    public void onClick(View control) {
        switch (control.getId()) {
            case R.id.BTConnect:
                Connect();
                break;
            case R.id.UpButton:
                dataToSend = "F";
                writeData(dataToSend);
                break;
            case R.id.DownButton:
                dataToSend = "B";
                writeData(dataToSend);
                break;
            case R.id.UpLeftButton:
                dataToSend = "G";
                writeData(dataToSend);
                break;
            case R.id.UpRightButton:
                dataToSend = "I";
                writeData(dataToSend);
                break;
            case R.id.DownLeftButton:
                dataToSend = "H";
                writeData(dataToSend);
                break;
            case R.id.DownRightButton:
                dataToSend = "J";
                writeData(dataToSend);
                break;
            case R.id.fireLaser:
                dataToSend = "2";
                writeData(dataToSend);
                break;
            default:
                dataToSend = "0";
                writeData(dataToSend);
                break;
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }


    @Override
    protected void onStart() {//activity is started and visible to the user
        Log.d(logtag, "onStart() called");
        super.onStart();
    }

    @Override
    protected void onResume() {//activity was resumed and is visible again
        Log.d(logtag, "onResume() called");
        super.onResume();

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    /*
    @Override
    protected void onPause() { //device goes to sleep or another activity appears
        Log.d(logtag, "onPause() called"); //another activity is currently running (or user has pressed Home)
        super.onPause();

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }
    */

    protected void checkBTState() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (!btAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Disabled !",
                    Toast.LENGTH_SHORT).show();
        }

        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Bluetooth null !", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void Connect() {
        Log.d(TAG, address);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        Log.d(TAG, "Connecting to ... " + device);
        btAdapter.cancelDiscovery();
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            Log.d(TAG, "Connection made.");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
            }
            Log.d(TAG, "Socket creation failed");
        }

        beginListenForData();
    }

    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "Bug BEFORE Sending stuff", e);
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(TAG, "Bug while sending stuff", e);
        }
    }

    /*
    private void queryPairedDevices() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }
    */
    @Override
    protected void onStop() { //the activity is not visible anymore
        Log.d(logtag, "onStop() called");
        super.onStop();

    }

    @Override
    protected void onDestroy() {//android has killed this activity
        Log.d(logtag, "onDestroy() called");
        super.onDestroy();

        try {
            btSocket.close();
        } catch (IOException e) {

        }
    }

    public void beginListenForData() {
        try {
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
        }

        Thread workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = inStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        public void run() {

                                            if (Result.getText().toString().equals("..")) {
                                                Result.setText(data);
                                            } else {
                                                Result.append("\n" + data);
                                            }

                                        /* You also can use Result.setText(data); it won't display multilines
                                        */

                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bt__controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
}

