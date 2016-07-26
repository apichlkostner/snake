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

    private static final float APPLE_RADIUS = 10;

    private static final float KEY_ACCELERATION = 80;

    private static final float ACCELEROMETER_SENSITIVITY = 10f;
    private static final float ACCELERATION_OF_GRAVITY = 9.8f;

    private static final float KICK_VELOCITY = 500.0f;

    private static final float TRAJECTORY_SAMPLE_DIST = 10f;
    private static final float TRAJECTORY_SAMPLE_TIME_MS = 1000;

    long lastKick;

    private LinkedList<Vector2> trajectory;
    private LinkedList<Vector2> apples;
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

    public Snake(Viewport viewport, int highscore, Preferences prefs) {
        this.viewport = viewport;
        this.applesEatenHighscore = highscore;
        this.prefs = prefs;
        init();
    }

    public void init() {
        trajectory = new LinkedList<Vector2>();
        apples = new LinkedList<Vector2>();
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
        //Gdx.app.log("Trajectory", "Current head = "+head);
        if (position.dst(head) > TRAJECTORY_SAMPLE_DIST) {
            Vector2 newHead = head.cpy().sub(position).setLength(TRAJECTORY_SAMPLE_DIST).add(position);
            //Gdx.app.log("Trajectory", "position = "+position+"\nhead = "+head+"\nnewHead = "+newHead+"\nnew distance = "+head.cpy().sub(newHead).len());
            if (newHead.dst(head) > TRAJECTORY_SAMPLE_DIST) {
                //Gdx.app.log("Trajectory", "new position added "+newHead+" distance = "+newHead.dst(head));
                trajectory.addFirst(newHead);

                long currentTime = TimeUtils.millis();
                if ((currentTime - trajectory_sampletime) < TRAJECTORY_SAMPLE_TIME_MS) {
                    //Gdx.app.log("Trajectory", "Position removed");
                    trajectory.removeLast();
                } else {
                    //Gdx.app.log("Trajectory", "New segment added, length = "+trajectory.size()+" dist to next segment = "+newHead.dst(trajectory.get(1)));
                    /*Vector2 oldSegment = new Vector2(0,0);
                    int i = 0;
                    for (Vector2 segment : trajectory){
                        Gdx.app.log("Trajectory", "Segment distance "+i+" = "+segment.dst(oldSegment));
                        oldSegment = segment;
                        i++;
                    }
                    Gdx.app.log("Trajectory", "\n");*/
                    trajectory_sampletime = currentTime;
                }
            }
        }


        if (Math.random() > 0.992 + 0.007*(apples.size()/5)) {
            float width = viewport.getWorldWidth();
            float height = viewport.getScreenHeight();
            apples.add(new Vector2((float)Math.random()*width, (float)Math.random()*height));
        }
        if (Math.random() > 0.998) {
            if (apples.size() > 0)
                apples.removeFirst();
        }

        if (Math.random() > 0.998 + 0.002*(bombs.size()/5)) {
            float width = viewport.getWorldWidth();
            float height = viewport.getScreenHeight();
            bombs.add(new Vector2((float)Math.random()*width, (float)Math.random()*height));
        }
        if (Math.random() > 0.9982) {
            if (bombs.size() > 0)
                bombs.removeFirst();
        }

        if (collideWithSegments()) {
            init();
        }

        if (collideWithApples()){
            if (trajectory.size() > 1)
                trajectory.removeLast();
        }

        if (collideWithBombs()){
            init();
        }

        collideWithWalls(baseRadius * radiusMultiplier, viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    private boolean collideWithApples(){
        for (Vector2 apple : apples){
            if (position.dst(apple) < 2*APPLE_RADIUS) {
                apples.remove(apple);
                applesEaten++;
                if (applesEaten > applesEatenHighscore) {
                    applesEatenHighscore = applesEaten;
                    prefs.putInteger("highscore", applesEatenHighscore);
                    prefs.flush();
                }
                return true;
            }
        }

        return false;
    }

    private boolean collideWithBombs(){
        for (Vector2 bomb : bombs){
            if (position.dst(bomb) < 2*APPLE_RADIUS) {
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
        renderer.set(ShapeType.Filled);
        renderer.setColor(Color.BLUE);
        for (Vector2 pos : trajectory)
            renderer.circle(pos.x, pos.y, baseRadius * radiusMultiplier);
        renderer.circle(position.x, position.y, baseRadius * radiusMultiplier);
        renderer.setColor(Color.RED);
        for (Vector2 pos : apples)
            renderer.circle(pos.x, pos.y, APPLE_RADIUS);

        renderer.setColor(Color.GREEN);
        for (Vector2 pos : bombs)
            renderer.circle(pos.x, pos.y, APPLE_RADIUS);
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
