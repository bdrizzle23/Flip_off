package com.flippingplugin;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Slf4j
@Singleton
public class PatternDetector
{
	private static final int HISTORY_SIZE = 20;
	private final Map<Integer, Queue<ItemPriceData>> priceHistory;

	public PatternDetector()
	{
		this.priceHistory = new HashMap<>();
	}

	/**
	 * Updates price history for an item
	 * @param priceData Current price data
	 */
	public void updateHistory(ItemPriceData priceData)
	{
		Queue<ItemPriceData> history = priceHistory.computeIfAbsent(
			priceData.getItemId(),
			k -> new LinkedList<>()
		);

		history.offer(priceData);
		if (history.size() > HISTORY_SIZE)
		{
			history.poll();
		}
	}

	/**
	 * Detects the most prominent market pattern for an item
	 * @param itemId The item to analyze
	 * @param currentPrice Current price data
	 * @return The detected pattern
	 */
	public MarketPattern detectPattern(int itemId, ItemPriceData currentPrice)
	{
		Queue<ItemPriceData> history = priceHistory.get(itemId);
		if (history == null || history.size() < 5)
		{
			return MarketPattern.NONE;
		}

		// Check for flash crash
		if (isFlashCrash(history, currentPrice))
		{
			return MarketPattern.FLASH_CRASH;
		}

		// Check for manipulation
		if (isManipulation(history, currentPrice))
		{
			return MarketPattern.MANIPULATION;
		}

		// Check for dump
		if (isDump(history, currentPrice))
		{
			return MarketPattern.DUMP;
		}

		// Check for recovery
		if (isRecovery(history, currentPrice))
		{
			return MarketPattern.RECOVERY;
		}

		// Check for volatility spike
		if (isVolatilitySpike(history, currentPrice))
		{
			return MarketPattern.VOLATILITY_SPIKE;
		}

		// Check for trend reversal
		if (isTrendReversal(history, currentPrice))
		{
			return MarketPattern.TREND_REVERSAL;
		}

		// Check for stable spread
		if (isStableSpread(history, currentPrice))
		{
			return MarketPattern.STABLE_SPREAD;
		}

		return MarketPattern.NONE;
	}

	/**
	 * Detects flash crash: sudden price drop >20% followed by signs of recovery
	 */
	private boolean isFlashCrash(Queue<ItemPriceData> history, ItemPriceData current)
	{
		ItemPriceData[] dataArray = history.toArray(new ItemPriceData[0]);
		if (dataArray.length < 5)
		{
			return false;
		}

		// Get average price from 5-10 entries ago
		int startIdx = Math.max(0, dataArray.length - 10);
		int endIdx = Math.max(0, dataArray.length - 5);
		double avgOldPrice = 0;
		int count = 0;

		for (int i = startIdx; i < endIdx; i++)
		{
			avgOldPrice += dataArray[i].getHighPrice();
			count++;
		}
		avgOldPrice /= count;

		// Current price dropped >20%
		double priceDrop = (avgOldPrice - current.getHighPrice()) / avgOldPrice;
		boolean suddenDrop = priceDrop > 0.20;

		// Volume spike indicates potential recovery
		long avgVolume = history.stream()
			.mapToLong(d -> d.getHighPriceVolume() + d.getLowPriceVolume())
			.sum() / history.size();
		long currentVolume = current.getHighPriceVolume() + current.getLowPriceVolume();
		boolean volumeSpike = currentVolume > avgVolume * 1.5;

		return suddenDrop && volumeSpike;
	}

	/**
	 * Detects price manipulation: unusual price movements with low volume
	 */
	private boolean isManipulation(Queue<ItemPriceData> history, ItemPriceData current)
	{
		double volatility = calculateVolatility(history);
		long avgVolume = history.stream()
			.mapToLong(d -> d.getHighPriceVolume() + d.getLowPriceVolume())
			.sum() / history.size();
		long currentVolume = current.getHighPriceVolume() + current.getLowPriceVolume();

		// High volatility with below average volume suggests manipulation
		return volatility > 0.15 && currentVolume < avgVolume * 0.7;
	}

