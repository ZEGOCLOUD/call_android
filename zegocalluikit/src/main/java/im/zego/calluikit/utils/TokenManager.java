package im.zego.calluikit.utils;

import android.util.Log;

import com.blankj.utilcode.util.SPStaticUtils;

import java.util.Timer;
import java.util.TimerTask;

import im.zego.callsdk.callback.ZegoRequestCallback;
import im.zego.callsdk.model.ZegoUserInfo;
import im.zego.calluikit.ZegoCallManager;
import im.zego.calluikit.constant.Constants;

public class TokenManager {

    private static volatile TokenManager singleton = null;
    private static final String TAG = "TokenManager";

    private TokenManager() {
        tokenWrapper = getTokenFromDisk();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (needUpdateToken()) {
                    requestRTCToken();
                }
            }
        };
        Timer timer = new Timer();
        // need check more frequently
        timer.schedule(task, 0, 1000);
    }

    private void requestRTCToken() {
        ZegoUserInfo userInfo = ZegoCallManager.getInstance().getLocalUserInfo();
        if (userInfo != null) {
            String userID = userInfo.userID;
            long effectiveTime = 3600;
            ZegoCallManager.getInstance().getToken(userID, effectiveTime, new ZegoRequestCallback() {
                @Override
                public void onResult(int errorCode, Object obj) {
                    Log.d(TAG,
                        "getToken onResult() called with: errorCode = [" + errorCode + "], obj = [" + obj
                            + "]");
                    if (errorCode == 0) {
                        ZegoCallManager.getInstance().setToken((String) obj);
                        saveToken((String) obj, effectiveTime * 1000L);
                    }
                }
            });
        }
    }

    public static TokenManager getInstance() {
        if (singleton == null) {
            synchronized (TokenManager.class) {
                if (singleton == null) {
                    singleton = new TokenManager();
                }
            }
        }
        return singleton;
    }

    private TokenWrapper tokenWrapper;

    public String getToken() {
        String result;
        if (tokenWrapper != null) {
            Log.d(TAG, "getToken() called,isTokenValid: " + tokenWrapper.isTokenValid());
            if (!tokenWrapper.isTokenValid()) {
                reset();
                result = null;
            } else {
                result = tokenWrapper.token;
            }
        } else {
            result = null;
        }
        Log.d(TAG, "getToken() called,result:" + result);
        return result;
    }

    public void reset() {
        saveToken(null, 0);
    }

    private void saveToken(String token, long effectiveTimeInSeconds) {
        if (token == null || effectiveTimeInSeconds == 0) {
            this.tokenWrapper = null;
            SPStaticUtils.remove(Constants.ZEGO_TOKEN_KEY);
            SPStaticUtils.remove(Constants.ZEGO_TOKEN_EXPIRY_TIME_KEY);
        } else {
            long expiryTime = System.currentTimeMillis() + effectiveTimeInSeconds;

            SPStaticUtils.put(Constants.ZEGO_TOKEN_KEY, token);
            SPStaticUtils.put(Constants.ZEGO_TOKEN_EXPIRY_TIME_KEY, expiryTime);

            this.tokenWrapper = new TokenWrapper(token, expiryTime);
        }
    }

    private boolean needUpdateToken() {
        if (tokenWrapper == null) {
            return true;
        }
        return System.currentTimeMillis() > tokenWrapper.expiryTime;
    }

    private TokenWrapper getTokenFromDisk() {
        String token = SPStaticUtils.getString(Constants.ZEGO_TOKEN_KEY);
        long expiryTime = SPStaticUtils.getLong(Constants.ZEGO_TOKEN_EXPIRY_TIME_KEY);

        return new TokenWrapper(token, expiryTime);
    }

    public static class TokenWrapper {

        public String token;
        public long expiryTime;

        TokenWrapper(String token, long expiryTime) {
            this.token = token;
            this.expiryTime = expiryTime;
        }

        boolean isTokenValid() {
            return expiryTime > System.currentTimeMillis();
        }
    }
}
