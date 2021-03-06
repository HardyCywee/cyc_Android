package com.example.admin.mybledemo.myservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.admin.mybledemo.MyApplication;
import com.example.admin.mybledemo.timeservice.TimeData;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by lihong on 2017/10/24.
 */

public class AdvertiserService extends Service {

    private static final String TAG = "AdvertiserService";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    public static boolean running = false;
    public static final String ADVERTISENG_FILED = "com.examle.lihong.bluetoothadvertisement.advertising_failed";
    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final int ADVERTISING_TIMED_OUT = 6;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdertiseCallback;
    private Handler mHandler;
    private Runnable timeoutRunnable;
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattCharacteristic characteristicRead;
    BluetoothManager mBluetoothManager;

    private static UUID UUID_SERVER = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARREAD = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARWRITE = UUID.fromString("00002A0F-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // UUID for Current Time Service (CTS)
    private static final UUID SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");
    private static final UUID LOCAL_TIME_INFO_CHARACTERISTIC_UUID = UUID.fromString("00002A0F-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR_TIME = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    @Override
    public void onCreate() {
        running = true;
        initialize();
        startAdvertising();
        setTimeout();
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        running = false;
        stopAdvertising();
        mHandler.removeCallbacks(timeoutRunnable);
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, "???????????????????????????", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setTimeout() {
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "????????????????????????" + TIMEOUT + "????????????????????????");
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    private void startAdvertising() {
        //goForeground();
        Log.d(TAG, "??????????????????");
        if (mAdertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdertiseCallback);
            }
        }
    }

    private void goForeground() {
        Log.d(TAG, "goForegroud????????????");
        //NotificationManager manager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MyApplication.getInstance().getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("???????????????????????????")
                .setContentText("??????????????????????????????????????????")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        //  manager.notify(FOREGROUND_NOTIFICATION_ID,notification);
    }

    private void stopAdvertising() {
        Log.d(TAG, "??????????????????");
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdertiseCallback);
            mAdertiseCallback = null;
        }
    }

    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        // dataBuilder.addServiceUuid(Constants.Service_UUID);
        dataBuilder.setIncludeDeviceName(true);

        return dataBuilder.build();
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);

        return settingsBuilder.build();
    }

    private class SampleAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d(TAG, "????????????");
            sendFailureIntent(errorCode);
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "??????????????????????????????");
            Log.d(TAG, "BLE?????????????????????????????????TxPowerLv=" + settingsInEffect.getTxPowerLevel() + "???mode=" + settingsInEffect.getMode() + "???timeout=" + settingsInEffect.getTimeout());
            initServices(getContext());//????????????????????????????????????????????????????????????????????????
        }
    }

    private void sendFailureIntent(int errorCode) {
        Intent failureIntent = new Intent();
        failureIntent.setAction(ADVERTISENG_FILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    //????????????????????????????????????????????????????????????????????????????????????????????????
    //???BluetoothGattServer?????????????????????????????????????????????
    private void initServices(Context context) {
        mBluetoothGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        characteristicRead = new BluetoothGattCharacteristic(UUID_CHARREAD, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicRead.addDescriptor(descriptor);
        service.addCharacteristic(characteristicRead);

        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);

        mBluetoothGattServer.addService(service);
        Log.d(TAG, "????????????????????????initServices ok");
    }

    //?????????????????????
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        //1?????????????????????????????????
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG, "?????????????????????????????????????????????onConnectionStateChange:device name=" + device.getName() + "address=" + device.getAddress() + "status=" + status + "newstate=" + newState);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, "????????????????????????????????????????????????onCharacteristicReadRequest()??????");
            TimeData timeData = new TimeData();
            byte[] bytes = timeData.exactTime256WithUpdateReason(Calendar.getInstance(), (byte) 0);
            boolean sendResult = mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, bytes);
            if (sendResult) {
                //   ToastUtils.show(getContext(),"??????????????????");


            }
        }

        //????????????????????????????????????????????????????????????????????????????????????????????????value
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, "????????????????????????????????????????????????onCharacteristicWriteRequest()??????");

            //??????????????????????????????????????????????????????????????????
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

            //??????????????????
            //value:??????????????????????????????
            onResponseToClient(value, device, requestId, characteristic);
        }

        //????????????????????????????????????????????????????????????????????????????????????
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        //2???????????????????????????????????????????????????????????????
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            // onResponseToClient(value,device,requestId,descriptor.getCharacteristic());
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG, "??????????????????????????????????????????onServiceAdded()??????");
        }
    };

    //4.??????????????????,requestBytes?????????????????????????????????
    private void onResponseToClient(byte[] requestBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic) {
        //????????????????????????
        String msg = OutputStringUtil.transferForPrint(requestBytes);
        Log.e(TAG, "?????????" + msg);
        //???????????????
        String str = new String(requestBytes) + "hello>";
        characteristicRead.setValue(str.getBytes());
        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false);//??????????????????characteristicRead??????????????????
    }

    private Context getContext() {
        return this;
    }
}





