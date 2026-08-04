package com.rubyrings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
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

	@Getter
	@AllArgsConstructor
	enum OverlayLocation
	{
		TOP_LEFT("Top left", OverlayPosition.TOP_LEFT),
		TOP_RIGHT("Top right", OverlayPosition.TOP_RIGHT),
		BOTTOM_LEFT("Bottom left", OverlayPosition.BOTTOM_LEFT),
		BOTTOM_RIGHT("Bottom right (above inventory)", OverlayPosition.BOTTOM_RIGHT),
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
