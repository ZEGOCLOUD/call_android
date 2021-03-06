package im.zego.callsdk.model;

public enum ZegoCallingState {
    DISCONNECTED(0),
    CONNECTING(1),
    CONNECTED(2);

    private int value;

    ZegoCallingState(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static ZegoCallingState getCallingState(int value) {
        try {
            if (DISCONNECTED.value == value) {
                return DISCONNECTED;
            } else if (CONNECTING.value == value) {
                return CONNECTING;
            } else {
                return CONNECTED.value == value ? CONNECTED : null;
            }
        } catch (Exception var2) {
            throw new RuntimeException("The enumeration cannot be found");
        }
    }
}
