package xd.harm.utils.waveycapes;

import xd.harm.utils.waveycapes.config.*;

public class WaveyCapesBase {
    public static WaveyCapesBase INSTANCE;
    public static Config config;

    public void init() {
        INSTANCE = this;
        config = new Config();
    }
}
