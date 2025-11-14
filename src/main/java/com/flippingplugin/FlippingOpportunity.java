package com.flippingplugin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlippingOpportunity
{
	private int itemId;
	private String itemName;
	private int buyPrice;
	private int sellPrice;
	private long dailyVolume;
	private int rawProfit;
	private int profitAfterTax;
	private double roiPercentage;
	private MarketPattern pattern;
	private OpportunityGrade grade;
	private int score;
	private long timestamp;
	private int tax;
	private double priceVolatility;
	private long highVolume;
	private long lowVolume;
}
