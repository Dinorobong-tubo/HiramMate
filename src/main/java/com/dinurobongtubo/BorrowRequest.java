package com.dinurobongtubo;

public class BorrowRequest {
    private String requestId;
    private Student student;
    private Item item;
    private String requestDate;
    private String status;

    public BorrowRequest(String requestId, Student student,
                         Item item, String requestDate, String status) {

        this.requestId = requestId;
        this.student = student;
        this.item = item;
        this.requestDate = requestDate;
        this.status = status;
    }

    public void submitRequest() {
        status = "Pending";
        System.out.println("Borrow request submitted.");
    }

    public void approve() {
        status = "Approved";
        System.out.println("Borrow request approved.");
    }

    public void reject() {
        status = "Rejected";
        System.out.println("Borrow request rejected.");
    }
}
