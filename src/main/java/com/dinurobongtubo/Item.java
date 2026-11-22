package com.dinurobongtubo;

public class Item {
    private String itemId;
    private String itemName;
    private String category;
    private String condition;
    private String status;

    public Item(String itemId, String itemName, String category, String condition, String status){
        this.itemId = itemId;
        this.itemName = itemName;
        this.category = category;
        this.condition = condition;
        this.status = status;
    }

    public void updateConditon(String condition){
        this.condition = condition;
    }

    public void updateStatus(String status){
        this.status = status;
    }
    public void displayItem(){
        System.out.println("Item Id: "+ itemId);
        System.out.println("Item Name: " + itemName);
        System.out.println("Category: " + category);
        System.out.println("Condition: " + condition);
        System.out.println("Status: " + status);
    }
    public String getItemName() {
    return itemName;
}
}
