package de.arthurpichlkostner.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.LinkedList;
import java.util.Random;


public class Snake extends InputAdapter {

    private static final Color COLOR = Color.RED;
    private static final float DRAG = 0.5f;
    private static final float RADIUS_FACTOR = 1.0f / 40;
    private static final float RADIUS_GROWTH_RATE = 1.5f;
    private static final float MIN_RADIUS_MULTIPLIER = 0.1f;
    private static final float ACCELERATION = 500.0f;
    private static final float MAX_SPEED = 4000.0f;

    private static final float BOMB_RADIUS = 10;

    private static final float KEY_ACCELERATION = 80;

    private static final float ACCELEROMETER_SENSITIVITY = 10f;
    private static final float ACCELERATION_OF_GRAVITY = 9.8f;

    private static final float KICK_VELOCITY = 500.0f;

    private static final float TRAJECTORY_SAMPLE_DIST = 15f;
    private static final float TRAJECTORY_SAMPLE_TIME_MS = 1000;

    long lastKick;

    private LinkedList<Vector2> trajectory;
    private Apples apples;
    private LinkedList<Vector2> bombs;

    private long trajectory_sampletime;
    private Preferences prefs;

    int applesEaten;
    int applesEatenHighscore;
    long startTime;

    float baseRadius;
    float radiusMultiplier;

    Vector2 position;
    Vector2 velocity;

    Viewport viewport;


    public Snake(Viewport viewport) {
        this.viewport = viewport;
        init();
    }

    public Snake(Viewport viewport, int highscore, Preferences prefs, Apples apples) {
        this.viewport = viewport;
        this.applesEatenHighscore = highscore;
        this.apples = apples;
        this.prefs = prefs;
        init();
    }

    public void init() {
        trajectory = new LinkedList<Vector2>();
        bombs = new LinkedList<Vector2>();
        position = new Vector2(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2);
        velocity = new Vector2();
        baseRadius = RADIUS_FACTOR * Math.min(viewport.getWorldWidth(), viewport.getWorldHeight());
        radiusMultiplier = 1;

        trajectory.add(position.cpy());
        trajectory_sampletime = TimeUtils.millis();
        startTime = trajectory_sampletime;
        applesEaten = 0;
    }


    public void update(float delta) {
        // Movement
        if (Gdx.input.isKeyPressed(Keys.LEFT)){
            velocity.x -= delta * ACCELERATION;
            position.x -= delta * KEY_ACCELERATION;

        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT)){
            velocity.x += delta * ACCELERATION;
            position.x += delta * KEY_ACCELERATION;

        }
        if (Gdx.input.isKeyPressed(Keys.UP)){
            velocity.y += delta * ACCELERATION;
            position.y += delta * KEY_ACCELERATION;

        }
        if (Gdx.input.isKeyPressed(Keys.DOWN)){
            velocity.y -= delta * ACCELERATION;
            position.y -= delta * KEY_ACCELERATION;
        }


        // Accelerometer Movement
        float x_dir = Gdx.input.getAccelerometerX();
        float y_dir = Gdx.input.getAccelerometerY();

        velocity.x =  delta * ACCELERATION * y_dir * ACCELEROMETER_SENSITIVITY;
        velocity.y = -delta * ACCELERATION * x_dir * ACCELEROMETER_SENSITIVITY;

        velocity.clamp(0, MAX_SPEED);

        velocity.x -= delta * DRAG * velocity.x;
        velocity.y -= delta * DRAG * velocity.y;

        position.x += delta * velocity.x;
        position.y += delta * velocity.y;

        Vector2 head = trajectory.getFirst();
        if (position.dst(head) > TRAJECTORY_SAMPLE_DIST) {
            Vector2 newHead = position.cpy().sub(head).setLength(TRAJECTORY_SAMPLE_DIST).add(head);
            if (newHead.dst(head) > TRAJECTORY_SAMPLE_DIST*0.1) {
                trajectory.addFirst(newHead);

                long currentTime = TimeUtils.millis();
                if ((currentTime - trajectory_sampletime) < TRAJECTORY_SAMPLE_TIME_MS) {
                    trajectory.removeLast();
                } else {
                    trajectory_sampletime = currentTime;
                }
            }
        }

