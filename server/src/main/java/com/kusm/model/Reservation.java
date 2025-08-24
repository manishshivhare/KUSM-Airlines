package com.kusm.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "reservations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String bookingReference;
    
    @NotBlank
    private String passengerName;
    
    @Email
    private String passengerEmail;
    
    @NotBlank
    private String passengerPhone;

        @UpdateTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Flight flight;
    
    @OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnoreProperties({"reservation", "flight"})
    private List<Seat> seats = new ArrayList<>();
    
    @NotNull
    private LocalDateTime bookingTime;
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private BookingStatus status;
    
    // Preferred seat class for auto-assignment
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preferred_seat_class")
    private Seat.SeatClass preferredSeatClass;
    
    public enum BookingStatus {
        CONFIRMED, CANCELLED, PENDING
    }
    
    // Constructors
    public Reservation() {
        this.bookingTime = LocalDateTime.now();
        this.status = BookingStatus.PENDING;
        this.preferredSeatClass = Seat.SeatClass.ECONOMY;
        this.seats = new ArrayList<>();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    
    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }
    
    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }
    
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    
    public List<Seat> getSeats() { 
        if (seats == null) {
            seats = new ArrayList<>();
        }
        return seats; 
    }
    public void setSeats(List<Seat> seats) { 
        this.seats = seats != null ? seats : new ArrayList<>(); 
    }
    
    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    
    public Seat.SeatClass getPreferredSeatClass() { return preferredSeatClass; }
    public void setPreferredSeatClass(Seat.SeatClass preferredSeatClass) { this.preferredSeatClass = preferredSeatClass; }
    
    // Utility methods
    public String getSeatNumbers() {
        if (seats != null && !seats.isEmpty()) {
            return seats.stream()
                    .map(Seat::getSeatNumber)
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .orElse("Not assigned");
        }
        return "Not assigned";
    }
    
    public int getSeatCount() {
        return seats != null ? seats.size() : 0;
    }
    
    public boolean hasSeats() {
        return seats != null && !seats.isEmpty();
    }
    
    // Add seat to reservation
    public void addSeat(Seat seat) {
        if (seats == null) {
            seats = new ArrayList<>();
        }
        seats.add(seat);
        seat.setReservation(this);
    }
    
    // Remove seat from reservation
    public void removeSeat(Seat seat) {
        if (seats != null) {
            seats.remove(seat);
            seat.setReservation(null);
        }
    }
    
    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", bookingReference='" + bookingReference + '\'' +
                ", passengerName='" + passengerName + '\'' +
                ", passengerEmail='" + passengerEmail + '\'' +
                ", passengerPhone='" + passengerPhone + '\'' +
                ", flightNumber=" + (flight != null ? flight.getFlightNumber() : "null") +
                ", seatCount=" + getSeatCount() +
                ", seatNumbers='" + getSeatNumbers() + '\'' +
                ", bookingTime=" + bookingTime +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", preferredSeatClass=" + preferredSeatClass +
                '}';
    }
}