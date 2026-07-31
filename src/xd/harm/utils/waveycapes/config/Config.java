package xd.harm.utils.waveycapes.config;


import xd.harm.utils.waveycapes.CapeMovement;
import xd.harm.utils.waveycapes.CapeStyle;
import xd.harm.utils.waveycapes.WindMode;

public class Config {

    public WindMode windMode = WindMode.WAVES;
    public CapeStyle capeStyle = CapeStyle.SMOOTH;
    public CapeMovement capeMovement = CapeMovement.BASIC_SIMULATION;
    public int gravity = 25;
    public int heightMultiplier = 5;
    public int strafeMultiplier = 3;

}