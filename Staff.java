package com.dinurobongtubo;

public class Staff extends User {
    String staffId;
    String position;

    public Staff(String userId, String name, String contactInfo, 
        boolean accountStatus, String staffId, String position){
        
        super(userId, name, contactInfo, accountStatus);

        this.staffId = staffId;
        this.position = position;
    }
    public void approveRequest(){
        System.out.println("Borrow request approved!");
    }
    public void rejectRequest(){
        System.out.println("Borrow request rejected!");
    }
    public void manageItem(){
        System.out.println("Managing inventory items!");
    }
    public void processReturn(){
        System.out.println("Item return processed!");
    }
}
