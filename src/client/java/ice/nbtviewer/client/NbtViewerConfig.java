package ice.nbtviewer.client;

/** Runtime configuration shared by the client entrypoint and the YACL screen. */
public final class NbtViewerConfig {
    private static boolean showItemTooltip = true;

    private NbtViewerConfig() {
    }

    public static boolean showItemTooltip() {
        return showItemTooltip;
    }

    public static void setShowItemTooltip(boolean value) {
        showItemTooltip = value;
    }
}
