package gym.gui;

import gym.database.DatabaseManager;
import gym.model.Member;
import gym.model.Trainer;
import gym.util.Money;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final MainFrame frame;

    public DashboardPanel(MainFrame f) {
        frame = f;
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(10, 28, 24, 28));
        setLayout(new BorderLayout(18, 18));
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
    }

    private JPanel header() {
        JPanel p = Theme.flat();
        p.setLayout(new BorderLayout());
        JPanel left = Theme.flat();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel h = Theme.title("Manage your fitness business");
        JLabel d = Theme.muted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a")));
        left.add(h);
        left.add(Box.createVerticalStrut(4));
        left.add(d);
        JButton add = Theme.primary("＋  New Member");
        add.addActionListener(e -> frame.navigate("Members"));
        p.add(left, BorderLayout.WEST);
        p.add(add, BorderLayout.EAST);
        return p;
    }

    private JPanel body() {
        JPanel root = Theme.flat();
        root.setLayout(new BorderLayout(18, 18));
        root.add(metrics(), BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(1, 2, 18, 0));
        center.setOpaque(false);
        center.add(capacityAndQuick());
        center.add(recent());
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private JPanel metrics() {
        JPanel p = new JPanel(new GridLayout(1, 4, 14, 0));
        p.setOpaque(false);
        try {
            p.add(metric("Revenue", Money.format(DatabaseManager.getTotalMemberPaymentsCents()), "Member payments"));
            p.add(metric("Active Members", String.valueOf(DatabaseManager.activeMemberCount()),
                    "With current membership"));
            p.add(metric("Trainers", String.valueOf(DatabaseManager.count("trainers")), "Active trainers"));
            p.add(metric("Money Available", Money.format(DatabaseManager.getAvailableBalanceCents()),
                    "After trainer payments"));
        } catch (Exception e) {
            p.add(metric("Revenue", "—", "Database error"));
            p.add(metric("Active Members", "—", ""));
            p.add(metric("Trainers", "—", ""));
            p.add(metric("Money Available", "—", ""));
        }
        return p;
    }

    private JPanel metric(String title, String value, String note) {
        JPanel c = Theme.panel();
        c.setLayout(new BorderLayout(6, 6));
        JLabel t = Theme.muted(title);
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel n = Theme.muted(note);
        c.add(t, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        c.add(n, BorderLayout.SOUTH);
        return c;
    }

    private JPanel capacityAndQuick() {
        JPanel wrap = Theme.flat();
        wrap.setLayout(new BorderLayout(14, 14));
        JPanel cap = Theme.panel();
        cap.setLayout(new BorderLayout(12, 12));
        JLabel t = Theme.section("Gym Capacity");
        JLabel sub = Theme.muted("Indoor & outdoor capacity overview");
        JPanel head = Theme.flat();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(t);
        head.add(sub);
        cap.add(head, BorderLayout.NORTH);
        JPanel dots = new JPanel(new GridLayout(6, 12, 7, 7));
        dots.setOpaque(false);
        int active = 0;
        try {
            active = DatabaseManager.activeMemberCount();
        } catch (Exception ignored) {
        }
        int used = Math.min(72, active);
        for (int i = 0; i < 72; i++) {
            JLabel dot = new JLabel("●", SwingConstants.CENTER);
            dot.setFont(new Font("SansSerif", Font.PLAIN, 12));
            dot.setForeground(i < used ? Theme.PRIMARY : Theme.BORDER);
            dots.add(dot);
        }
        cap.add(dots, BorderLayout.CENTER);
        JLabel status = Theme.muted("Space status  •  " + Math.min(100, (used * 100 / 72)) + "% occupied");
        cap.add(status, BorderLayout.SOUTH);
        wrap.add(cap, BorderLayout.CENTER);
        JPanel quick = Theme.panel();
        quick.setLayout(new BorderLayout(8, 8));
        quick.add(Theme.section("Quick actions"), BorderLayout.NORTH);
        JPanel q = new JPanel(new GridLayout(1, 3, 8, 0));
        q.setOpaque(false);
        JButton a = Theme.button("Members");
        JButton b = Theme.button("Memberships");
        JButton c = Theme.button("Payments");
        a.addActionListener(e -> frame.navigate("Members"));
        b.addActionListener(e -> frame.navigate("Membership"));
        c.addActionListener(e -> frame.navigate("Payments"));
        q.add(a);
        q.add(b);
        q.add(c);
        quick.add(q, BorderLayout.CENTER);
        wrap.add(quick, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel recent() {
        JPanel p = Theme.panel();
        p.setLayout(new BorderLayout(10, 10));
        p.add(Theme.section("All Members"), BorderLayout.NORTH);
        DefaultTableModel m = new DefaultTableModel(new Object[] { "Member", "Plan", "Status", "Phone" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        Theme.styleTable(t);
        try {
            List<Member> ms = DatabaseManager.recentMembers(7);
            for (Member x : ms) {
                String plan = "—", status = "No plan";
                var active = DatabaseManager.getActiveMembership(x.getId());
                if (active != null) {
                    plan = active.getPlan();
                    status = active.getStatus();
                }
                m.addRow(new Object[] { x.getName(), plan, status, x.getPhone() });
            }
        } catch (Exception e) {
        }
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        JLabel note = Theme.muted("Expiring within 7 days: " + safeExpiring());
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    private int safeExpiring() {
        try {
            return DatabaseManager.expiringSoonCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
