package android.os;

public final class ProcessParams {
    public final int[] gids;
    public final boolean isTopApp;
    public final String packageName;
    public final int targetSdkVersion;
    public final int zygotePolicyFlags;

    private ProcessParams(Builder builder) {
        throw new RuntimeException("STUB");
    }

    public static final class Builder {
        public Builder setGids(int[] r1) {
            throw new RuntimeException("STUB");
        }

        public Builder setBindMountAppsData(boolean z) {
            throw new RuntimeException("STUB");
        }

        public Builder setZygotePolicyFlags(int r1) {
            throw new RuntimeException("STUB");
        }

        public ProcessParams build() {
            throw new RuntimeException("STUB");
        }
    }
}
