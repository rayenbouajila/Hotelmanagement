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
import java.util.logging.Logger;

public class AdminDashboard extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboard.class.getName());

    private final Color PRIMARY = new Color(33, 150, 243);
    private final Color SECONDARY = new Color(245, 245, 245);
    private final Color ACCENT = new Color(100, 181, 246);
    private final Color DANGER = new Color(229, 57, 53);

    private final User currentUser;
    private Connection connection;
    private JTable reservationTable;
    private DefaultTableModel reservationModel;
    private DefaultTableModel roomModel;
    private DefaultTableModel userModel;

    public AdminDashboard(User user) {
        this.currentUser = user;
        try {
            this.connection = DatabaseConnection.getConnection();
            initializeUI();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion à la base de données: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeUI() {
        setTitle("Tableau de Bord Admin - " + currentUser.getFullName());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.severe("Failed to set look and feel: " + e.getMessage());
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(SECONDARY);

        mainPanel.add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));

        tabs.addTab("Chambres", createRoomPanel());
        tabs.addTab("Réservations", createReservationPanel());
        tabs.addTab("Comptes", createUserPanel());
        tabs.addTab("Statistiques", createAnalyticsPanel());

        mainPanel.add(tabs, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(new EmptyBorder(10, 20, 10, 20));
        header.setPreferredSize(new Dimension(getWidth(), 70));

        JLabel title = new JLabel("GESTION HÔTELLIÈRE - ADMIN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.BLACK);
        header.add(title, BorderLayout.WEST);

        JLabel userInfo = new JLabel("Connecté en tant que: " + currentUser.getUsername());
        userInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userInfo.setForeground(Color.BLACK);
        header.add(userInfo, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton logoutBtn = createStyledButton("Déconnexion", DANGER, Color.RED);
        logoutBtn.addActionListener(e -> logout());

        actions.add(logoutBtn);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JPanel createRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(SECONDARY);

        roomModel = new DefaultTableModel(
            new Object[]{"ID", "Type", "Prix", "Statut", "Modifier", "Supprimer", "Voir Réservations"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 4 && row < getRowCount();
            }
        };

        JTable table = new JTable(roomModel);
        loadRoomsData(roomModel);
        customizeTable(table);

        TableColumn editColumn = table.getColumnModel().getColumn(4);
        editColumn.setCellRenderer(new ButtonRenderer("Modifier", PRIMARY));
        editColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "Modifier", roomModel));

        TableColumn deleteColumn = table.getColumnModel().getColumn(5);
        deleteColumn.setCellRenderer(new ButtonRenderer("Supprimer", DANGER));
        deleteColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "Supprimer", roomModel));

        TableColumn viewReservationsColumn = table.getColumnModel().getColumn(6);
        viewReservationsColumn.setCellRenderer(new ButtonRenderer("Voir Réservations", ACCENT));
        viewReservationsColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "Voir Réservations", roomModel));

        JButton addBtn = createStyledButton("+ Ajouter Chambre", ACCENT, Color.BLACK);
        addBtn.addActionListener(e -> showAddRoomDialog(roomModel));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(SECONDARY);
        btnPanel.add(addBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadRoomsData(DefaultTableModel model) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY room_id")) {

            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("room_id"),
                    rs.getString("room_type"),
                    String.format("$%.2f", rs.getDouble("price")),
                    rs.getString("status"),
                    "Modifier",
                    "Supprimer",
                    "Voir Réservations"
                });
            }
            model.fireTableDataChanged();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des chambres: " + e.getMessage());
            LOGGER.severe("Failed to load rooms: " + e.getMessage());
        }
    }

    private void showAddRoomDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Ajouter une Chambre", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Standard", "Deluxe", "Suite"});
        JTextField priceField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Available", "Reserved", "Maintenance", "Pending"});
        panel.add(new JLabel("Numéro chambre*:"));
        panel.add(idField);
        panel.add(new JLabel("Type*:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Prix*:"));
        panel.add(priceField);
        panel.add(new JLabel("Statut*:"));
        panel.add(statusCombo);

        JButton saveBtn = createStyledButton("Enregistrer", PRIMARY, Color.BLACK);
        saveBtn.addActionListener(e -> {
            try {
                String roomId = idField.getText().trim();
                if (roomId.isEmpty() || priceField.getText().isEmpty()) {
                    throw new IllegalArgumentException("Tous les champs sont obligatoires");
                }

                String checkSql = "SELECT COUNT(*) FROM rooms WHERE room_id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, roomId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        showError("Une chambre avec ce numéro existe déjà.");
                        return;
                    }
                }

                double price = Double.parseDouble(priceField.getText());

                String sql = "INSERT INTO rooms (room_id, room_type, price, status) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, roomId);
                    pstmt.setString(2, (String)typeCombo.getSelectedItem());
                    pstmt.setDouble(3, price);
                    pstmt.setString(4, (String)statusCombo.getSelectedItem());
                    pstmt.executeUpdate();

                    loadRoomsData(model);
                    dialog.dispose();
                    showSuccess("Chambre ajoutée avec succès!");
                }
            } catch (NumberFormatException ex) {
                showError("Format de prix invalide");
            } catch (SQLException ex) {
                showError("Erreur base de données: " + ex.getMessage());
                LOGGER.severe("Failed to add room: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        });

        panel.add(new JLabel());
        panel.add(saveBtn);

        dialog.add(panel);
        dialog.setVisible(true);
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

                StringBuilder message = new StringBuilder("Réservations pour la chambre " + roomId + ":\n\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                boolean hasReservations = false;

                while (rs.next()) {
                    hasReservations = true;
                    message.append("Du ")
                           .append(sdf.format(rs.getDate("check_in")))
                           .append(" au ")
                           .append(sdf.format(rs.getDate("check_out")))
                           .append(" (")
                           .append(rs.getString("status"))
                           .append(")\n");
                }

                if (!hasReservations) {
                    message.append("Aucune réservation active.");
                }

                JOptionPane.showMessageDialog(this, message.toString(), 
                    "Dates de Réservation", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            showError("Erreur lors de la récupération des réservations: " + e.getMessage());
            LOGGER.severe("Failed to load reservation dates: " + e.getMessage());
        }
    }

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(SECONDARY);

        userModel = new DefaultTableModel(
            new Object[]{"Username", "Full Name", "Role", "Change Role", "Delete"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (row >= getRowCount()) return false;
                String username = (String) getValueAt(row, 0);
                return column >= 3 && !username.equals(currentUser.getUsername());
            }
        };

        JTable table = new JTable(userModel);
        customizeTable(table);
        loadUsersData(userModel);

        TableColumn changeRoleColumn = table.getColumnModel().getColumn(3);
        changeRoleColumn.setCellRenderer(new ButtonRenderer("Change Role", PRIMARY));
        changeRoleColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "Change Role", userModel));

        TableColumn deleteColumn = table.getColumnModel().getColumn(4);
        deleteColumn.setCellRenderer(new ButtonRenderer("Delete", DANGER));
        deleteColumn.setCellEditor(new ButtonEditor(new JCheckBox(), "Delete", userModel));

        JButton addUserBtn = createStyledButton("+ Add User", ACCENT, Color.BLACK);
        addUserBtn.addActionListener(e -> showAddUserDialog(userModel));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(SECONDARY);
        btnPanel.add(addUserBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadUsersData(DefaultTableModel model) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, full_name, role FROM users ORDER BY username")) {

            model.setRowCount(0);

            while (rs.next()) {
                String username = rs.getString("username");
                model.addRow(new Object[]{
                    username,
                    rs.getString("full_name"),
                    rs.getString("role"),
                    username.equals(currentUser.getUsername()) ? "" : "Change Role",
                    username.equals(currentUser.getUsername()) ? "" : "Delete"
                });
            }
            model.fireTableDataChanged();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des utilisateurs: " + e.getMessage());
            LOGGER.severe("Failed to load users: " + e.getMessage());
        }
    }

    private void showAddUserDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Ajouter un Utilisateur", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField usernameField = new JTextField();
        JTextField fullNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"client", "admin"});

        panel.add(new JLabel("Nom d'utilisateur*:"));
        panel.add(usernameField);
        panel.add(new JLabel("Nom complet*:"));
        panel.add(fullNameField);
        panel.add(new JLabel("Mot de passe*:"));
        panel.add(passwordField);
        panel.add(new JLabel("Rôle*:"));
        panel.add(roleCombo);

        JButton saveBtn = createStyledButton("Enregistrer", PRIMARY, Color.BLACK);
        saveBtn.addActionListener(e -> {
            try {
                String username = usernameField.getText().trim();
                String fullName = fullNameField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();
                String role = (String) roleCombo.getSelectedItem();

                if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
                    throw new IllegalArgumentException("Tous les champs sont obligatoires");
                }

                String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        showError("Ce nom d'utilisateur existe déjà.");
                        return;
                    }
                }

                String insertSql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password); 
                    pstmt.setString(3, role);
                    pstmt.setString(4, fullName);
                    pstmt.executeUpdate();

                    loadUsersData(model);
                    dialog.dispose();
                    showSuccess("Utilisateur ajouté avec succès!");
                }
            } catch (SQLException ex) {
                showError("Erreur base de données: " + ex.getMessage());
                LOGGER.severe("Failed to add user: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        });

        panel.add(new JLabel());
        panel.add(saveBtn);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private JPanel createReservationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(SECONDARY);

        reservationModel = new DefaultTableModel(
            new Object[]{"ID", "Client", "Chambre", "Arrivée", "Départ", "Statut", "Confirmer", "Annuler", "Supprimer"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (row >= getRowCount()) return false;
                String status = (String) getValueAt(row, 5);
                if (column == 6) {
                    return "Pending".equalsIgnoreCase(status);
                } else if (column == 7) {
                    return "Pending".equalsIgnoreCase(status) || "Confirmed".equalsIgnoreCase(status);
                } else if (column == 8) {
                    return "Cancelled".equalsIgnoreCase(status);
                }
                return false;
            }
        };

        reservationTable = new JTable(reservationModel);
        customizeTable(reservationTable);

        TableColumn confirmCol = reservationTable.getColumnModel().getColumn(6);
        confirmCol.setCellRenderer(new ButtonRenderer("Confirmer", PRIMARY));
        confirmCol.setCellEditor(new ButtonEditor(new JCheckBox(), "Confirmer", reservationModel));
        confirmCol.setPreferredWidth(100);

        TableColumn cancelCol = reservationTable.getColumnModel().getColumn(7);
        cancelCol.setCellRenderer(new ButtonRenderer("Annuler", DANGER));
        cancelCol.setCellEditor(new ButtonEditor(new JCheckBox(), "Annuler", reservationModel));
        cancelCol.setPreferredWidth(100);

        TableColumn deleteCol = reservationTable.getColumnModel().getColumn(8);
        deleteCol.setCellRenderer(new ButtonRenderer("Supprimer", DANGER));
        deleteCol.setCellEditor(new ButtonEditor(new JCheckBox(), "Supprimer", reservationModel));
        deleteCol.setPreferredWidth(100);

        JButton deleteCancelledBtn = createStyledButton("Supprimer Réservations Annulées", DANGER, Color.BLACK);
        deleteCancelledBtn.addActionListener(e -> deleteAllCancelledReservations());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(SECONDARY);
        btnPanel.add(deleteCancelledBtn);

        panel.add(new JScrollPane(reservationTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadReservationsData(reservationModel);

        return panel;
    }

    private void loadReservationsData(DefaultTableModel model) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reservations ORDER BY reservation_id")) {

            model.setRowCount(0);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                String status = rs.getString("status");
                String confirmAction = "Pending".equalsIgnoreCase(status) ? "Confirmer" : "";
                String cancelAction = ("Pending".equalsIgnoreCase(status) || "Confirmed".equalsIgnoreCase(status)) ? "Annuler" : "";
                String deleteAction = "Cancelled".equalsIgnoreCase(status) ? "Supprimer" : "";

                model.addRow(new Object[]{
                    rs.getString("reservation_id"),
                    rs.getString("guest_name"),
                    rs.getString("room_id"),
                    dateFormat.format(rs.getDate("check_in")),
                    dateFormat.format(rs.getDate("check_out")),
                    status,
                    confirmAction,
                    cancelAction,
                    deleteAction
                });
            }
            model.fireTableDataChanged();
            if (reservationTable != null) {
                reservationTable.repaint();
                LOGGER.info("Reservations table data loaded and repainted");
            } else {
                LOGGER.warning("reservationTable is null, skipping repaint");
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des réservations: " + e.getMessage());
            LOGGER.severe("Failed to load reservations: " + e.getMessage());
        }
    }

    private void deleteAllCancelledReservations() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Supprimer toutes les réservations annulées?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String deleteSql = "DELETE FROM reservations WHERE status = 'Cancelled'";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    int affected = deleteStmt.executeUpdate();
                    loadReservationsData(reservationModel);
                    showSuccess(affected + " réservations annulées supprimées!");
                }
            } catch (SQLException ex) {
                showError("Erreur lors de la suppression: " + ex.getMessage());
                LOGGER.severe("Failed to delete cancelled reservations: " + ex.getMessage());
            }
        }
    }

    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(SECONDARY);

        panel.add(createMetricCard("Taux Occupation", calculateOccupancyRate(), PRIMARY));
        panel.add(createMetricCard("Revenu Mensuel", calculateMonthlyRevenue(), ACCENT));
        panel.add(createMetricCard("Clients Total", calculateTotalGuests(), new Color(46, 125, 50)));
        panel.add(createMetricCard("Chambres Libres", calculateAvailableRooms(), new Color(156, 39, 176)));

        return panel;
    }

    private String calculateOccupancyRate() {
        try (CallableStatement cstmt = connection.prepareCall("{? = call get_occupancy_rate}")) {
            cstmt.registerOutParameter(1, Types.VARCHAR);
            cstmt.execute();
            return cstmt.getString(1);
        } catch (SQLException e) {
            LOGGER.severe("Failed to calculate occupancy rate: " + e.getMessage());
            return "N/A";
        }
    }

    private String calculateMonthlyRevenue() {
        try (CallableStatement cstmt = connection.prepareCall("{? = call get_monthly_revenue}")) {
            cstmt.registerOutParameter(1, Types.VARCHAR);
            cstmt.execute();
            return "$" + cstmt.getString(1);
        } catch (SQLException e) {
            LOGGER.severe("Failed to calculate monthly revenue: " + e.getMessage());
            return "$0.00";
        }
    }

    private String calculateTotalGuests() {
        try (CallableStatement cstmt = connection.prepareCall("{? = call get_total_guests}")) {
            cstmt.registerOutParameter(1, Types.VARCHAR);
            cstmt.execute();
            return cstmt.getString(1);
        } catch (SQLException e) {
            LOGGER.severe("Failed to calculate total guests: " + e.getMessage());
            return "0";
        }
    }

    private String calculateAvailableRooms() {
        try (CallableStatement cstmt = connection.prepareCall("{? = call get_available_rooms}")) {
            cstmt.registerOutParameter(1, Types.VARCHAR);
            cstmt.execute();
            return cstmt.getString(1);
        } catch (SQLException e) {
            LOGGER.severe("Failed to calculate available rooms: " + e.getMessage());
            return "0/0";
        }
    }

    private JPanel createMetricCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(SECONDARY);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 2),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
    }

    private void logout() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to close database connection: " + e.getMessage());
        }
        this.dispose();
        new Login().setVisible(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        private Color bg;
        private JLabel emptyLabel;

        public ButtonRenderer(String text, Color bg) {
            super(text);
            this.bg = bg;
            this.emptyLabel = new JLabel();
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null || value.toString().isEmpty() || row >= table.getRowCount() || table.getRowCount() == 0) {
                emptyLabel.setBackground(table.getBackground());
                emptyLabel.setOpaque(true);
                LOGGER.fine("Rendering empty label for row: " + row + ", column: " + column + ", value: " + value);
                return emptyLabel;
            }

            String text = (String) value;
            setText(text);
            setEnabled(table.isCellEditable(row, column));
            setBackground(bg);
            setForeground(Color.BLACK);
            LOGGER.fine("Rendering button for row: " + row + ", column: " + column + ", text: " + text + ", enabled: " + isEnabled());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private String buttonType;
        private DefaultTableModel tableModel;
        private Color bg;
        private JButton button;

        public ButtonEditor(JCheckBox checkBox, String buttonType, DefaultTableModel model) {
            super(checkBox);
            this.buttonType = buttonType;
            this.tableModel = model;
            this.bg = buttonType.equals("Annuler") || buttonType.equals("Supprimer") || buttonType.equals("Delete") ? DANGER :
                      buttonType.equals("Voir Réservations") ? ACCENT : PRIMARY;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            String text = (value instanceof String) ? (String) value : "";
            button = new JButton(text);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBackground(bg);
            button.setForeground(Color.BLACK);
            button.setEnabled(table.isCellEditable(row, column));

            button.addActionListener(e -> {
                if (row >= tableModel.getRowCount() || tableModel.getRowCount() == 0) {
                    showError("Aucune sélection ou tableau vide.");
                    fireEditingStopped();
                    return;
                }

                LOGGER.info("Button clicked: " + buttonType + " at row: " + row);
                if ("Modifier".equals(buttonType)) {
                    editRoom(row);
                } else if ("Supprimer".equals(buttonType) && table == reservationTable) {
                    deleteReservation(row);
                } else if ("Supprimer".equals(buttonType)) {
                    deleteRoom(row);
                } else if ("Confirmer".equals(buttonType) || "Annuler".equals(buttonType)) {
                    processReservation(row, buttonType);
                } else if ("Voir Réservations".equals(buttonType)) {
                    String roomId = (String) tableModel.getValueAt(row, 0);
                    showReservationDates(roomId);
                } else if ("Change Role".equals(buttonType)) {
                    changeUserRole(row);
                } else if ("Delete".equals(buttonType)) {
                    deleteUser(row);
                }
                fireEditingStopped();
            });
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        private void editRoom(int row) {
            String roomId = (String) tableModel.getValueAt(row, 0);

            try (PreparedStatement pstmt = connection.prepareStatement(
                 "SELECT * FROM rooms WHERE room_id = ?")) {

                pstmt.setString(1, roomId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    JDialog editDialog = new JDialog(AdminDashboard.this, "Modifier Chambre", true);
                    editDialog.setSize(400, 300);
                    editDialog.setLocationRelativeTo(AdminDashboard.this);

                    JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
                    panel.setBorder(new EmptyBorder(20, 20, 20, 20));

                    JTextField idField = new JTextField(rs.getString("room_id"));
                    idField.setEditable(false);
                    JComboBox<String> typeCombo = new JComboBox<>(
                        new String[]{"Standard", "Deluxe", "Suite"});
                    typeCombo.setSelectedItem(rs.getString("room_type"));
                    JTextField priceField = new JTextField(String.valueOf(rs.getDouble("price")));
                    JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Available", "Reserved", "Maintenance", "Pending"});
                    statusCombo.setSelectedItem(rs.getString("status"));

                    panel.add(new JLabel("Numéro:"));
                    panel.add(idField);
                    panel.add(new JLabel("Type:"));
                    panel.add(typeCombo);
                    panel.add(new JLabel("Prix:"));
                    panel.add(priceField);
                    panel.add(new JLabel("Statut:"));
                    panel.add(statusCombo);

                    JButton updateBtn = createStyledButton("Mettre à jour", PRIMARY, Color.BLACK);
                    updateBtn.addActionListener(ev -> {
                        try {
                            double price = Double.parseDouble(priceField.getText());

                            try (PreparedStatement updateStmt = connection.prepareStatement(
                                "UPDATE rooms SET room_type = ?, price = ?, status = ? WHERE room_id = ?")) {

                                updateStmt.setString(1, (String) typeCombo.getSelectedItem());
                                updateStmt.setDouble(2, price);
                                updateStmt.setString(3, (String) statusCombo.getSelectedItem());
                                updateStmt.setString(4, roomId);
                                updateStmt.executeUpdate();

                                loadRoomsData(tableModel);
                                editDialog.dispose();
                                showSuccess("Chambre mise à jour!");
                            }
                        } catch (NumberFormatException ex) {
                            showError("Format de prix invalide");
                        } catch (SQLException ex) {
                            showError("Erreur base de données: " + ex.getMessage());
                            LOGGER.severe("Failed to update room: " + ex.getMessage());
                        }
                    });

                    panel.add(new JLabel());
                    panel.add(updateBtn);

                    editDialog.add(panel);
                    editDialog.setVisible(true);
                }
            } catch (SQLException ex) {
                showError("Erreur: " + ex.getMessage());
                LOGGER.severe("Failed to load room for editing: " + ex.getMessage());
            }
        }

        private void deleteRoom(int row) {
            String roomId = (String) tableModel.getValueAt(row, 0);

            int confirm = JOptionPane.showConfirmDialog(
                AdminDashboard.this,
                "Supprimer la chambre " + roomId + "?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String statusSql = "SELECT status FROM rooms WHERE room_id = ?";
                    try (PreparedStatement statusStmt = connection.prepareStatement(statusSql)) {
                        statusStmt.setString(1, roomId);
                        ResultSet rs = statusStmt.executeQuery();
                        if (rs.next()) {
                            String status = rs.getString("status");
                            if (!"Available".equalsIgnoreCase(status)) {
                                showError("Seules les chambres disponibles peuvent être supprimées.");
                                return;
                            }
                        } else {
                            showError("Chambre non trouvée.");
                            return;
                        }
                    }

                    String checkSql = "SELECT COUNT(*) FROM reservations WHERE room_id = ? AND status != 'Cancelled'";
                    try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                        checkStmt.setString(1, roomId);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            showError("Impossible de supprimer: réservations actives.");
                            return;
                        }
                    }

                    String deleteSql = "DELETE FROM rooms WHERE room_id = ?";
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, roomId);
                        int affected = deleteStmt.executeUpdate();

                        if (affected > 0) {
                            tableModel.removeRow(row);
                            showSuccess("Chambre supprimée!");
                        }
                    }
                } catch (SQLException ex) {
                    showError("Erreur suppression: " + ex.getMessage());
                    LOGGER.severe("Failed to delete room: " + ex.getMessage());
                }
            }
        }

        private void deleteReservation(int row) {
            String reservationId = (String) tableModel.getValueAt(row, 0);

            int confirm = JOptionPane.showConfirmDialog(
                AdminDashboard.this,
                "Supprimer la réservation " + reservationId + "?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String checkSql = "SELECT status FROM reservations WHERE reservation_id = ?";
                    try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                        checkStmt.setString(1, reservationId);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            String status = rs.getString("status");
                            if (!"Cancelled".equalsIgnoreCase(status)) {
                                showError("Seules les réservations annulées peuvent être supprimées.");
                                return;
                            }
                        } else {
                            showError("Réservation non trouvée.");
                            return;
                        }
                    }

                    String deleteSql = "DELETE FROM reservations WHERE reservation_id = ?";
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, reservationId);
                        int affected = deleteStmt.executeUpdate();

                        if (affected > 0) {
                            tableModel.removeRow(row);
                            showSuccess("Réservation supprimée!");
                        }
                    }
                } catch (SQLException ex) {
                    showError("Erreur suppression: " + ex.getMessage());
                    LOGGER.severe("Failed to delete reservation: " + ex.getMessage());
                }
            }
        }

        private void changeUserRole(int row) {
            String username = (String) tableModel.getValueAt(row, 0);
            String currentRole = (String) tableModel.getValueAt(row, 2);

            if (username.equals(currentUser.getUsername())) {
                showError("Vous ne pouvez pas modifier votre propre rôle.");
                return;
            }

            String newRole = "admin".equalsIgnoreCase(currentRole) ? "client" : "admin";
            int confirm = JOptionPane.showConfirmDialog(
                AdminDashboard.this,
                "Changer le rôle de " + username + " de " + currentRole + " à " + newRole + " ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String updateSql = "UPDATE users SET role = ? WHERE username = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                        pstmt.setString(1, newRole);
                        pstmt.setString(2, username);
                        int affected = pstmt.executeUpdate();

                        if (affected > 0) {
                            tableModel.setValueAt(newRole, row, 2);
                            tableModel.fireTableDataChanged();
                            showSuccess("Rôle de l'utilisateur mis à jour!");
                        } else {
                            showError("Échec de la mise à jour du rôle.");
                        }
                    }
                } catch (SQLException ex) {
                    showError("Erreur lors de la mise à jour du rôle: " + ex.getMessage());
                    LOGGER.severe("Failed to change user role: " + ex.getMessage());
                }
            }
        }

        private void deleteUser(int row) {
            String username = (String) tableModel.getValueAt(row, 0);

            if (username.equals(currentUser.getUsername())) {
                showError("Vous ne pouvez pas supprimer votre propre compte.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                AdminDashboard.this,
                "Supprimer l'utilisateur " + username + " ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String checkReservationsSql = "SELECT COUNT(*) FROM reservations WHERE guest_name = ? AND status != 'Cancelled'";
                    try (PreparedStatement checkStmt = connection.prepareStatement(checkReservationsSql)) {
                        checkStmt.setString(1, (String) tableModel.getValueAt(row, 1));
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            showError("Impossible de supprimer: l'utilisateur a des réservations actives.");
                            return;
                        }
                    }

                    String deleteSql = "DELETE FROM users WHERE username = ?";
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, username);
                        int affected = deleteStmt.executeUpdate();

                        if (affected > 0) {
                            tableModel.removeRow(row);
                            showSuccess("Utilisateur supprimé!");
                        } else {
                            showError("Échec de la suppression de l'utilisateur.");
                        }
                    }
                } catch (SQLException ex) {
                    showError("Erreur lors de la suppression: " + ex.getMessage());
                    LOGGER.severe("Failed to delete user: " + ex.getMessage());
                }
            }
        }

        private void processReservation(int row, String action) {
            String reservationId = (String) tableModel.getValueAt(row, 0);
            String roomId = (String) tableModel.getValueAt(row, 2);
            String currentStatus = (String) tableModel.getValueAt(row, 5);
            String checkInDateStr = (String) tableModel.getValueAt(row, 3);
            String checkOutDateStr = (String) tableModel.getValueAt(row, 4);

            String newStatus = null;
            String newRoomStatus = null;

            if ("Confirmer".equals(action) && "Pending".equalsIgnoreCase(currentStatus)) {
                newStatus = "Confirmed";
                LocalDate currentDate = LocalDate.now();
                LocalDate checkInDate = LocalDate.parse(checkInDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                newRoomStatus = currentDate.isEqual(checkInDate) ? "Reserved" : "Available";
            } else if ("Annuler".equals(action) && ("Pending".equalsIgnoreCase(currentStatus) || "Confirmed".equalsIgnoreCase(currentStatus))) {
                newStatus = "Cancelled";
                newRoomStatus = "Available";
            } else {
                showError("Action non autorisée pour le statut: " + currentStatus);
                return;
            }

            String confirmMessage = "Confirmer".equals(action) ? 
                "Confirmer la réservation " + reservationId + " ?" :
                "Annuler la réservation " + reservationId + " ?";
            int confirm = JOptionPane.showConfirmDialog(
                AdminDashboard.this,
                confirmMessage,
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                connection.setAutoCommit(false);

                if ("Confirmer".equals(action)) {
                    String checkOverlapSql = "SELECT COUNT(*) FROM reservations " +
                                            "WHERE room_id = ? AND status != 'Cancelled' AND " +
                                            "reservation_id != ? AND " +
                                            "(check_in <= ? AND check_out >= ?)";
                    try (PreparedStatement checkStmt = connection.prepareStatement(checkOverlapSql)) {
                        checkStmt.setString(1, roomId);
                        checkStmt.setString(2, reservationId);
                        checkStmt.setDate(3, java.sql.Date.valueOf(checkOutDateStr));
                        checkStmt.setDate(4, java.sql.Date.valueOf(checkInDateStr));
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException("Conflit de réservation: la chambre est déjà réservée pour ces dates.");
                        }
                    }
                }

                String checkRoomSql = "SELECT status FROM rooms WHERE room_id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkRoomSql)) {
                    checkStmt.setString(1, roomId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (!rs.next()) {
                        throw new SQLException("Chambre non trouvée: " + roomId);
                    }
                    String currentRoomStatus = rs.getString("status");
                    if ("Confirmer".equals(action) && !"Available".equalsIgnoreCase(currentRoomStatus) && !"Pending".equalsIgnoreCase(currentRoomStatus)) {
                        throw new SQLException("La chambre n'est pas disponible pour confirmation.");
                    }
                }

                String updateReservationSql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateReservationSql)) {
                    pstmt.setString(1, newStatus);
                    pstmt.setString(2, reservationId);
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Échec de la mise à jour de la réservation.");
                    }
                }

                String updateRoomSql = "UPDATE rooms SET status = ? WHERE room_id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateRoomSql)) {
                    pstmt.setString(1, newRoomStatus);
                    pstmt.setString(2, roomId);
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Échec de la mise à jour du statut de la chambre.");
                    }
                }

                connection.commit();

                tableModel.setValueAt(newStatus, row, 5);
                tableModel.setValueAt("Confirmed".equals(newStatus) ? "" : "Confirmer", row, 6);
                tableModel.setValueAt("Cancelled".equals(newStatus) ? "" : "Annuler", row, 7);
                tableModel.setValueAt("Cancelled".equals(newStatus) ? "Supprimer" : "", row, 8);
                tableModel.fireTableDataChanged();
                reservationTable.clearSelection();
                reservationTable.revalidate();
                reservationTable.repaint();

                loadRoomsData(roomModel);
                showSuccess("Réservation " + (action.equals("Confirmer") ? "confirmée" : "annulée") + " avec succès!");
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    showError("Erreur de rollback: " + rollbackEx.getMessage());
                    LOGGER.severe("Rollback failed: " + rollbackEx.getMessage());
                }
                showError("Erreur: " + e.getMessage());
                LOGGER.severe("Failed to process reservation: " + e.getMessage());
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    showError("Erreur lors de la restauration de l'auto-commit: " + ex.getMessage());
                    LOGGER.severe("Failed to restore auto-commit: " + ex.getMessage());
                }
            }
        }
    }
}