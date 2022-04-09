package im.zego.callsdk.core.manager;

import android.app.Application;
import android.util.Log;

import com.google.gson.Gson;

import im.zego.callsdk.command.ZegoCommandManager;
import im.zego.callsdk.listener.ZegoCallingState;
import im.zego.zegoexpress.entity.ZegoUser;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import im.zego.callsdk.callback.ZegoCallback;
import im.zego.callsdk.core.interfaceimpl.ZegoCallServiceImpl;
import im.zego.callsdk.core.interfaceimpl.ZegoDeviceServiceImpl;
import im.zego.callsdk.core.interfaceimpl.ZegoRoomServiceImpl;
import im.zego.callsdk.core.interfaceimpl.ZegoStreamServiceImpl;
import im.zego.callsdk.core.interfaceimpl.ZegoUserServiceImpl;
import im.zego.callsdk.core.interfaces.ZegoCallService;
import im.zego.callsdk.core.interfaces.ZegoDeviceService;
import im.zego.callsdk.core.interfaces.ZegoRoomService;
import im.zego.callsdk.core.interfaces.ZegoStreamService;
import im.zego.callsdk.core.interfaces.ZegoUserService;
import im.zego.callsdk.listener.ZegoDeviceServiceListener;
import im.zego.callsdk.model.ZegoNetWorkQuality;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoAudioRoute;
import im.zego.zegoexpress.constants.ZegoRemoteDeviceState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoStreamQualityLevel;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoEngineProfile;
import im.zego.zegoexpress.entity.ZegoStream;

/**
 * Class LiveAudioRoom business logic management.
 * <p> Description: This class contains the LiveAudioRoom business logics, manages the service instances of different
 * modules, and also distributing the data delivered by the SDK.
 */
public class ZegoServiceManager {

    private static volatile ZegoServiceManager singleton = null;

    private ZegoServiceManager() {
    }

    /**
     * Get the ZegoRoomManager singleton instance.
     * <p> Description: This method can be used to get the ZegoRoomManager singleton instance.
     * <p> Call this method at: Any time
     *
     * @return
     */
    public static ZegoServiceManager getInstance() {
        if (singleton == null) {
            synchronized (ZegoServiceManager.class) {
                if (singleton == null) {
                    singleton = new ZegoServiceManager();
                }
            }
        }
        return singleton;
    }

    /**
     * The user information management instance, contains the in-room user information management, logged-in user
     * information and other business logics.
     */
    public ZegoUserService userService;
    public ZegoCallService callService;
    public ZegoRoomService roomService;
    public ZegoDeviceService deviceService;
    public ZegoStreamService streamService;
    public Gson mGson = new Gson();
    private static final String TAG = "ZegoService";

    private static final int MIC = 0x01;
    private static final int CAMERA = 0x02;

