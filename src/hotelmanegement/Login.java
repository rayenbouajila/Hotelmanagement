package hotelmanegement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;

public class Login extends JFrame {
    
    public Login() {
        setTitle("Hotel Management System - Login");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(23, 42, 58);
                Color color2 = new Color(31, 58, 80);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridBagLayout());
        loginPanel.setBackground(new Color(255, 255, 255, 230));
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
            new EmptyBorder(40, 60, 40, 60)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel titleLabel = new JLabel("HOTEL LOGIN");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(23, 42, 58));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        loginPanel.add(titleLabel, gbc);
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 5, 10);
        loginPanel.add(userLabel, gbc);
        
        JTextField userText = new JTextField(20);
        styleTextField(userText);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        loginPanel.add(userText, gbc);
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        loginPanel.add(passwordLabel, gbc);
        JPasswordField passwordText = new JPasswordField(20);
        styleTextField(passwordText);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        loginPanel.add(passwordText, gbc);
        JLabel forgotPassword = new JLabel("Forgot password?");
        forgotPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotPassword.setForeground(new Color(41, 128, 185));
        forgotPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(Login.this, 
                    "Please contact the hotel administration.", 
                    "Forgot Password", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 20, 0);
        loginPanel.add(forgotPassword, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        JButton loginButton = createButton("LOGIN", 
            new Color(41, 128, 185), Color.black, new Color(31, 97, 141));
        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passwordText.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(Login.this, 
                    "Please enter both username and password", 
                    "Login Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            User user = AuthService.authenticate(username, password);
            if (user != null) {
                dispose();
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    new AdminDashboard(user).setVisible(true);
                } else {
                    new ClientDashboard(user).setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(Login.this, 
                    "Invalid username or password", 
                    "Login Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton signupButton = createButton("SIGN UP",
            new Color(245, 245, 245), new Color(41, 128, 185), new Color(200, 200, 200));
        signupButton.addActionListener(e -> showSignUpDialog());
        
        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(buttonPanel, gbc);
        
        mainPanel.add(loginPanel);
        add(mainPanel);
    }
    
    private void showSignUpDialog() {
        JDialog signUpDialog = new JDialog(this, "Create Account", true);
        signUpDialog.setSize(400, 500);
        signUpDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel title = new JLabel("CREATE ACCOUNT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);
        
        gbc.gridy = 1;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridy = 2;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        gbc.gridy = 3;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridy = 4;
        JTextField userField = new JTextField(20);
        panel.add(userField, gbc);
 
        gbc.gridy = 5;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridy = 6;
        JPasswordField passField = new JPasswordField(20);
        panel.add(passField, gbc);
        
        gbc.gridy = 7;
        panel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridy = 8;
        JPasswordField confirmField = new JPasswordField(20);
        panel.add(confirmField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton createButton = new JButton("Create Account");
        createButton.addActionListener(e -> {
            String fullName = nameField.getText();
            String username = userField.getText();
            String password = new String(passField.getPassword());
            String confirmPassword = new String(confirmField.getPassword());
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(signUpDialog, "Passwords don't match!");
                return;
            }
            
            if (AuthService.registerUser(fullName, username, password)) {
                JOptionPane.showMessageDialog(signUpDialog, "Account created successfully!");
                signUpDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(signUpDialog, "Error creating account. Username may already exist.");
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> signUpDialog.dispose());
        
        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);
        
        signUpDialog.add(panel);
        signUpDialog.setVisible(true);
    }
    
    private void styleTextField(JComponent field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(250, 35));
    }
    
    private JButton createButton(String text, Color bgColor, Color textColor, Color borderColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            new EmptyBorder(5, 20, 5, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}