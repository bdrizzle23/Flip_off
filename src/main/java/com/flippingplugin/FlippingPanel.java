package com.flippingplugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FlippingPanel extends PluginPanel
{
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00");
	private static final Color PROFIT_COLOR = new Color(0, 200, 0);
	private static final Color LOSS_COLOR = new Color(200, 0, 0);

	private final JPanel listContainer = new JPanel();
	private final PluginErrorPanel errorPanel = new PluginErrorPanel();
	private final JLabel titleLabel = new JLabel();
	private final JButton refreshButton = new JButton("Refresh");

	@Inject
	public FlippingPanel()
	{
		super(false);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Title panel
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		titleLabel.setText("Flipping Opportunities");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

		refreshButton.setToolTipText("Refresh opportunities");
		refreshButton.setFocusable(false);

		titlePanel.add(titleLabel, BorderLayout.WEST);
		titlePanel.add(refreshButton, BorderLayout.EAST);

		// List container
		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
		listContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(listContainer, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(listWrapper);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// Error panel
		errorPanel.setContent("Flipping Opportunities",
			"No opportunities found. Make sure you're logged in and price data is available.");

		add(titlePanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		showError();
	}

	public void setRefreshAction(Runnable action)
	{
		for (var listener : refreshButton.getActionListeners())
		{
			refreshButton.removeActionListener(listener);
		}
		refreshButton.addActionListener(e -> action.run());
	}

	public void updateOpportunities(List<FlippingOpportunity> opportunities)
	{
		SwingUtilities.invokeLater(() ->
		{
			listContainer.removeAll();

			if (opportunities == null || opportunities.isEmpty())
			{
				showError();
				return;
			}

			hideError();

			for (FlippingOpportunity opportunity : opportunities)
			{
				listContainer.add(buildOpportunityPanel(opportunity));
				listContainer.add(Box.createRigidArea(new Dimension(0, 5)));
			}

			revalidate();
			repaint();
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

	private Color getGradeColor(OpportunityGrade grade)
	{
		switch (grade)
		{
			case A_PLUS:
			case A:
				return new Color(0, 255, 0); // Bright green
			case B_PLUS:
			case B:
				return new Color(100, 255, 100); // Light green
			case C_PLUS:
			case C:
				return new Color(255, 255, 0); // Yellow
			case D:
				return new Color(255, 165, 0); // Orange
			case F:
			default:
				return new Color(255, 0, 0); // Red
		}
	}

	private Color getPatternColor(MarketPattern pattern)
	{
		switch (pattern)
		{
			case FLASH_CRASH:
				return new Color(255, 50, 50); // Bright red
			case DUMP:
				return new Color(255, 100, 100); // Light red
			case RECOVERY:
				return new Color(100, 255, 100); // Light green
			case MANIPULATION:
				return new Color(255, 165, 0); // Orange
			case VOLATILITY_SPIKE:
				return new Color(255, 255, 0); // Yellow
			case TREND_REVERSAL:
				return new Color(173, 216, 230); // Light blue
			case STABLE_SPREAD:
				return new Color(144, 238, 144); // Light green
			default:
				return Color.LIGHT_GRAY;
		}
	}

	private void showError()
	{
		removeAll();
		add(errorPanel, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	private void hideError()
	{
		remove(errorPanel);
	}
}
