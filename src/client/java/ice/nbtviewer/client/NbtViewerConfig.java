package ice.nbtviewer.client;

/** Runtime configuration shared by the client entrypoint and the YACL screen. */
public final class NbtViewerConfig {
    private static boolean enabled = true;
    private static boolean showItemTooltip = true;

    private NbtViewerConfig() {
    }

    public static boolean showItemTooltip() {
        return showItemTooltip;
    }

    public static boolean enabled() {
        return enabled;
    }
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void setShowItemTooltip(boolean value) {
        showItemTooltip = value;
    }
}
