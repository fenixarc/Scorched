package scorched.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class MenuUI {

    // Centralized Style Constants
    public static final Color BOX_BG_COLOR = new Color(25, 30, 55);
    public static final Color BOX_BORDER_SELECTED = Color.YELLOW;
    public static final Color BOX_BORDER_UNSELECTED = Color.CYAN;
    public static final Color TEXT_COLOR = Color.WHITE;
    public static final Font MENU_FONT = new Font("Arial", Font.BOLD, 22);

    /**
     * Draws a standardized menu choice box centered on the screen.
     */
    public static void drawMenuOptionBox(Graphics2D g2d, String text, int boxY, int width, int height, boolean isSelected) {
        int screenWidth = g2d.getClipBounds().width;
        int boxX = (screenWidth - width) / 2;

        // 1. Draw Background Box
        g2d.setColor(BOX_BG_COLOR);
        g2d.fillRect(boxX, boxY, width, height);

        // 2. Draw Highlight/Border
        g2d.setColor(isSelected ? BOX_BORDER_SELECTED : BOX_BORDER_UNSELECTED);
        g2d.drawRect(boxX, boxY, width, height);

        // 3. Draw Centered Text
        g2d.setFont(MENU_FONT);
        g2d.setColor(TEXT_COLOR);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = screenWidth / 2 - (fm.stringWidth(text) / 2);
        int textY = boxY + (height / 2) + (fm.getAscent() / 2) - 2; // Mathematically centered

        g2d.drawString(text, textX, textY);
    }
}