package com.scorched;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FloatingTextTest {

    private int startX;
    private int startY;
    private String text;
    private Color textColor;
    private int maxLife;

    @BeforeEach
    void setUp() {
        startX = 100;
        startY = 200;
        text = "Critical Hit!";
        textColor = Color.RED;
        maxLife = 60; // 60 frames
    }

    @Test
    @DisplayName("Initialization should set starting positions and fields correctly")
    void testInitialization() {
        FloatingText floatingText = new FloatingText(startX, startY, text, textColor, maxLife);
        
        // Since fields are private, we use update() to verify lifetime/boundaries
        // Object should stay alive for exactly maxLife updates
        for (int i = 0; i < maxLife - 1; i++) {
            assertTrue(floatingText.update(), "Should be alive at frame " + (i + 1));
        }
        assertFalse(floatingText.update(), "Should expire exactly at maxLife frame");
    }

    @Test
    @DisplayName("Update should drift text upwards and expire at the end of life")
    void testUpdateDriftAndExpiration() {
        FloatingText floatingText = new FloatingText(startX, startY, text, textColor, 5);

        // Frame 1 to 4: Should move up by -1.2 pixels per update and remain alive
        assertTrue(floatingText.update());
        assertTrue(floatingText.update());
        assertTrue(floatingText.update());
        assertTrue(floatingText.update());
        
        // Frame 5: Reaches 0 life, returns false
        assertFalse(floatingText.update(), "Should return false when life expires");
    }

    @Test
    @DisplayName("Draw should correctly render text, apply alpha fading, and restore graphics state")
    void testDrawRenderingLogic() {
        FloatingText floatingText = new FloatingText(startX, startY, text, textColor, maxLife);
        
        Graphics2D mockG2d = mock(Graphics2D.class);
        Composite mockOriginalComposite = mock(Composite.class);
        Font mockOriginalFont = mock(Font.class);

        when(mockG2d.getComposite()).thenReturn(mockOriginalComposite);
        when(mockG2d.getFont()).thenReturn(mockOriginalFont);

        // Create an InOrder verifier
        org.mockito.InOrder inOrder = inOrder(mockG2d);

        // Act
        floatingText.draw(mockG2d);

        // 1. Verify original state was gathered
        inOrder.verify(mockG2d).getComposite();
        inOrder.verify(mockG2d).getFont();

        // 2. Verify FIRST setComposite call (custom alpha)
        ArgumentCaptor<Composite> compositeCaptor = ArgumentCaptor.forClass(Composite.class);
        inOrder.verify(mockG2d).setComposite(compositeCaptor.capture());
        AlphaComposite appliedAlpha = (AlphaComposite) compositeCaptor.getValue();
        assertEquals(1.0f, appliedAlpha.getAlpha());

        // 3. Verify FIRST setFont call (Arial font)
        ArgumentCaptor<Font> fontCaptor = ArgumentCaptor.forClass(Font.class);
        inOrder.verify(mockG2d).setFont(fontCaptor.capture());
        Font appliedFont = fontCaptor.getValue();
        assertEquals("Arial", appliedFont.getName());
        assertEquals(Font.BOLD, appliedFont.getStyle());
        assertEquals(16, appliedFont.getSize());

        // 4. Verify drawing operations
        inOrder.verify(mockG2d).setColor(Color.BLACK);
        inOrder.verify(mockG2d).drawString(text, startX + 1, 181);
        inOrder.verify(mockG2d).setColor(textColor);
        inOrder.verify(mockG2d).drawString(text, startX, 180);

        // 5. Verify SECOND calls (Restoration)
        inOrder.verify(mockG2d).setComposite(mockOriginalComposite);
        inOrder.verify(mockG2d).setFont(mockOriginalFont);
    }

    @Test
    @DisplayName("Draw should apply a progressive fade-out effect as life decreases")
    void testDrawFadesOutProgressively() {
        int shortLife = 2;
        FloatingText floatingText = new FloatingText(startX, startY, text, textColor, shortLife);
        Graphics2D mockG2d = mock(Graphics2D.class);
        Composite mockOriginalComposite = mock(Composite.class);

        when(mockG2d.getComposite()).thenReturn(mockOriginalComposite);

        // Create an InOrder verifier
        org.mockito.InOrder inOrder = inOrder(mockG2d);

        // Frame 1: life becomes 1 (1 / 2 = 0.5f alpha)
        floatingText.update();
        floatingText.draw(mockG2d);

        // Verify the first setComposite call strictly in sequence
        ArgumentCaptor<Composite> compositeCaptor = ArgumentCaptor.forClass(Composite.class);
        inOrder.verify(mockG2d).setComposite(compositeCaptor.capture());
        
        AlphaComposite appliedAlpha = (AlphaComposite) compositeCaptor.getValue();
        assertEquals(0.5f, appliedAlpha.getAlpha(), 0.01f);

        // Verify the restoration call safely follows next
        inOrder.verify(mockG2d).setComposite(mockOriginalComposite);
    }
}