package com.rubyrings;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RubyRingCounterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RubyRingCounterPlugin.class);
		RuneLite.main(args);
	}
}
