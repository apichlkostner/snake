package de.arthurpichlkostner.game;

import com.badlogic.gdx.Game;

public class SnakeGame extends Game {

	@Override
	public void create() {
		setScreen(new SnakeGameScreen());
	}

}
