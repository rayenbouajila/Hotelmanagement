package hotelmanegement;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class ClientDashboard extends JFrame {
    private final Color PRIMARY = new Color(33, 150, 243);
    private final Color PRIMARY_DARK = new Color(25, 118, 210);
    private final Color SECONDARY = new Color(245, 245, 245);
    private final Color BACKGROUND = new Color(250, 250, 250);
    private final Color CARD = new Color(255, 255, 255);

    private final User currentUser;
    private Connection connection;
    private DefaultTableModel roomsModel;
    private DefaultTableModel reservationsModel;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JComboBox<String> roomTypeCombo;

    public ClientDashboard(User user) {
        this.currentUser = user;
        try {
            this.connection = DatabaseConnection.getConnection();
            initializeUI();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeUI() {
        setTitle("Client Dashboard - Welcome " + currentUser.getFullName());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createContentPanel(), BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_DARK);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("CLIENT DASHBOARD");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.BLACK);
        header.add(title, BorderLayout.WEST);
 
        
        JLabel userInfo = new JLabel("User: " + currentUser.getUsername());
        userInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userInfo.setForeground(Color.BLACK);
        header.add(userInfo, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton refreshBtn = createStyledButton("⟳ Refresh", PRIMARY, Color.BLACK);
        refreshBtn.addActionListener(e -> {
            loadAvailableRooms();
            loadUserReservations(reservationsModel);
        });

        JButton logoutBtn = createStyledButton("Logout", PRIMARY_DARK, Color.BLACK);
        logoutBtn.addActionListener(e -> logout());

        actions.add(refreshBtn);
        actions.add(logoutBtn);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BACKGROUND);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.setBackground(Color.WHITE);

        tabs.addTab("Available Rooms", createRoomsPanel());
        tabs.addTab("My Reservations", createReservationsPanel());

        contentPanel.add(tabs, BorderLayout.CENTER);
        return contentPanel;
    }

    private JPanel createRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBackground(BACKGROUND);
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        checkInField = new JTextField("yyyy-MM-dd");
        checkOutField = new JTextField("yyyy-MM-dd");
        roomTypeCombo = new JComboBox<>(new String[]{"All", "Standard", "Deluxe", "Suite"});

        inputPanel.add(new JLabel("Check-In Date (YYYY-MM-DD):"));
        inputPanel.add(checkInField);
        inputPanel.add(new JLabel("Check-Out Date (YYYY-MM-DD):"));
        inputPanel.add(checkOutField);
        inputPanel.add(new JLabel("Room Type:"));
        inputPanel.add(roomTypeCombo);

        JButton searchBtn = createStyledButton("Search", PRIMARY, Color.BLACK);
        searchBtn.addActionListener(e -> loadAvailableRooms());
        inputPanel.add(new JLabel());
        inputPanel.add(searchBtn);

        roomsModel = new DefaultTableModel(
                new Object[]{"ID", "Type", "Price", "Status", "Reserve", "View Reservations"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 4 && row < getRowCount();
            }
        };

        JTable table = new JTable(roomsModel);
        customizeTable(table);
        loadAvailableRooms();

        TableColumn reserveColumn = table.getColumnModel().getColumn(4);
        reserveColumn.setCellRenderer(new ButtonRenderer("Reserve", PRIMARY));
        reserveColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "Reserve", roomsModel));

        TableColumn viewColumn = table.getColumnModel().getColumn(5);
        viewColumn.setCellRenderer(new ButtonRenderer("View Reservations", PRIMARY_DARK));
        viewColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "View Reservations", roomsModel));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(BACKGROUND);
        controlPanel.add(inputPanel);

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);

        reservationsModel = new DefaultTableModel(
                new Object[]{"ID", "Room", "Check-In", "Check-Out", "Status", "Total Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(reservationsModel);
        customizeTable(table);
        loadUserReservations(reservationsModel);

        JButton cancelBtn = createStyledButton("Cancel Selected Reservation", Color.RED.darker(), Color.BLACK);
        cancelBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a reservation first.");
                return;
            }

            String reservationId = (String) reservationsModel.getValueAt(selectedRow, 0);
            String status = (String) reservationsModel.getValueAt(selectedRow, 4);

            if (!"Pending".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "Only pending reservations can be cancelled.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel this reservation?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String updateRoomSql = "UPDATE rooms SET status = 'Available' WHERE room_id = (SELECT room_id FROM reservations WHERE reservation_id = ?)";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateRoomSql)) {
                        updateStmt.setString(1, reservationId);
                        updateStmt.executeUpdate();
                    }

                    String deleteSql = "DELETE FROM reservations WHERE reservation_id = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                        stmt.setString(1, reservationId);
                        stmt.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(this, "Reservation cancelled.");
                    SwingUtilities.invokeLater(() -> {
                        loadUserReservations(reservationsModel);
                        loadAvailableRooms();
                    });
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error cancelling reservation: " + ex.getMessage());
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BACKGROUND);
        btnPanel.add(cancelBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAvailableRooms() {
        try {
            String checkInStr = checkInField.getText().trim();
            String checkOutStr = checkOutField.getText().trim();
            String roomType = (String) roomTypeCombo.getSelectedItem();

            LocalDate checkIn = null;
            LocalDate checkOut = null;
            if (!checkInStr.equals("yyyy-MM-dd") && !checkOutStr.equals("yyyy-MM-dd")) {
                try {
                    checkIn = LocalDate.parse(checkInStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    checkOut = LocalDate.parse(checkOutStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
                        showError("Check-out date must be after check-in date.");
                        return;
                    }
                    if (checkIn.isBefore(LocalDate.now())) {
                        showError("Check-in date cannot be in the past.");
                        return;
                    }
                } catch (DateTimeParseException e) {
                    showError("Invalid date format. Use YYYY-MM-DD.");
                    return;
                }
            }

            String sql = "SELECT r.room_id, r.room_type, r.price, r.status " +
                        "FROM rooms r " +
                        "WHERE r.status IN ('Available', 'Pending') " +
                        (roomType.equals("All") ? "" : "AND r.room_type = ? ") +
                        (checkIn != null ? "AND NOT EXISTS (" +
                        "    SELECT 1 FROM reservations res " +
                        "    WHERE res.room_id = r.room_id " +
                        "    AND res.status IN ('Pending', 'Confirmed') " +
                        "    AND (res.check_in <= ? AND res.check_out >= ?)" +
                        ") " : "") +
                        "ORDER BY r.price";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int paramIndex = 1;
                if (!roomType.equals("All")) {
                    pstmt.setString(paramIndex++, roomType);
                }
                if (checkIn != null) {
                    pstmt.setDate(paramIndex++, java.sql.Date.valueOf(checkOut));
                    pstmt.setDate(paramIndex, java.sql.Date.valueOf(checkIn));
                }

                ResultSet rs = pstmt.executeQuery();
                roomsModel.setRowCount(0);

                while (rs.next()) {
                    roomsModel.addRow(new Object[]{
                        rs.getString("room_id"),
                        rs.getString("room_type"),
                        String.format("$%.2f", rs.getDouble("price")),
                        rs.getString("status"),
                        "Reserve",
                        "View Reservations"
                    });
                }
                roomsModel.fireTableDataChanged();
                if (roomsModel.getRowCount() == 0 && checkIn != null) {
                    showError("No rooms available for the selected dates.");
                }
            }
        } catch (SQLException e) {
            showError("Error loading rooms: " + e.getMessage());
        }
    }

    private void showReservationDates(String roomId) {
        try {
            String sql = "SELECT check_in, check_out, status " +
                        "FROM reservations " +
                        "WHERE room_id = ? AND status IN ('Pending', 'Confirmed') " +
                        "ORDER BY check_in";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, roomId);
                ResultSet rs = pstmt.executeQuery();

                StringBuilder message = new StringBuilder("Booked dates for Room " + roomId + ":\n\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                boolean hasReservations = false;

                while (rs.next()) {
                    hasReservations = true;
                    message.append("From ")
                           .append(sdf.format(rs.getDate("check_in")))
                           .append(" to ")
                           .append(sdf.format(rs.getDate("check_out")))
                           .append(" (")
                           .append(rs.getString("status"))
                           .append(")\n");
                }

                if (!hasReservations) {
                    message.append("No existing reservations.");
                }

                JOptionPane.showMessageDialog(this, message.toString(), 
                    "Reservation Dates", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            showError("Error retrieving reservation dates: " + e.getMessage());
        }
    }

    private void showReservationDialog(int selectedRow) {
        if (selectedRow == -1 || selectedRow >= roomsModel.getRowCount()) {
            showError("Please select a valid room first!");
            return;
        }

        String roomId = (String) roomsModel.getValueAt(selectedRow, 0);
        JDialog dialog = new JDialog(this, "Make Reservation for Room " + roomId, true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(CARD);

        JTextField checkInField = new JTextField();
        JTextField checkOutField = new JTextField();
        JButton viewDatesBtn = createStyledButton("View Booked Dates", PRIMARY_DARK, Color.BLACK);

        panel.add(new JLabel("Check-In Date (YYYY-MM-DD):"));
        panel.add(checkInField);
        panel.add(new JLabel("Check-Out Date (YYYY-MM-DD):"));
        panel.add(checkOutField);
        panel.add(new JLabel());
        panel.add(viewDatesBtn);

        viewDatesBtn.addActionListener(e -> showReservationDates(roomId));

        JButton submitBtn = createStyledButton("Submit Reservation", PRIMARY, Color.BLACK);
        submitBtn.addActionListener(e -> {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate checkIn;
                LocalDate checkOut;

                try {
                    checkIn = LocalDate.parse(checkInField.getText().trim(), formatter);
                    checkOut = LocalDate.parse(checkOutField.getText().trim(), formatter);
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
                }

                LocalDate today = LocalDate.now();
                if (checkIn.isBefore(today)) {
                    throw new IllegalArgumentException("Check-in date cannot be in the past.");
                }

                if (!checkOut.isAfter(checkIn)) {
                    throw new IllegalArgumentException("Check-out date must be after check-in date.");
                }

                String checkOverlapSql = "SELECT COUNT(*) FROM reservations " +
                                        "WHERE room_id = ? AND status != 'Cancelled' AND " +
                                        "(check_in <= ? AND check_out >= ?)";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkOverlapSql)) {
                    checkStmt.setString(1, roomId);
                    checkStmt.setDate(2, java.sql.Date.valueOf(checkOut));
                    checkStmt.setDate(3, java.sql.Date.valueOf(checkIn));
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalArgumentException("Room is already reserved for the selected dates.");
                    }
                }

                String reservationId = "RES" + System.currentTimeMillis();

                String sql = "INSERT INTO reservations (reservation_id, guest_name, room_id, check_in, check_out, status) " +
                             "VALUES (?, ?, ?, ?, ?, 'Pending')";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, reservationId);
                    pstmt.setString(2, currentUser.getFullName());
                    pstmt.setString(3, roomId);
                    pstmt.setDate(4, java.sql.Date.valueOf(checkIn));
                    pstmt.setDate(5, java.sql.Date.valueOf(checkOut));
                    pstmt.executeUpdate();
                }

                String updateRoomSql = "UPDATE rooms SET status = 'Pending' WHERE room_id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateRoomSql)) {
                    pstmt.setString(1, roomId);
                    pstmt.executeUpdate();
                }

                showSuccess("Reservation submitted!\nYour Reservation ID: " + reservationId);
                dialog.dispose();
                SwingUtilities.invokeLater(() -> {
                    loadAvailableRooms();
                    loadUserReservations(reservationsModel);
                });
            } catch (IllegalArgumentException ex) {
                showError("Error: " + ex.getMessage());
            } catch (SQLException ex) {
                showError("Database error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel());
        panel.add(submitBtn);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.darker(), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.darker());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });

        return btn;
    }

    private void customizeTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(40);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setForeground(Color.BLACK);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
    }

    private void loadUserReservations(DefaultTableModel model) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT r.reservation_id, r.room_id, r.check_in, r.check_out, r.status, rm.price " +
                "FROM reservations r " +
                "JOIN rooms rm ON r.room_id = rm.room_id " +
                "WHERE r.guest_name = ? ORDER BY r.check_in")) {
            pstmt.setString(1, currentUser.getFullName());
            ResultSet rs = pstmt.executeQuery();

            model.setRowCount(0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            while (rs.next()) {
                String checkInStr = sdf.format(rs.getDate("check_in"));
                String checkOutStr = sdf.format(rs.getDate("check_out"));
                LocalDate checkIn = LocalDate.parse(checkInStr, formatter);
                LocalDate checkOut = LocalDate.parse(checkOutStr, formatter);
                long days = ChronoUnit.DAYS.between(checkIn, checkOut);
                double pricePerNight = rs.getDouble("price");
                double totalPrice = days * pricePerNight;

                model.addRow(new Object[]{
                        rs.getString("reservation_id"),
                        rs.getString("room_id"),
                        checkInStr,
                        checkOutStr,
                        rs.getString("status"),
                        String.format("$%.2f", totalPrice)
                });
            }
            model.fireTableDataChanged();
        } catch (SQLException e) {
            showError("Error loading reservations: " + e.getMessage());
        }
    }

    private void logout() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.dispose();
        new Login().setVisible(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        private Color bg;

        public ButtonRenderer(String text, Color bg) {
            super(text);
            this.bg = bg;
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value instanceof String) ? (String) value : "";
            setText(text);
            if (row >= table.getRowCount() || table.getRowCount() == 0) {
                setEnabled(false);
                setBackground(Color.LIGHT_GRAY);
                setForeground(Color.GRAY);
            } else {
                setEnabled(true);
                setBackground(bg);
                setForeground(Color.BLACK);
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private String buttonType;
        private DefaultTableModel tableModel;
        private JButton button;

        public ButtonEditor(JCheckBox checkBox, String buttonType, DefaultTableModel model) {
            super(checkBox);
            this.buttonType = buttonType;
            this.tableModel = model;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            String text = (String) value;
            button = new JButton(text);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBackground(buttonType.equals("Reserve") ? PRIMARY : PRIMARY_DARK);
            button.setForeground(Color.WHITE);

            button.addActionListener(e -> {
                if (row >= tableModel.getRowCount() || tableModel.getRowCount() == 0) {
                    showError("No room selected or table is empty.");
                    fireEditingStopped();
                    return;
                }

                String roomId = (String) tableModel.getValueAt(row, 0);
                if ("Reserve".equals(buttonType)) {
                    showReservationDialog(row);
                } else if ("View Reservations".equals(buttonType)) {
                    showReservationDates(roomId);
                }
                fireEditingStopped();
            });
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }

    public static void main(String[] args) {
        User testUser = new User("client", "client123", "client", "John Doe");
        SwingUtilities.invokeLater(() -> new ClientDashboard(testUser).setVisible(true));
    }
}