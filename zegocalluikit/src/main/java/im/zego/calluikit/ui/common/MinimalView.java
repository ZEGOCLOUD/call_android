package im.zego.calluikit.ui.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import com.blankj.utilcode.util.ActivityUtils;
import com.jeremyliao.liveeventbus.LiveEventBus;
import im.zego.callsdk.core.manager.ZegoServiceManager;
import im.zego.callsdk.model.ZegoUserInfo;
import im.zego.callsdk.utils.ZegoCallHelper;
import im.zego.calluikit.R;
import im.zego.calluikit.constant.Constants;
import im.zego.calluikit.databinding.LayoutMinimalViewBinding;
import im.zego.calluikit.ui.call.CallActivity;
import im.zego.calluikit.ui.call.CallStateManager;
import java.util.Objects;

public class MinimalView extends ConstraintLayout {

    private LayoutMinimalViewBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Observer<String> timerObserver = s -> {
        if (isShowMinimal) {
            binding.voiceTv.setText(s);
        }
    };

    private ZegoUserInfo remoteUserInfo;
    private MinimalStatus currentStatus;
    public static boolean isShowMinimal;
    private boolean isShowVideo;
    private Boolean isClickable = true;

    public MinimalView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public MinimalView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MinimalView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public MinimalView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
        int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        binding = LayoutMinimalViewBinding.inflate(LayoutInflater.from(context), this, true);
        binding.voiceTouchView.setOnClickListener(v -> {
            if (!isClickable) {
                return;
            }
            LiveEventBus.get(Constants.EVENT_MINIMAL, Boolean.class).post(false);
            ActivityUtils.startActivity(CallActivity.class);
        });
        binding.videoTouchView.setOnClickListener(v -> {
            if (!isClickable) {
                return;
            }
            LiveEventBus.get(Constants.EVENT_MINIMAL, Boolean.class).post(false);
            ActivityUtils.startActivity(CallActivity.class);
        });
        LiveEventBus
            .get(Constants.EVENT_MINIMAL_CLICKABLE, Boolean.class)
            .observeForever(clickable -> isClickable = clickable);
        updateStatus(MinimalStatus.Initialized);
    }

    public void updateStatus() {
        binding.voiceTv.setText("");
        updateStatus(currentStatus);
    }

    public void updateStatus(MinimalStatus next) {
        currentStatus = next;

        if (!isShowMinimal) {
            toggleVoice(false);
            toggleVideo(false);
            return;
        }

        if (isVideoCall() && remoteUserInfo != null) {
            for (ZegoUserInfo zegoUserInfo : ZegoServiceManager.getInstance().userService.userInfoList) {
                if (Objects.equals(zegoUserInfo, remoteUserInfo)) {
                    remoteUserInfo = zegoUserInfo;
                    break;
                }
            }

            ZegoUserInfo localUserInfo = ZegoServiceManager.getInstance().userService.getLocalUserInfo();

            if (CallStateManager.getInstance().isOutgoing() && localUserInfo.camera) {
                ZegoServiceManager.getInstance().streamService.startPreview(binding.videoTextureView);
                toggleVideo(true);
            } else if (CallStateManager.getInstance().isConnected()) {
                if (remoteUserInfo.camera || localUserInfo.camera) {
                    String userID = remoteUserInfo.camera ? remoteUserInfo.userID : localUserInfo.userID;
                    if (remoteUserInfo.camera) {
                        ZegoServiceManager.getInstance().streamService.startPlaying(userID, binding.videoTextureView);
                    } else {
                        ZegoServiceManager.getInstance().streamService.startPreview(binding.videoTextureView);
                    }
                    toggleVideo(true);
                } else {
                    toggleVideo(false);
                }
            } else {
                toggleVideo(false);
            }
        }

        toggleVoice(true);

        switch (next) {
            case Calling:
                binding.voiceTv.setText(R.string.call_page_status_calling);
                break;
            case Connected:
                LiveEventBus
                    .get(Constants.EVENT_TIMER_CHANGE_KEY, String.class)
                    .observeForever(timerObserver);
                break;
            case Cancel:
                delayDismiss();
                binding.voiceTv.setText(R.string.call_page_status_canceled);
                break;
            case Decline:
                delayDismiss();
                binding.voiceTv.setText(R.string.call_page_status_declined);
                break;
            case Missed:
                delayDismiss();
                binding.voiceTv.setText(R.string.call_page_status_missed);
                break;
            case Ended:
                delayDismiss();
                binding.voiceTv.setText(R.string.call_page_status_ended);
                break;
            case Initialized:
            default:
                isShowMinimal = false;
                toggleVoice(false);
                toggleVideo(false);
                break;
        }

        binding.videoTouchView.setVisibility(isShowVideo ? VISIBLE : GONE);
        binding.layoutVideoTextureView.setVisibility(isShowVideo ? VISIBLE : GONE);
    }

    private void delayDismiss() {
        isShowMinimal = false;
        LiveEventBus.get(Constants.EVENT_TIMER_CHANGE_KEY, String.class).removeObserver(timerObserver);
        if (binding.layoutVideoTextureView.getVisibility() == VISIBLE) {
            toggleVoice(false);
            toggleVideo(false);
        } else {
            handler.postDelayed(() -> {
                toggleVoice(false);
                toggleVideo(false);
            }, 1000L);
        }
    }

    private void toggleVoice(boolean show) {
        binding.voiceTouchView.setVisibility(show ? VISIBLE : GONE);
        binding.voiceBg.setVisibility(show ? VISIBLE : GONE);
        binding.voiceTv.setVisibility(show ? VISIBLE : GONE);
        binding.voiceIv.setVisibility(show ? VISIBLE : GONE);
    }

    private void toggleVideo(boolean show) {
        isShowVideo = show;
        binding.videoTouchView.setVisibility(show ? VISIBLE : GONE);
        binding.layoutVideoTextureView.setVisibility(show ? VISIBLE : GONE);
    }

    private boolean isVideoCall() {
        int callState = CallStateManager.getInstance().getCallState();
        return callState == CallStateManager.TYPE_OUTGOING_CALLING_VIDEO
            || callState == CallStateManager.TYPE_CONNECTED_VIDEO;
    }

    public void onUserInfoUpdated(ZegoUserInfo userInfo) {
        updateRemoteUserInfo(userInfo);
        updateStatus(currentStatus);
    }

    public void updateRemoteUserInfo(ZegoUserInfo userInfo) {
        if (userInfo == null || ZegoCallHelper.isUserIDSelf(userInfo.userID)) {
            return;
        }

        for (ZegoUserInfo zegoUserInfo : ZegoServiceManager.getInstance().userService.userInfoList) {
            if (Objects.equals(zegoUserInfo, userInfo)) {
                userInfo = zegoUserInfo;
                break;
            }
        }
        remoteUserInfo = userInfo;
    }

    public void reset() {
        remoteUserInfo = null;
    }
}
