package com.dinurobongtubo;

import java.util.ArrayList;

public class Inventory {
    private ArrayList<Item> items;
    private int itemCount;

    public Inventory() {
        items = new ArrayList<>();
        itemCount = 0;
    }

    public void addItem(Item item) {
        items.add(item);
        itemCount++;
        System.out.println("Item added to inventory.");
    }

    public void removeItem(Item item) {
        if (items.remove(item)) {
            itemCount--;
            System.out.println("Item removed from inventory.");
        } else {
            System.out.println("Item not found in inventory.");
        }
    }

    public Item searchItem(String itemName) {
        for (Item item : items) {
            if (item.getItemName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }

        return null;
    }

    public void displayItems() {
        for (Item item : items) {
            item.displayItem();
            System.out.println("--------------------");
        }
    }
}
