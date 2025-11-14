package com.flippingplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages trade history and flip transactions
 */
@Slf4j
@Singleton
public class TradeHistoryManager
{
	private static final String CONFIG_GROUP = "flipping";
	private static final String TRADES_KEY = "tradeHistory";
	private static final String FLIPS_KEY = "flipHistory";
	private static final int MAX_TRADE_HISTORY = 1000;
	private static final int MAX_FLIP_HISTORY = 500;

	private final ConfigManager configManager;
	private final Gson gson;

	// Active trades (currently in GE slots)
	private final Map<Integer, Trade> activeTrades;

	// Completed trades (for matching buy/sell pairs)
	private final Map<Integer, List<Trade>> completedTrades;

	// Completed flips
	private final List<FlipTransaction> completedFlips;

	// Pending buy offers waiting for matching sells
	private final Map<Integer, Queue<Trade>> pendingBuys;

	@Inject
	public TradeHistoryManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.activeTrades = new ConcurrentHashMap<>();
		this.completedTrades = new ConcurrentHashMap<>();
		this.completedFlips = new ArrayList<>();
		this.pendingBuys = new ConcurrentHashMap<>();

		loadHistory();
	}

	/**
	 * Update or create a trade from GE offer
	 */
	public void updateTrade(int slot, int itemId, String itemName, int quantity, int price,
							int currentQuantity, int totalQuantity, Trade.TradeType type, Trade.TradeState state)
	{
		Trade trade = Trade.builder()
			.itemId(itemId)
			.itemName(itemName)
			.quantity(quantity)
			.price(price)
			.timestamp(System.currentTimeMillis())
			.type(type)
			.state(state)
			.slot(slot)
			.currentQuantityInTrade(currentQuantity)
			.totalQuantityInTrade(totalQuantity)
			.build();

		// Update active trades
		if (trade.isActive())
		{
			activeTrades.put(slot, trade);
		}
		else if (trade.isComplete())
		{
			// Remove from active
			activeTrades.remove(slot);

			// Add to completed
			completedTrades.computeIfAbsent(itemId, k -> new ArrayList<>()).add(trade);

			// Try to match with opposite trade type
			matchTrade(trade);

			// Save history
			saveHistory();

			log.debug("Trade completed: {} {} x{} @ {} gp", type, itemName, quantity, price);
		}
		else if (state == Trade.TradeState.CANCELLED || state == Trade.TradeState.ABORTED)
		{
			activeTrades.remove(slot);
			log.debug("Trade cancelled/aborted: {} {}", type, itemName);
		}
	}

	/**
	 * Try to match a completed trade with opposite type to create a flip
	 */
	private void matchTrade(Trade trade)
	{
		if (trade.getType() == Trade.TradeType.BUY)
		{
			// Store buy for later matching with sell
			pendingBuys.computeIfAbsent(trade.getItemId(), k -> new LinkedList<>()).offer(trade);
		}
		else if (trade.getType() == Trade.TradeType.SELL)
		{
			// Try to match with pending buys
			Queue<Trade> buys = pendingBuys.get(trade.getItemId());
			if (buys != null && !buys.isEmpty())
			{
				int remainingQuantity = trade.getQuantity();

				while (remainingQuantity > 0 && !buys.isEmpty())
				{
					Trade buyTrade = buys.peek();
					int matchQuantity = Math.min(remainingQuantity, buyTrade.getQuantity());

					// Create flip transaction
					FlipTransaction flip = createFlip(buyTrade, trade, matchQuantity);
					completedFlips.add(flip);

					log.info("Flip completed: {} x{} - Bought @ {} gp, Sold @ {} gp, Profit: {} gp ({}%)",
						flip.getItemName(), flip.getQuantity(), flip.getBuyPrice(), flip.getSellPrice(),
						flip.getProfit(), String.format("%.2f", flip.getRoi()));

					// Update quantities
					remainingQuantity -= matchQuantity;
					int newBuyQuantity = buyTrade.getQuantity() - matchQuantity;

					if (newBuyQuantity <= 0)
					{
						buys.poll(); // Remove fully matched buy
					}
					else
					{
						// Update buy quantity
						buyTrade.setQuantity(newBuyQuantity);
					}
				}

				// Limit flip history size
				if (completedFlips.size() > MAX_FLIP_HISTORY)
				{
					completedFlips.subList(0, completedFlips.size() - MAX_FLIP_HISTORY).clear();
				}
			}
		}
	}

	/**
	 * Create a flip transaction from matched buy and sell
	 */
	private FlipTransaction createFlip(Trade buyTrade, Trade sellTrade, int quantity)
	{
		int buyPrice = buyTrade.getPrice();
		int sellPrice = sellTrade.getPrice();

		// Calculate tax
		int tax = TaxCalculator.calculateTax(sellPrice) * quantity;

		// Calculate profit
		long revenue = (long) sellPrice * quantity;
		long cost = (long) buyPrice * quantity;
		int profit = (int) (revenue - cost - tax);

		// Calculate ROI
		double roi = cost > 0 ? (profit * 100.0 / cost) : 0;

		return FlipTransaction.builder()
			.itemId(buyTrade.getItemId())
			.itemName(buyTrade.getItemName())
			.quantity(quantity)
			.buyPrice(buyPrice)
			.sellPrice(sellPrice)
			.buyTime(buyTrade.getTimestamp())
			.sellTime(sellTrade.getTimestamp())
			.tax(tax)
			.profit(profit)
			.roi(roi)
			.build();
	}

	/**
	 * Get all active trades
	 */
	public List<Trade> getActiveTrades()
	{
		return new ArrayList<>(activeTrades.values());
	}

	/**
	 * Get trade by GE slot number
	 */
	public Trade getTradeBySlot(int slot)
	{
		return activeTrades.get(slot);
	}

	/**
	 * Get completed flips
	 */
	public List<FlipTransaction> getCompletedFlips(int limit)
	{
		int size = completedFlips.size();
		int fromIndex = Math.max(0, size - limit);
		return new ArrayList<>(completedFlips.subList(fromIndex, size));
	}

	/**
	 * Get all completed flips
	 */
	public List<FlipTransaction> getAllCompletedFlips()
	{
		return new ArrayList<>(completedFlips);
	}

	/**
	 * Get total profit from all flips
	 */
	public long getTotalProfit()
	{
		return completedFlips.stream()
			.mapToLong(FlipTransaction::getProfit)
			.sum();
	}

	/**
	 * Get profit statistics for a time period
	 */
	public Map<String, Object> getStats(long sinceTimestamp)
	{
		List<FlipTransaction> recentFlips = completedFlips.stream()
			.filter(f -> f.getSellTime() >= sinceTimestamp)
			.collect(Collectors.toList());

		long totalProfit = recentFlips.stream().mapToLong(FlipTransaction::getProfit).sum();
		long totalRevenue = recentFlips.stream().mapToLong(f -> (long) f.getSellPrice() * f.getQuantity()).sum();
		long totalTax = recentFlips.stream().mapToLong(FlipTransaction::getTax).sum();
		double avgROI = recentFlips.stream().mapToDouble(FlipTransaction::getRoi).average().orElse(0);

		Map<String, Object> stats = new HashMap<>();
		stats.put("flipCount", recentFlips.size());
		stats.put("totalProfit", totalProfit);
		stats.put("totalRevenue", totalRevenue);
		stats.put("totalTax", totalTax);
		stats.put("averageROI", avgROI);

		return stats;
	}

	/**
	 * Clear all history
	 */
	public void clearHistory()
	{
		activeTrades.clear();
		completedTrades.clear();
		completedFlips.clear();
		pendingBuys.clear();
		saveHistory();
	}

	/**
	 * Save history to config
	 */
	private void saveHistory()
	{
		try
		{
			// Save completed trades (limit size)
			List<Trade> allTrades = completedTrades.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());

			if (allTrades.size() > MAX_TRADE_HISTORY)
			{
				allTrades = allTrades.subList(allTrades.size() - MAX_TRADE_HISTORY, allTrades.size());
			}

			String tradesJson = gson.toJson(allTrades);
			configManager.setConfiguration(CONFIG_GROUP, TRADES_KEY, tradesJson);

			// Save completed flips
			String flipsJson = gson.toJson(completedFlips);
			configManager.setConfiguration(CONFIG_GROUP, FLIPS_KEY, flipsJson);
		}
		catch (Exception e)
		{
			log.error("Error saving trade history", e);
		}
	}

	/**
	 * Load history from config
	 */
	private void loadHistory()
	{
		try
		{
			// Load trades
			String tradesJson = configManager.getConfiguration(CONFIG_GROUP, TRADES_KEY);
			if (tradesJson != null && !tradesJson.isEmpty())
			{
				Type tradeListType = new TypeToken<List<Trade>>(){}.getType();
				List<Trade> trades = gson.fromJson(tradesJson, tradeListType);

				for (Trade trade : trades)
				{
					completedTrades.computeIfAbsent(trade.getItemId(), k -> new ArrayList<>()).add(trade);

					// Restore pending buys
					if (trade.getType() == Trade.TradeType.BUY)
					{
						pendingBuys.computeIfAbsent(trade.getItemId(), k -> new LinkedList<>()).offer(trade);
					}
				}

				log.debug("Loaded {} trades from config", trades.size());
			}

			// Load flips
			String flipsJson = configManager.getConfiguration(CONFIG_GROUP, FLIPS_KEY);
			if (flipsJson != null && !flipsJson.isEmpty())
			{
				Type flipListType = new TypeToken<List<FlipTransaction>>(){}.getType();
				List<FlipTransaction> flips = gson.fromJson(flipsJson, flipListType);
				completedFlips.addAll(flips);

				log.debug("Loaded {} flips from config", flips.size());
			}
		}
		catch (Exception e)
		{
			log.error("Error loading trade history", e);
		}
	}
}
