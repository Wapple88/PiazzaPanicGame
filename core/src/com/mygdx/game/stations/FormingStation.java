package com.mygdx.game.stations;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.mygdx.game.Chef;
import com.mygdx.game.Match;
import com.mygdx.game.Node;
import com.mygdx.game.foodClasses.Food;
import com.mygdx.game.foodClasses.FoodItems;
import com.mygdx.game.utils.SoundUtils;

import java.util.*;

public class FormingStation extends CookingStation {
    public Stack<Food> inventory;
    public HashMap<List<String>, Food> operationLookupTable_Forming;


    public FormingStation(float operationTimer, boolean canLeaveUnattended, Texture stationTexture) {
        super(operationTimer, canLeaveUnattended, stationTexture);
        operationLookupTable_Forming = new HashMap<>();
        this.inventory = new Stack<>();

        // finished food ingredients
        List<String> burgerIngredients = Arrays.asList(FoodItems.TOASTED_BUN.getName(), FoodItems.COOKED_PATTY.getName());
        List<String> saladIngredients = Arrays.asList(FoodItems.CHOPPED_LETTUCE.getName(), FoodItems.CHOPPED_TOMATO.getName(), FoodItems.CHOPPED_ONION.getName());
        operationLookupTable_Forming.put(burgerIngredients, FoodItems.BURGER);
        operationLookupTable_Forming.put(saladIngredients, FoodItems.SALAD);
    }
    // A function that loops through the stack of the forming station and
    // performs a hash map swap converting the group of items into one finished item
    private Food groupHashMapSwap() {
        //Convert the inventory into a string list
        List<String> components = new ArrayList<>();
        for(Food food: inventory)components.add(food.getName());
        for(List<String> recipe: operationLookupTable_Forming.keySet()){
            if(recipe.containsAll(components) && recipe.size() == components.size()) return new Food(operationLookupTable_Forming.get(recipe));
        }
        return null;
    }
    @Override
    public void onInteract(Chef chef, Node interactedNode, TiledMap tiledMap, Node[][] grid, Match match) {
        // check if chef's stack is empty
        if(chef.foodStack.isEmpty()) return;
        // chef if item isn't formable
        if(!chef.foodStack.peek().isFormable()) return;
        // if the water bucket is used with the station, the station is cleared
        if(chef.foodStack.peek().equals(FoodItems.WATER_BUCKET)) {
            System.out.println("Clearing Forming Station");
            chef.foodStack.pop();
            this.inventory.clear();
            return;
        }

        SoundUtils.getFormingSound().play();
        // if the inventory is empty, then we just push an item, do not check hash map swap
        // this is because there are no recipes that form one item into a finished food
        if (this.inventory.isEmpty()) {
            System.out.println("Pushing onto empty: "+ chef.foodStack.peek().getName());
            this.inventory.push(chef.foodStack.pop());
        } else if (inventory.contains(chef.foodStack.peek()))  {
            System.out.println("Item already in inventory");
            chef.foodStack.pop();
        } else  {
            System.out.println("Pushing more: "+ chef.foodStack.peek().getName());
            System.out.println("Stack length:"+this.inventory.size());
            this.inventory.push(chef.foodStack.pop());
            Food newFood = groupHashMapSwap();
            // checking to see if a finished food has been created
            if (newFood != null)    {
                System.out.println("Finished Food: "+ newFood.getName());
                chef.foodStack.push(newFood);
            }
        }
        SoundUtils.getFormingSound().stop();
    }
}
