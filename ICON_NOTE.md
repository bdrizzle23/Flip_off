# Icon File Note

The file `src/main/resources/icon.png` currently contains a placeholder text file.

For the plugin to display correctly in RuneLite, you need to replace this with an actual PNG image:

- **Size**: Maximum 48x72 pixels (recommended)
- **Format**: PNG
- **Content**: A coin, chart, or trading-related symbol
- **Location**: `src/main/resources/icon.png`

You can create this using any image editor or online tool. A simple gold coin or upward-trending chart icon would work well for this flipping plugin.

The icon will be loaded in FlippingPlugin.java at line 95:
```java
final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
```

Once you have a proper icon PNG file, replace the current placeholder file at `src/main/resources/icon.png`.
