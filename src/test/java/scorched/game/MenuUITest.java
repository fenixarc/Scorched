package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuUITest {

    @Mock
    private Graphics2D g2d;

    @Mock
    private FontMetrics fontMetrics;

    private final int screenWidth = 800;
    private final int boxY = 100;
    private final int boxWidth = 200;
    private final int boxHeight = 50;
    private final String text = "Start Game";

    @BeforeEach
    void setUp() {
        // Mock screen bounds
        Rectangle clipBounds = new Rectangle(0, 0, screenWidth, 600);
        when(g2d.getClipBounds()).thenReturn(clipBounds);

        // Mock font metrics calculation
        when(g2d.getFontMetrics()).thenReturn(fontMetrics);
        when(fontMetrics.stringWidth(anyString())).thenReturn(100);
        when(fontMetrics.getAscent()).thenReturn(20);
    }

    @Test
    @DisplayName("Should draw option box with selected highlight border when isSelected is true")
    void testDrawMenuOptionBoxSelected() {
        // Expected calculations:
        // boxX = (800 - 200) / 2 = 300
        int expectedBoxX = 300;
        // textX = (800 / 2) - (100 / 2) = 400 - 50 = 350
        int expectedTextX = 350;
        // textY = 100 + (50 / 2) + (20 / 2) - 2 = 100 + 25 + 10 - 2 = 133
        int expectedTextY = 133;

        MenuUI.drawMenuOptionBox(g2d, text, boxY, boxWidth, boxHeight, true);

        // Verify the rendering order and interactions
        InOrder inOrder = inOrder(g2d);

        // 1. Draw background box
        inOrder.verify(g2d).setColor(MenuUI.BOX_BG_COLOR);
        inOrder.verify(g2d).fillRect(expectedBoxX, boxY, boxWidth, boxHeight);

        // 2. Draw selected border
        inOrder.verify(g2d).setColor(MenuUI.BOX_BORDER_SELECTED);
        inOrder.verify(g2d).drawRect(expectedBoxX, boxY, boxWidth, boxHeight);

        // 3. Draw centered text
        inOrder.verify(g2d).setFont(MenuUI.MENU_FONT);
        inOrder.verify(g2d).setColor(MenuUI.TEXT_COLOR);
        inOrder.verify(g2d).drawString(text, expectedTextX, expectedTextY);
    }

    @Test
    @DisplayName("Should draw option box with unselected highlight border when isSelected is false")
    void testDrawMenuOptionBoxUnselected() {
        int expectedBoxX = 300;

        MenuUI.drawMenuOptionBox(g2d, text, boxY, boxWidth, boxHeight, false);

        InOrder inOrder = inOrder(g2d);

        // Verify that unselected border color is used
        inOrder.verify(g2d).setColor(MenuUI.BOX_BG_COLOR);
        inOrder.verify(g2d).fillRect(expectedBoxX, boxY, boxWidth, boxHeight);
        
        inOrder.verify(g2d).setColor(MenuUI.BOX_BORDER_UNSELECTED);
        inOrder.verify(g2d).drawRect(expectedBoxX, boxY, boxWidth, boxHeight);
    }
}