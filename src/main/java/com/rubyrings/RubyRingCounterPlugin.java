package com.rubyrings;

import com.google.inject.Provides;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Varrock Bank Ruby Rings",
	description = "Shows a box above the inventory with your Ruby ring count, and tracks profit/XP from high"
		+ " alching them in the Varrock west bank basement",
	tags = {"ruby", "ring", "alch", "alchemy", "profit", "crafting", "varrock"}
)
public class RubyRingCounterPlugin extends Plugin
{
	static final int RUBY_RING_ID = ItemID.RUBY_RING;
	private static final int HIGH_ALCHEMY_XP = 65;

	// Varrock west bank basement.
	private static final Set<Integer> ALLOWED_REGIONS = Set.of(12697);

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RubyRingCounterConfig config;

	@Inject
	private RubyRingCounterOverlay overlay;

	@Getter
	private int ringCount;

	@Getter
	private int alchCount;

	@Getter
	private long totalProfit;

	@Getter
	private int totalXpGained;

	private boolean highAlchArmed;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		ringCount = 0;
		highAlchArmed = false;
	}

	void resetStats()
	{
		alchCount = 0;
		totalProfit = 0;
		totalXpGained = 0;
	}

	boolean isInTargetArea()
	{
		if (ALLOWED_REGIONS.isEmpty())
		{
			return true;
		}

		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return false;
		}

		return ALLOWED_REGIONS.contains(local.getWorldLocation().getRegionID());
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		// Don't touch the counters just because the player logged out/hopped worlds - only a
		// genuine departure from the basement while logged in should reset them.
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (!isInTargetArea())
		{
			ringCount = 0;
			resetStats();
			return;
		}

		updateRingCount();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!isInTargetArea())
		{
			highAlchArmed = false;
			return;
		}

		String option = event.getMenuOption();
		String target = event.getMenuTarget();

		// Single-click cast (e.g. shift-click cast bound to the item) - spell name and item arrive together.
		if ("Cast".equals(option) && target != null && target.contains("High Level Alchemy") && event.getItemId() != -1)
		{
			highAlchArmed = false;
			handleHighAlch(event.getItemId());
			return;
		}

		// Spell selected from the spellbook - arm and wait for the item click that follows.
		if ("Cast".equals(option) && "High Level Alchemy".equals(target))
		{
			highAlchArmed = true;
			return;
		}

		if (highAlchArmed)
		{
			highAlchArmed = false;
			if (event.getItemId() != -1)
			{
				handleHighAlch(event.getItemId());
			}
		}
	}

	private void handleHighAlch(int itemId)
	{
		if (itemId != RUBY_RING_ID)
		{
			return;
		}

		int haPrice = itemManager.getItemComposition(RUBY_RING_ID).getHaPrice();
		int profit = haPrice - config.natureRuneCost();

		alchCount++;
		totalProfit += profit;
		totalXpGained += HIGH_ALCHEMY_XP;
	}

	private void updateRingCount()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		int count = 0;

		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() == RUBY_RING_ID)
				{
					count += item.getQuantity();
				}
			}
		}

		ringCount = count;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("rubyringcounter".equals(event.getGroup()) && "overlayPosition".equals(event.getKey()))
		{
			overlay.setPosition(config.overlayPosition().getPosition());
		}
	}

	@Provides
	RubyRingCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RubyRingCounterConfig.class);
	}
}
