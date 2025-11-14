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
		name = "Discord",
		description = "Discord webhook integration settings",
		position = 3
	)
	String discordSection = "discord";

	@ConfigSection(
		name = "Trade Tracking",
		description = "Personal trade and flip history tracking",
		position = 4
	)
	String trackingSection = "tracking";

	@ConfigSection(
		name = "Advanced",
		description = "Advanced plugin settings",
		position = 5
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

	// Discord
	@ConfigItem(
		keyName = "enableDiscord",
		name = "Enable Discord Notifications",
		description = "Send flipping opportunities to Discord via webhook",
		section = discordSection,
		position = 0
	)
	default boolean enableDiscord()
	{
		return false;
	}

	@ConfigItem(
		keyName = "discordWebhookUrl",
		name = "Discord Webhook URL",
		description = "Your Discord webhook URL (create one in Server Settings > Integrations)",
		section = discordSection,
		position = 1
	)
	default String discordWebhookUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discordMinGrade",
		name = "Discord Grade Threshold",
		description = "Only send Discord notifications for this grade or better",
		section = discordSection,
		position = 2
	)
	default OpportunityGrade discordMinGrade()
	{
		return OpportunityGrade.B_PLUS;
	}

	@ConfigItem(
		keyName = "discordCooldown",
		name = "Cooldown (minutes)",
		description = "Minimum time between Discord notifications for the same item",
		section = discordSection,
		position = 3
	)
	@Range(min = 1, max = 60)
	default int discordCooldown()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "discordBatchNotifications",
		name = "Batch Notifications",
		description = "Send opportunities as a summary instead of individual messages",
		section = discordSection,
		position = 4
	)
	default boolean discordBatchNotifications()
	{
		return false;
	}

	@ConfigItem(
		keyName = "discordNotifyFlips",
		name = "Notify on Completed Flips",
		description = "Send Discord notifications when your flips complete",
		section = discordSection,
		position = 5
	)
	default boolean discordNotifyFlips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "discordFlipCooldown",
		name = "Flip Cooldown (minutes)",
		description = "Minimum time between Discord notifications for completed flips of same item",
		section = discordSection,
		position = 6
	)
	@Range(min = 0, max = 60)
	default int discordFlipCooldown()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "discordPatternFilter",
		name = "Pattern Filter",
		description = "Only send Discord notifications for specific pattern (ALL for any pattern)",
		section = discordSection,
		position = 7
	)
	default DiscordPatternFilter discordPatternFilter()
	{
		return DiscordPatternFilter.ALL;
	}

	// Trade Tracking
	@ConfigItem(
		keyName = "enableTradeTracking",
		name = "Enable Trade Tracking",
		description = "Track your GE offers and flip history",
		section = trackingSection,
		position = 0
	)
	default boolean enableTradeTracking()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTradeHistory",
		name = "Show Trade History",
		description = "Display completed flips in the panel",
		section = trackingSection,
		position = 1
	)
	default boolean showTradeHistory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maxTradeHistory",
		name = "Max History Entries",
		description = "Maximum number of completed flips to show",
		section = trackingSection,
		position = 2
	)
	@Range(min = 10, max = 200)
	default int maxTradeHistory()
	{
		return 50;
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
