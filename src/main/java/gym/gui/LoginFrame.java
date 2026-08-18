package gym.gui;

import gym.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField user = Theme.field();
    private final JPasswordField pass = Theme.password();

    public LoginFrame() {
        setTitle("New JR Gym • Sign in");
        setSize(980, 650);
        setMinimumSize(new Dimension(850, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);
        JPanel card = Theme.panel();
        card.setPreferredSize(new Dimension(470, 455));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("💪  New JR Gym");
        logo.setFont(new Font("SansSerif", Font.BOLD, 28));
        logo.setForeground(Theme.PRIMARY);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logo);
        card.add(Box.createVerticalStrut(8));
        JLabel sub = Theme.muted("Fitness business management");
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(30));

        card.add(Theme.fieldBox("USERNAME", user));
        card.add(Box.createVerticalStrut(16));
        card.add(Theme.fieldBox("PASSWORD", pass));
        card.add(Box.createVerticalStrut(22));

        JButton login = Theme.primary("Sign in to dashboard");
        login.setAlignmentX(Component.CENTER_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(login);
        card.add(Box.createVerticalStrut(12));

        JButton register = Theme.button("Create new admin account");
        register.setAlignmentX(Component.CENTER_ALIGNMENT);
        register.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        card.add(register);
        card.add(Box.createVerticalStrut(14));

        JLabel demo = Theme.muted("Default admin: admin / admin123");
        demo.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(demo);

        login.addActionListener(e -> login());
        pass.addActionListener(e -> login());
        register.addActionListener(e -> showRegistration());

        root.add(card);
        add(root);
    }

    private void login() {
        try {
            if (!DatabaseManager.login(user.getText().trim(), new String(pass.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Sign in failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dispose();
            new MainFrame().setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRegistration() {
        JTextField username = Theme.field();
        JPasswordField password = Theme.password();
        JPasswordField confirm = Theme.password();

        JPanel panel = new JPanel();
        panel.setBackground(Theme.CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Theme.fieldBox("USERNAME", username));
        panel.add(Box.createVerticalStrut(12));
        panel.add(Theme.fieldBox("PASSWORD", password));
        panel.add(Box.createVerticalStrut(12));
        panel.add(Theme.fieldBox("CONFIRM PASSWORD", confirm));
        panel.add(Box.createVerticalStrut(10));
        JLabel rules = Theme.muted("4–25 chars • start with a letter • password: 8+ chars, upper/lowercase + number");
        rules.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rules);

        int result = JOptionPane.showConfirmDialog(this, panel, "Register New Admin", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            DatabaseManager.registerAdmin(username.getText(), new String(password.getPassword()), new String(confirm.getPassword()));
            JOptionPane.showMessageDialog(this, "Admin account created successfully. You can sign in now.", "Registration complete", JOptionPane.INFORMATION_MESSAGE);
            user.setText(username.getText().trim());
            pass.setText("");
            pass.requestFocusInWindow();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Registration failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
