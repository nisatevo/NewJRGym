package gym.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    public static final int WIDTH = 1280, HEIGHT = 820;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String activeNav = "Home";

    public MainFrame() {
        setTitle("New JR Gym • Management System");
        setSize(WIDTH, HEIGHT);
        setMinimumSize(new Dimension(1050, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        content.setBackground(Theme.BG);
        setLayout(new BorderLayout());
        add(topBar(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        showDashboard();
        setActiveNav("Home");
    }

    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setBackground(Theme.BG);
        bar.setBorder(BorderFactory.createEmptyBorder(18, 28, 14, 28));

        JPanel brand = new JPanel(new BorderLayout(8, 0));
        brand.setOpaque(false);
        JPanel mark = new MuscleLogo();
        mark.setBackground(new Color(24, 28, 19));
        mark.setPreferredSize(new Dimension(42, 42));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel n = new JLabel("New JR Gym");
        n.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel s = Theme.muted("Gym Management");
        words.add(n);
        words.add(s);
        brand.add(mark, BorderLayout.WEST);
        brand.add(words, BorderLayout.CENTER);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        nav.setBackground(Theme.CARD);
        nav.setBorder(new LineBorder(Theme.BORDER, 1, true));
        String[] names = { "Home", "Members", "Trainer", "Membership", "Payments" };
        for (String x : names) {
            JButton b = Theme.button(x);
            b.setBorder(new EmptyBorder(9, 15, 9, 15));
            b.setBackground(Theme.CARD);
            b.setForeground(Theme.TEXT);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setFocusPainted(false);
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!x.equals(activeNav)) {
                        b.setBackground(Theme.INPUT);
                        b.setForeground(Theme.PRIMARY);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!x.equals(activeNav)) {
                        b.setBackground(Theme.CARD);
                        b.setForeground(Theme.TEXT);
                    }
                }
            });
            b.addActionListener(e -> {
                navigate(x);
                setActiveNav(x);
            });
            navButtons.put(x, b);
            nav.add(b);
        }

        bar.add(brand, BorderLayout.WEST);
        bar.add(nav, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton logout = Theme.button("Logout");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        right.add(logout);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void setActiveNav(String name) {
        activeNav = name;
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(name);
            JButton b = entry.getValue();
            b.setBackground(active ? Theme.TEXT : Theme.CARD);
            b.setForeground(active ? Color.BLACK : Theme.TEXT);
        }
    }

    public void navigate(String x) {
        switch (x) {
            case "Home" -> showDashboard();
            case "Members" -> show(new MemberPanel());
            case "Trainer" -> show(new TrainerPanel());
            case "Membership" -> show(new MembershipPanel());
            case "Payments" -> show(new PaymentPanel());
        }
    }

    private void show(JPanel p) {
        content.removeAll();
        content.add(p, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private void showDashboard() {
        show(new DashboardPanel(this));
    }

    /**
     * Small built-in vector-style flexed-arm logo; no external image/font
     * dependency.
     */
    private static final class MuscleLogo extends JPanel {
        MuscleLogo() {
            setOpaque(true);
            setBorder(new LineBorder(new Color(52, 60, 38), 1, true));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.PRIMARY);
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Forearm / wrist
            g2.drawLine(12, 29, 20, 21);
            g2.drawLine(20, 21, 27, 27);
            // Bicep curve
            g2.drawArc(15, 8, 20, 20, 70, 150);
            g2.drawArc(20, 12, 15, 13, 10, 155);
            // Fist / fingers
            g2.drawLine(27, 13, 32, 10);
            g2.drawLine(30, 14, 35, 12);
            g2.drawLine(32, 16, 37, 15);
            g2.drawLine(28, 18, 34, 19);
            // thumb
            g2.drawArc(23, 17, 10, 9, 250, 120);
            g2.dispose();
        }
    }

}
