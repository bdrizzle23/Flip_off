package com.flippingplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("flipping")
public interface FlippingConfig extends Config
{
	@ConfigSection(
		name = "Volume Filters",
		description = "Settings for filtering items by trading volume",
		position = 0
	)
	String volumeSection = "volume";

	@ConfigSection(
		name = "Profit Filters",
		description = "Settings for filtering items by profit potential",
		position = 1
	)
	String profitSection = "profit";

	@ConfigSection(
		name = "Alerts",
		description = "Notification settings for opportunities",
		position = 2
	)
	String alertsSection = "alerts";

	@ConfigSection(
		name = "Advanced",
		description = "Advanced plugin settings",
		position = 3
	)
	String advancedSection = "advanced";

	// Volume Filters
	@ConfigItem(
		keyName = "minVolume",
		name = "Minimum Daily Volume",
		description = "Minimum number of daily trades required (default: 1000)",
		section = volumeSection,
		position = 0
	)
	@Range(min = 100, max = 100000)
	default int minVolume()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "preferHighVolume",
		name = "Prefer High Volume",
		description = "Prioritize items with higher trading volume",
		section = volumeSection,
		position = 1
	)
	default boolean preferHighVolume()
	{
		return true;
	}

	// Profit Filters
	@ConfigItem(
		keyName = "minROI",
		name = "Minimum ROI %",
		description = "Minimum return on investment percentage (after tax)",
		section = profitSection,
		position = 0
	)
	@Range(min = 0, max = 50)
	default int minROI()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "minProfit",
		name = "Minimum Profit (GP)",
		description = "Minimum profit per flip in gold pieces (after tax)",
		section = profitSection,
		position = 1
	)
	@Range(min = 0, max = 1000000)
	default int minProfit()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "minGrade",
		name = "Minimum Grade",
		description = "Only show opportunities with this grade or better",
		section = profitSection,
		position = 2
	)
	default OpportunityGrade minGrade()
	{
		return OpportunityGrade.C;
	}

	// Alerts
	@ConfigItem(
		keyName = "enableNotifications",
		name = "Enable Notifications",
		description = "Show notifications for high-quality opportunities",
		section = alertsSection,
		position = 0
	)
	default boolean enableNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyGrade",
		name = "Notification Grade Threshold",
		description = "Only notify for opportunities at or above this grade",
		section = alertsSection,
		position = 1
	)
	default OpportunityGrade notifyGrade()
	{
		return OpportunityGrade.B;
	}

	@ConfigItem(
		keyName = "notifyPatterns",
		name = "Notify on Patterns",
		description = "Send notifications when specific patterns are detected",
		section = alertsSection,
		position = 2
	)
	default boolean notifyPatterns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashCrashAlert",
		name = "Flash Crash Alerts",
		description = "Always notify on flash crash patterns (high priority)",
		section = alertsSection,
		position = 3
	)
	default boolean flashCrashAlert()
	{
		return true;
	}

	// Advanced
	@ConfigItem(
		keyName = "updateInterval",
		name = "Update Interval (seconds)",
		description = "How often to fetch new price data",
		section = advancedSection,
		position = 0
	)
	@Range(min = 30, max = 600)
	default int updateInterval()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "maxOpportunities",
		name = "Max Opportunities Shown",
		description = "Maximum number of opportunities to display",
		section = advancedSection,
		position = 1
	)
	@Range(min = 5, max = 100)
	default int maxOpportunities()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "enablePanel",
		name = "Enable Side Panel",
		description = "Show the flipping opportunities panel",
		section = advancedSection,
		position = 2
	)
	default boolean enablePanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoRefresh",
		name = "Auto Refresh",
		description = "Automatically refresh opportunities at set interval",
		section = advancedSection,
		position = 3
	)
	default boolean autoRefresh()
	{
		return true;
	}
}
