# Varrock Bank Ruby Rings

A RuneLite plugin for tracking Ruby rings picked up, High Level Alchemy profit, and world hopping in the Varrock west
bank basement - a one-stop shop for the whole Crack the Clue III loot loop.

## Features

- Shows a small box (position configurable) with the number of Ruby rings currently in your inventory.
- Tracks how many Ruby rings you've high alched, along with total profit and Magic XP gained from those casts, plus
  live profit/hr and XP/hr estimates for the current session.
- Profit is calculated from the ring's live high alchemy value minus a configurable nature rune cost (defaults to a
  common rune shop price - handy for ironmen, who can't rely on the Grand Exchange).
- Everything only tracks/displays while you're in the Varrock west bank basement, and resets automatically when you
  leave.
- Right-click the overlay for a "Reset" option to clear the session totals at any time.
- A sidebar panel tracks which worlds you've already picked a ring up on (5 minute cooldown each) and can hop you to
  a random eligible world under your ping threshold, skipping worlds still on cooldown.
- Highlights the next emote to perform for Crack the Clue III directly in your Emotes tab and auto-scrolls it into
  view, the same way you'd normally check it manually.

## Configuration

- **Nature rune cost (gp)** - cost per nature rune, subtracted from the alch profit shown.
- **Overlay position** - where to place the box on screen (top/bottom, left/right, or above the chatbox).
- **Max ping for hop (ms)** - the random-hop button only considers worlds at or under this ping.
- **World types** - which world categories (Normal, Deadman, PVP, etc.) and whether F2P worlds are eligible for the
  random hop.

## Credits

The Crack the Clue III emote sequence and detection/highlighting approach are adapted from Clank1337's
[CrackTheClue3 Emote](https://github.com/Clank1337/ctc3-emote) plugin (BSD 2-Clause License).
