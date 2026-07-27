package scorched.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scorched.enums.PlayerDifficulty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    private Player player;

    @Mock
    private Inventory mockInventory;

    @BeforeEach
    void setUp() {
        player = new Player();
    }

    @Test
    @DisplayName("Constructor should initialize default inventory and default fields")
    void testConstructorInitialization() {
        assertNotNull(player.getInventory(), "Inventory should be initialized by default");
        assertNull(player.getPlayerName(), "Player name should default to null");
        assertFalse(player.isAI(), "isAI should default to false");
        assertNull(player.getPlayerDifficulty(), "Player difficulty should default to null");
        assertEquals(0, player.getMoney(), "Money should default to 0");
    }

    @Test
    @DisplayName("Should correctly set and get player name")
    void testSetAndGetPlayerName() {
        String expectedName = "Player1";
        player.setPlayerName(expectedName);

        assertEquals(expectedName, player.getPlayerName());
    }

    @Test
    @DisplayName("Should correctly set and get AI flag")
    void testSetAndIsAI() {
        player.setAI(true);
        assertTrue(player.isAI());

        player.setAI(false);
        assertFalse(player.isAI());
    }

    @Test
    @DisplayName("Should correctly set and get player difficulty")
    void testSetAndGetPlayerDifficulty() {
        PlayerDifficulty difficulty = PlayerDifficulty.HARD; // Example enum value
        player.setPlayerDifficulty(difficulty);

        assertEquals(difficulty, player.getPlayerDifficulty());
    }

    @Test
    @DisplayName("Should set and get mocked inventory dependency")
    void testSetAndGetInventory() {
        player.setInventory(mockInventory);

        assertEquals(mockInventory, player.getInventory());
    }

    @Test
    @DisplayName("Should correctly set and get money")
    void testSetAndGetMoney() {
        int expectedMoney = 500;
        player.setMoney(expectedMoney);

        assertEquals(expectedMoney, player.getMoney());
    }
}