package com.mumu.libjoshgame.device;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.mumu.libjoshgame.GameDevice;
import com.mumu.libjoshgame.GameDeviceHWEventListener;
import com.mumu.libjoshgame.GameLibrary20;
import com.mumu.libjoshgame.IGameDevice;
import com.mumu.libjoshgame.Log;
import com.mumu.libjoshgame.ScreenPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

/**
 * AndroidInternal
 * this class implements the old school JoshAutomation method of Android command executor
 * import of Android related files here only
 */
public class AndroidInternal extends GameDevice implements IGameDevice, ServiceConnection {
    private static final String TAG = GameLibrary20.TAG;
    private static final String DEVICE_NAME              = "AndroidInternal";
    private static final String DEVICE_VERSION           = "1.0.190924";
    private static final int    DEVICE_SYS_TYPE          = DEVICE_SYS_LINUX;
    private static final String PRELOAD_PATH_INTERNAL    = Environment.getExternalStorageDirectory().toString() + "/internal.dump";
    private static final String PRELOAD_PATH_FIND_COLOR  = Environment.getExternalStorageDirectory().toString() + "/find_color.dump";
    private static final String PRELOAD_PATH_USER_SLOT_0 = Environment.getExternalStorageDirectory().toString() + "/user_slot_0.dump";
    private static final String PRELOAD_PATH_USER_SLOT_1 = Environment.getExternalStorageDirectory().toString() + "/user_slot_1.dump";
    private static final String PRELOAD_PATH_USER_SLOT_2 = Environment.getExternalStorageDirectory().toString() + "/user_slot_2.dump";
    private static final String PRELOAD_PATH_USER_SLOT_3 = Environment.getExternalStorageDirectory().toString() + "/user_slot_3.dump";
    private static final String PRELOAD_PATH_USER_SLOT_4 = Environment.getExternalStorageDirectory().toString() + "/user_slot_4.dump";
    private static final String PRELOAD_PATH_USER_SLOT_5 = Environment.getExternalStorageDirectory().toString() + "/user_slot_5.dump";
    private static final String PRELOAD_PATH_USER_SLOT_6 = Environment.getExternalStorageDirectory().toString() + "/user_slot_6.dump";

    private String[] mPreloadedPath;
    private int mPreloadedPathCount;
    private Context mContext;

    private boolean mPMPathAvailable = false;
    private Method mRunCmdMethod;
    private boolean mHacked = false;
    private boolean mHackConnected = false;
    private IBinder mHackBinder;
    private String mSSPackageName, mSSServiceName, mSSInterfaceName;
    private int mSSCode = 0;

    private int mWaitTransactTime = 150;

    private ArrayList<GameDeviceHWEventListener> mVibratorEventListenerList;

