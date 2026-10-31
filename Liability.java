package com.dinurobongtubo;

public class Liability {
    private String liabilityId;
    private Student student;
    private Item item;
    private String type;
    private double amount;
    private String status;

    public Liability(String liabilityId, Student student,
                     Item item, String type, double amount,
                     String status) {

        this.liabilityId = liabilityId;
        this.student = student;
        this.item = item;
        this.type = type;
        this.amount = amount;
        this.status = status;
    }

    public void calculateAmount() {
        System.out.println("Liability amount: ₱" + amount);
    }

    public void settleLiability() {
        status = "Settled";
        System.out.println("Liability has been settled.");
    }
}
