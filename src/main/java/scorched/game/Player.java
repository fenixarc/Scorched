package scorched.game;

import scorched.enums.PlayerDifficulty;

public class Player {
	
	private String playerName;
	private boolean isAI;
	private PlayerDifficulty playerDifficulty;
	private Inventory inventory;
	private int money;
	
	public Player() {
		this.inventory = new Inventory();
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public boolean isAI() {
		return isAI;
	}

	public void setAI(boolean isAI) {
		this.isAI = isAI;
	}

	public PlayerDifficulty getPlayerDifficulty() {
		return playerDifficulty;
	}

	public void setPlayerDifficulty(PlayerDifficulty playerDifficulty) {
		this.playerDifficulty = playerDifficulty;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	public int getMoney() {
		return money;
	}

	public void setMoney(int money) {
		this.money = money;
	}

}
