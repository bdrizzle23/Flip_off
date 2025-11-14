package com.flippingplugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class FlippingPanel extends PluginPanel
{
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MMM dd HH:mm");
	private static final Color PROFIT_COLOR = new Color(0, 200, 0);
	private static final Color LOSS_COLOR = new Color(200, 0, 0);

	private final FlippingConfig config;
	private final TradeHistoryManager tradeHistoryManager;

	// Main components
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JButton refreshButton = new JButton("Refresh");

	// Opportunities tab
	private final JPanel opportunitiesContainer = new JPanel();
	private final PluginErrorPanel opportunitiesErrorPanel = new PluginErrorPanel();

	// History tab
	private final JPanel historyContainer = new JPanel();
	private final JButton clearHistoryButton = new JButton("Clear History");
	private final PluginErrorPanel historyErrorPanel = new PluginErrorPanel();

	// Stats tab
	private final JPanel statsContainer = new JPanel();
	private final JLabel totalProfitLabel = new JLabel();
	private final JLabel totalFlipsLabel = new JLabel();
	private final JLabel avgRoiLabel = new JLabel();
	private final JLabel todayProfitLabel = new JLabel();
	private final JLabel weekProfitLabel = new JLabel();

	@Inject
	public FlippingPanel(FlippingConfig config, TradeHistoryManager tradeHistoryManager)
	{
		super(false);
		this.config = config;
		this.tradeHistoryManager = tradeHistoryManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Title panel
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel titleLabel = new JLabel("Flipping Optimizer");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

		refreshButton.setToolTipText("Refresh data");
		refreshButton.setFocusable(false);

		titlePanel.add(titleLabel, BorderLayout.WEST);
		titlePanel.add(refreshButton, BorderLayout.EAST);

		// Setup tabs
		setupOpportunitiesTab();
		setupHistoryTab();
		setupStatsTab();

		tabbedPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(titlePanel, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);
	}

	private void setupOpportunitiesTab()
	{
		JPanel tabPanel = new JPanel(new BorderLayout());
		tabPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		opportunitiesContainer.setLayout(new BoxLayout(opportunitiesContainer, BoxLayout.Y_AXIS));
		opportunitiesContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(opportunitiesContainer, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(listWrapper);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		opportunitiesErrorPanel.setContent("No Opportunities",
			"No opportunities found. Make sure you're logged in and price data is available.");

		tabPanel.add(scrollPane, BorderLayout.CENTER);
		tabbedPane.addTab("Opportunities", tabPanel);
	}

	private void setupHistoryTab()
	{
		JPanel tabPanel = new JPanel(new BorderLayout());
		tabPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		clearHistoryButton.setToolTipText("Clear all flip history");
		clearHistoryButton.setFocusable(false);
		clearHistoryButton.addActionListener(e -> confirmAndClearHistory());
		buttonPanel.add(clearHistoryButton);

		historyContainer.setLayout(new BoxLayout(historyContainer, BoxLayout.Y_AXIS));
		historyContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(historyContainer, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(listWrapper);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		historyErrorPanel.setContent("No History",
			"No completed flips yet. Start flipping items to build your history!");

		tabPanel.add(buttonPanel, BorderLayout.NORTH);
		tabPanel.add(scrollPane, BorderLayout.CENTER);
		tabbedPane.addTab("History", tabPanel);
	}

	private void setupStatsTab()
	{
		JPanel tabPanel = new JPanel(new BorderLayout());
		tabPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		statsContainer.setLayout(new BoxLayout(statsContainer, BoxLayout.Y_AXIS));
		statsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statsContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

		// Create stat panels
		statsContainer.add(createStatSection("All Time", totalFlipsLabel, totalProfitLabel, avgRoiLabel));
		statsContainer.add(Box.createRigidArea(new Dimension(0, 15)));
		statsContainer.add(createStatSection("Today", todayProfitLabel, null, null));
		statsContainer.add(Box.createRigidArea(new Dimension(0, 15)));
		statsContainer.add(createStatSection("This Week", weekProfitLabel, null, null));

		JScrollPane scrollPane = new JScrollPane(statsContainer);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		tabPanel.add(scrollPane, BorderLayout.CENTER);
		tabbedPane.addTab("Statistics", tabPanel);
	}

	private JPanel createStatSection(String title, JLabel... labels)
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
			new EmptyBorder(10, 10, 10, 10)
		));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		section.add(titleLabel);
		section.add(Box.createRigidArea(new Dimension(0, 10)));

		for (JLabel label : labels)
		{
			if (label != null)
			{
				label.setForeground(Color.LIGHT_GRAY);
				label.setFont(new Font("Arial", Font.PLAIN, 12));
				label.setAlignmentX(Component.LEFT_ALIGNMENT);
				section.add(label);
				section.add(Box.createRigidArea(new Dimension(0, 5)));
			}
		}

		return section;
	}

	public void setRefreshAction(Runnable action)
	{
		for (var listener : refreshButton.getActionListeners())
		{
			refreshButton.removeActionListener(listener);
		}
		refreshButton.addActionListener(e -> {
			action.run();
			updateHistory();
			updateStatistics();
		});
	}

	public void updateOpportunities(List<FlippingOpportunity> opportunities)
	{
		SwingUtilities.invokeLater(() ->
		{
			opportunitiesContainer.removeAll();

			if (opportunities == null || opportunities.isEmpty())
			{
				opportunitiesContainer.add(opportunitiesErrorPanel);
			}
			else
			{
				for (FlippingOpportunity opportunity : opportunities)
				{
					opportunitiesContainer.add(buildOpportunityPanel(opportunity));
					opportunitiesContainer.add(Box.createRigidArea(new Dimension(0, 5)));
				}
			}

			opportunitiesContainer.revalidate();
			opportunitiesContainer.repaint();
		});
	}

	public void updateHistory()
	{
		if (!config.showTradeHistory())
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			historyContainer.removeAll();

			List<FlipTransaction> flips = tradeHistoryManager.getCompletedFlips(config.maxTradeHistory());

			if (flips == null || flips.isEmpty())
			{
				historyContainer.add(historyErrorPanel);
			}
			else
			{
				// Show most recent first
				for (int i = flips.size() - 1; i >= 0; i--)
				{
					historyContainer.add(buildFlipPanel(flips.get(i)));
					historyContainer.add(Box.createRigidArea(new Dimension(0, 5)));
				}
			}

			historyContainer.revalidate();
			historyContainer.repaint();
		});
	}

	public void updateStatistics()
	{
		SwingUtilities.invokeLater(() ->
		{
			// All time stats
			long totalProfit = tradeHistoryManager.getTotalProfit();
			List<FlipTransaction> allFlips = tradeHistoryManager.getAllCompletedFlips();
			double avgRoi = allFlips.stream()
				.mapToDouble(FlipTransaction::getRoi)
				.average()
				.orElse(0);

			totalFlipsLabel.setText("Total Flips: " + allFlips.size());
			totalProfitLabel.setText("Total Profit: " + formatProfit(totalProfit));
			avgRoiLabel.setText("Average ROI: " + DECIMAL_FORMAT.format(avgRoi) + "%");

			// Today stats
			long todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
			Map<String, Object> todayStats = tradeHistoryManager.getStats(todayStart);
			long todayProfit = (long) todayStats.get("totalProfit");
			int todayFlips = (int) todayStats.get("flipCount");
			todayProfitLabel.setText("Flips: " + todayFlips + " | Profit: " + formatProfit(todayProfit));

			// Week stats
			long weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
			Map<String, Object> weekStats = tradeHistoryManager.getStats(weekStart);
			long weekProfit = (long) weekStats.get("totalProfit");
			int weekFlips = (int) weekStats.get("flipCount");
			weekProfitLabel.setText("Flips: " + weekFlips + " | Profit: " + formatProfit(weekProfit));

			statsContainer.revalidate();
			statsContainer.repaint();
		});
	}

	private JPanel buildOpportunityPanel(FlippingOpportunity opportunity)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		// Top row: Item name and grade
		JPanel topRow = new JPanel(new BorderLayout());
		topRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel itemName = new JLabel(opportunity.getItemName());
		itemName.setForeground(Color.WHITE);
		itemName.setFont(new Font("Arial", Font.BOLD, 12));

		JLabel gradeLabel = new JLabel(opportunity.getGrade().getDisplayName());
		gradeLabel.setForeground(getGradeColor(opportunity.getGrade()));
		gradeLabel.setFont(new Font("Arial", Font.BOLD, 14));

		topRow.add(itemName, BorderLayout.WEST);
		topRow.add(gradeLabel, BorderLayout.EAST);

		// Middle row: Buy/Sell prices
		JPanel priceRow = new JPanel(new GridLayout(1, 2));
		priceRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel buyLabel = new JLabel("Buy: " + QuantityFormatter.formatNumber(opportunity.getBuyPrice()) + " gp");
		buyLabel.setForeground(Color.LIGHT_GRAY);
		buyLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		JLabel sellLabel = new JLabel("Sell: " + QuantityFormatter.formatNumber(opportunity.getSellPrice()) + " gp");
		sellLabel.setForeground(Color.LIGHT_GRAY);
		sellLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		priceRow.add(buyLabel);
		priceRow.add(sellLabel);

		// Profit row
		JPanel profitRow = new JPanel(new BorderLayout());
		profitRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String profitText = "Profit: " + QuantityFormatter.formatNumber(opportunity.getProfitAfterTax()) +
			" gp (" + DECIMAL_FORMAT.format(opportunity.getRoiPercentage()) + "%)";
		JLabel profitLabel = new JLabel(profitText);
		profitLabel.setForeground(opportunity.getProfitAfterTax() > 0 ? PROFIT_COLOR : LOSS_COLOR);
		profitLabel.setFont(new Font("Arial", Font.BOLD, 12));

		profitRow.add(profitLabel, BorderLayout.WEST);

		// Pattern and volume row
		JPanel infoRow = new JPanel(new BorderLayout());
		infoRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel patternLabel = new JLabel(opportunity.getPattern().getDisplayName());
		patternLabel.setForeground(getPatternColor(opportunity.getPattern()));
		patternLabel.setFont(new Font("Arial", Font.ITALIC, 10));

		JLabel volumeLabel = new JLabel("Vol: " + QuantityFormatter.formatNumber(opportunity.getDailyVolume()));
		volumeLabel.setForeground(Color.LIGHT_GRAY);
		volumeLabel.setFont(new Font("Arial", Font.PLAIN, 10));

		infoRow.add(patternLabel, BorderLayout.WEST);
		infoRow.add(volumeLabel, BorderLayout.EAST);

		// Assemble panel
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		content.add(topRow);
		content.add(Box.createRigidArea(new Dimension(0, 3)));
		content.add(priceRow);
		content.add(Box.createRigidArea(new Dimension(0, 3)));
		content.add(profitRow);
		content.add(Box.createRigidArea(new Dimension(0, 3)));
		content.add(infoRow);

		panel.add(content, BorderLayout.CENTER);

		return panel;
	}

	private JPanel buildFlipPanel(FlipTransaction flip)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		// Top row: Item name and profit
		JPanel topRow = new JPanel(new BorderLayout());
		topRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel itemName = new JLabel(flip.getItemName() + " x" + flip.getQuantity());
		itemName.setForeground(Color.WHITE);
		itemName.setFont(new Font("Arial", Font.BOLD, 12));

		JLabel profitLabel = new JLabel(formatProfit(flip.getProfit()));
		profitLabel.setForeground(flip.getProfit() > 0 ? PROFIT_COLOR : LOSS_COLOR);
		profitLabel.setFont(new Font("Arial", Font.BOLD, 13));

		topRow.add(itemName, BorderLayout.WEST);
		topRow.add(profitLabel, BorderLayout.EAST);

		// Prices row
		JPanel priceRow = new JPanel(new GridLayout(1, 2));
		priceRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel buyLabel = new JLabel("Buy: " + QuantityFormatter.formatNumber(flip.getBuyPrice()) + " gp");
		buyLabel.setForeground(Color.LIGHT_GRAY);
		buyLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		JLabel sellLabel = new JLabel("Sell: " + QuantityFormatter.formatNumber(flip.getSellPrice()) + " gp");
		sellLabel.setForeground(Color.LIGHT_GRAY);
		sellLabel.setFont(new Font("Arial", Font.PLAIN, 11));

		priceRow.add(buyLabel);
		priceRow.add(sellLabel);

		// Stats row
		JPanel statsRow = new JPanel(new BorderLayout());
		statsRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String roiText = "ROI: " + DECIMAL_FORMAT.format(flip.getRoi()) + "% | Tax: " +
			QuantityFormatter.formatNumber(flip.getTax()) + " gp";
		JLabel roiLabel = new JLabel(roiText);
		roiLabel.setForeground(Color.LIGHT_GRAY);
		roiLabel.setFont(new Font("Arial", Font.PLAIN, 10));

		String timeText = TIME_FORMAT.format(new Date(flip.getSellTime()));
		JLabel timeLabel = new JLabel(timeText);
		timeLabel.setForeground(Color.GRAY);
		timeLabel.setFont(new Font("Arial", Font.ITALIC, 9));

		statsRow.add(roiLabel, BorderLayout.WEST);
		statsRow.add(timeLabel, BorderLayout.EAST);

		// Assemble panel
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		content.add(topRow);
		content.add(Box.createRigidArea(new Dimension(0, 3)));
		content.add(priceRow);
		content.add(Box.createRigidArea(new Dimension(0, 3)));
		content.add(statsRow);

		panel.add(content, BorderLayout.CENTER);

		return panel;
	}

	private void confirmAndClearHistory()
	{
		int result = JOptionPane.showConfirmDialog(
			this,
			"Are you sure you want to clear all flip history? This cannot be undone.",
			"Clear History",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);

		if (result == JOptionPane.YES_OPTION)
		{
			tradeHistoryManager.clearHistory();
			updateHistory();
			updateStatistics();
		}
	}

	private String formatProfit(long profit)
	{
		String sign = profit >= 0 ? "+" : "";
		return sign + QuantityFormatter.formatNumber(profit) + " gp";
	}

	private Color getGradeColor(OpportunityGrade grade)
	{
		switch (grade)
		{
			case A_PLUS:
			case A:
				return new Color(0, 255, 0);
			case B_PLUS:
			case B:
				return new Color(100, 255, 100);
			case C_PLUS:
			case C:
				return new Color(255, 255, 0);
			case D:
				return new Color(255, 165, 0);
			case F:
			default:
				return new Color(255, 0, 0);
		}
	}

	private Color getPatternColor(MarketPattern pattern)
	{
		switch (pattern)
		{
			case FLASH_CRASH:
				return new Color(255, 50, 50);
			case DUMP:
				return new Color(255, 100, 100);
			case RECOVERY:
				return new Color(100, 255, 100);
			case MANIPULATION:
				return new Color(255, 165, 0);
			case VOLATILITY_SPIKE:
				return new Color(255, 255, 0);
			case TREND_REVERSAL:
				return new Color(173, 216, 230);
			case STABLE_SPREAD:
				return new Color(144, 238, 144);
			default:
				return Color.LIGHT_GRAY;
		}
	}
}
