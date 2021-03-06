package im.zego.callsdk.core.interfaces;

import im.zego.callsdk.callback.ZegoCallback;
import im.zego.callsdk.listener.ZegoCallServiceListener;
import im.zego.callsdk.model.ZegoCallInfo;
import im.zego.callsdk.model.ZegoCallType;
import im.zego.callsdk.model.ZegoLocalUserStatus;
import im.zego.callsdk.model.ZegoUserInfo;

public abstract class ZegoCallService {

    private static final String TAG = "ZegoCallService";

    // callService refers to the listener instance of call service.
    protected ZegoCallServiceListener listener;
    // The status of a local user.
    protected ZegoLocalUserStatus status = ZegoLocalUserStatus.Free;
    // The call information.
    protected ZegoCallInfo callInfo = new ZegoCallInfo();

    /**
     * Make an outbound call
     * <p>
     * Description: This method can be used to initiate a call to a online user. The called user receives a notification
     * once this method gets called. And if the call is not answered in 60 seconds, you will need to call a method to
     * cancel the call.
     * <p>
     * Call this method at: After the user login
     *
     * @param userInfo         refers to the ID of the user you want call.
     * @param callType         refers to the call type.  ZegoCallTypeVoice: Voice call.  ZegoCallTypeVideo: Video call.
     * @param createRoomToken: refers to the authentication token. To get this, see the documentation:
     *                         https://docs.zegocloud.com/article/11648
     * @param callback         refers to the callback for make a outbound call.
     */
    public abstract void callUser(ZegoUserInfo userInfo, ZegoCallType callType, String createRoomToken,
        ZegoCallback callback);

    /**
     * Cancel a call
     * <p>
     * Description: This method can be used to cancel a call. And the called user receives a notification through
     * callback that the call has been canceled.
     * <p>
     * Call this method at: After the user login
     *
     * @param callback: refers to the callback for cancel a call.
     */
    public abstract void cancelCall(ZegoCallback callback);

    /**
     * Accept a call
     * <p>
     * Description: This method can be used to accept a call. And the caller receives a callback when the call has been
     * accepted by the callee.
     * <p>
     * Call this method at: After the user login
     *
     * @param callback refers to the callback for accept a call.
     */
    public abstract void acceptCall(String joinToken, ZegoCallback callback);

    /**
     * Decline a call
     * <p>
     * Description: This method can be used to decline a call. And the caller receives a callback when the call has been
     * declined by the callee.
     * <p>
     * Call this method at: after the user login
     *
     * @param callback refers to the callback for decline a call.
     */
    public abstract void declineCall(ZegoCallback callback);

    /**
     * End a call
     * <p>
     * Description: This method can be used to end a call. After the call is ended, both the caller and called user will
     * be logged out from the room, and the stream publishing and playing stop upon ending.
     * <p>
     * Call this method at: After the user login
     *
     * @param callback refers to the callback for end a call.
     */
    public abstract void endCall(ZegoCallback callback);

    public abstract void setCallInfo(ZegoCallInfo callInfo);

    public ZegoCallInfo getCallInfo() {
        return callInfo;
    }

    public void setListener(ZegoCallServiceListener listener) {
        this.listener = listener;
    }

    public ZegoCallServiceListener getListener() {
        return listener;
    }
}