    /**
     * Initialize the SDK.
     * <p>Call this method at: Before you log in. We recommend you call this method when the application starts.
     *
     * @param appID       refers to the project ID. To get this, go to ZEGOCLOUD Admin Console:
     *                    https://console.zego.im/dashboard?lang=en
     * @param application th app context
     */
    public void init(long appID, Application application) {
        userService = new ZegoUserServiceImpl();
        callService = new ZegoCallServiceImpl();
        roomService = new ZegoRoomServiceImpl();
        deviceService = new ZegoDeviceServiceImpl();
        streamService = new ZegoStreamServiceImpl();

        ZegoCommandManager.getInstance();

        ZegoEngineProfile profile = new ZegoEngineProfile();
        profile.appID = appID;
        profile.scenario = ZegoScenario.COMMUNICATION;
        profile.application = application;
        ZegoEngineConfig config = new ZegoEngineConfig();
        config.advancedConfig.put("room_retry_time", "60");
        ZegoExpressEngine.setEngineConfig(config);
        ZegoExpressEngine.createEngine(profile, new IZegoEventHandler() {
            @Override
            public void onNetworkQuality(String userID, ZegoStreamQualityLevel upstreamQuality,
                ZegoStreamQualityLevel downstreamQuality) {
                super.onNetworkQuality(userID, upstreamQuality, downstreamQuality);
                Log.d(TAG,
                    "onNetworkQuality() called with: userID = [" + userID + "], upstreamQuality = [" + upstreamQuality
                        + "], downstreamQuality = [" + downstreamQuality + "]");
                if (userService.listener != null) {
                    if (upstreamQuality == ZegoStreamQualityLevel.BAD
                        || upstreamQuality == ZegoStreamQualityLevel.DIE
                        || upstreamQuality == ZegoStreamQualityLevel.UNKNOWN) {
                        userService.listener.onNetworkQuality(userID, ZegoNetWorkQuality.Bad);
                    } else if (upstreamQuality == ZegoStreamQualityLevel.MEDIUM) {
                        userService.listener.onNetworkQuality(userID, ZegoNetWorkQuality.Medium);
                    } else {
                        userService.listener.onNetworkQuality(userID, ZegoNetWorkQuality.Good);
                    }
                }
            }

            @Override
            public void onCapturedSoundLevelUpdate(float soundLevel) {
                super.onCapturedSoundLevelUpdate(soundLevel);

            }


            @Override
            public void onRemoteSoundLevelUpdate(HashMap<String, Float> soundLevels) {
                super.onRemoteSoundLevelUpdate(soundLevels);

            }

            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList,
                JSONObject extendedData) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData);
                for (ZegoStream zegoStream : streamList) {
                    Log.d(TAG, "onRoomStreamUpdate: " + zegoStream.streamID + ",updateType:" + updateType);
                }
            }

            @Override
            public void onAudioRouteChange(ZegoAudioRoute audioRoute) {
                super.onAudioRouteChange(audioRoute);
                for (ZegoDeviceServiceListener listener : deviceService.listeners) {
                    listener.onAudioRouteChange(audioRoute);
                }
            }

            @Override
            public void onRemoteMicStateUpdate(String streamID, ZegoRemoteDeviceState state) {
                super.onRemoteMicStateUpdate(streamID, state);
                userService.onRemoteMicStateUpdate(streamID, state);
            }

            @Override
            public void onRemoteCameraStateUpdate(String streamID, ZegoRemoteDeviceState state) {
                super.onRemoteCameraStateUpdate(streamID, state);
                userService.onRemoteCameraStateUpdate(streamID, state);
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                super.onRoomStateUpdate(roomID, state, errorCode, extendedData);
                Log.d(TAG,
                    "onRoomStateUpdate() called with: roomID = [" + roomID + "], state = [" + state + "], errorCode = ["
                        + errorCode + "], extendedData = [" + extendedData + "]");
                if (callService instanceof ZegoCallServiceImpl) {
                    ((ZegoCallServiceImpl) callService).onRoomStateUpdate(roomID, state, errorCode, extendedData);
                }
                ZegoCallingState callingState = ZegoCallingState.getCallingState(state.value());
                if (callService.getListener() != null) {
                    callService.getListener().onCallingStateUpdated(callingState);
                }
            }

            @Override
            public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                super.onRoomUserUpdate(roomID, updateType, userList);
                if (userService instanceof ZegoUserServiceImpl) {
                    ((ZegoUserServiceImpl) userService).onRoomUserUpdate(roomID, updateType, userList);
                }
            }
        });

        deviceService.setBestConfig();
    }

    /**
     * The method to deinitialize the SDK.
     * <p> Description: This method can be used to deinitialize the SDK and release the resources it occupies.</>
     * <p> Call this method at: When the SDK is no longer be used. We recommend you call this method when the
     * application exits.</>
     */
    public void unInit() {
        ZegoExpressEngine.destroyEngine(null);
    }

    /**
     * Upload local logs to the ZEGOCLOUD server.
     * <p>Description: You can call this method to upload the local logs to the ZEGOCLOUD Server for troubleshooting
     * when exception occurs.</>
     * <p>Call this method at: When exceptions occur </>
     *
     * @param callback refers to the callback that be triggered when the logs are upload successfully or failed to
     *                 upload logs.
     */
    public void uploadLog(final ZegoCallback callback) {
        ZegoExpressEngine.getEngine().uploadLog();
        if (callback != null) {
            callback.onResult(0);
        }
    }
}
