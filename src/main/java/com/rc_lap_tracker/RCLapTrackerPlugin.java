package com.rc_lap_tracker;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.config.ConfigManager;

import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import net.runelite.api.events.AnimationChanged;


@Slf4j
@PluginDescriptor(
		name = "RC Lap Tracker",
		description = "Shows you how many laps until your largest pouch degrades",
		tags = {"rc", "runecraft", "rune", "lap", "tracker", "pouch", "essence", "zmi"}
)
public class RCLapTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private RCLapTrackerConfig config;
	@Inject
	private ItemManager itemManager;

	@Provides
	RCLapTrackerConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RCLapTrackerConfig.class);
	}
	private jwowWriteableCounter counterBox;

	private long lastAction;
	private int target;
	private int cycle;
	private boolean hasCrafted;
	private boolean isMidRun;

	private boolean isActive;
	private static final int SPELL_CONTACT_ANIMATION_ID = 4413;
	private static final int CRAFT_RUNES_ANIMATION_ID = 791;

	private int getIntConfig(String key){
		Integer value = configManager.getRSProfileConfiguration(RCLapTrackerConfig.GROUP_NAME, key, int.class);
		return value == null ? -1 : value;
	}

	private boolean getBooleanConfig(String key){
		Boolean value = configManager.getRSProfileConfiguration(RCLapTrackerConfig.GROUP_NAME, key, boolean.class);
		return value != null && value;
	}

	private void setConfig(String key, Object value){
		configManager.setRSProfileConfiguration(RCLapTrackerConfig.GROUP_NAME, key, value);
	}

	@Override
	protected void startUp(){
		target = config.highestPouch().getTarget();
		cycle = getIntConfig(RCLapTrackerConfig.CYCLE_KEY);
		if (cycle == -1)
			cycle = target;

		hasCrafted = getBooleanConfig(RCLapTrackerConfig.HASCRAFTED_KEY);
		isMidRun = getBooleanConfig(RCLapTrackerConfig.ISMIDRUN_KEY);
		counterBox = null;
		lastAction = System.currentTimeMillis();

		updateInfoBox();
	}
	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean wasActive = isActive;

		if (System.currentTimeMillis() - lastAction < 300000)
			isActive = true;
		else
			isActive = false;

		if (wasActive != isActive)
			updateInfoBox();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged ev)
	{
		int oldTarget = target;
		target = config.highestPouch().getTarget();
		if (oldTarget != target && !isMidRun) {
			cycle = target;
			updateInfoBox();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		setConfig(RCLapTrackerConfig.CYCLE_KEY, cycle);
		setConfig(RCLapTrackerConfig.HASCRAFTED_KEY, hasCrafted);
		setConfig(RCLapTrackerConfig.ISMIDRUN_KEY, isMidRun);

		removeInfobox();
	}

	private void removeInfobox()
	{
		if (counterBox != null) {
			infoBoxManager.removeInfoBox(counterBox);
		}
		counterBox = null;
	}

	private void updateInfoBox()
	{
		if (!isActive)
		{
			removeInfobox();
			return;
		}

		if (counterBox == null)
		{
			final BufferedImage image = itemManager.getImage(ItemID.WRATH_RUNE, 1, false);
			counterBox = new jwowWriteableCounter(image, this, cycle);
			infoBoxManager.addInfoBox(counterBox);
		}

		counterBox.count = cycle;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}

		String playerName = client.getLocalPlayer().getName();
		String actorName = event.getActor().getName();

		if (!playerName.equals(actorName)) {
			return;
		}

		int animId = event.getActor().getAnimation();
		if (animId == SPELL_CONTACT_ANIMATION_ID) {
			cycle = target;
			isMidRun = false;
			lastAction = System.currentTimeMillis();
			updateInfoBox();
		}
		else if (!hasCrafted && animId == CRAFT_RUNES_ANIMATION_ID) {
			hasCrafted = true;
			isMidRun = true;
			lastAction = System.currentTimeMillis();
			if (cycle > 0) {
				cycle--;
			}
			updateInfoBox();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();

		if (containerId == InventoryID.BANK.getId())
		{
			hasCrafted = false;
		}
	}
}
