package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import scorched.sound.SoundEngine;

import java.awt.Color;
import java.awt.Graphics2D;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class WeatherManagerTest {

    private WeatherManager weatherManager;
    private Terrain mockTerrain;
    private Graphics2D mockGraphics;
    
    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;

    @BeforeEach
    void setUp() {
        mockTerrain = mock(Terrain.class);
        mockGraphics = mock(Graphics2D.class);
        weatherManager = new WeatherManager(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Test
    void testInitialWeatherIsClear() {
        assertEquals(WeatherManager.WeatherType.CLEAR, weatherManager.getCurrentType());
    }

    @Test
    void testRandomizeWeatherChangesState() {
        // Run multiple times to ensure it eventually hits non-CLEAR weather due to randomness
        boolean changed = false;
        for (int i = 0; i < 20; i++) {
            weatherManager.randomizeWeather();
            if (weatherManager.getCurrentType() != WeatherManager.WeatherType.CLEAR) {
                changed = true;
                break;
            }
        }
        assertTrue(changed, "Weather should eventually randomize to a non-CLEAR state.");
    }

    @Test
    void testUpdateClearWeatherDoesNotInteractWithTerrain() {
        // Assert initial state is CLEAR
        assertEquals(WeatherManager.WeatherType.CLEAR, weatherManager.getCurrentType());

        weatherManager.update(mockTerrain);

        // Verify terrain interaction never occurs when clear
        verify(mockTerrain, never()).isSolidAt(anyDouble(), anyDouble());
    }

    @Test
    void testUpdateRainOrSnowGeneratesParticles() {
        // Force the weather state by looping until we get RAIN or SNOW
        forceWeatherType(WeatherManager.WeatherType.RAIN);
        
        // Terrain is open sky, particles shouldn't hit solid ground immediately
        when(mockTerrain.isSolidAt(anyDouble(), anyDouble())).thenReturn(false);

        // Update once to spawn particles
        weatherManager.update(mockTerrain);

        // If particles were successfully generated, rendering should invoke drawing operations
        weatherManager.draw(mockGraphics);
        verify(mockGraphics, atLeastOnce()).setColor(any(Color.class));
        verify(mockGraphics, atLeastOnce()).drawLine(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testParticlesAreRemovedWhenHittingTerrain() {
        forceWeatherType(WeatherManager.WeatherType.RAIN);

        // Simulate solid ground everywhere so particles are destroyed immediately upon update
        when(mockTerrain.isSolidAt(anyDouble(), anyDouble())).thenReturn(true);

        weatherManager.update(mockTerrain);

        // Draw should not render any particles since they were removed due to collision
        weatherManager.draw(mockGraphics);
        verify(mockGraphics, never()).drawLine(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testConsumeStrikeResetsFlag() {
        // Initially no strike has occurred
        assertFalse(weatherManager.consumeStrike());

        // We can use reflection or brute force cycles to trigger a lightning strike under STORMY weather
        forceWeatherType(WeatherManager.WeatherType.STORMY);
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(300);

        // Mocking the static sound engine to prevent exceptions/unwanted side effects during heavy cycles
        try (MockedStatic<SoundEngine> mockedSound = Mockito.mockStatic(SoundEngine.class)) {
            // Update until a lightning strike triggers (lightningTimer counts down to 0)
            boolean strikeTriggered = false;
            for (int i = 0; i < 1000; i++) {
                weatherManager.update(mockTerrain);
                if (weatherManager.consumeStrike()) {
                    strikeTriggered = true;
                    break;
                }
            }
            
            assertTrue(strikeTriggered, "A lightning strike should trigger during an extended storm sequence.");
            // Verify that consuming it immediately resets the flag
            assertFalse(weatherManager.consumeStrike(), "Subsequent consumes should return false until a new strike occurs.");
        }
    }

    @Test
    void testLightningStrikeTriggersVisualsAndSound() {
        forceWeatherType(WeatherManager.WeatherType.STORMY);
        when(mockTerrain.getHeightAt(anyInt())).thenReturn(400);

        try (MockedStatic<SoundEngine> mockedSound = Mockito.mockStatic(SoundEngine.class)) {
            // Cycle until flash activates
            for (int i = 0; i < 1000; i++) {
                weatherManager.update(mockTerrain);
                if (weatherManager.hasStrikeImpacted()) {
                    break;
                }
            }

            assertTrue(weatherManager.hasStrikeImpacted());
            assertTrue(weatherManager.getStrikeX() >= 0 && weatherManager.getStrikeX() < SCREEN_WIDTH);
            assertEquals(400, weatherManager.getStrikeY());

            // Verify the static sound engine played thunder
            mockedSound.verify(SoundEngine::playThunderSound, times(1));

            // Verify full-screen flash rendering is drawn
            weatherManager.draw(mockGraphics);
            verify(mockGraphics, atLeastOnce()).fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }
    }

    /**
     * Helper method to brute force random generation to hit a target WeatherType.
     */
    private void forceWeatherType(WeatherManager.WeatherType targetType) {
        int attempts = 0;
        while (weatherManager.getCurrentType() != targetType && attempts < 100) {
            weatherManager.randomizeWeather();
            attempts++;
        }
        assertEquals(targetType, weatherManager.getCurrentType(), "Failed to force target weather type.");
    }
}