package com.dinurobongtubo;

public class Reservation {

    private String reservationId;
    private Student student;
    private Item item;
    private String reservationDate;
    private String status;

    public Reservation(
            String reservationId,
            Student student,
            Item item,
            String reservationDate,
            String status
    ) {

        this.reservationId = reservationId;
        this.student = student;
        this.item = item;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public void createReservation() {
        status = "Pending";
        System.out.println("Reservation created.");
    }

    public void cancelReservation() {
        status = "Cancelled";
        System.out.println("Reservation cancelled.");
    }
}
