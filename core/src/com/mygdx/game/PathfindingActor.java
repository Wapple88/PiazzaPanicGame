package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.enums.Facing;
import com.mygdx.game.threads.PathfindingRunnable;
import com.mygdx.game.utils.PathfindingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The type Pathfinding actor.
 */
public class PathfindingActor {

    private List<Vector2> worldPath;
    private int pathfindingCounter;
    private Facing facing;
    /**
     * The Start.
     */
    Node start;
    /**
     * The End.
     */
    Node end;
    /**
     * The Grid.
     */
    Node[][] grid;
    /**
     * The Tiled map.
     */
    TiledMap tiledMap;

    /**
     * Instantiates a new Pathfinding actor.
     *
     * @param start    the start
     * @param end      the end
     * @param grid     the grid
     * @param tiledMap the tiled map
     */
    public PathfindingActor(Node start, Node end, Node[][] grid, TiledMap tiledMap){
        this.start = start;
        this.end = end;
        this.grid = grid;
        this.tiledMap = tiledMap;
        worldPath = new ArrayList<>();
        pathfindingCounter = 0;
        facing = Facing.UP;
    }

    /**
     * Get world path list.
     *
     * @return the list
     */
    public List<Vector2> getWorldPath(){
        return worldPath;
    }

    /**
     * Get pathfinding counter int.
     *
     * @return the int
     */
    public int getPathfindingCounter(){
        return pathfindingCounter;
    }

    /**
     * Set pathfinding counter.
     *
     * @param pCounter the p counter
     */
    public void setPathfindingCounter(int pCounter){
        pathfindingCounter = pCounter;
    }

    /**
     * Set facing.
     *
     * @param sprite           the sprite
     * @param direction        the direction
     * @param movementTextures the movement textures
     */
    public void setFacing(Sprite sprite, Facing direction, Texture[] movementTextures){
        facing = direction;
        sprite.setTexture(movementTextures[facing.ordinal()]);
        //sprite.setRotation(90f * facing.ordinal());
    }

    /**
     * Get facing facing.
     *
     * @return the facing
     */
    public Facing getFacing(){
        return facing;
    }

    /**
     * Create thread and pathfind list.
     *
     * @return the list
     */
    public List<Vector2> createThreadAndPathfind(){
        //If the start or end is invalid, return an empty list
        if(start == null || end == null) return Collections.emptyList();

        //Set up thread to do pathfinding
        PathfindingRunnable pathfindingObj = new PathfindingRunnable(start, end, grid);
        Thread pathfindingThread = new Thread(pathfindingObj);
        pathfindingThread.start();
        //Stalls the main thread until the pathfinding is complete
        while (pathfindingThread.isAlive()){}
        //Gets the path from the thread in grid co-ordinates
        worldPath = PathfindingUtils.convertGridPathToWorld(pathfindingObj.getGridPath(), tiledMap);
        return worldPath;
    }

    /**
     * Follow path.
     *
     * @param sprite           the sprite
     * @param speed            the speed
     * @param movementTextures the movement textures
     */
//Makes the sprite follow the path
    public void followPath(Sprite sprite, float speed, Texture[] movementTextures){

        if(pathfindingCounter >= worldPath.size()) return;
        int pointBuffer = 2;

        //The direction from point a to point b = atan(bY-aY,bX-aX)
        float angle = (float) Math.atan2(worldPath.get(pathfindingCounter).y - sprite.getY(), worldPath.get(pathfindingCounter).x - sprite.getX());
        Vector2 movementDir = new Vector2((float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed);
        setPathfinderFacing(movementDir, sprite, movementTextures);
        sprite.setPosition(sprite.getX() + movementDir.x * Gdx.graphics.getDeltaTime(), sprite.getY() + movementDir.y * Gdx.graphics.getDeltaTime());

        if(Math.abs(worldPath.get(pathfindingCounter).x - sprite.getX()) <= pointBuffer && Math.abs(worldPath.get(pathfindingCounter).y - sprite.getY()) <= pointBuffer){
            pathfindingCounter++;
        }
    }

    /**
     * This function controls the direction the sprite is facing during its pathfinding
     * @param movementDir
     * @param sprite
     * @param movementTextures
     */

    private void setPathfinderFacing(Vector2 movementDir, Sprite sprite, Texture[] movementTextures){
        //check which movement direction is the largest and face that way
        if(Math.abs(movementDir.x) > Math.abs(movementDir.y)){
            if(movementDir.x > 0) setFacing(sprite, Facing.RIGHT, movementTextures);
            else setFacing(sprite, Facing.LEFT, movementTextures);
        }
        else{
            if(movementDir.y > 0) setFacing(sprite, Facing.UP, movementTextures);
            else setFacing(sprite, Facing.DOWN, movementTextures);
        }
    }

    /**
     * Draw path.
     * This function draws a line along the path to provide a visual indicator
     * @param camera the camera
     * @param sprite the sprite
     */

    public void drawPath(Camera camera, Sprite sprite){
        if(worldPath.isEmpty()) return;
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for(int i = pathfindingCounter; i < worldPath.size() - 1; i++){

            if(i == 0){
                shapeRenderer.line(modifyVectorForDrawing(sprite.getX(),sprite.getY()),
                        modifyVectorForDrawing(worldPath.get(0)));
            }
            shapeRenderer.line(modifyVectorForDrawing(worldPath.get(i)),
                    modifyVectorForDrawing(worldPath.get(i + 1)));
        }
        shapeRenderer.end();
        shapeRenderer.dispose();
    }

    /**
     * Helper methods to convert world vectors into vectors to be drawn on the camera
     * @param inputX
     * @param inputY
     * @return the modified vector
     */
    private Vector2 modifyVectorForDrawing(float inputX, float inputY){
        return modifyVectorForDrawing(new Vector2(inputX, inputY));
    }

    /**
     * Same as previous function, however allows the user to inout a vector, instead of (x,y)
     * @param inputVector
     * @return the modified vector
     */
    private Vector2 modifyVectorForDrawing(Vector2 inputVector){
        final float mid = (float)256/2;
        Vector2 midpoint = new Vector2(mid, mid);
        return new Vector2(inputVector.x + midpoint.x, inputVector.y + midpoint.y);
    }

}
