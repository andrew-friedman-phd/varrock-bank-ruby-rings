package com.rubyrings;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.cluescrolls.clues.emote.Emote;
import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

@Slf4j
@PluginDescriptor(
	name = "Varrock Bank Ruby Rings",
	description = "Utility for streamlining Varrock Bank heists",
	tags = {"ruby", "ring", "alch", "alchemy", "profit", "loot", "varrock"}
)
public class RubyRingCounterPlugin extends Plugin
{
	static final int RUBY_RING_ID = ItemID.RUBY_RING;
	private static final int HIGH_ALCHEMY_XP = 65;
	private static final int LOW_ALCHEMY_XP = 31;
	private static final double LOW_ALCHEMY_MULTIPLIER = 0.4;

	// Varrock west bank basement.
	private static final Set<Integer> ALLOWED_REGIONS = Set.of(12697);

	// Crack the Clue III gate: the 17-emote sequence and detection approach are adapted from
	// Clank1337's "CrackTheClue3 Emote" plugin (BSD 2-Clause) - credited in the README.
	private static final Set<WorldPoint> EMOTE_GATE_TILES = Set.of(
		new WorldPoint(3191, 9825, 0),
		new WorldPoint(3192, 9825, 0),
		new WorldPoint(3191, 9824, 0),
		new WorldPoint(3192, 9824, 0));
	private static final List<Emote> EMOTE_SEQUENCE = List.of(
		Emote.PANIC, Emote.NO, Emote.BECKON, Emote.LAUGH, Emote.SHRUG, Emote.CRY, Emote.SPIN, Emote.YES,
		Emote.THINK, Emote.DANCE, Emote.BLOW_KISS, Emote.WAVE, Emote.BOW, Emote.PANIC, Emote.HEADBANG,
		Emote.JUMP_FOR_JOY, Emote.ANGRY);
	// Animation id -> which emote it is, so a performed emote can be checked against what's
	// actually expected next, not just "was some emote performed".
	private static final Map<Integer, Emote> ANIMATION_TO_EMOTE = Map.ofEntries(
		Map.entry(2105, Emote.PANIC),
		Map.entry(856, Emote.NO),
		Map.entry(864, Emote.BECKON),
		Map.entry(861, Emote.LAUGH),
		Map.entry(2113, Emote.SHRUG),
		Map.entry(860, Emote.CRY),
		Map.entry(2107, Emote.SPIN),
		Map.entry(855, Emote.YES),
		Map.entry(857, Emote.THINK),
		Map.entry(866, Emote.DANCE),
		Map.entry(1374, Emote.BLOW_KISS),
		Map.entry(863, Emote.WAVE),
		Map.entry(858, Emote.BOW),
		Map.entry(2108, Emote.HEADBANG),
		Map.entry(2109, Emote.JUMP_FOR_JOY),
		Map.entry(859, Emote.ANGRY));
	private static final Duration MISTAKE_DISPLAY_DURATION = Duration.ofSeconds(1);

	private static final Duration WORLD_COOLDOWN = Duration.ofMinutes(5);
	private static final Duration HOP_BUTTON_COOLDOWN = Duration.ofSeconds(2);

	private static final String CONFIG_GROUP = "rubyringcounter";
	private static final String KEY_ALCH_COUNT = "sessionAlchCount";
	private static final String KEY_TOTAL_PROFIT = "sessionTotalProfit";
	private static final String KEY_TOTAL_XP = "sessionTotalXp";
	private static final String KEY_SESSION_START = "sessionStart";
	private static final String KEY_WORLD_COOLDOWNS = "worldCooldowns";
	private static final int MAX_QUICK_HOP_ATTEMPTS = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WorldService worldService;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	@Inject
	private RubyRingCounterConfig config;

	@Inject
	private RubyRingCounterOverlay overlay;

	@Inject
	private EmoteHelperOverlay emoteHelperOverlay;

	@Getter
	private int ringCount;

	@Getter
	private int alchCount;

	@Getter
	private long totalProfit;

	@Getter
	private int totalXpGained;

	@Getter
	private Instant sessionStart;

	@Getter
	private long profitPerHour;

	@Getter
	private int xpPerHour;

