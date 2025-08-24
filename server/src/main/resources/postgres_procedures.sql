-- Advanced PostgreSQL features for Flight Reservation System
-- Run this manually after the application starts (optional)

-- Views for common queries
CREATE OR REPLACE VIEW available_seats_view AS
SELECT 
    s.flight_id,
    s.seat_class,
    COUNT(*) as available_count
FROM seats s 
WHERE s.status = 'AVAILABLE'
GROUP BY s.flight_id, s.seat_class;

CREATE OR REPLACE VIEW flight_seat_summary AS
SELECT 
    f.id as flight_id,
    f.flight_number,
    f.airline,
    f.origin,
    f.destination,
    f.departure_time,
    f.total_seats,
    COUNT(CASE WHEN s.status = 'AVAILABLE' THEN 1 END) as available_seats,
    COUNT(CASE WHEN s.status = 'BOOKED' THEN 1 END) as booked_seats,
    COUNT(CASE WHEN s.status = 'BLOCKED' THEN 1 END) as blocked_seats
FROM flights f
LEFT JOIN seats s ON f.id = s.flight_id
GROUP BY f.id, f.flight_number, f.airline, f.origin, f.destination, f.departure_time, f.total_seats;

-- Function to automatically update flight available seats when seat status changes
CREATE OR REPLACE FUNCTION update_flight_available_seats()
RETURNS TRIGGER AS $$
BEGIN
    -- Update the flights table with actual available seat count
    UPDATE flights 
    SET available_seats = (
        SELECT COUNT(*) 
        FROM seats 
        WHERE flight_id = COALESCE(NEW.flight_id, OLD.flight_id) 
        AND status = 'AVAILABLE'
    )
    WHERE id = COALESCE(NEW.flight_id, OLD.flight_id);
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Triggers to maintain flight seat counts
DROP TRIGGER IF EXISTS update_flight_seats_on_seat_change ON seats;
CREATE TRIGGER update_flight_seats_on_seat_change
    AFTER INSERT OR UPDATE OR DELETE ON seats
    FOR EACH ROW EXECUTE FUNCTION update_flight_available_seats();

-- Function to get seat map for a flight
CREATE OR REPLACE FUNCTION get_seat_map(p_flight_id BIGINT)
RETURNS TABLE(
    seat_number VARCHAR(5),
    seat_class seat_class,
    status seat_status,
    seat_type VARCHAR(10),
    reservation_id BIGINT,
    row_number INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        s.seat_number,
        s.seat_class,
        s.status,
        CASE 
            WHEN s.seat_number ~ '[AF]$' THEN 'Window'::VARCHAR(10)
            WHEN s.seat_number ~ '[CD]$' THEN 'Aisle'::VARCHAR(10)
            ELSE 'Middle'::VARCHAR(10)
        END as seat_type,
        s.reservation_id,
        CAST(REGEXP_REPLACE(s.seat_number, '[A-Z]', '', 'g') AS INTEGER) as row_number
    FROM seats s
    WHERE s.flight_id = p_flight_id
    ORDER BY 
        CAST(REGEXP_REPLACE(s.seat_number, '[A-Z]', '', 'g') AS INTEGER),
        RIGHT(s.seat_number, 1);
END;
$$ LANGUAGE plpgsql;

-- Function to get available seats by preference (window, aisle, middle)
CREATE OR REPLACE FUNCTION get_available_seats_by_preference(
    p_flight_id BIGINT,
    p_seat_class seat_class DEFAULT NULL,
    p_preference VARCHAR(10) DEFAULT 'window'
)
RETURNS TABLE(
    seat_number VARCHAR(5),
    seat_class seat_class,
    seat_type VARCHAR(10)
) AS $$
DECLARE
    preference_order INTEGER;
BEGIN
    -- Set preference order
    preference_order := CASE 
        WHEN LOWER(p_preference) = 'window' THEN 1
        WHEN LOWER(p_preference) = 'aisle' THEN 2
        ELSE 3 -- middle
    END;

    RETURN QUERY
    SELECT 
        s.seat_number,
        s.seat_class,
        CASE 
            WHEN s.seat_number ~ '[AF]$' THEN 'Window'::VARCHAR(10)
            WHEN s.seat_number ~ '[CD]$' THEN 'Aisle'::VARCHAR(10)
            ELSE 'Middle'::VARCHAR(10)
        END as seat_type
    FROM seats s
    WHERE s.flight_id = p_flight_id 
        AND s.status = 'AVAILABLE'
        AND (p_seat_class IS NULL OR s.seat_class = p_seat_class)
    ORDER BY 
        -- Preference order
        CASE 
            WHEN s.seat_number ~ '[AF]$' THEN 1  -- Window
            WHEN s.seat_number ~ '[CD]$' THEN 2  -- Aisle
            ELSE 3                               -- Middle
        END = preference_order DESC,
        s.seat_class::TEXT,
        CAST(REGEXP_REPLACE(s.seat_number, '[A-Z]', '', 'g') AS INTEGER),
        RIGHT(s.seat_number, 1);
END;
$$ LANGUAGE plpgsql;

-- Function to check seat availability and book
CREATE OR REPLACE FUNCTION book_seat(
    p_flight_id BIGINT,
    p_seat_number VARCHAR(5),
    p_reservation_id BIGINT
)
RETURNS BOOLEAN AS $$
DECLARE
    seat_available BOOLEAN := FALSE;
BEGIN
    -- Check if seat is available and update in one operation
    UPDATE seats 
    SET status = 'BOOKED', 
        reservation_id = p_reservation_id,
        updated_at = CURRENT_TIMESTAMP
    WHERE flight_id = p_flight_id 
        AND seat_number = p_seat_number 
        AND status = 'AVAILABLE';
    
    -- Check if update was successful
    GET DIAGNOSTICS seat_available = ROW_COUNT;
    
    RETURN seat_available > 0;
END;
$$ LANGUAGE plpgsql;

-- Function to release seats for a reservation
CREATE OR REPLACE FUNCTION release_seats_for_reservation(p_reservation_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    seats_released INTEGER;
BEGIN
    UPDATE seats 
    SET status = 'AVAILABLE', 
        reservation_id = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE reservation_id = p_reservation_id;
    
    GET DIAGNOSTICS seats_released = ROW_COUNT;
    
    RETURN seats_released;
END;
$$ LANGUAGE plpgsql;

