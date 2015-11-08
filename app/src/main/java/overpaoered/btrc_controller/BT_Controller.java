package overpaoered.btrc_controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public class BT_Controller extends Activity implements OnTouchListener {


    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;

    private static final String TAG = "bluetooth1";

    private BluetoothAdapter mBluetoothAdapter = null;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit these lines under car1 and car2 or address)
    private static String address = "20:15:04:30:64:35";
    private static String car1 = "20:15:04:30:64:35";
    private static String car2 = "20:15:04:30:64:35";
    private static String logtag = "BTRC_Controller";
    private String dataToSend;
    private int mState = STATE_NONE;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_controller);


        //Button Stuff
        ImageButton buttonUp = (ImageButton) findViewById(R.id.UpButton);
        buttonUp.setOnTouchListener(this);

        ImageButton buttonDown = (ImageButton) findViewById(R.id.DownButton);
        buttonDown.setOnTouchListener(this);

        ImageButton buttonUpRight = (ImageButton) findViewById(R.id.UpRightButton);
        buttonUpRight.setOnTouchListener(this);

        ImageButton buttonUpLeft = (ImageButton) findViewById(R.id.UpLeftButton);
        buttonUpLeft.setOnTouchListener(this);

        ImageButton buttonDownLeft = (ImageButton) findViewById(R.id.DownLeftButton);
        buttonDownLeft.setOnTouchListener(this);

        ImageButton buttonDownRight = (ImageButton) findViewById(R.id.DownRightButton);
        buttonDownRight.setOnTouchListener(this);

        ImageButton buttonFire = (ImageButton) findViewById(R.id.fireLaser);
        buttonFire.setOnTouchListener(this);

        Button buttonBluetooth = (Button) findViewById(R.id.BTConnect);
        buttonBluetooth.setOnTouchListener(this);

        Button buttonBluetooth2 = (Button) findViewById(R.id.BTConnect2);
        buttonBluetooth2.setOnTouchListener(this);

        //Bluetooth Stuff

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public boolean onTouch(View control, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            control.setPressed(true);
            if (control.getId() == R.id.BTConnect) {
                address = car1;
                ConnectDevice();
            } else if (control.getId() == R.id.BTConnect2) {
                address = car2;
                ConnectDevice();
            }
            if (mConnectedThread != null) {
                switch (control.getId()) {
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
                        dataToSend = "S";
                        writeData(dataToSend);
                        break;
                }
            }
            return true;
        } else {
            control.setPressed(false);
            if (mConnectedThread != null) {
                writeData("S");
            }
            return false;
        }
    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }


    @Override
    protected void onStart() {//activity is started and visible to the user
        Log.d(logtag, "onStart() called");
        super.onStart();

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    //activity was resumed and is visible again
    @Override
    protected void onResume() {
        Log.d(logtag, "onResume() called");
        super.onResume();


    }

    private void ConnectDevice() {
        Log.d(TAG, address);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        Log.d(TAG, "Connecting to ... " + device);

        connect(device);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket) {
        Log.d(TAG, "connected, Socket");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
                Log.i(TAG, "Connection attempt");
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    Log.e(TAG, "Socket closed on exception: ", e);
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done

            mConnectThread = null;
            Log.i(TAG, "Connect thread kill");

            // Start the connected thread
            connected(mmSocket);
            Log.i(TAG, "Connected thread start");
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread: ");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * Write to the connected OutStream.
         *
         * @param data The bytes to write
         */
        public void write(String data) {
            try {
                byte[] out = data.getBytes();
                mmOutStream.write(out);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public void writeData(String data) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(data);
    }

    //the activity is not visible anymore
    @Override
    protected void onStop() {
        Log.d(logtag, "onStop() called");
        super.onStop();

    }

    //android has killed this activity
    @Override
    protected void onDestroy() {
        Log.d(logtag, "onDestroy() called");
        super.onDestroy();

        stop();
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bt_controller, menu);
        return true;
    }

    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.BTConnect:
                address = car1;
                ConnectDevice();
                return true;
            case R.id.BTConnect2:
                address = car2;
                ConnectDevice();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
}