	// Which alchemy spell (if any) is currently selected from the spellbook, waiting for the
	// item click that follows it.
	private String armedAlchSpell;

	private Emote mistakeEmote;
	private Instant mistakeTimestamp;

	private final Map<Integer, Instant> worldCooldowns = new HashMap<>();
	private NavigationButton navButton;
	private net.runelite.api.World quickHopTarget;
	private int quickHopAttempts;
	private net.runelite.api.World pendingHopWorld;
	private boolean pendingHopWaitedATick;
	private int emoteIndex;
	private Instant lastHopButtonPress;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(emoteHelperOverlay);

		Integer savedAlchCount = configManager.getConfiguration(CONFIG_GROUP, KEY_ALCH_COUNT, Integer.class);
		Long savedTotalProfit = configManager.getConfiguration(CONFIG_GROUP, KEY_TOTAL_PROFIT, Long.class);
		Integer savedTotalXp = configManager.getConfiguration(CONFIG_GROUP, KEY_TOTAL_XP, Integer.class);
		Long savedSessionStart = configManager.getConfiguration(CONFIG_GROUP, KEY_SESSION_START, Long.class);

		alchCount = savedAlchCount == null ? 0 : savedAlchCount;
		totalProfit = savedTotalProfit == null ? 0 : savedTotalProfit;
		totalXpGained = savedTotalXp == null ? 0 : savedTotalXp;
		sessionStart = (savedSessionStart == null || savedSessionStart < 0) ? null : Instant.ofEpochMilli(savedSessionStart);

		loadCooldowns();