	/**
	 * Detects market dump: consistent price decline with high volume
	 */
	private boolean isDump(Queue<ItemPriceData> history, ItemPriceData current)
	{
		ItemPriceData[] dataArray = history.toArray(new ItemPriceData[0]);
		if (dataArray.length < 5)
		{
			return false;
		}

		// Check if last 5 prices are declining
		int decliningCount = 0;
		for (int i = dataArray.length - 5; i < dataArray.length - 1; i++)
		{
			if (dataArray[i + 1].getHighPrice() < dataArray[i].getHighPrice())
			{
				decliningCount++;
			}
		}

		// High volume
		long avgVolume = history.stream()
			.mapToLong(d -> d.getHighPriceVolume() + d.getLowPriceVolume())
			.sum() / history.size();
		long currentVolume = current.getHighPriceVolume() + current.getLowPriceVolume();

		return decliningCount >= 3 && currentVolume > avgVolume * 1.2;
	}

	/**
	 * Detects price recovery: price increasing after a decline
	 */
	private boolean isRecovery(Queue<ItemPriceData> history, ItemPriceData current)
	{
		ItemPriceData[] dataArray = history.toArray(new ItemPriceData[0]);
		if (dataArray.length < 8)
		{
			return false;
		}

		// Price was declining
		int midPoint = dataArray.length - 4;
		int oldPrice = dataArray[Math.max(0, dataArray.length - 8)].getHighPrice();
		int lowPrice = dataArray[midPoint].getHighPrice();

		// Now recovering
		int currentPrice = current.getHighPrice();

		boolean wasDecline = lowPrice < oldPrice * 0.9;
		boolean isRecovering = currentPrice > lowPrice * 1.1;

		return wasDecline && isRecovering;
	}

	/**
	 * Detects volatility spike: sudden increase in price variance
	 */
	private boolean isVolatilitySpike(Queue<ItemPriceData> history, ItemPriceData current)
	{
		double volatility = calculateVolatility(history);
		return volatility > 0.20;
	}

	/**
	 * Detects trend reversal: price direction changing
	 */
	private boolean isTrendReversal(Queue<ItemPriceData> history, ItemPriceData current)
	{
		ItemPriceData[] dataArray = history.toArray(new ItemPriceData[0]);
		if (dataArray.length < 10)
		{
			return false;
		}

		// Calculate trend in first half
		int midPoint = dataArray.length / 2;
		double firstHalfAvg = 0;
		for (int i = 0; i < midPoint; i++)
		{
			firstHalfAvg += dataArray[i].getHighPrice();
		}
		firstHalfAvg /= midPoint;

		// Calculate trend in second half
		double secondHalfAvg = 0;
		for (int i = midPoint; i < dataArray.length; i++)
		{
			secondHalfAvg += dataArray[i].getHighPrice();
		}
		secondHalfAvg /= (dataArray.length - midPoint);

		// Trend reversed if direction changed significantly
		double change = Math.abs(secondHalfAvg - firstHalfAvg) / firstHalfAvg;
		return change > 0.15;
	}

	/**
	 * Detects stable spread: consistent buy/sell spread with good volume
	 */
	private boolean isStableSpread(Queue<ItemPriceData> history, ItemPriceData current)
	{
		double volatility = calculateVolatility(history);
		long avgVolume = history.stream()
			.mapToLong(d -> d.getHighPriceVolume() + d.getLowPriceVolume())
			.sum() / history.size();

		// Low volatility with decent volume
		return volatility < 0.05 && avgVolume > 1000;
	}

	/**
	 * Calculates price volatility (standard deviation / mean)
	 */
	public double calculateVolatility(Queue<ItemPriceData> history)
	{
		if (history == null || history.isEmpty())
		{
			return 0;
		}

		double mean = history.stream()
			.mapToInt(ItemPriceData::getHighPrice)
			.average()
			.orElse(0);

		if (mean == 0)
		{
			return 0;
		}

		double variance = history.stream()
			.mapToDouble(d -> Math.pow(d.getHighPrice() - mean, 2))
			.average()
			.orElse(0);

		return Math.sqrt(variance) / mean;
	}

	/**
	 * Clears price history for all items
	 */
	public void clearHistory()
	{
		priceHistory.clear();
	}
}
