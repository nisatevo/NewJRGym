package gym.gui;

import gym.database.DatabaseManager;
import gym.model.Membership;
import gym.model.Payment;
import gym.model.Trainer;
import gym.model.TrainerPayment;
import gym.util.Money;
import gym.util.Validation;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentPanel extends JPanel {
    private final MemberPayments memberPayments = new MemberPayments();
    private final TrainerPayments trainerPayments = new TrainerPayments();

    public PaymentPanel() {
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(10, 28, 24, 28));
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.CARD);
        tabs.setForeground(Theme.TEXT);
        tabs.addTab("Member Payments", memberPayments);
        tabs.addTab("Trainer Payments", trainerPayments);
        tabs.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(tabs);
    }

    private static class MemberPayments extends JPanel {
        private final JComboBox<MemberItem> membership = new JComboBox<>();
        private final JComboBox<String> method = Theme.combo("Cash", "Card", "bKash", "Nagad", "Bank Transfer");
        private final JTextField date = Theme.field();
        private final JLabel amount = new JLabel("—");
        private final JLabel balance = new JLabel();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[] { "ID", "Member", "Plan", "Amount", "Method", "Date", "Status" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        private final JTable table = new JTable(model);

        MemberPayments() {
            setBackground(Theme.BG);
            setBorder(new EmptyBorder(16, 0, 0, 0));
            setLayout(new BorderLayout(14, 14));
            JPanel head = Theme.panel();
            head.setLayout(new BorderLayout(12, 10));
            JPanel f = new JPanel(new GridLayout(1, 4, 12, 0));
            f.setOpaque(false);
            f.add(Theme.fieldBox("DUE MEMBERSHIP", membership));
            f.add(Theme.fieldBox("AMOUNT", amount));
            f.add(Theme.fieldBox("PAYMENT DATE", date));
            f.add(Theme.fieldBox("METHOD", method));
            JButton pay = Theme.primary("Record Payment");
            pay.addActionListener(e -> record());
            JPanel right = Theme.flat();
            right.setLayout(new BorderLayout(10, 8));
            right.add(pay, BorderLayout.NORTH);
            right.add(Theme.fieldBox("MONEY AVAILABLE", balance), BorderLayout.SOUTH);
            head.add(f, BorderLayout.CENTER);
            head.add(right, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            Theme.styleTable(table);
            add(new JScrollPane(table), BorderLayout.CENTER);
            date.setText(LocalDate.now().toString());
            membership.addActionListener(e -> updateAmount());
            load();
        }

        private void load() {
            try {
                membership.removeAllItems();
                model.setRowCount(0);
                for (Membership m : DatabaseManager.getUnpaidMemberships())
                    membership.addItem(new MemberItem(m));
                for (Payment p : DatabaseManager.getPayments())
                    model.addRow(new Object[] { p.getId(), p.getMemberName(), p.getPlan(),
                            Money.format(p.getAmountCents()), p.getMethod(), p.getDate(), p.getStatus() });
                balance.setText(Money.format(DatabaseManager.getAvailableBalanceCents()));
                updateAmount();
            } catch (Exception e) {
                error(e);
            }
        }

        private void updateAmount() {
            MemberItem i = (MemberItem) membership.getSelectedItem();
            amount.setText(i == null ? "—" : Money.format(i.m.getPriceCents()));
        }

        private void record() {
            try {
                MemberItem i = (MemberItem) membership.getSelectedItem();
                if (i == null)
                    throw new IllegalArgumentException("There are no unpaid active memberships.");
                LocalDate d = Validation.date(date.getText(), "Payment date");
                if (d.isAfter(LocalDate.now()))
                    throw new IllegalArgumentException("Payment date cannot be in the future.");
                DatabaseManager.recordMembershipPayment(i.m.getId(), i.m.getPriceCents(),
                        (String) method.getSelectedItem(), d);
                success("Member payment recorded. Gym balance increased by " + Money.format(i.m.getPriceCents()) + ".");
                load();
            } catch (Exception e) {
                error(e);
            }
        }

        private static class MemberItem {
            final Membership m;

            MemberItem(Membership m) {
                this.m = m;
            }

            public String toString() {
                return "#" + m.getMemberId() + " • " + m.getMemberName() + " • " + m.getPlan();
            }
        }

        private void success(String s) {
            JOptionPane.showMessageDialog(this, s, "New JR Gym", JOptionPane.INFORMATION_MESSAGE);
        }

        private void error(Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Please check the input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class TrainerPayments extends JPanel {
        private final JComboBox<TrainerItem> trainer = new JComboBox<>();
        private final JComboBox<String> method = Theme.combo("Cash", "Bank Transfer", "bKash", "Nagad");
        private final JTextField month = Theme.field(), date = Theme.field();
        private final JLabel amount = new JLabel("—"), balance = new JLabel();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[] { "ID", "Trainer", "Amount", "Month", "Date", "Method", "Status" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        private final JTable table = new JTable(model);

        TrainerPayments() {
            setBackground(Theme.BG);
            setBorder(new EmptyBorder(16, 0, 0, 0));
            setLayout(new BorderLayout(14, 14));
            JPanel head = Theme.panel();
            head.setLayout(new BorderLayout(12, 10));
            JPanel f = new JPanel(new GridLayout(1, 4, 12, 0));
            f.setOpaque(false);
            f.add(Theme.fieldBox("TRAINER", trainer));
            f.add(Theme.fieldBox("FIXED AMOUNT", amount));
            f.add(Theme.fieldBox("PAYMENT MONTH", month));
            f.add(Theme.fieldBox("PAYMENT DATE", date));
            JPanel r = Theme.flat();
            r.setLayout(new BorderLayout(8, 8));
            JButton pay = Theme.primary("Pay Trainer");
            pay.addActionListener(e -> pay());
            r.add(pay, BorderLayout.NORTH);
            r.add(Theme.fieldBox("MONEY AVAILABLE", balance), BorderLayout.SOUTH);
            head.add(f, BorderLayout.CENTER);
            head.add(r, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            Theme.styleTable(table);
            add(new JScrollPane(table), BorderLayout.CENTER);
            month.setText(YearMonth.now().toString());
            date.setText(LocalDate.now().toString());
            trainer.addActionListener(e -> setAmount());
            load();
        }

        private void load() {
            try {
                trainer.removeAllItems();
                for (Trainer t : DatabaseManager.getTrainers())
                    trainer.addItem(new TrainerItem(t));
                model.setRowCount(0);
                for (TrainerPayment p : DatabaseManager.getTrainerPayments())
                    model.addRow(new Object[] { p.getId(), p.getTrainerName(), Money.format(p.getAmountCents()),
                            p.getPaymentMonth(), p.getDate(), p.getMethod(), p.getStatus() });
                balance.setText(Money.format(DatabaseManager.getAvailableBalanceCents()));
                setAmount();
            } catch (Exception e) {
                error(e);
            }
        }

        private void setAmount() {
            TrainerItem i = (TrainerItem) trainer.getSelectedItem();
            amount.setText(i == null ? "—" : Money.format(i.t.getMonthlyPayCents()));
        }

        private void pay() {
            try {
                TrainerItem i = (TrainerItem) trainer.getSelectedItem();
                if (i == null)
                    throw new IllegalArgumentException("Add a trainer first.");
                YearMonth ym;
                try {
                    ym = YearMonth.parse(Validation.required(month.getText(), "Payment month"));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Payment month must use YYYY-MM.");
                }
                LocalDate d = Validation.date(date.getText(), "Payment date");
                if (d.isAfter(LocalDate.now()))
                    throw new IllegalArgumentException("Payment date cannot be in the future.");
                if (ym.isAfter(YearMonth.now()))
                    throw new IllegalArgumentException("Payment month cannot be in the future.");
                DatabaseManager.payTrainer(i.t.getId(), i.t.getMonthlyPayCents(), ym, d,
                        (String) method.getSelectedItem());
                success("Trainer paid " + Money.format(i.t.getMonthlyPayCents()) + ". Balance reduced immediately.");
                load();
            } catch (Exception e) {
                error(e);
            }
        }

        private static class TrainerItem {
            final Trainer t;

            TrainerItem(Trainer t) {
                this.t = t;
            }

            public String toString() {
                return "#" + t.getId() + " • " + t.getName();
            }
        }

        private void success(String s) {
            JOptionPane.showMessageDialog(this, s, "New JR Gym", JOptionPane.INFORMATION_MESSAGE);
        }

        private void error(Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Please check the input", JOptionPane.ERROR_MESSAGE);
        }
    }
}