		BufferedImage icon = ImageUtil.loadImageResource(RubyRingCounterPlugin.class, "icon.png");
		RubyRingCounterPanel panel = new RubyRingCounterPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Varrock Bank Ruby Rings")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(emoteHelperOverlay);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		ringCount = 0;
		armedAlchSpell = null;
	}

	void resetStats()
	{
		alchCount = 0;
		totalProfit = 0;
		totalXpGained = 0;
		sessionStart = null;
		profitPerHour = 0;
		xpPerHour = 0;
		persistStats();
	}

	/**
	 * Restarts just the session clock (used for the /hr figures) without touching the totals -
	 * it'll start ticking again from the next ring pickup or alch cast, same as a fresh session.
	 */
	void resetSessionTimer()
	{
		sessionStart = null;
		profitPerHour = 0;
		xpPerHour = 0;
		persistStats();
	}

	private void persistStats()
	{
		configManager.setConfiguration(CONFIG_GROUP, KEY_ALCH_COUNT, alchCount);
		configManager.setConfiguration(CONFIG_GROUP, KEY_TOTAL_PROFIT, totalProfit);
		configManager.setConfiguration(CONFIG_GROUP, KEY_TOTAL_XP, totalXpGained);
		configManager.setConfiguration(CONFIG_GROUP, KEY_SESSION_START, sessionStart == null ? -1L : sessionStart.toEpochMilli());
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

	Map<Integer, Instant> getWorldCooldowns()
	{
		return worldCooldowns;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || event.getActor() != localPlayer || !isAtEmoteGate(localPlayer))
		{
			return;
		}

		Emote performed = ANIMATION_TO_EMOTE.get(localPlayer.getAnimation());
		if (performed == null)
		{
			return;
		}

		if (performed == getCurrentEmote())
		{
			emoteIndex++;
		}
		else
		{
			// Wrong emote resets the sequence in-game too, so mirror that here.
			mistakeEmote = performed;
			mistakeTimestamp = Instant.now();
			emoteIndex = 0;
		}
	}

	boolean isAtEmoteGate(Player player)
	{
		return player.getWorldLocation().getRegionID() == 12697 && EMOTE_GATE_TILES.contains(player.getWorldLocation());
	}

	/**
	 * The emote the player mistakenly performed, if it was within the last second - the overlay
	 * shows a red X over it briefly before falling back to highlighting the (now reset) sequence.
	 */
	Emote getMistakeEmote()
	{
		if (mistakeEmote == null || Duration.between(mistakeTimestamp, Instant.now()).compareTo(MISTAKE_DISPLAY_DURATION) > 0)
		{
			mistakeEmote = null;
			return null;
		}

		return mistakeEmote;
	}

	Emote getCurrentEmote()
	{
		if (emoteIndex >= EMOTE_SEQUENCE.size())
		{
			emoteIndex = 0;
			return null;
		}

		return EMOTE_SEQUENCE.get(emoteIndex);
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

		progressPendingHop();
		progressQuickHop();

		if (!isInTargetArea())
		{
			ringCount = 0;
			resetStats();
			return;
		}

		int previousRingCount = ringCount;
		updateRingCount();

		if (ringCount > previousRingCount)
		{
			recordWorldCooldown();
			startSessionIfNeeded();
		}
	}

	private void startSessionIfNeeded()
	{
		if (sessionStart == null)
		{
			sessionStart = Instant.now();
			persistStats();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!isInTargetArea())
		{
			armedAlchSpell = null;
			return;
		}

		String option = event.getMenuOption();
		String target = event.getMenuTarget();

		// Single-click cast (e.g. shift-click cast bound to the item) - spell name and item arrive together.
		if ("Cast".equals(option) && target != null && event.getItemId() != -1)
		{
			if (target.contains("High Level Alchemy"))
			{
				armedAlchSpell = null;
				handleAlch(event.getItemId(), true);
				return;
			}

			if (target.contains("Low Level Alchemy"))
			{
				armedAlchSpell = null;
				handleAlch(event.getItemId(), false);
				return;
			}
		}

		// Spell selected from the spellbook - arm and wait for the item click that follows.
		if ("Cast".equals(option) && ("High Level Alchemy".equals(target) || "Low Level Alchemy".equals(target)))
		{
			armedAlchSpell = target;
			return;
		}

		if (armedAlchSpell != null)
		{
			String spell = armedAlchSpell;
			armedAlchSpell = null;
			if (event.getItemId() != -1)
			{
				handleAlch(event.getItemId(), "High Level Alchemy".equals(spell));
			}
		}
	}

	private void handleAlch(int itemId, boolean isHighAlch)
	{
		if (itemId != RUBY_RING_ID)
		{
			return;
		}

		int alchValue;
		int xp;
		if (isHighAlch)
		{
			alchValue = itemManager.getItemComposition(RUBY_RING_ID).getHaPrice();
			xp = HIGH_ALCHEMY_XP;
		}
		else
		{
			alchValue = (int) (itemManager.getItemComposition(RUBY_RING_ID).getPrice() * LOW_ALCHEMY_MULTIPLIER);
			xp = LOW_ALCHEMY_XP;
		}

		int profit = alchValue - config.natureRuneCost();

		alchCount++;
		totalProfit += profit;
		totalXpGained += xp;

		startSessionIfNeeded();
		updateRates();
		persistStats();
	}

	// Only recomputed when an alch actually lands, not continuously - if it were live, the rate
	// swings wildly in the first few seconds of a session (e.g. one 500gp alch over 2 elapsed
	// seconds briefly reads as an absurd gp/hr) before settling down.
	private void updateRates()
	{
		if (sessionStart == null)
		{
			profitPerHour = 0;
			xpPerHour = 0;
			return;
		}

		long elapsedMillis = Duration.between(sessionStart, Instant.now()).toMillis();
		if (elapsedMillis <= 0)
		{
			profitPerHour = 0;
			xpPerHour = 0;
			return;
		}

		profitPerHour = Math.round(totalProfit * 3_600_000.0 / elapsedMillis);
		xpPerHour = (int) Math.round(totalXpGained * 3_600_000.0 / elapsedMillis);
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

	private void recordWorldCooldown()
	{
		int world = client.getWorld();
		worldCooldowns.put(world, Instant.now().plus(WORLD_COOLDOWN));
		persistCooldowns();
	}

	private void loadCooldowns()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, KEY_WORLD_COOLDOWNS);
		if (json == null || json.isEmpty())
		{
			return;
		}

		Type type = new TypeToken<Map<Integer, Long>>()
		{
		}.getType();
		Map<Integer, Long> saved = gson.fromJson(json, type);
		if (saved == null)
		{
			return;
		}

		Instant now = Instant.now();
		for (Map.Entry<Integer, Long> entry : saved.entrySet())
		{
			Instant expiry = Instant.ofEpochMilli(entry.getValue());
			if (expiry.isAfter(now))
			{
				worldCooldowns.put(entry.getKey(), expiry);
			}
		}
	}

	private void persistCooldowns()
	{
		Map<Integer, Long> toSave = new HashMap<>();
		for (Map.Entry<Integer, Instant> entry : worldCooldowns.entrySet())
		{
			toSave.put(entry.getKey(), entry.getValue().toEpochMilli());
		}
		configManager.setConfiguration(CONFIG_GROUP, KEY_WORLD_COOLDOWNS, gson.toJson(toSave));
	}

	/**
	 * Finds a random eligible world under the configured ping threshold and hops to it. Runs the
	 * world fetch/filter/ping scan off the client thread since pinging can take a couple seconds.
	 * Debounced so spam-clicking the button doesn't queue up a pile of redundant scans/hops.
	 */
	void hopToRandomWorld()
	{
		Instant now = Instant.now();
		if (lastHopButtonPress != null && Duration.between(lastHopButtonPress, now).compareTo(HOP_BUTTON_COOLDOWN) < 0)
		{
			return;
		}
		lastHopButtonPress = now;

		log.info("Ruby Ring Counter: hop button clicked");
		executor.execute(this::findAndHopToWorld);
	}

	private void findAndHopToWorld()
	{
		try
		{
			findAndHopToWorld0();
		}
		catch (Exception e)
		{
			log.warn("Ruby Ring Counter: hop scan failed", e);
			sendChatMessage("Something went wrong finding a world - see the client log.");
		}
	}

	private void findAndHopToWorld0()
	{
		log.info("Ruby Ring Counter: fetching world list");
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null)
		{
			log.info("Ruby Ring Counter: world list was null");
			sendChatMessage("Couldn't fetch the world list, try again in a moment.");
			return;
		}

		Instant now = Instant.now();
		List<World> candidates = new ArrayList<>();
		for (World world : worldResult.getWorlds())
		{
			if (!matchesWorldTypeFilters(world) || isOnCooldown(world.getId(), now))
			{
				continue;
			}

			candidates.add(world);
		}

		log.info("Ruby Ring Counter: {} candidate worlds after filtering", candidates.size());

		if (candidates.isEmpty())
		{
			sendChatMessage("No worlds match your filters right now - check the World types settings.");
			return;
		}

		Collections.shuffle(candidates);

		for (World world : candidates)
		{
			int ping = Ping.ping(world, true);
			log.info("Ruby Ring Counter: world {} ping {}ms", world.getId(), ping);
			if (ping >= 0 && ping <= config.pingThreshold())
			{
				sendChatMessage("Hopping to World " + world.getId() + "...");
				hopTo(world);
				return;
			}
		}

		sendChatMessage("No worlds under " + config.pingThreshold() + "ms ping match your filters right now.");
	}

	private boolean isOnCooldown(int worldId, Instant now)
	{
		Instant expiry = worldCooldowns.get(worldId);
		return expiry != null && expiry.isAfter(now);
	}

	// Mirrors net.runelite.client.plugins.worldhopper.WorldTypeFilter, which is package-private and
	// so can't be referenced directly from an external plugin.
	private boolean matchesWorldTypeFilters(World world)
	{
		Set<WorldType> types = world.getTypes();

		if (!config.allowF2pWorlds() && !types.contains(WorldType.MEMBERS))
		{
			return false;
		}

		return (config.allowNormalWorlds() && isNormalWorld(types))
			|| (config.allowDeadmanWorlds() && types.contains(WorldType.DEADMAN))
			|| (config.allowSeasonalWorlds() && (types.contains(WorldType.SEASONAL) || types.contains(WorldType.TOURNAMENT)))
			|| (config.allowQuestSpeedrunningWorlds() && types.contains(WorldType.QUEST_SPEEDRUNNING))
			|| (config.allowFreshStartWorlds() && types.contains(WorldType.FRESH_START_WORLD))
			|| (config.allowPvpWorlds() && types.contains(WorldType.PVP))
			|| (config.allowSkillTotalWorlds() && types.contains(WorldType.SKILL_TOTAL))
			|| (config.allowHighRiskWorlds() && types.contains(WorldType.HIGH_RISK) && !types.contains(WorldType.PVP))
			|| (config.allowBountyHunterWorlds() && types.contains(WorldType.BOUNTY));
	}

	// Deliberately stricter than RuneLite's own WorldTypeFilter.NORMAL, which also lumps in Skill
	// Total and Last Man Standing worlds. Since those have their own separate checkboxes here,
	// "Normal" should mean only plain worlds (optionally members), not those special modes too.
	private static boolean isNormalWorld(Set<WorldType> types)
	{
		EnumSet<WorldType> ignorable = EnumSet.of(WorldType.MEMBERS);
		EnumSet<WorldType> everythingElse = EnumSet.complementOf(ignorable);
		return Sets.intersection(types, everythingElse).isEmpty();
	}

	private void hopTo(World world)
	{
		log.info("Ruby Ring Counter: hopping to world {}", world.getId());
		clientThread.invoke(() ->
		{
			try
			{
				net.runelite.api.World rsWorld = client.createWorld();
				rsWorld.setActivity(world.getActivity());
				rsWorld.setAddress(world.getAddress());
				rsWorld.setId(world.getId());
				rsWorld.setPlayerCount(world.getPlayers());
				rsWorld.setLocation(world.getLocation());
				rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

				if (client.getGameState() == GameState.LOGIN_SCREEN)
				{
					// On the login screen there's no world switcher widget to open - this just
					// changes the world directly.
					client.changeWorld(rsWorld);
					return;
				}

				// Held for a tick before the quick-hop actually starts (see progressPendingHop()),
				// so the "Hopping to World..." chat message has a chance to land before the
				// world change tears down and resets the chatbox.
				pendingHopWorld = rsWorld;
				pendingHopWaitedATick = false;
			}
			catch (Exception e)
			{
				log.warn("Ruby Ring Counter: hop failed", e);
			}
		});
	}

	/**
	 * Waits one full game tick after hopTo() before handing the request off to the quick-hop
	 * widget flow, giving the chat message sent alongside it time to land first.
	 */
	private void progressPendingHop()
	{
		if (pendingHopWorld == null)
		{
			return;
		}

		if (!pendingHopWaitedATick)
		{
			pendingHopWaitedATick = true;
			return;
		}

		quickHopTarget = pendingHopWorld;
		quickHopAttempts = 0;
		pendingHopWorld = null;
	}

	/**
	 * Drives the quick-hop request set up by progressPendingHop(): opens the World Switcher widget
	 * if it isn't already, and hops as soon as it appears. Mirrors WorldHopperPlugin's own
	 * onGameTick logic, since that's the only way hopToWorld() reliably takes effect.
	 */
	private void progressQuickHop()
	{
		if (quickHopTarget == null)
		{
			return;
		}

		if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null)
		{
			client.openWorldHopper();

			if (++quickHopAttempts >= MAX_QUICK_HOP_ATTEMPTS)
			{
				log.warn("Ruby Ring Counter: failed to open the world switcher after {} attempts", quickHopAttempts);
				sendChatMessage("Failed to open the world switcher - try hopping again.");
				quickHopTarget = null;
				quickHopAttempts = 0;
			}

			return;
		}

		log.info("Ruby Ring Counter: world switcher open, hopping to world {}", quickHopTarget.getId());
		client.hopToWorld(quickHopTarget);
		quickHopTarget = null;
		quickHopAttempts = 0;
	}

	private void sendChatMessage(String message)
	{
		String formatted = new ChatMessageBuilder()
			.append("[")
			.append(Color.RED, "VBRR")
			.append("] " + message)
			.build();

		clientThread.invoke(() ->
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(formatted)
				.build()));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("overlayPosition".equals(event.getKey()))
		{
			// A manually dragged position is saved by RuneLite and overrides setPosition() on its
			// own, so clear that saved override before applying the newly selected position.
			overlayManager.resetOverlay(overlay);
			overlay.setPosition(config.overlayPosition().getPosition());
		}
	}

	@Provides
	RubyRingCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RubyRingCounterConfig.class);
	}
}
