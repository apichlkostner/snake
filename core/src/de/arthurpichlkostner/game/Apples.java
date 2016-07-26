package de.arthurpichlkostner.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.LinkedList;


public class Apples {
    private static final float APPLE_RADIUS = 10;

    private LinkedList<Vector2> apples;

    public Apples() {
        apples = new LinkedList<Vector2>();
    }

    public boolean collide(Vector2 position) {
        for (Vector2 apple : apples){
            if (position.dst(apple) < 2*APPLE_RADIUS) {
                apples.remove(apple);

                return true;
            }
        }

        return false;
    }

    public void update(float delta, float width, float height) {
        float prob = Math.max(0, 1-delta);
        float prob2 = Math.max(0, 1-delta*0.19f);
        if (Math.random() > prob + (1-prob)* apples.size() / 5) {
            apples.add(new Vector2((float) Math.random() * width, (float) Math.random() * height));
        }
        if (Math.random() > prob2) {
            if (apples.size() > 0)
                apples.removeFirst();
        }
    }

    public void render(ShapeRenderer renderer) {
        renderer.setColor(Color.RED);
        for (Vector2 pos : apples)
            renderer.circle(pos.x, pos.y, APPLE_RADIUS);
    }
}
