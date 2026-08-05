package com.rubyrings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.ui.overlay.OverlayPosition;

@ConfigGroup("rubyringcounter")
public interface RubyRingCounterConfig extends Config
{
	@ConfigItem(
		keyName = "natureRuneCost",
		name = "Nature rune cost (gp)",
		description = "Cost of the nature rune consumed per High Level Alchemy cast, subtracted from the profit shown."
			+ " Defaults to a common rune shop price - adjust to whatever you're actually paying.",
		position = 0
	)
	default int natureRuneCost()
	{
		return 180;
	}

	@ConfigItem(
		keyName = "overlayPosition",
		name = "Overlay position",
		description = "Where to place the Ruby Ring Counter box on screen.",
		position = 1
	)
	default OverlayLocation overlayPosition()
	{
		return OverlayLocation.BOTTOM_RIGHT;
	}

	@Range(min = 0, max = 400)
	@ConfigItem(
		keyName = "pingThreshold",
		name = "Max ping for hop (ms)",
		description = "The random-hop button only considers worlds with a ping at or below this value.",
		position = 2
	)
	default int pingThreshold()
	{
		return 150;
	}

	@ConfigSection(
		name = "World types",
		description = "Which world types the random-hop button is allowed to pick from.",
		position = 3
	)
	String worldTypesSection = "worldTypesSection";

	@ConfigItem(
		keyName = "allowNormalWorlds",
		name = "Normal worlds",
		description = "Standard worlds, including minigame worlds. Excludes Deadman, Skill Total, and Last Man Standing.",
		position = 0,
		section = worldTypesSection
	)
	default boolean allowNormalWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "allowDeadmanWorlds",
		name = "Deadman worlds",
		description = "Allow hopping to Deadman mode worlds.",
		position = 1,
		section = worldTypesSection
	)
	default boolean allowDeadmanWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowSeasonalWorlds",
		name = "Seasonal/Tournament worlds",
		description = "Allow hopping to seasonal (e.g. Leagues) or tournament worlds.",
		position = 2,
		section = worldTypesSection
	)
	default boolean allowSeasonalWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowQuestSpeedrunningWorlds",
		name = "Quest speedrunning worlds",
		description = "Allow hopping to quest speedrunning worlds.",
		position = 3,
		section = worldTypesSection
	)
	default boolean allowQuestSpeedrunningWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowFreshStartWorlds",
		name = "Fresh start worlds",
		description = "Allow hopping to Fresh Start worlds.",
		position = 4,
		section = worldTypesSection
	)
	default boolean allowFreshStartWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowPvpWorlds",
		name = "PVP worlds",
		description = "Allow hopping to PVP worlds.",
		position = 5,
		section = worldTypesSection
	)
	default boolean allowPvpWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowSkillTotalWorlds",
		name = "Skill total worlds",
		description = "Allow hopping to worlds with a minimum total level requirement.",
		position = 6,
		section = worldTypesSection
	)
	default boolean allowSkillTotalWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowHighRiskWorlds",
		name = "High risk worlds",
		description = "Allow hopping to High Risk worlds.",
		position = 7,
		section = worldTypesSection
	)
	default boolean allowHighRiskWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowBountyHunterWorlds",
		name = "Bounty Hunter worlds",
		description = "Allow hopping to Bounty Hunter worlds.",
		position = 8,
		section = worldTypesSection
	)
	default boolean allowBountyHunterWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "allowF2pWorlds",
		name = "Allow F2P worlds",
		description = "If unchecked, only members worlds are considered for the random hop.",
		position = 9,
		section = worldTypesSection
	)
	default boolean allowF2pWorlds()
	{
		return false;
	}

	@Getter
	@AllArgsConstructor
	enum OverlayLocation
	{
		TOP_LEFT("Top left", OverlayPosition.TOP_LEFT),
		TOP_RIGHT("Top right", OverlayPosition.TOP_RIGHT),
		BOTTOM_LEFT("Bottom left", OverlayPosition.BOTTOM_LEFT),
		BOTTOM_RIGHT("Bottom right", OverlayPosition.BOTTOM_RIGHT),
		ABOVE_CHATBOX("Above chatbox", OverlayPosition.ABOVE_CHATBOX_RIGHT);

		private final String label;
		private final OverlayPosition position;

		@Override
		public String toString()
		{
			return label;
		}
	}
}
