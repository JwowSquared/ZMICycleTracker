package com.rc_lap_tracker;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
	name = "RC Lap Tracker"
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

	private Counter counterBox = null;

	private int target;
	private int cycle;
	private boolean hasCrafted;
	private boolean isMidRun;
	private boolean inGuardiansOfTheRift;

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

		updateInfoBox();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged ev)
	{
		target = config.highestPouch().getTarget();
		if (!isMidRun) {
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

		infoBoxManager.removeInfoBox(counterBox);
		counterBox = null;
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
		removeInfobox();
		final BufferedImage image = itemManager.getImage(ItemID.WRATH_RUNE, 1, false);
		counterBox = new Counter(image, this, cycle);
		infoBoxManager.addInfoBox(counterBox);
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
			updateInfoBox();
		}
		else if (!hasCrafted && animId == CRAFT_RUNES_ANIMATION_ID) {
			hasCrafted = true;
			isMidRun = true;
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

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null) {
			if (inventory.contains(ItemID.CATALYTIC_GUARDIAN_STONE) || inventory.contains(ItemID.ELEMENTAL_GUARDIAN_STONE)) {
				inGuardiansOfTheRift = true;
			}
			else if (inGuardiansOfTheRift) {
				hasCrafted = false;
				inGuardiansOfTheRift = false;
			}
		}

		if (containerId == InventoryID.BANK.getId())
		{
			hasCrafted = false;
			inGuardiansOfTheRift = false;
		}
	}
}
