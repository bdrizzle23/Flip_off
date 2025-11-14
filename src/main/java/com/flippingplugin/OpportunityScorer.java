package com.flippingplugin;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class OpportunityScorer
{
	private final PatternDetector patternDetector;

	@Inject
	public OpportunityScorer(PatternDetector patternDetector)
	{
		this.patternDetector = patternDetector;
	}

	/**
	 * Scores a flipping opportunity based on multiple factors
	 * @param itemId Item ID
	 * @param priceData Current price data
	 * @param pattern Detected market pattern
	 * @return Score from 0-100
	 */
	public int scoreOpportunity(int itemId, ItemPriceData priceData, MarketPattern pattern)
	{
		double score = 0;

		// 1. ROI Score (0-30 points)
		score += calculateROIScore(priceData);

		// 2. Volume Score (0-25 points)
		score += calculateVolumeScore(priceData);

		// 3. Pattern Score (0-25 points)
		score += calculatePatternScore(pattern);

		// 4. Volatility Score (0-20 points)
		score += calculateVolatilityScore(itemId, priceData);

		// Apply pattern multiplier
		score *= pattern.getScoreMultiplier();

		// Cap at 100
		return Math.min(100, (int) Math.round(score));
	}

	/**
	 * Calculates score based on ROI (0-30 points)
	 */
	private double calculateROIScore(ItemPriceData priceData)
	{
		if (priceData.getLowPrice() == 0)
		{
			return 0;
		}

		double roi = TaxCalculator.calculateROI(priceData.getLowPrice(), priceData.getHighPrice());

		if (roi >= 10)
		{
			return 30; // 10%+ ROI = max points
		}
		else if (roi >= 5)
		{
			return 25; // 5-10% ROI
		}
		else if (roi >= 3)
		{
			return 20; // 3-5% ROI
		}
		else if (roi >= 2)
		{
			return 15; // 2-3% ROI
		}
		else if (roi >= 1)
		{
			return 10; // 1-2% ROI
		}
		else if (roi > 0)
		{
			return 5; // 0-1% ROI
		}

		return 0;
	}

	/**
	 * Calculates score based on trading volume (0-25 points)
	 */
	private double calculateVolumeScore(ItemPriceData priceData)
	{
		long totalVolume = priceData.getHighPriceVolume() + priceData.getLowPriceVolume();

		if (totalVolume >= 10000)
		{
			return 25; // Very high volume
		}
		else if (totalVolume >= 5000)
		{
			return 20; // High volume
		}
		else if (totalVolume >= 2000)
		{
			return 15; // Good volume
		}
		else if (totalVolume >= 1000)
		{
			return 10; // Minimum acceptable volume
		}

		return 0; // Below threshold
	}

	/**
	 * Calculates score based on detected pattern (0-25 points)
	 */
	private double calculatePatternScore(MarketPattern pattern)
	{
		switch (pattern)
		{
			case FLASH_CRASH:
				return 25; // Highest priority
			case DUMP:
				return 22;
			case RECOVERY:
				return 20;
			case MANIPULATION:
				return 18;
			case VOLATILITY_SPIKE:
				return 15;
			case TREND_REVERSAL:
				return 12;
			case STABLE_SPREAD:
				return 10;
			case NONE:
			default:
				return 5;
		}
	}

	/**
	 * Calculates score based on price volatility (0-20 points)
	 * Higher volatility can mean more opportunity but also more risk
	 */
	private double calculateVolatilityScore(int itemId, ItemPriceData priceData)
	{
		double volatility = patternDetector.calculateVolatility(
			patternDetector.priceHistory.get(itemId)
		);

		// Sweet spot is moderate volatility (5-15%)
		if (volatility >= 0.05 && volatility <= 0.15)
		{
			return 20; // Optimal volatility
		}
		else if (volatility >= 0.03 && volatility <= 0.20)
		{
			return 15; // Good volatility
		}
		else if (volatility >= 0.01 && volatility <= 0.25)
		{
			return 10; // Acceptable volatility
		}
		else if (volatility < 0.01)
		{
			return 5; // Too stable, limited profit potential
		}

		return 3; // Too volatile, high risk
	}

	/**
	 * Creates a complete FlippingOpportunity object with all calculated metrics
	 */
	public FlippingOpportunity createOpportunity(int itemId, String itemName, ItemPriceData priceData, MarketPattern pattern)
	{
		int buyPrice = priceData.getLowPrice();
		int sellPrice = priceData.getHighPrice();
		int tax = TaxCalculator.calculateTax(sellPrice);
		int profitAfterTax = TaxCalculator.calculateProfitAfterTax(buyPrice, sellPrice);
		double roi = TaxCalculator.calculateROI(buyPrice, sellPrice);
		int score = scoreOpportunity(itemId, priceData, pattern);
		OpportunityGrade grade = OpportunityGrade.fromScore(score);

		return FlippingOpportunity.builder()
			.itemId(itemId)
			.itemName(itemName)
			.buyPrice(buyPrice)
			.sellPrice(sellPrice)
			.dailyVolume(priceData.getHighPriceVolume() + priceData.getLowPriceVolume())
			.rawProfit(sellPrice - buyPrice)
			.profitAfterTax(profitAfterTax)
			.roiPercentage(roi)
			.pattern(pattern)
			.grade(grade)
			.score(score)
			.timestamp(System.currentTimeMillis())
			.tax(tax)
			.priceVolatility(patternDetector.calculateVolatility(
				patternDetector.priceHistory.get(itemId)))
			.highVolume(priceData.getHighPriceVolume())
			.lowVolume(priceData.getLowPriceVolume())
			.build();
	}
}
