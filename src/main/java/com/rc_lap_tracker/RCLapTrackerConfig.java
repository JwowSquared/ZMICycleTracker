package com.rc_lap_tracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RCLapTrackerConfig.GROUP_NAME)
public interface RCLapTrackerConfig extends Config
{
	String GROUP_NAME = "rc-lap-tracker";
	String CYCLE_KEY = "cycle";
	String HASCRAFTED_KEY = "hascrafted";
	String ISMIDRUN_KEY = "ismidrun";
}
