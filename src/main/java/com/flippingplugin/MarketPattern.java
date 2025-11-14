package com.flippingplugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketPattern
{
	FLASH_CRASH("Flash Crash", "Sudden price drop with quick recovery potential", 1.5),
	MANIPULATION("Price Manipulation", "Unusual price movements suggesting market manipulation", 1.2),
	DUMP("Market Dump", "Large sell-off creating buying opportunity", 1.4),
	RECOVERY("Price Recovery", "Item recovering from previous crash", 1.3),
	VOLATILITY_SPIKE("Volatility Spike", "High price variance indicating trading opportunity", 1.25),
	TREND_REVERSAL("Trend Reversal", "Price trend changing direction", 1.15),
	STABLE_SPREAD("Stable Spread", "Consistent buy/sell spread with volume", 1.1),
	NONE("No Pattern", "No specific pattern detected", 1.0);

	private final String displayName;
	private final String description;
	private final double scoreMultiplier;
}
