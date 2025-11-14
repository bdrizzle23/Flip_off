package com.flippingplugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Filter options for Discord notifications by market pattern
 */
@Getter
@RequiredArgsConstructor
public enum DiscordPatternFilter
{
	ALL("All Patterns", null),
	FLASH_CRASH("Flash Crash Only", MarketPattern.FLASH_CRASH),
	MANIPULATION("Manipulation Only", MarketPattern.MANIPULATION),
	DUMP("Dump Only", MarketPattern.DUMP),
	RECOVERY("Recovery Only", MarketPattern.RECOVERY),
	VOLATILITY_SPIKE("Volatility Spike Only", MarketPattern.VOLATILITY_SPIKE),
	TREND_REVERSAL("Trend Reversal Only", MarketPattern.TREND_REVERSAL),
	STABLE_SPREAD("Stable Spread Only", MarketPattern.STABLE_SPREAD);

	private final String displayName;
	private final MarketPattern pattern;

	/**
	 * Check if an opportunity matches this filter
	 */
	public boolean matches(FlippingOpportunity opportunity)
	{
		if (this == ALL)
		{
			return true;
		}
		return opportunity.getPattern() == this.pattern;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