    /**
     * init for AndroidInternal device
     *
     * @param objects The object array for AndroidInternal requires the following sequence.
     *                objects[0]: Must be Context send from Activity or Service.
     *                objects[1]: Must be a Map<String, String> for HackSS initialization
     *                            the key should contains the following
     *                            {packageName, serviceName, interfaceName, code}.
     * @return 0 upon success
     */
    @Override
    public int init(Object[] objects) {
        int ret;

        if (objects.length != 2) {
            Log.e(TAG, "Initial for " + DEVICE_NAME + " error: should include 2 objects");
            return -1;
        }

        /* Android Context initial */
        if (objects[0] instanceof Context) {
            mContext = (Context) objects[0];
        } else {
            Log.e(TAG, "Initial for " + DEVICE_NAME + " error: the 1st object should be a Context");
            return -2;
        }

        /* HackSS initial */
        if (objects[1] instanceof Map) {
            Map map = (Map) objects[1];
            String codeString;

            if (map.containsKey("packageName") && map.containsKey("serviceName") &&
                    map.containsKey("interfaceName") && map.containsKey("code")) {
                mSSPackageName = (String)map.get("packageName");
                mSSServiceName = (String)map.get("serviceName");
                mSSInterfaceName = (String)map.get("interfaceName");
                codeString = (String)map.get("code");

                try {
                    mSSCode = Integer.parseInt(codeString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Initial for " + DEVICE_NAME + " error: SSHack params code is not valid " + codeString);
                    mSSCode = 0;
                    return -3;
                }
            } else {
                Log.e(TAG, "Initial for " + DEVICE_NAME + " error: illegal hack SS parameters");
                return -3;
            }
        } else {
            Log.e(TAG, "Initial for " + DEVICE_NAME + " error: the 2nd object should be a Map<String,String>");
            return -4;
        }

        /* initial for command proxy, we have PMPath and HackSS two ways to send command */
        ret = initCmdProxy();
        if (ret < 0) {
            Log.e(TAG, "Initial for " + DEVICE_NAME + " error: command proxy init failed with " + ret);
            return -3;
        }

        ret = initDeviceHWInterface();
        if (ret < 0) {
            Log.e(TAG, "Initial for " + DEVICE_NAME + " error: device hw interface init failed with " + ret);
            return -5;
        }

        mPreloadedPath = new String[] {
                PRELOAD_PATH_USER_SLOT_0,
                PRELOAD_PATH_USER_SLOT_1,
                PRELOAD_PATH_USER_SLOT_2,
                PRELOAD_PATH_USER_SLOT_3,
                PRELOAD_PATH_USER_SLOT_4,
                PRELOAD_PATH_USER_SLOT_5,
                PRELOAD_PATH_USER_SLOT_6,
                //PRELOAD_PATH_INTERNAL,   //internal path is deprecated
                //PRELOAD_PATH_FIND_COLOR, //find color path is deprecated
        };
        mPreloadedPathCount = mPreloadedPath.length;

        /* initial for this device is fully done, calling super's init */
        ret = super.init(DEVICE_NAME, this);

        if (ret != 0) {
            mInitialized = false;
            return ret;
        }

        mInitialized = true;
        return ret;
    }

    private int initCmdProxy() {
        int ret = 0;

        try {
            Class<?>[] run_types = new Class[]{String.class, String.class};
            mRunCmdMethod = mContext.getPackageManager().getClass().getMethod("joshCmd", run_types);
            mPMPathAvailable = true;
            return 0;
        } catch (NoSuchMethodException e) {
            mPMPathAvailable = false;
            Log.w(TAG, "Sorry, your device is not support PackageManager command runner. Fix your sw or try HackBinder.");
        }

        // try to use binder connection
        // from here, the hackSS parameters should be ready
        if (!mPMPathAvailable) {
            Log.d(TAG, "Try to set HackSS true");
            ret = setHackSS(true);
        }

        return ret;
    }

    private int initDeviceHWInterface() {
        // currently we only support vibrator
        mVibratorEventListenerList = new ArrayList<>();
        return 0;
    }

    @Override
    public int[] getScreenDimension() {
        String wmResult = runShellCommand("wm size");
        String[] wmSize = wmResult.split(":");

        if (wmSize.length == 2) {
            String sizeString = wmSize[1];
            String[] sizeXY = sizeString.split("x");

            if (sizeXY.length == 2) {
                if (sizeXY[0].startsWith(" "))
                    sizeXY[0] = sizeXY[0].substring(1);

                try {
                    int[] ret = new int[2];
                    ret[0] = Integer.parseInt(sizeXY[0]);
                    ret[1] = Integer.parseInt(sizeXY[1]);

                    return ret;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Parse size error " + sizeString);
                }
            }
        }

        Log.e(TAG, "wm size returned error " + wmResult);
        return null;
    }

    @Override
    public int getScreenMainOrientation() {
        // This simulates an Android phone device, the real orientation will be override by user
        return ScreenPoint.SO_Portrait;
    }

    @Override
    public boolean getInitialized() {
        if (!mInitialized)
            return false;

        if (mPMPathAvailable)
            return true;

        return mHacked && mHackConnected;
    }

    /*
     * Implement of IGameDevice
     */
    @Override
    public String[] queryPreloadedPaths() {
        return mPreloadedPath;
    }

    @Override
    public int queryPreloadedPathCount() {
        return mPreloadedPathCount;
    }

    @Override
    public String getVersion() {
        return DEVICE_VERSION;
    }

    @Override
    public int getSystemType() {
        return DEVICE_SYS_TYPE;
    }

    @Override
    public int getWaitTransactionTimeMs() {
        return mWaitTransactTime;
    }

    @Override
    public void setWaitTransactionTimeMsOverride(int ms) {
        mWaitTransactTime = ms;
    }

    @Override
    public int dumpScreen(String path) {
        return runCommand("screencap " + path);
    }

    @Override
    public int mouseEvent(int x, int y, int tx, int ty, int event) {
        if (event < 0 || event >= MOUSE_EVENT_MAX) {
            throw new IllegalArgumentException("Unknown mouse event " + event);
        }

        switch (event) {
            case MOUSE_TAP:
                runCommand("input tap " + x + " " + y);
                break;
            case MOUSE_DOUBLE_TAP:
                runCommand("input tap " + x + " " + y);
                runCommand("input tap " + x + " " + y);
                break;
            case MOUSE_TRIPLE_TAP:
                runCommand("input tap " + x + " " + y);
                runCommand("input tap " + x + " " + y);
                runCommand("input tap " + x + " " + y);
                break;
            case MOUSE_PRESS:
                break;
            case MOUSE_RELEASE:
                break;
            case MOUSE_MOVE_TO:
                break;
            case MOUSE_SWIPE:
                runCommand("input swipe " + x + " " + y + " " + tx + " " + ty);
                break;
            default: //should not happen
                break;
        }

        return 0;
    }

    @Override
    public int registerVibratorEvent(GameDeviceHWEventListener el) {
        //TODO: implement needed for reading path
        //      /sys/devices/platform/soc/c440000.qcom,spmi/spmi-0/spmi0-03/c440000.qcom,spmi:qcom,pm8150b@3:qcom,haptics@c000/state
        if (mVibratorEventListenerList != null) {
            if (!mVibratorEventListenerList.contains(el)) {
                mVibratorEventListenerList.add(el);
            } else {
                Log.w(TAG, "This listener is already in list");
            }
        } else {
            Log.e(TAG, "register vibrator event failed, event listener is null");
            return -1;
        }
        return 0;
    }

    @Override
    public int deregisterVibratorEvent(GameDeviceHWEventListener el) {
        mVibratorEventListenerList.remove(el);
        return 0;
    }

    @Override
    public String runShellCommand(String shellCmd) {
        String[] cmd = {"/system/bin/sh", "-c", shellCmd};
        StringBuilder sb = new StringBuilder();

        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            // append newline at each readLine
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            // delete the last newline for consistent
            if (sb.length() > 1)
                sb.deleteCharAt(sb.length() - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    @Override
    public int runCommand(String cmd) {
        try {
            if (!mHacked && mPMPathAvailable) {
                if (mInitialized) {
                    mRunCmdMethod.invoke(mContext, cmd, "");
                } else {
                    return -100;
                }
            } else if (mHacked && mHackConnected) {
                if (mHackBinder != null) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    if (mSSInterfaceName != null && !mSSInterfaceName.equals(""))
                        data.writeInterfaceToken(mSSPackageName + mSSInterfaceName);
                    data.writeString(cmd);
                    try {
                        mHackBinder.transact(mSSCode, data, reply, 0);
                    } catch (RemoteException e) {
                        Log.w(TAG, "transact failed " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    return -200;
                }
            } else {
                return -1;
            }
        } catch (Exception e)  {
            e.printStackTrace();
            return -2;
        }

        return 0;
    }

    @Override
    public int onStart() {
        return 0;
    }

    @Override
    public int onExit() {
        setHackSS(false);
        return 0;
    }

    @Override
    public void logDevice(int level, String tag, String msg) {
        switch(level) {
            case LOG_VERBOSE:
                android.util.Log.v(tag, msg);
                break;
            case LOG_DEBUG:
                android.util.Log.d(tag, msg);
                break;
            case LOG_WARNING:
                android.util.Log.w(tag, msg);
                break;
            case LOG_ERROR:
                android.util.Log.e(tag, msg);
                break;
            case LOG_FATAL:
                android.util.Log.wtf(tag, msg);
                break;
            default:
                android.util.Log.i(tag, msg);
                break;
        }
    }

    /*
     * HackSS basic
     */
    private int setHackSS(boolean hack) {
        int ret = 0;
        mHacked = hack;
        if (mHacked && !mHackConnected)
            ret = connectToHackSS();
        else if (!mHacked && mHackConnected)
            ret = disconnectToHackSS();

        return ret;
    }

    synchronized private int connectToHackSS() {
        if (!mHackConnected) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(mSSPackageName,
                        mSSPackageName + mSSServiceName));
                if (!mContext.bindService(intent, this, Service.BIND_AUTO_CREATE)) {
                    Log.w(TAG, "Cannot hack this device, parameter wrong or you don't have a implemented service");
                    mHackConnected = false;
                }
            } catch (SecurityException e) {
                Log.e(TAG, "can't bind to Service. Your service is not implemented correctly");
                return -1;
            }
            mHackConnected = true;
        } else {
            Log.d(TAG, "Hack service is already connected");
        }
        return 0;
    }

    synchronized private int disconnectToHackSS() {
        if (mHackConnected)
            mContext.unbindService(this);

        return 0;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mHackConnected = true;
        mHackBinder = service;
        Log.d(TAG, "Hack service connected.");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mHackConnected = false;
        Log.d(TAG, "Hack service disconnected.");
    }
}
