DROP TABLE reservations CASCADE CONSTRAINTS;
DROP TABLE rooms CASCADE CONSTRAINTS;
DROP TABLE users CASCADE CONSTRAINTS;

CREATE TABLE users (
    username VARCHAR2(50) PRIMARY KEY,
    password VARCHAR2(100),
    role VARCHAR2(20),
    full_name VARCHAR2(100)
);

CREATE TABLE rooms (
    room_id VARCHAR2(20) PRIMARY KEY,
    room_type VARCHAR2(50),
    price NUMBER(10,2),
    status VARCHAR2(20) CHECK (status IN ('Available', 'Reserved', 'Maintenance', 'Pending'))
);

CREATE TABLE reservations (
    reservation_id VARCHAR2(50) PRIMARY KEY,
    guest_name VARCHAR2(100),
    room_id VARCHAR2(20),
    check_in DATE,
    check_out DATE,
    status VARCHAR2(20) CHECK (status IN ('Pending', 'Confirmed', 'Cancelled')),
    CONSTRAINT fk_room FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    CONSTRAINT chk_dates CHECK (check_out > check_in) 
);
CREATE INDEX idx_reservations_dates ON reservations(room_id, check_in, check_out);
INSERT INTO users (username, password, role, full_name) VALUES ('client', 'client123', 'client', 'yazid rebei');
INSERT INTO users (username, password, role, full_name) VALUES ('admin', 'admin123', 'admin', 'rayen bouajila');

INSERT INTO rooms (room_id, room_type, price, status) VALUES ('101', 'Standard', 100.00, 'Available');
INSERT INTO rooms (room_id, room_type, price, status) VALUES ('102', 'Deluxe', 150.00, 'Available');

CREATE OR REPLACE FUNCTION get_occupancy_rate RETURN VARCHAR2
AS
    v_rate NUMBER;
BEGIN
    SELECT (SELECT COUNT(*) FROM rooms WHERE status = 'Reserved') * 100.0 /
           NULLIF((SELECT COUNT(*) FROM rooms), 0)
    INTO v_rate
    FROM dual;

    RETURN TO_CHAR(v_rate, 'FM999.9') || '%';
EXCEPTION
    WHEN OTHERS THEN
        RETURN 'N/A';
END;
/

CREATE OR REPLACE FUNCTION get_monthly_revenue RETURN VARCHAR2
AS
    v_revenue NUMBER;
BEGIN
    SELECT NVL(SUM(r.price), 0)
    INTO v_revenue
    FROM reservations res
    JOIN rooms r ON res.room_id = r.room_id
    WHERE res.status = 'Confirmed' AND check_in >= TRUNC(SYSDATE, 'MONTH');

    RETURN TO_CHAR(v_revenue, 'FM999999.00');
EXCEPTION
    WHEN OTHERS THEN
        RETURN '0.00';
END;
/

CREATE OR REPLACE FUNCTION get_total_guests RETURN VARCHAR2
AS
    v_total NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_total
    FROM reservations
    WHERE status = 'Confirmed';

    RETURN TO_CHAR(v_total);
EXCEPTION
    WHEN OTHERS THEN
        RETURN '0';
END;
/

CREATE OR REPLACE FUNCTION get_available_rooms RETURN VARCHAR2
AS
    v_available NUMBER;
    v_total NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_available
    FROM rooms
    WHERE status = 'Available';

    SELECT COUNT(*) INTO v_total
    FROM rooms;

    RETURN v_available || '/' || v_total;   
EXCEPTION
    WHEN OTHERS THEN
        RETURN '0/0';
END;
/