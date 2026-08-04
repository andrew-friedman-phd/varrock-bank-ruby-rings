package com.rubyrings;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class RubyRingCounterOverlay extends OverlayPanel
{
	private final RubyRingCounterPlugin plugin;

	@Inject
	private RubyRingCounterOverlay(RubyRingCounterPlugin plugin, RubyRingCounterConfig config)
	{
		this.plugin = plugin;
		setPosition(config.overlayPosition().getPosition());
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Reset", "Varrock Bank Ruby Rings", e -> plugin.resetStats());
	}

	@Override
	public java.awt.Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInTargetArea())
		{
			return null;
		}

		panelComponent.getChildren().clear();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Ruby Rings")
			.color(Color.ORANGE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("In inventory:")
			.right(String.valueOf(plugin.getRingCount()))
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Alched:")
			.right(String.valueOf(plugin.getAlchCount()))
			.build());

		long profit = plugin.getTotalProfit();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Profit:")
			.right(formatProfit(profit))
			.rightColor(profit >= 0 ? Color.GREEN : Color.RED)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Magic XP:")
			.right(String.valueOf(plugin.getTotalXpGained()))
			.build());

		return super.render(graphics);
	}

	private static String formatProfit(long profit)
	{
		long abs = Math.abs(profit);
		String sign = profit >= 0 ? "+" : "-";
		String amount = abs >= 100_000 ? (abs / 1000) + "k" : abs + " gp";
		return sign + amount;
	}
}
