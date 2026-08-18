package gym.gui;

import gym.database.DatabaseManager;
import gym.model.Membership;
import gym.util.Money;
import gym.util.Validation;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.*;

public class MembershipPanel extends JPanel {
    private final JTextField memberId = Theme.field(), start = Theme.field();
    private final JComboBox<String> plan = Theme.combo("Basic", "Standard", "Premium");
    private final JComboBox<String> payment = Theme.combo("Paid", "Not Paid");
    private final JComboBox<String> method = Theme.combo("Cash", "Card", "bKash", "Nagad", "Bank Transfer");
    private final JLabel price = new JLabel("৳ 0.00");
    private final JTextField search = Theme.field();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Member", "Plan", "Duration", "Price", "Start", "End", "Remaining", "Status" }, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public MembershipPanel() {
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(10, 28, 24, 28));
        setLayout(new BorderLayout(16, 16));
        JPanel h = Theme.flat();
        h.setLayout(new BorderLayout());
        h.add(Theme.title("Memberships"), BorderLayout.WEST);
        h.add(Theme.muted("Plan prices are fixed here; membership dates are calculated automatically."),
                BorderLayout.EAST);
        add(h, BorderLayout.NORTH);
        JPanel center = Theme.flat();
        center.setLayout(new BorderLayout(16, 16));
        center.add(top(), BorderLayout.NORTH);
        center.add(tableArea(), BorderLayout.CENTER);
        add(center);
        start.setText(LocalDate.now().toString());
        plan.addActionListener(e -> updatePlanInfo());
        payment.addActionListener(e -> method.setEnabled("Paid".equals(payment.getSelectedItem())));
        start.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updatePlanInfo();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updatePlanInfo();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updatePlanInfo();
            }
        });
        load();
        updatePlanInfo();
    }

    private JPanel top() {
        JPanel root = Theme.flat();
        root.setLayout(new BorderLayout(12, 12));
        JPanel prices = new JPanel(new GridLayout(1, 3, 12, 0));
        prices.setOpaque(false);
        for (String p : new String[] { "Basic", "Standard", "Premium" })
            prices.add(planCard(p));
        root.add(prices, BorderLayout.NORTH);
        JPanel form = Theme.panel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        add(g, form, 0, 0, "MEMBER ID", memberId);
        add(g, form, 1, 0, "PLAN", plan);
        add(g, form, 2, 0, "START DATE", start);
        add(g, form, 3, 0, "PRICE", price);
        add(g, form, 4, 0, "PAYMENT", payment);
        add(g, form, 5, 0, "PAYMENT METHOD", method);
        JButton create = Theme.primary("Create Membership");
        create.addActionListener(e -> create());
        g.gridx = 6;
        g.gridy = 0;
        g.gridheight = 1;
        form.add(create, g);
        root.add(form, BorderLayout.SOUTH);
        return root;
    }

    private void add(GridBagConstraints g, JPanel p, int x, int y, String label, JComponent c) {
        g.gridx = x;
        g.gridy = y;
        g.gridheight = 1;
        p.add(Theme.fieldBox(label, c), g);
    }

    private JPanel planCard(String p) {
        JPanel c = Theme.panel();
        c.setLayout(new BorderLayout(4, 4));
        JLabel n = Theme.muted(p);
        JLabel v = new JLabel("—");
        v.setFont(new Font("SansSerif", Font.BOLD, 21));
        c.add(n, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        try {
            var x = DatabaseManager.getPlan(p);
            if (x != null)
                v.setText(Money.format((long) x.get("price")) + " / " + x.get("months") + " mo");
        } catch (Exception ignored) {
        }
        return c;
    }

    private JPanel tableArea() {
        JPanel p = Theme.panel();
        p.setLayout(new BorderLayout(10, 10));
        JPanel s = Theme.flat();
        s.setLayout(new BorderLayout(8, 0));
        s.add(Theme.muted("Search memberships"), BorderLayout.WEST);
        s.add(search, BorderLayout.CENTER);
        JButton go = Theme.button("Search");
        go.addActionListener(e -> load());
        s.add(go, BorderLayout.EAST);
        p.add(s, BorderLayout.NORTH);
        Theme.styleTable(table);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void updatePlanInfo() {
        try {
            var x = DatabaseManager.getPlan((String) plan.getSelectedItem());
            if (x == null)
                return;
            long cents = (long) x.get("price");
            price.setText(Money.format(cents));
        } catch (Exception e) {
            price.setText("—");
        }
    }

    private void create() {
        try {
            int id;
            try {
                id = Integer.parseInt(Validation.required(memberId.getText(), "Member ID"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Member ID must be a whole number.");
            }
            if (id <= 0)
                throw new IllegalArgumentException("Member ID must be positive.");
            LocalDate st = Validation.date(start.getText(), "Start date");
            if (st.isBefore(LocalDate.now().minusDays(1)))
                throw new IllegalArgumentException("Start date cannot be in the past.");
            String pl = (String) plan.getSelectedItem();
            var x = DatabaseManager.getPlan(pl);
            int months = (int) x.get("months");
            long cents = (long) x.get("price");
            boolean paid = "Paid".equals(payment.getSelectedItem());
            if (paid)
                DatabaseManager.createMembership(id, pl, months, cents, st, true, (String) method.getSelectedItem());
            else
                DatabaseManager.createMembership(id, pl, months, cents, st, false, (String) method.getSelectedItem());
            success(paid ? "Membership created and payment recorded." : "Membership created. Payment remains due.");
            memberId.setText("");
            load();
        } catch (Exception e) {
            error(e);
        }
    }

    private void load() {
        try {
            model.setRowCount(0);
            for (Membership m : DatabaseManager.getMemberships(search.getText().trim()))
                model.addRow(new Object[] { m.getId(), "#" + m.getMemberId() + " • " + m.getMemberName(), m.getPlan(),
                        m.getDurationMonths() + " mo", Money.format(m.getPriceCents()), m.getStartDate(),
                        m.getEndDate(), m.remainingDays() + " days", m.getStatus() });
        } catch (Exception e) {
            error(e);
        }
    }

    private void success(String s) {
        JOptionPane.showMessageDialog(this, s, "New JR Gym", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Please check the input", JOptionPane.ERROR_MESSAGE);
    }
}
