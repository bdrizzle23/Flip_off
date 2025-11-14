package com.flippingplugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Flipping Optimizer",
	description = "Advanced flipping plugin with pattern recognition, opportunity scoring, and smart alerts",
	tags = {"flipping", "grand exchange", "ge", "trading", "money making"}
)
public class FlippingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FlippingConfig config;

	@Inject
	private PriceDataService priceDataService;

	@Inject
	private PatternDetector patternDetector;

	@Inject
	private OpportunityScorer opportunityScorer;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Notifier notifier;

	@Inject
	private DiscordNotifier discordNotifier;

	@Inject
	private TradeHistoryManager tradeHistoryManager;

	@Inject
	private ScheduledExecutorService executorService;

	private FlippingPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> updateTask;
	private final List<FlippingOpportunity> currentOpportunities = new ArrayList<>();
	private final Map<Integer, Long> discordCooldowns = new HashMap<>();
	private final Map<Integer, Long> flipNotificationCooldowns = new HashMap<>();
	private final Map<Integer, Trade.TradeType> slotTypes = new HashMap<>(); // Track buy/sell for each slot
	private int pendingOfferSlot = -1; // Track which slot is being set up

	@Override
	protected void startUp() throws Exception
	{
		log.info("Flipping Optimizer started!");

		// Create and add panel
		panel = injector.getInstance(FlippingPanel.class);
		// Wrap refresh action in executor to run on background thread (avoid AWT thread blocking)
		panel.setRefreshAction(() -> executorService.submit(this::refreshOpportunities));

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		if (icon == null)
		{
			log.warn("Could not load icon.png, creating default icon");
			// Create a simple green square icon as default
			icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = icon.createGraphics();
			g.setColor(new java.awt.Color(0, 200, 0)); // Green
			g.fillRect(0, 0, 16, 16);
			g.setColor(java.awt.Color.WHITE);
			g.drawString("$", 4, 12);
			g.dispose();
		}

		navButton = NavigationButton.builder()
			.tooltip("Flipping Optimizer")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		if (config.enablePanel())
		{
			clientToolbar.addNavigation(navButton);
			log.info("Side panel added to toolbar");
		}
		else
		{
			log.info("Side panel disabled in config");
		}

		// Start auto-refresh if enabled
		if (config.autoRefresh())
		{
			scheduleUpdates();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Flipping Optimizer stopped!");

		// Cancel scheduled updates
		if (updateTask != null && !updateTask.isCancelled())
		{
			updateTask.cancel(false);
		}

		// Remove panel
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}

		// Clear data
		currentOpportunities.clear();
		discordCooldowns.clear();
		priceDataService.clearCache();
		patternDetector.clearHistory();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Player logged in, fetching initial price data");
			executorService.schedule(this::refreshOpportunities, 5, TimeUnit.SECONDS);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		// Track when player clicks "Buy" or "Sell" in GE interface to determine offer type
		// This helps us determine the type of offer for each slot
		String menuOption = event.getMenuOption();
		String menuTarget = event.getMenuTarget();

		// Check if this is a GE "Create Offer" click (when setting up buy/sell)
		if (menuOption.equals("Examine") && menuTarget.contains("Grand Exchange"))
		{
			// Player is using the GE, next click will determine type
			return;
		}

		// When player confirms a buy or sell offer
		if (menuOption.equals("Confirm"))
		{
			// The offer type was set in previous interactions
			// We'll track it based on the offer changes
			return;
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (!config.enableTradeTracking())
		{
			return;
		}

		GrandExchangeOffer offer = event.getOffer();
		int slot = event.getSlot();

		// Get item info
		int itemId = offer.getItemId();
		String itemName = getItemName(itemId);
		int price = offer.getPrice();
		int totalQuantity = offer.getTotalQuantity();
		int currentQuantity = offer.getQuantitySold();
		int actualQuantity = currentQuantity; // Quantity that has been bought/sold

		// Determine trade type from cached slot info
		Trade.TradeType tradeType = slotTypes.get(slot);
		if (tradeType == null)
		{
			// Try to infer from the trade history manager's knowledge of this slot
			Trade existingTrade = tradeHistoryManager.getTradeBySlot(slot);
			if (existingTrade != null)
			{
				tradeType = existingTrade.getType();
				slotTypes.put(slot, tradeType);
			}
			else
			{
				// No existing knowledge - we'll infer based on the state change
				// If the offer is being created (BUYING/SELLING state), we need to determine type
				// For now, we'll ask the history manager to help track this
				// Default to BUY if we can't determine
				tradeType = Trade.TradeType.BUY;
				log.debug("Unknown slot type for slot {}, defaulting to BUY. " +
					"Manual correction may be needed via trade history.", slot);
			}
		}

		// Map GE offer state to trade state
		Trade.TradeState tradeState = mapOfferState(offer.getState());

		// Update trade in history manager
		tradeHistoryManager.updateTrade(
			slot,
			itemId,
			itemName,
			actualQuantity,
			price,
			currentQuantity,
			totalQuantity,
			tradeType,
			tradeState
		);

		// Update panel when trade completes
		if (tradeState == Trade.TradeState.COMPLETED && panel != null)
		{
			panel.updateHistory();
			panel.updateStatistics();
		}

		// Check for completed flips and send Discord notifications
		if (tradeState == Trade.TradeState.COMPLETED && config.enableDiscord() && config.discordNotifyFlips())
		{
			// Get recent flips for this item
			List<FlipTransaction> recentFlips = tradeHistoryManager.getAllCompletedFlips().stream()
				.filter(f -> f.getItemId() == itemId)
				.filter(f -> System.currentTimeMillis() - f.getSellTime() < 5000) // Last 5 seconds
				.collect(Collectors.toList());

			for (FlipTransaction flip : recentFlips)
			{
				sendFlipDiscordNotification(flip);
			}
		}
	}

	/**
	 * Map GrandExchangeOfferState to Trade.TradeState
	 */
	private Trade.TradeState mapOfferState(GrandExchangeOfferState state)
	{
		switch (state)
		{
			case EMPTY:
				return Trade.TradeState.EMPTY;
			case CANCELLED_BUY:
			case CANCELLED_SELL:
				return Trade.TradeState.CANCELLED;
			case BUYING:
			case SELLING:
				return Trade.TradeState.PENDING;
			case BOUGHT:
			case SOLD:
				return Trade.TradeState.COMPLETED;
			default:
				return Trade.TradeState.EMPTY;
		}
	}

	/**
	 * Schedules periodic updates based on config
	 */
	private void scheduleUpdates()
	{
		if (updateTask != null && !updateTask.isCancelled())
		{
			updateTask.cancel(false);
		}

		int intervalSeconds = config.updateInterval();
		updateTask = executorService.scheduleAtFixedRate(
			this::refreshOpportunities,
			0,
			intervalSeconds,
			TimeUnit.SECONDS
		);

		log.debug("Scheduled updates every {} seconds", intervalSeconds);
	}

	/**
	 * Refreshes flipping opportunities from OSRS Wiki API
	 */
	private void refreshOpportunities()
	{
		try
		{
			log.info("Refreshing flipping opportunities...");

			// Fetch latest prices
			Map<Integer, ItemPriceData> priceData = priceDataService.fetchLatestPrices();
			log.info("Fetched {} items from OSRS Wiki API", priceData.size());

			if (priceData.isEmpty())
			{
				log.warn("No price data available from API");
				return;
			}

			List<FlippingOpportunity> opportunities = new ArrayList<>();
			int filteredByVolume = 0;
			int filteredByPrice = 0;
			int filteredByRoi = 0;
			int filteredByProfit = 0;
			int filteredByGrade = 0;

			for (Map.Entry<Integer, ItemPriceData> entry : priceData.entrySet())
			{
				int itemId = entry.getKey();
				ItemPriceData data = entry.getValue();

				// Filter by volume threshold
				if (!priceDataService.meetsVolumeThreshold(data, config.minVolume()))
				{
					filteredByVolume++;
					continue;
				}

				// Filter by valid prices
				if (data.getLowPrice() <= 0 || data.getHighPrice() <= 0)
				{
					filteredByPrice++;
					continue;
				}

				// Update pattern history
				patternDetector.updateHistory(data);

				// Detect pattern
				MarketPattern pattern = patternDetector.detectPattern(itemId, data);

				// Get item name
				String itemName = getItemName(itemId);

				// Create opportunity
				FlippingOpportunity opportunity = opportunityScorer.createOpportunity(
					itemId,
					itemName,
					data,
					pattern
				);

				// Filter by minimum ROI
				if (opportunity.getRoiPercentage() < config.minROI())
				{
					filteredByRoi++;
					continue;
				}

				// Filter by minimum profit
				if (opportunity.getProfitAfterTax() < config.minProfit())
				{
					filteredByProfit++;
					continue;
				}

				// Filter by grade
				if (opportunity.getGrade().getMinScore() < config.minGrade().getMinScore())
				{
					filteredByGrade++;
					continue;
				}

				opportunities.add(opportunity);
			}

			// Log filtering results
			log.info("Filtered items - Volume: {}, Price: {}, ROI: {}, Profit: {}, Grade: {}",
				filteredByVolume, filteredByPrice, filteredByRoi, filteredByProfit, filteredByGrade);
			log.info("Found {} opportunities after filtering", opportunities.size());

			// Sort by score (highest first)
			opportunities.sort(Comparator.comparingInt(FlippingOpportunity::getScore).reversed());

			// Limit to max opportunities
			int maxOpportunities = config.maxOpportunities();
			if (opportunities.size() > maxOpportunities)
			{
				opportunities = opportunities.subList(0, maxOpportunities);
			}

			// Update current opportunities
			synchronized (currentOpportunities)
			{
				currentOpportunities.clear();
				currentOpportunities.addAll(opportunities);
			}

			log.info("Updating panel with {} opportunities", opportunities.size());

			// Update panel
			if (panel != null)
			{
				panel.updateOpportunities(opportunities);
				panel.updateHistory();
				panel.updateStatistics();
				log.info("Panel updated successfully");
			}
			else
			{
				log.warn("Panel is null, cannot update UI");
			}

			// Send notifications for high-quality opportunities
			if (config.enableNotifications())
			{
				sendNotifications(opportunities);
			}

			// Send Discord notifications if enabled
			if (config.enableDiscord())
			{
				sendDiscordNotifications(opportunities);
			}

			log.debug("Found {} flipping opportunities", opportunities.size());
		}
		catch (Exception e)
		{
			log.error("Error refreshing opportunities", e);
		}
	}

	/**
	 * Sends notifications for high-quality opportunities
	 */
	private void sendNotifications(List<FlippingOpportunity> opportunities)
	{
		OpportunityGrade notifyGrade = config.notifyGrade();

		for (FlippingOpportunity opp : opportunities)
		{
			// Flash crash always notifies if enabled
			if (config.flashCrashAlert() && opp.getPattern() == MarketPattern.FLASH_CRASH)
			{
				notifier.notify("Flash Crash Detected: " + opp.getItemName() +
					" - " + opp.getProfitAfterTax() + " gp profit");
				continue;
			}

			// Notify on pattern detection
			if (config.notifyPatterns() && opp.getPattern() != MarketPattern.NONE &&
				opp.getGrade().getMinScore() >= notifyGrade.getMinScore())
			{
				notifier.notify(opp.getPattern().getDisplayName() + ": " + opp.getItemName() +
					" [" + opp.getGrade().getDisplayName() + "]");
				continue;
			}

			// Notify on high grade
			if (opp.getGrade().getMinScore() >= notifyGrade.getMinScore())
			{
				notifier.notify("Opportunity: " + opp.getItemName() +
					" [" + opp.getGrade().getDisplayName() + "] - " +
					opp.getProfitAfterTax() + " gp");
			}
		}
	}

	/**
	 * Gets item name from item ID
	 */
	private String getItemName(int itemId)
	{
		ItemComposition itemComp = itemManager.getItemComposition(itemId);
		if (itemComp != null && itemComp.getName() != null)
		{
			return itemComp.getName();
		}
		return "Item #" + itemId;
	}

	/**
	 * Gets current opportunities (for external access)
	 */
	public List<FlippingOpportunity> getCurrentOpportunities()
	{
		synchronized (currentOpportunities)
		{
			return new ArrayList<>(currentOpportunities);
		}
	}

	/**
	 * Sends Discord notifications for high-quality opportunities
	 */
	private void sendDiscordNotifications(List<FlippingOpportunity> opportunities)
	{
		String webhookUrl = config.discordWebhookUrl();
		if (webhookUrl == null || webhookUrl.trim().isEmpty())
		{
			log.debug("Discord webhook URL not configured");
			return;
		}

		OpportunityGrade minGrade = config.discordMinGrade();
		DiscordPatternFilter patternFilter = config.discordPatternFilter();
		long cooldownMillis = config.discordCooldown() * 60 * 1000L;
		long currentTime = System.currentTimeMillis();

		// Filter opportunities by grade, pattern, and cooldown
		List<FlippingOpportunity> discordOpportunities = opportunities.stream()
			.filter(opp -> opp.getGrade().getMinScore() >= minGrade.getMinScore())
			.filter(patternFilter::matches)
			.filter(opp -> {
				Long lastNotified = discordCooldowns.get(opp.getItemId());
				if (lastNotified == null || (currentTime - lastNotified) >= cooldownMillis)
				{
					discordCooldowns.put(opp.getItemId(), currentTime);
					return true;
				}
				return false;
			})
			.collect(Collectors.toList());

		if (discordOpportunities.isEmpty())
		{
			log.debug("No opportunities meet Discord notification criteria");
			return;
		}

		// Send notifications
		if (config.discordBatchNotifications())
		{
			// Send as batch summary
			discordNotifier.sendOpportunitySummary(webhookUrl, discordOpportunities, discordOpportunities.size());
		}
		else
		{
			// Send individual notifications
			for (FlippingOpportunity opp : discordOpportunities)
			{
				discordNotifier.sendOpportunity(webhookUrl, opp, opp.getItemName());
			}
		}

		log.debug("Sent {} Discord notification(s)", discordOpportunities.size());
	}

	/**
	 * Send Discord notification for a completed flip
	 */
	private void sendFlipDiscordNotification(FlipTransaction flip)
	{
		String webhookUrl = config.discordWebhookUrl();
		if (webhookUrl == null || webhookUrl.trim().isEmpty())
		{
			return;
		}

		// Check cooldown
		long cooldownMillis = config.discordFlipCooldown() * 60 * 1000L;
		long currentTime = System.currentTimeMillis();
		Long lastNotified = flipNotificationCooldowns.get(flip.getItemId());

		if (lastNotified != null && (currentTime - lastNotified) < cooldownMillis)
		{
			return; // Still in cooldown
		}

		flipNotificationCooldowns.put(flip.getItemId(), currentTime);

		// Send notification via DiscordNotifier
		discordNotifier.sendFlipCompletion(webhookUrl, flip);
	}

	@Provides
	FlippingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlippingConfig.class);
	}
}
