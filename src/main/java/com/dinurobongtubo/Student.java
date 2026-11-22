package com.dinurobongtubo;

import java.util.ArrayList;

public class Student extends User{
    private String studentId;
    private String block;
    private String department;

    private  ArrayList<String> borrowItems;
    private ArrayList<String> liabilities;

    public Student(String userId,String studentId, String name, boolean accountStatus,String contactInfo,String block, String department){
        super(userId, name, contactInfo, accountStatus );

        this.studentId = studentId;
        this.block = block;
        this.department = department;

        borrowItems = new ArrayList<>();
        liabilities = new ArrayList<>();
    }
    public void requestBorrow(){
        System.out.println("Borrow request submitted!");
    }
    public void reserveItem(){
        System.out.println("Item reservation submitted!");
    }
    public void viewRecords(){
        System.out.println("Borrowed items: " + borrowItems);
        System.out.println("Liabilities: " + liabilities);
    }

}