        float width = viewport.getWorldWidth();
        float height = viewport.getScreenHeight();
        apples.update(delta, width, height);

        if (Math.random() > 0.998 + 0.002*(bombs.size()/5)) {
            bombs.add(new Vector2((float)Math.random()*width, (float)Math.random()*height));
        }
        if (Math.random() > 0.9985) {
            if (bombs.size() > 0)
                bombs.removeFirst();
        }

        if (collideWithSegments()) {
            init();
        }

        if (apples.collide(position)) {
            applesEaten++;
            if (applesEaten > applesEatenHighscore) {
                applesEatenHighscore = applesEaten;
                prefs.putInteger("highscore", applesEatenHighscore);
                prefs.flush();
            }
        }

        if (collideWithBombs()){
            trajectory.addFirst(trajectory.getFirst().cpy());
        }

        collideWithWalls(baseRadius * radiusMultiplier, viewport.getWorldWidth(), viewport.getWorldHeight());
    }


    private boolean collideWithBombs(){
        for (Vector2 bomb : bombs){
            if (position.dst(bomb) < 2*BOMB_RADIUS) {
				bombs.remove(bomb);
                return true;
            }
        }

        return false;
    }

    private boolean collideWithSegments(){
        boolean collide = false;
        boolean first = true;

        for (Vector2 segment : trajectory){
            if (first){
                first = false;
            } else {
                if (position.dst(segment) < baseRadius * radiusMultiplier * 0.5)
                    collide = true;
            }
        }

        return collide;
    }

    private void collideWithWalls(float radius, float viewportWidth, float viewportHeight) {
        if (position.x - radius < 0) {
            position.x = radius;
            velocity.x = -velocity.x;
        }
        if (position.x + radius > viewportWidth) {
            position.x = viewportWidth - radius;
            velocity.x = -velocity.x;
        }
        if (position.y - radius < 0) {
            position.y = radius;
            velocity.y = -velocity.y;
        }
        if (position.y + radius > viewportHeight) {
            position.y = viewportHeight - radius;
            velocity.y = -velocity.y;
        }
    }

    public void render(ShapeRenderer renderer) {
        Vector2 lastPos = position;

        renderer.set(ShapeType.Filled);

        // draw body segments
        for (Vector2 pos : trajectory) {
            renderer.setColor(Color.BLUE);
            renderer.circle(pos.x, pos.y, baseRadius * radiusMultiplier);

            // draw pattern on body element
            renderer.setColor(Color.GRAY);

            Vector2 orientation = pos.cpy().sub(lastPos).setLength(5);
            Vector2 ortho = new Vector2(orientation.y, -orientation.x);
            Vector2 p1 = pos.cpy().sub(orientation);
            Vector2 p2 = pos.cpy().add(orientation).add(ortho);
            Vector2 p3 = pos.cpy().add(orientation).sub(ortho);
            renderer.triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);

            lastPos = pos;
        }

        // draw head
        Vector2 orientation = position.cpy().sub(trajectory.getFirst()).setLength(8);
        Vector2 ortho = new Vector2(orientation.y, -orientation.x);
        renderer.setColor(Color.NAVY);
        renderer.circle(position.x, position.y, baseRadius * radiusMultiplier);
        renderer.setColor(Color.WHITE);

        // draw eyes on head
        Vector2 eyepos1 = new Vector2(position.cpy().add(orientation.cpy().add(ortho).setLength(5)));
        renderer.circle(eyepos1.x, eyepos1.y, 3);
        Vector2 eyepos2 = new Vector2(position.cpy().add(orientation.cpy().sub(ortho).setLength(5)));
        renderer.circle(eyepos2.x, eyepos2.y, 3);


        apples.render(renderer);

        renderer.setColor(Color.GREEN);
        for (Vector2 pos : bombs)
            renderer.circle(pos.x, pos.y, BOMB_RADIUS);
    }


    @Override
    public boolean keyDown(int keycode) {

        if (keycode == Keys.R){
            init();
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        Vector2 worldClick = viewport.unproject(new Vector2(screenX, screenY));

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        return true;
    }
}
