# Flipping Optimizer - RuneLite Plugin

An advanced Grand Exchange flipping plugin for RuneLite with intelligent pattern recognition, opportunity scoring, and smart alerts.

## Features

### Volume Filtering
- **Minimum Volume Threshold**: Only alerts on items with >1,000 daily trades (configurable)
- **High Volume Preference**: Prioritizes items with higher trading volumes for safer flips

### Pattern Recognition
Detects 7 distinct market patterns:
1. **Flash Crash** - Sudden price drops with quick recovery potential (highest priority)
2. **Price Manipulation** - Unusual price movements suggesting market manipulation
3. **Market Dump** - Large sell-offs creating buying opportunities
4. **Price Recovery** - Items recovering from previous crashes
5. **Volatility Spike** - High price variance indicating trading opportunities
6. **Trend Reversal** - Price trends changing direction
7. **Stable Spread** - Consistent buy/sell spreads with good volume

### Accurate Tax Calculation
- Full 2% Grand Exchange tax implementation
- 5M gold cap on taxes (as per OSRS mechanics)
- Real profit calculations accounting for tax

### Opportunity Scoring
- Grades items from **A+** to **F** based on:
  - ROI (Return on Investment) - 0-30 points
  - Trading Volume - 0-25 points
  - Pattern Detection - 0-25 points
  - Price Volatility - 0-20 points
- Pattern multipliers boost scores for high-value patterns
- Configurable minimum grade filter

### Real-time Price Data
- Uses official OSRS Wiki API for accurate pricing
- 1-minute price cache to avoid API spam
- Auto-refresh at configurable intervals (default: 60 seconds)

### Smart Alerts
- Notifications only for high-quality opportunities
- Configurable grade threshold for alerts
- Special flash crash alerts (always notifies if enabled)
- Pattern-specific notifications

## Configuration Options

### Volume Filters
- **Minimum Daily Volume**: 100-100,000 trades (default: 1,000)
- **Prefer High Volume**: Toggle to prioritize high-volume items

### Profit Filters
- **Minimum ROI %**: 0-50% (default: 2%)
- **Minimum Profit (GP)**: 0-1M gold (default: 1,000 GP)
- **Minimum Grade**: A+ to F (default: C)

### Alerts
- **Enable Notifications**: Toggle all notifications
- **Notification Grade Threshold**: Only notify at or above this grade
- **Notify on Patterns**: Alert when specific patterns detected
- **Flash Crash Alerts**: Always alert on flash crashes (recommended)

### Advanced
- **Update Interval**: 30-600 seconds (default: 60)
- **Max Opportunities Shown**: 5-100 items (default: 20)
- **Enable Side Panel**: Toggle the opportunities panel
- **Auto Refresh**: Automatically refresh at set intervals

## Installation

1. Build the plugin using Gradle:
   ```bash
   ./gradlew build
   ```

2. The plugin will be available for testing via the test runner:
   ```bash
   ./gradlew shadowJar
   java -jar build/libs/Flip_off-1.0-SNAPSHOT-all.jar
   ```

3. For Plugin Hub submission:
   - Fork the [RuneLite Plugin Hub](https://github.com/runelite/plugin-hub)
   - Add your repository URL and commit hash to the plugins directory
   - Create a pull request

## Usage

1. Enable the plugin in RuneLite's plugin panel
2. Log into OSRS
3. Click the Flipping Optimizer icon in the sidebar
4. View ranked flipping opportunities with:
   - Item names and grades (A+ to F)
   - Buy/sell prices
   - Profit after tax and ROI percentage
   - Detected patterns
   - Trading volume

## Technical Details

### API Integration
- **OSRS Wiki Prices API**: `https://prices.runescape.wiki/api/v1/osrs`
- Endpoints: `/latest` for current prices
- User-Agent: "RuneLite Flipping Plugin"
- Respects rate limits with 1-minute caching

### Pattern Detection Algorithms
- **Flash Crash**: >20% price drop with volume spike
- **Manipulation**: High volatility with low volume
- **Dump**: Consistent price decline with high volume
- **Recovery**: Price increasing after decline
- **Volatility Spike**: Standard deviation >20% of mean
- **Trend Reversal**: Direction change >15%
- **Stable Spread**: Volatility <5% with volume >1,000

### Scoring System
- ROI contributes 0-30 points (max at 10%+ ROI)
- Volume contributes 0-25 points (max at 10K+ trades)
- Pattern contributes 0-25 points (weighted by pattern type)
- Volatility contributes 0-20 points (optimal at 5-15%)
- Pattern multipliers: 1.0x to 1.5x based on pattern significance
- Final score capped at 100

## Dependencies

- RuneLite Client API (latest.release)
- Lombok 1.18.30
- OkHttp (via RuneLite)
- Gson (via RuneLite)

## License

BSD 2-Clause License - see LICENSE file for details

## Contributing

Contributions welcome! Please ensure:
- Code follows RuneLite plugin standards
- No malicious code or violations of Jagex guidelines
- Proper testing before submission
- Documentation for new features

## Disclaimer

This plugin is for educational and quality-of-life purposes. Always follow Jagex's official guidelines for third-party clients. The plugin does not:
- Automate any gameplay
- Provide unfair advantages
- Violate OSRS game rules
- Guarantee profits (market conditions vary)

## Support

For issues, suggestions, or questions:
- Open an issue on GitHub
- Check RuneLite documentation
- Review OSRS Wiki for item data accuracy