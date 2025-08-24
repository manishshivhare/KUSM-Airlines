package com.kusm.dto.flightDTO;

import com.kusm.model.Seat;

public class SeatDTO {
    private Long id;
    private String seatNumber;
    private String seatClass;
    private String status;
    private boolean isWindow;
    private boolean isAisle;
    private boolean isMiddle;
    private String reservationReference;
    private String passengerName;
    
    // Constructor from Seat entity
    public SeatDTO(Seat seat) {
        this.id = seat.getId();
        this.seatNumber = seat.getSeatNumber();
        this.seatClass = seat.getSeatClass().name();
        this.status = seat.getStatus().name();
        this.isWindow = seat.isWindow();
        this.isAisle = seat.isAisle();
        this.isMiddle = seat.isMiddle();
        
        if (seat.getReservation() != null) {
            this.reservationReference = seat.getReservation().getBookingReference();
            this.passengerName = seat.getReservation().getPassengerName();
        }
    }
    
    // Default constructor
    public SeatDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    
    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isWindow() { return isWindow; }
    public void setWindow(boolean window) { isWindow = window; }
    
    public boolean isAisle() { return isAisle; }
    public void setAisle(boolean aisle) { isAisle = aisle; }
    
    public boolean isMiddle() { return isMiddle; }
    public void setMiddle(boolean middle) { isMiddle = middle; }
    
    public String getReservationReference() { return reservationReference; }
    public void setReservationReference(String reservationReference) { this.reservationReference = reservationReference; }
    
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    
    // Utility methods
    public boolean isAvailable() {
        return "AVAILABLE".equals(this.status);
    }
    
    public boolean isBooked() {
        return "BOOKED".equals(this.status);
    }
    
    public String getSeatType() {
        if (isWindow) return "Window";
        if (isAisle) return "Aisle";
        if (isMiddle) return "Middle";
        return "Unknown";
    }
}