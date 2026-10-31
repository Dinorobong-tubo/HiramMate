package com.dinurobongtubo;

public class User {
    // Attributes of users
    private String userId;
    private String name;
    private String contactInfo;
    private boolean accountStatus;

    // Constructor
    public User(String userId, String name, String contactInfo, boolean accountStatus){
        this.userId = userId;
        this.name = name;
        this.contactInfo = contactInfo;
        this.accountStatus = accountStatus;
    }

    public void displayInformation(){
        System.out.println("User ID: " + userId);
        System.out.println("Name: " + name);
        System.out.println("Contact Information: " + contactInfo);
        System.out.println("Account Status: " + accountStatus);
    }
    public void updateInfo(String name, String contactInfo){
        this.name = name;
        this.contactInfo = contactInfo;
    }
}
