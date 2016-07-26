package de.arthurpichlkostner.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.Locale;


public class SnakeGameScreen extends ScreenAdapter {

    public static final float WORLD_SIZE = 480.0f;

    private Preferences prefs;

	I18NBundle snakeStrings;

    ShapeRenderer renderer;
    ExtendViewport viewport;
    Snake snake;
    SpriteBatch batch;
    BitmapFont font;

    @Override
    public void show() {
        renderer = new ShapeRenderer();
        batch = new SpriteBatch();
        // Create the default font
        font = new BitmapFont();
        // Scale it up
        font.getData().setScale(1);
        // Set the filter
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear);
        renderer.setAutoShapeType(true);
        viewport = new ExtendViewport(WORLD_SIZE, WORLD_SIZE);


        prefs = Gdx.app.getPreferences("SnakePreferences");
        int highscore = prefs.getInteger("highscore", 0);

        snake = new Snake(viewport, highscore, prefs);
        Gdx.input.setInputProcessor(snake);

		Locale locale = Locale.getDefault();
        snakeStrings = I18NBundle.createBundle(Gdx.files.internal("SnakeStrings"),
                locale, "windows-1252");
        Gdx.app.log("Init", "Locale = " + locale);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        snake.init();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        prefs.putInteger("highscore", snake.applesEatenHighscore);
        prefs.flush();
    }

    @Override
    public void render(float delta) {
        viewport.apply();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.setProjectionMatrix(viewport.getCamera().combined);
        snake.update(delta);

        renderer.begin(ShapeType.Filled);
        snake.render(renderer);
        renderer.end();

        long deltaTime = TimeUtils.millis() - snake.startTime;

        batch.begin();
        font.draw(batch, snakeStrings.get("time")+": "+deltaTime/1000, viewport.getWorldWidth()-200,
                viewport.getWorldHeight()-10);
        font.draw(batch, snakeStrings.get("apples")+": "+snake.applesEaten,
                viewport.getWorldWidth()-200, viewport.getWorldHeight()-25);
        font.draw(batch, snakeStrings.get("highscore")+": "+snake.applesEatenHighscore,
                viewport.getWorldWidth()-200, viewport.getWorldHeight()-40);
        batch.end();
    }
}
