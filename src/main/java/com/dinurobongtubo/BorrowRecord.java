package com.dinurobongtubo;

public class BorrowRecord {
     private String transactionId;
    private Student student;
    private Item item;
    private String borrowDate;
    private String dueDate;
    private String returnDate;

    public BorrowRecord(String transactionId, Student student,
                        Item item, String borrowDate, String dueDate) {

        this.transactionId = transactionId;
        this.student = student;
        this.item = item;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = "Not yet returned";
    }

    public void markReturned(String returnDate) {
        this.returnDate = returnDate;
        System.out.println("Item marked as returned.");
    }

    public boolean checkOverdue(String currentDate) {
        if (returnDate.equals("Not yet returned")
                && currentDate.compareTo(dueDate) > 0) {
            return true;
        }

        return false;
    }

    public void calculatePenalty() {
        System.out.println("Penalty calculation will be implemented later.");
    }

    public void createReservation() {
        System.out.println("Reservation created.");
    }

    public void cancelReservation() {
        System.out.println("Reservation cancelled.");
    }
}
