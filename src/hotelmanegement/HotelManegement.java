package hotelmanegement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;

public class HotelManegement extends JFrame {
    
    public HotelManegement() {
        setTitle("Hotel Management System");
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
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(50, 50, 50, 50));
        
        JLabel titleLabel = new JLabel("WELCOME TO OUR HOTEL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Experience luxury and comfort");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton getStartedButton = new JButton("GET STARTED");
        getStartedButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        getStartedButton.setBackground(new Color(41, 128, 185));
        getStartedButton.setForeground(Color.WHITE);
        getStartedButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        getStartedButton.addActionListener(e -> {
            dispose();
            new Login().setVisible(true);
        });
        
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        contentPanel.add(getStartedButton);
        
        mainPanel.add(contentPanel);
        add(mainPanel);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotelManegement().setVisible(true));
    }
}