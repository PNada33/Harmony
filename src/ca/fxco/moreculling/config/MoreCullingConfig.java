package ca.fxco.moreculling.config;

import ca.fxco.moreculling.config.option.LeavesCullingMode;

public final class MoreCullingConfig
{
    public static boolean useBlockStateCulling = true;
    public static LeavesCullingMode leavesCullingMode = LeavesCullingMode.DEFAULT;
    public static int leavesCullingDepth = 2;

    private MoreCullingConfig()
    {
    }
}
