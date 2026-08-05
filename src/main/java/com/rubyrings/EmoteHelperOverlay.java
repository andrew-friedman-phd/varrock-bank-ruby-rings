package com.rubyrings;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.cluescrolls.clues.emote.Emote;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Highlights the next required emote in the vanilla Emotes tab while standing at the Crack the
 * Clue III gate, and auto-scrolls it into view. Adapted from Clank1337's "CrackTheClue3 Emote"
 * plugin (BSD 2-Clause) - https://github.com/Clank1337/ctc3-emote - with credit given in the README.
 */
public class EmoteHelperOverlay extends Overlay
{
	private static final Color HIGHLIGHT_BORDER_COLOR = Color.ORANGE;
	private static final Color HIGHLIGHT_HOVER_BORDER_COLOR = HIGHLIGHT_BORDER_COLOR.darker();
	private static final Color HIGHLIGHT_FILL_COLOR = new Color(0, 255, 0, 20);

	private static final Color MISTAKE_COLOR = Color.RED;
	private static final Color MISTAKE_FILL_COLOR = new Color(255, 0, 0, 30);

	private final RubyRingCounterPlugin plugin;
	private final Client client;
	private boolean hasScrolled;

	@Inject
	private EmoteHelperOverlay(RubyRingCounterPlugin plugin, Client client)
	{
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || !plugin.isAtEmoteGate(localPlayer))
		{
			hasScrolled = false;
			return null;
		}

		Widget emoteContainer = client.getWidget(InterfaceID.Emote.CONTENTS);
		if (emoteContainer == null || emoteContainer.isHidden())
		{
			return null;
		}

		Widget emoteWindow = client.getWidget(InterfaceID.Emote.UNIVERSE);
		if (emoteWindow == null)
		{
			return null;
		}

		// A recent misclick takes priority - show what went wrong instead of the next step,
		// which has already been reset back to the beginning of the sequence.
		Emote mistake = plugin.getMistakeEmote();
		if (mistake != null)
		{
			for (Widget emoteWidget : emoteContainer.getDynamicChildren())
			{
				if (emoteWidget.getSpriteId() == mistake.getSpriteId())
				{
					markMistake(graphics, emoteWidget, emoteWindow);
				}
			}
			hasScrolled = false;
			return null;
		}

		Emote currentEmote = plugin.getCurrentEmote();
		if (currentEmote == null || !currentEmote.hasSprite())
		{
			hasScrolled = false;
			return null;
		}

		Widget matchingWidget = null;
		for (Widget emoteWidget : emoteContainer.getDynamicChildren())
		{
			if (emoteWidget.getSpriteId() == currentEmote.getSpriteId())
			{
				matchingWidget = emoteWidget;
				highlightWidget(graphics, emoteWidget, emoteWindow);
			}
		}

		if (!hasScrolled && matchingWidget != null)
		{
			hasScrolled = true;
			scrollToWidget(matchingWidget);
		}

		return null;
	}

	private void highlightWidget(Graphics2D graphics, Widget toHighlight, Widget container)
	{
		Rectangle bounds = visibleBounds(toHighlight, container);
		if (bounds == null)
		{
			return;
		}

		OverlayUtil.renderHoverableArea(graphics, new Area(bounds), client.getMouseCanvasPosition(),
			HIGHLIGHT_FILL_COLOR, HIGHLIGHT_BORDER_COLOR, HIGHLIGHT_HOVER_BORDER_COLOR);
	}

	private void markMistake(Graphics2D graphics, Widget toHighlight, Widget container)
	{
		Rectangle bounds = visibleBounds(toHighlight, container);
		if (bounds == null)
		{
			return;
		}

		OverlayUtil.renderHoverableArea(graphics, new Area(bounds), client.getMouseCanvasPosition(),
			MISTAKE_FILL_COLOR, MISTAKE_COLOR, MISTAKE_COLOR);

		Stroke originalStroke = graphics.getStroke();
		Color originalColor = graphics.getColor();
		graphics.setColor(MISTAKE_COLOR);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
		graphics.drawLine(bounds.x + bounds.width, bounds.y, bounds.x, bounds.y + bounds.height);
		graphics.setStroke(originalStroke);
		graphics.setColor(originalColor);
	}

	private static Rectangle visibleBounds(Widget toHighlight, Widget container)
	{
		net.runelite.api.Point canvasLocation = toHighlight.getCanvasLocation();
		if (canvasLocation == null)
		{
			return null;
		}

		net.runelite.api.Point windowLocation = container.getCanvasLocation();
		if (windowLocation.getY() > canvasLocation.getY() + toHighlight.getHeight()
			|| windowLocation.getY() + container.getHeight() < canvasLocation.getY())
		{
			return null;
		}

		return new Rectangle(
			canvasLocation.getX(),
			Math.max(canvasLocation.getY(), windowLocation.getY()),
			toHighlight.getWidth(),
			Math.min(
				Math.min(windowLocation.getY() + container.getHeight() - canvasLocation.getY(), toHighlight.getHeight()),
				Math.min(canvasLocation.getY() + toHighlight.getHeight() - windowLocation.getY(), toHighlight.getHeight())));
	}

	private void scrollToWidget(Widget toHighlight)
	{
		Widget scrollable = client.getWidget(InterfaceID.Emote.SCROLLABLE);
		if (scrollable == null)
		{
			return;
		}

		int centralY = toHighlight.getRelativeY() + toHighlight.getHeight() / 2;
		int newScroll = Math.max(0, Math.min(scrollable.getScrollHeight(), centralY - scrollable.getHeight() / 2));

		client.runScript(ScriptID.UPDATE_SCROLLBAR, InterfaceID.Emote.SCROLLBAR, InterfaceID.Emote.SCROLLABLE, newScroll);
	}
}
