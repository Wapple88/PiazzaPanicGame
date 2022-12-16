package com.mygdx.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.mygdx.game.Chef;
import com.mygdx.game.Facing;
import com.mygdx.game.Node;
import com.mygdx.game.interfaces.IPathfinder;

import java.util.*;

public class PathfindingUtils {

    //Pathfinds between two grid co-ordinates
    public static Vector2[] findPath(Node start, Node end, Node[][] walls){

        if(!isValidNode(end.getGridX(), end.getGridY(), walls)) return null;
        if(start == end) return new Vector2[] {new Vector2(start.getGridX(), start.getGridY())};
        if(end.getWall()) return null;
        clearParents(walls);


        List<Node> openList = new ArrayList<>();
        List<Node> closedList = new ArrayList<>();

        openList.add(start);
        while(openList.size() > 0){
            Node current = getSmallestF(openList);
            openList.remove(current);
            Node[] neighbours = getNeighbours(current, walls);
            for(Node n : neighbours){
                if(n.getGridX() == end.getGridX() && n.getGridY() == end.getGridY()) {
                    n.setParent(current);
                    return backTrackPath(n);
                }
                else if (!closedList.contains(n) && !n.getWall()){
                    //Calculate g,h and f for n
                    float g = calculateMoveCost(current, n);
                    float h = calculateHeuristic(n, end);
                    float f = g + h;
                    if(openList.contains(n) && n.getF() < f) continue;
                    if (closedList.contains(n) && n.getF() < f) continue;
                    n.setG(g);
                    n.setH(h);
                    n.setParent(current);
                    openList.add(n);
                }
            }
            closedList.add(current);
        }
        //Path not found
        return null;
    }

    private static Node getSmallestF(List<Node> openList){
        Node smallestNode = null;
        float smallestF = 10000;
        for(Node n : openList){
            if(n.getF() < smallestF){
                smallestF = n.getF();
                smallestNode = n;
            }
        }
        return smallestNode;
    }

    //Converts a path in grid co-ordinates to world co-ordinates
    public static List<Vector2> convertGridPathToWorld(Vector2[] path, TiledMap tiledMap){
        List<Vector2> worldPath = new ArrayList<>();
        for (int i = 0; i < path.length; i++){
            float worldX = TileMapUtils.coordToPosition((int) path[i].x, tiledMap);
            float worldY = TileMapUtils.coordToPosition((int)path[i].y, tiledMap);
            worldPath.add(new Vector2(worldX, worldY));
        }
        return worldPath;
    }

    private static void clearParents(Node[][] walls){
        for(int x = 0; x < walls.length; x++){
            for(int y = 0; y < walls.length; y++){
                walls[x][y].setParent(null);
            }
        }
    }

    private static boolean isValidNode(int gridX, int gridY, Node[][] walls){
        return gridY < walls.length && gridY >= 0 && gridX >= 0 && gridX < walls.length;
    }

    //Get the neighbouring nodes of the current Node
    private static Node[] getNeighbours(Node current, Node[][] walls){
        int[] xMod = {1,-1,0,0, 1,1,-1,-1};
        int[] yMod = {0,0,1,-1,1,-1,1,-1};
        List<Node> neighbourList = new ArrayList<>();

        for(int i = 0; i < xMod.length; i++){
            if(isValidNode(current.getGridX() + xMod[i], current.getGridY() + yMod[i], walls))
                neighbourList.add(walls[current.getGridX() + xMod[i]][current.getGridY() + yMod[i]]);
        }
        return (Node[])neighbourList.toArray(new Node[0]);
    }

    private static float calculateHeuristic(Node current, Node end){
        return Math.abs(end.getGridX() - current.getGridX()) + Math.abs(end.getGridY() - current.getGridY());
    }

    private static float calculateMoveCost(Node current, Node n){
       return Math.abs(n.getGridX() - current.getGridX()) + Math.abs(n.getGridY() - current.getGridY()) == 2 ? 1.4142f : 1f;
    }

    private static Vector2[] backTrackPath(Node end){
        List<Vector2> path = new ArrayList<>();
        Node current = end;
        while(current.getParent() != null){
            path.add(new Vector2(current.getGridX(), current.getGridY()));
            if(current.getParent() == null) break;
            current = current.getParent();
        }
        Collections.reverse(path);
        return (Vector2[]) path.toArray(new Vector2[0]);
    }

    public static void followPath(Sprite sprite, List<Vector2> path, float speed, IPathfinder pathfinder){
        int pointBuffer = 2;

        if(pathfinder.getPathCounter() < path.size()){
            //The direction from point a to point b = atan(bY-aY,bX-aX)
            float angle = (float) Math.atan2(path.get(pathfinder.getPathCounter()).y - sprite.getY(), path.get(pathfinder.getPathCounter()).x - sprite.getX());
            Vector2 movementDir = new Vector2((float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed);
            setPathfinderFacing(movementDir, pathfinder);
            sprite.setPosition(sprite.getX() + movementDir.x * Gdx.graphics.getDeltaTime(), sprite.getY() + movementDir.y * Gdx.graphics.getDeltaTime());

            if(Math.abs(path.get(pathfinder.getPathCounter()).x - sprite.getX()) <= pointBuffer && Math.abs(path.get(pathfinder.getPathCounter()).y - sprite.getY()) <= pointBuffer){
                pathfinder.setPathCounter(pathfinder.getPathCounter() + 1);
            }
        }
    }

    private static void setPathfinderFacing(Vector2 movementDir, IPathfinder pathfinder){
        //check which movement direction is the largest and face that way
        if(Math.abs(movementDir.x) > Math.abs(movementDir.y)){
            if(movementDir.x > 0) pathfinder.setFacing(Facing.RIGHT);
            else pathfinder.setFacing(Facing.LEFT);
        }
        else{
            if(movementDir.y > 0) pathfinder.setFacing(Facing.UP);
            else pathfinder.setFacing(Facing.DOWN);
        }
    }

    public static void drawPath(List<Vector2> path, Camera camera){
        ShapeRenderer shapeRenderer = new ShapeRenderer();

        for(int i = 0; i < path.size() - 1; i++){
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);

            Vector2 midpoint1 = new Vector2(256/2, 256/2);
            Vector2 midpoint2 = new Vector2(256/2, 256/2);
            shapeRenderer.line(midpoint1.add(path.get(i)), midpoint2.add(path.get(i + 1)));
            shapeRenderer.end();
        }
    }

}