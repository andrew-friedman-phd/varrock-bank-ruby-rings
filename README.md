# Varrock Bank Ruby Rings

A RuneLite plugin for tracking Ruby rings picked up and High Level Alchemy profit in the Varrock west bank basement.

## Features

- Shows a small box (position configurable) with the number of Ruby rings currently in your inventory.
- Tracks how many Ruby rings you've high alched, along with total profit and Magic XP gained from those casts.
- Profit is calculated from the ring's live high alchemy value minus a configurable nature rune cost (defaults to a
  common rune shop price - handy for ironmen, who can't rely on the Grand Exchange).
- Everything only tracks/displays while you're in the Varrock west bank basement, and resets automatically when you
  leave.
- Right-click the overlay for a "Reset" option to clear the session stats manually at any time.

## Configuration

- **Nature rune cost (gp)** - cost per nature rune, subtracted from the alch profit shown.
- **Overlay position** - where to place the box on screen (top/bottom, left/right, or above the chatbox).
