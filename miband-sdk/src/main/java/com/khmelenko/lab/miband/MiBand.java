package com.khmelenko.lab.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.khmelenko.lab.miband.listeners.HeartRateNotifyListener;
import com.khmelenko.lab.miband.listeners.NotifyListener;
import com.khmelenko.lab.miband.listeners.RealtimeStepsNotifyListener;
import com.khmelenko.lab.miband.model.BatteryInfo;
import com.khmelenko.lab.miband.model.LedColor;
import com.khmelenko.lab.miband.model.Profile;
import com.khmelenko.lab.miband.model.Protocol;
import com.khmelenko.lab.miband.model.UserInfo;
import com.khmelenko.lab.miband.model.VibrationMode;

import java.util.Arrays;
import java.util.UUID;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

/**
 * Main class for interacting with MiBand
 *
 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
public final class MiBand implements BluetoothListener {

    private static final String TAG = "miband-android";

    private final Context mContext;
    private final BluetoothIO mBluetoothIO;

    private PublishSubject<Boolean> mConnectionSubject;
    private PublishSubject<Integer> mRssiSubject;
    private PublishSubject<BatteryInfo> mBatteryInfoSubject;
    private PublishSubject<Void> mPairSubject;
    private PublishSubject<Void> mStartVibrationSubject;
    private PublishSubject<Void> mStopVibrationSubject;
    private PublishSubject<Boolean> mSensorNotificationSubject;
    private PublishSubject<Boolean> mRealtimeNotificationSubject;
    private PublishSubject<Void> mLedColorSubject;
    private PublishSubject<Void> mUserInfoSubject;
    private PublishSubject<Void> mHeartRateSubject;
    private PublishSubject<BluetoothGattCharacteristic> mReadWriteSubject;

    public MiBand(Context context) {
        mContext = context;
        mBluetoothIO = new BluetoothIO(this);

        mConnectionSubject = PublishSubject.create();
        mRssiSubject = PublishSubject.create();
        mBatteryInfoSubject = PublishSubject.create();
        mPairSubject = PublishSubject.create();
        mStartVibrationSubject = PublishSubject.create();
        mStopVibrationSubject = PublishSubject.create();
        mSensorNotificationSubject = PublishSubject.create();
        mRealtimeNotificationSubject = PublishSubject.create();
        mLedColorSubject = PublishSubject.create();
        mUserInfoSubject = PublishSubject.create();
        mHeartRateSubject = PublishSubject.create();

        mReadWriteSubject = PublishSubject.create();
    }

    /**
     * Starts scanning for devices
     *
     * @param callback Callback
     */
    public static void startScan(@NonNull ScanCallback callback) {
        // TODO Change to Rx
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.startScan(callback);
            } else {
                Log.e(TAG, "BluetoothLeScanner is null");
            }
        } else {
            Log.e(TAG, "BluetoothAdapter is null");
        }
    }

    /**
     * Stops scanning for devices
     *
     * @param callback Callback
     */
    public static void stopScan(ScanCallback callback) {
        // TODO Change to Rx
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(callback);
            } else {
                Log.e(TAG, "BluetoothLeScanner is null");
            }
        } else {
            Log.e(TAG, "BluetoothAdapter is null");
        }
    }

    /**
     * Starts connection process to the device
     *
     * @param device Device to connect
     */
    public Observable<Boolean> connect(final BluetoothDevice device) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                mConnectionSubject.subscribe(subscriber);
                mBluetoothIO.connect(mContext, device);
            }
        });
    }

    /**
     * Gets connected device
     *
     * @return Connected device or null, if device is not connected
     */
    public BluetoothDevice getDevice() {
        return mBluetoothIO.getConnectedDevice();
    }

    /**
     * Executes device pairing
     */
    public Observable<Void> pair() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                mPairSubject.subscribe(subscriber);
                // TODO mBluetoothIO.writeAndRead(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_PAIR, Protocol.PAIR, ioCallback);
            }
        });
    }

    /**
     * Reads Received Signal Strength Indication (RSSI)
     */
    public Observable<Integer> readRssi() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                mRssiSubject.subscribe(subscriber);
                mBluetoothIO.readRssi();
            }
        });
    }

    /**
     * Requests battery info
     *
     * @return Battery info instance
     */
    public Observable<BatteryInfo> getBatteryInfo() {
        return Observable.create(new Observable.OnSubscribe<BatteryInfo>() {
            @Override
            public void call(Subscriber<? super BatteryInfo> subscriber) {
                mBatteryInfoSubject.subscribe(subscriber);
                mBluetoothIO.readCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_BATTERY);
            }
        });
    }

    /**
     * Requests starting vibration
     */
    public Observable<Void> startVibration(final VibrationMode mode) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                byte[] protocol;
                switch (mode) {
                    case VIBRATION_WITH_LED:
                        protocol = Protocol.VIBRATION_WITH_LED;
                        break;
                    case VIBRATION_10_TIMES_WITH_LED:
                        protocol = Protocol.VIBRATION_10_TIMES_WITH_LED;
                        break;
                    case VIBRATION_WITHOUT_LED:
                        protocol = Protocol.VIBRATION_WITHOUT_LED;
                        break;
                    default:
                        return;
                }
                mStartVibrationSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, protocol);
            }
        });
    }

    /**
     * Requests stopping vibration
     */
    public Observable<Void> stopVibration() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                mStopVibrationSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION,
                        Protocol.STOP_VIBRATION);
            }
        });
    }

    /**
     * Enables sensor notifications
     */
    public Observable<Boolean> enableSensorDataNotify() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                mSensorNotificationSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                        Protocol.ENABLE_SENSOR_DATA_NOTIFY);
            }
        });
    }

    /**
     * Disables sensor notifications
     */
    public Observable<Boolean> disableSensorDataNotify() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                mSensorNotificationSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                        Protocol.DISABLE_SENSOR_DATA_NOTIFY);
            }
        });
    }

    /**
     * Enables realtime steps notification
     */
    public Observable<Boolean> enableRealtimeStepsNotify() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                mRealtimeNotificationSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                        Protocol.ENABLE_REALTIME_STEPS_NOTIFY);
            }
        });
    }

    /**
     * Disables realtime steps notification
     */
    public Observable<Boolean> disableRealtimeStepsNotify() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                mRealtimeNotificationSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                        Protocol.DISABLE_REALTIME_STEPS_NOTIFY);
            }
        });
    }

    public void setNormalNotifyListener(NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_NOTIFICATION, listener);
    }

    /**
     * 重力感应器数据通知监听, 设置完之后需要另外使用 {@link MiBand#enableRealtimeStepsNotify} 开启 和
     * {@link MiBand##disableRealtimeStepsNotify} 关闭通知
     *
     * @param listener
     */
    public void setSensorDataNotifyListener(final NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_SENSOR_DATA, new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {
                listener.onNotify(data);
            }
        });
    }

    /**
     * 实时步数通知监听器, 设置完之后需要另外使用 {@link MiBand#enableRealtimeStepsNotify} 开启 和
     * {@link MiBand##disableRealtimeStepsNotify} 关闭通知
     *
     * @param listener
     */
    public void setRealtimeStepsNotifyListener(final RealtimeStepsNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_REALTIME_STEPS, new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 4) {
                    int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                    listener.onNotify(steps);
                }
            }
        });
    }

    /**
     * Sets LED color
     *
     * @param color Color
     */
    public Observable<Void> setLedColor(final LedColor color) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                byte[] protocol;
                switch (color) {
                    case RED:
                        protocol = Protocol.SET_COLOR_RED;
                        break;
                    case BLUE:
                        protocol = Protocol.SET_COLOR_BLUE;
                        break;
                    case GREEN:
                        protocol = Protocol.SET_COLOR_GREEN;
                        break;
                    case ORANGE:
                        protocol = Protocol.SET_COLOR_ORANGE;
                        break;
                    default:
                        return;
                }
                mLedColorSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, protocol);
            }
        });

    }

    /**
     * Sets user info
     *
     * @param userInfo User info
     */
    public Observable<Void> setUserInfo(final UserInfo userInfo) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                mUserInfoSubject.subscribe(subscriber);

                BluetoothDevice device = mBluetoothIO.getConnectedDevice();
                byte[] data = userInfo.getBytes(device.getAddress());
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_USER_INFO, data);
            }
        });
    }

    /**
     * Starts heart rate scanner
     */
    public Observable<Void> startHeartRateScan() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                mHeartRateSubject.subscribe(subscriber);
                mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_CHAR_HEARTRATE, Protocol.START_HEART_RATE_SCAN);
            }
        });
    }

    /**
     * Sets heart rate scanner listener
     *
     * @param listener Listener
     */
    public void setHeartRateScanListener(final HeartRateNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE, new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 2 && data[0] == 6) {
                    int heartRate = data[1] & 0xFF;
                    listener.onNotify(heartRate);
                }
            }
        });
    }

    /**
     * Notify for connection results
     *
     * @param result True, if connected. False if disconnected
     */
    private void notifyConnectionResult(boolean result) {
        mConnectionSubject.onNext(true);
        mConnectionSubject.onCompleted();

        // create new connection subject
        mConnectionSubject = PublishSubject.create();
    }

    @Override
    public void onConnectionEstablished() {
        notifyConnectionResult(true);
    }

    @Override
    public void onDisconnected() {
        notifyConnectionResult(false);
    }

    @Override
    public void onResult(BluetoothGattCharacteristic data) {
        UUID serviceId = data.getService().getUuid();
        UUID characteristicId = data.getUuid();
        if (serviceId.equals(Profile.UUID_SERVICE_MILI)) {

            // Battery info
            if (characteristicId.equals(Profile.UUID_CHAR_BATTERY)) {
                Log.d(TAG, "getBatteryInfo result " + Arrays.toString(data.getValue()));
                if (data.getValue().length == 10) {
                    BatteryInfo info = BatteryInfo.fromByteData(data.getValue());

                    mBatteryInfoSubject.onNext(info);
                    mBatteryInfoSubject.onCompleted();
                } else {
                    mBatteryInfoSubject.onError(new Exception("Wrong data format for battery info"));
                }
                mBatteryInfoSubject = PublishSubject.create();
            }

            // Pair
            if (characteristicId.equals(Profile.UUID_CHAR_PAIR)) {
                Log.d(TAG, "Pair result " + Arrays.toString(data.getValue()));
                if (data.getValue().length == 1 && data.getValue()[0] == 2) {
                    mPairSubject.onNext(null);
                    mPairSubject.onCompleted();
                } else {
                    mPairSubject.onError(new Exception("Pairing failed"));
                }
                mPairSubject = PublishSubject.create();
            }

            // sensor notify
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                byte[] changedValue = data.getValue();
                if (changedValue == Protocol.ENABLE_SENSOR_DATA_NOTIFY) {
                    mSensorNotificationSubject.onNext(true);
                } else {
                    mSensorNotificationSubject.onNext(false);
                }
                mSensorNotificationSubject.onCompleted();
                mSensorNotificationSubject = PublishSubject.create();
            }

            // realtime notify
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                byte[] changedValue = data.getValue();
                if (changedValue == Protocol.ENABLE_REALTIME_STEPS_NOTIFY) {
                    mRealtimeNotificationSubject.onNext(true);
                } else {
                    mRealtimeNotificationSubject.onNext(false);
                }
                mRealtimeNotificationSubject.onCompleted();
                mRealtimeNotificationSubject = PublishSubject.create();
            }

            // led color
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                byte[] changedValue = data.getValue();
                if (changedValue == Protocol.SET_COLOR_RED
                        || changedValue == Protocol.SET_COLOR_BLUE
                        || changedValue == Protocol.SET_COLOR_GREEN
                        || changedValue == Protocol.SET_COLOR_ORANGE) {
                    mLedColorSubject.onNext(null);
                }
                mLedColorSubject.onCompleted();
                mLedColorSubject = PublishSubject.create();
            }

            // user info
            if (characteristicId.equals(Profile.UUID_CHAR_USER_INFO)) {
                mUserInfoSubject.onNext(null);
                mUserInfoSubject.onCompleted();

                mUserInfoSubject = PublishSubject.create();
            }
        }

        // vibration service
        if (serviceId.equals(Profile.UUID_SERVICE_VIBRATION)) {
            if (characteristicId.equals(Profile.UUID_CHAR_VIBRATION)) {
                byte[] changedValue = data.getValue();
                // TODO Improve arrays comparision
                if (changedValue == Protocol.STOP_VIBRATION) {
                    mStopVibrationSubject.onNext(null);
                    mStopVibrationSubject.onCompleted();

                    mStopVibrationSubject = PublishSubject.create();
                } else {
                    mStartVibrationSubject.onNext(null);
                    mStartVibrationSubject.onCompleted();

                    mStartVibrationSubject = PublishSubject.create();

                }
            }
        }

        // heart rate
        if(serviceId.equals(Profile.UUID_SERVICE_HEARTRATE)) {
            if(characteristicId.equals(Profile.UUID_CHAR_HEARTRATE)) {
                byte[] changedValue = data.getValue();
                if(changedValue == Protocol.START_HEART_RATE_SCAN) {
                    mHeartRateSubject.onNext(null);
                    mHeartRateSubject.onCompleted();

                    mHeartRateSubject = PublishSubject.create();
                }
            }
        }
    }

    @Override
    public void onResultRssi(int rssi) {
        mRssiSubject.onNext(rssi);
        mRssiSubject.onCompleted();

        mRssiSubject = PublishSubject.create();
    }

    @Override
    public void onFail(int errorCode, String msg) {
        // TODO
    }
}
