package com.kaist.kbms.bluetoothrfid;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private ListView mListView;
    private Button mClearButton;

    private String mConnectedDeviceName = null;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothService mBluetoothService = null;

    private ArrayAdapter<String> mReadArrayAdapter;

    private ArrayList<String> mReadTagList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothService == null) {
            setupReader();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mListView = (ListView) view.findViewById(R.id.in);
        mClearButton = (Button) view.findViewById(R.id.button_clear);
    }

    private void setupReader() {
        Log.d(TAG, "setupReader()");

        // Initialize the array adapter for the conversation thread
        mReadArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mListView.setAdapter(mReadArrayAdapter);
        
        mReadTagList = new ArrayList<String>();
        
        // Initialize the send button with a listener that for click events
        //mSendButton.setOnClickListener(new View.OnClickListener() { sendMessage() }
        
        mClearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mReadArrayAdapter.clear();
                mReadTagList.clear();
            }
        });

        // Initialize the BluetoothService to perform bluetooth connections
        mBluetoothService = new BluetoothService(getActivity(), mHandler);
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mReadArrayAdapter.clear();
                            //mReadTagList.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mReadArrayAdapter.add("Send:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mReadArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

                    String readTagNo = packetResult(readBuf);
                    if(!readTagNo.isEmpty()){
                        //서버로 태그번호 전송 (HTTP, REST, ...) "http://192.168.1.71:1337/?tag="+tagNo
                        requestTagInfo(readTagNo); //<-- mReadArrayAdapter.add(readTagNo);

                        //String[] readAdapter = HttpRestService.requestTagInfo(readTagNo);
                        //Log.d(TAG, "return adaptor:" + readAdapter[0]);
                        //mReadArrayAdapter.add(readAdapter[0]);
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a reader session
                    setupReader();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }
    
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothService.connect(device);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String byteArrayToHex(byte[] buffer, String stype) {
        String hexFormat = stype == "hex" ? "%02X " : "%02X"; //hex: BB 01 36... , str: BB0136...
        StringBuilder sb = new StringBuilder();
        for(final byte b: buffer)
            sb.append(String.format(hexFormat, b&0xff));
        return sb.toString();
    }

    private String packetResult(byte[] buffer) {
        byte[] packetHeader = new byte[7];  //0~6, 7byte
        byte[] packetBody   = new byte[12]; //7~18, 12byte
        byte packetTail     = (byte)0x7E;   //19, 1byte

        String packetResult = "";
        String packetTagNo  = "";
        String returnTagNo  = "";

        //Log.d(TAG, "READ: " + byteArrayToHex(buffer, "hex"));
        System.arraycopy(buffer, 0, packetHeader, 0, 7);
        packetResult = byteArrayToHex(packetHeader, "hex");

        if(Arrays.equals(Constants.READ_BLOCK, packetHeader)){
            if(buffer[19] == packetTail) { //packetTail check
                System.arraycopy(buffer, 7, packetBody, 0, 12);
                packetTagNo = byteArrayToHex(packetBody, "str");
                packetResult += " >> " + packetTagNo;

                if(!mReadTagList.contains(packetTagNo)){
                    mReadTagList.add(packetTagNo);

                    Log.i(TAG, "BLOCK READ : " + packetResult);
                    returnTagNo = packetTagNo;
                }
            };
        }else if(Arrays.equals(Constants.READ_START, packetHeader)){
            Log.i(TAG, "READ START : " + packetResult);
            Toast.makeText(getActivity(), "RFID Reader Read Start", Toast.LENGTH_SHORT).show();
        }else if(Arrays.equals(Constants.READ_STOP, packetHeader)){
            Log.i(TAG, "READ STOP  : " + packetResult);
            Toast.makeText(getActivity(), "RFID Reader Read Stop", Toast.LENGTH_SHORT).show();
        }else if(Arrays.equals(Constants.DEVICE_OFF, packetHeader)){
            Log.i(TAG, "DEVICE OFF : " + packetResult);
            Toast.makeText(getActivity(), "RFID Reader Device Off", Toast.LENGTH_SHORT).show();
        }
        return returnTagNo;
    }

    private void requestTagInfo(final String tagId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpRestService.REST_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpRestService.HttpRestClient client = retrofit.create(HttpRestService.HttpRestClient.class);
        Call<ResponseBody> call = client.getTagUserInfo(tagId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    //Log.d(TAG, "response: " + response + " , response.body(): " + response.body() + " , response.body().string(): " + response.body().string());
                    String responseBody = response.body().string();
                    Log.d(TAG, "responseBody: " + responseBody);
                    if(responseBody.equals("null") || TextUtils.isEmpty(responseBody)){
                        String readAdapter = tagId;
                        mReadArrayAdapter.add(readAdapter);
                    }else{
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String readAdapter = json.getString("tagId") + " | " + json.getString("bicId") + " | " + json.getString("userId");
                            mReadArrayAdapter.add(readAdapter);
                        } catch (JSONException e) {
                            Log.e(TAG, "jsonobject error", e);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "response error", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Rest failure", t);
            }
        });
    }
}
