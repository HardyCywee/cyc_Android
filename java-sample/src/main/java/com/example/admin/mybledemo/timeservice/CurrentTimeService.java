package com.example.admin.mybledemo.timeservice;

/* Copyright (C) 2017 Relish Technologies Ltd. - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the MIT license.
 *
 * You should have received a copy of the MIT license with
 * this file. If not, please visit https://opensource.org/licenses/MIT
 */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;

public class CurrentTimeService {

    private static final String TAG = CurrentTimeService.class.getSimpleName();
    private Context mContext;

    public CurrentTimeService(Context context) {
        this.mContext=context;
    }

    // UUID for Current Time Service (CTS)
    private static final UUID SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");
    private static final UUID LOCAL_TIME_INFO_CHARACTERISTIC_UUID = UUID.fromString("00002A0F-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR_TIME = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private static BluetoothGattServer sGattServer = null;

    private static BluetoothGattService GATT_SERVICE = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
//    static {
//
//        BluetoothGattCharacteristic bluetoothGattCharacteristic=   new BluetoothGattCharacteristic(CURRENT_TIME_CHARACTERISTIC_UUID,
//                BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_INDICATE|BluetoothGattCharacteristic.PROPERTY_WRITE,
//                BluetoothGattCharacteristic.PERMISSION_WRITE|BluetoothGattCharacteristic.PERMISSION_READ);
//        BluetoothGattDescriptor descriptor=new BluetoothGattDescriptor(UUID_DESCRIPTOR_TIME, BluetoothGattCharacteristic.PERMISSION_WRITE);
//        bluetoothGattCharacteristic.addDescriptor(descriptor);
//        GATT_SERVICE.addCharacteristic(bluetoothGattCharacteristic);
//
//        GATT_SERVICE.addCharacteristic(
//                new BluetoothGattCharacteristic(LOCAL_TIME_INFO_CHARACTERISTIC_UUID,
//                        BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_READ| BluetoothGattCharacteristic.PROPERTY_INDICATE,
//                        BluetoothGattCharacteristic.PERMISSION_READ| BluetoothGattCharacteristic.PERMISSION_WRITE)
//        );
//    }

    static {
        GATT_SERVICE.addCharacteristic(
                new BluetoothGattCharacteristic(CURRENT_TIME_CHARACTERISTIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ)
        );
        GATT_SERVICE.addCharacteristic(
                new BluetoothGattCharacteristic(LOCAL_TIME_INFO_CHARACTERISTIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ)
        );
    }

    private  class CurrentTimeCallback extends BluetoothGattServerCallback {

        private BluetoothGattServer mGattServer = null;

        void setGattServer(BluetoothGattServer gattServer) {
            mGattServer = gattServer;
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (CURRENT_TIME_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                TimeData timeData=new TimeData();
                byte[] bytes=timeData.exactTime256WithUpdateReason(Calendar.getInstance(), TimeData.UPDATE_REASON_UNKNOWN);
                boolean sendResult= mGattServer.sendResponse(device, 2, BluetoothGatt.GATT_SUCCESS, 0, bytes);
                Log.i("tttt","send data "+bytes+"---"+sendResult);

                //mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, TimeData.exactTime256WithUpdateReason(Calendar.getInstance(), TimeData.UPDATE_REASON_UNKNOWN));
            } else if (LOCAL_TIME_INFO_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, TimeData.timezoneWithDstOffset(Calendar.getInstance()));
            } else {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (descriptor.getCharacteristic().getUuid().equals(CURRENT_TIME_CHARACTERISTIC_UUID)){
                TimeData timeData=new TimeData();
                byte[] bytes=timeData.exactTime256WithUpdateReason(Calendar.getInstance(), TimeData.UPDATE_REASON_UNKNOWN);
                Log.i("tttt","send data "+bytes);
                boolean sendResult= mGattServer.sendResponse(device, 2, BluetoothGatt.GATT_SUCCESS, 0, bytes);

                Log.i("tttt","send data "+bytes+"---"+sendResult);
            }else {
                Log.i("tttt","send data fail uuid not equal");
            }




        }
    }

    /**
     * Start the CurrentTimeService GATT server
     * @return true if the GATT server starts successfully or is already running
     */
    public  boolean startServer(Context context) {
        if (sGattServer == null) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
            CurrentTimeCallback callback = new CurrentTimeCallback();
            sGattServer = manager.openGattServer(context, callback);
            if (sGattServer == null) {
                Log.e(TAG, "Unable to start GATT server");
                return false;
            }
            sGattServer.addService(GATT_SERVICE);
            callback.setGattServer(sGattServer);
        } else {
            Log.w(TAG, "Already started");
        }
        return true;
    }

    public static void stopServer() {
        if (sGattServer != null) {
            sGattServer.close();
            sGattServer = null;
        }
    }

}
