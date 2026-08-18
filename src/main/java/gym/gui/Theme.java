package gym.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class Theme {
    public static final Color BG = new Color(9, 10, 12), SIDEBAR = new Color(13, 15, 18), CARD = new Color(18, 20, 24),
            CARD2 = new Color(22, 24, 29), INPUT = new Color(28, 31, 36), BORDER = new Color(42, 45, 52),
            TEXT = new Color(244, 246, 248), MUTED = new Color(151, 158, 168), PRIMARY = new Color(190, 255, 82),
            PRIMARY_DARK = new Color(140, 210, 42), SUCCESS = new Color(75, 220, 153),
            DANGER = new Color(255, 104, 112), WARNING = new Color(255, 197, 75);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 30), H2 = new Font("SansSerif", Font.BOLD, 19),
            BODY = new Font("SansSerif", Font.PLAIN, 13);

    private Theme() {
    }

    public static void setup() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.background", INPUT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("ComboBox.background", INPUT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("Table.background", CARD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", new Color(54, 70, 38));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.background", CARD2);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("OptionPane.messageForeground", TEXT);
    }

    public static JPanel panel() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    public static JPanel flat() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    public static JLabel title(String s) {
        JLabel l = new JLabel(s);
        l.setFont(TITLE);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel section(String s) {
        JLabel l = new JLabel(s);
        l.setFont(H2);
        l.setForeground(TEXT);
        return l;
    }

    public static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(BODY);
        f.setBackground(INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
        f.setPreferredSize(new Dimension(160, 38));
        return f;
    }

    public static JPasswordField password() {
        JPasswordField f = new JPasswordField();
        f.setFont(BODY);
        f.setBackground(INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
        f.setPreferredSize(new Dimension(160, 38));
        return f;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(BODY);
        c.setBackground(INPUT);
        c.setForeground(TEXT);
        c.setBorder(new LineBorder(BORDER, 1, true));
        return c;
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setForeground(TEXT);
        b.setBackground(INPUT);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(9, 14, 9, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton primary(String text) {
        JButton b = button(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.BLACK);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        return b;
    }

    public static JPanel fieldBox(String label, JComponent c) {
        JPanel p = flat();
        p.setLayout(new BorderLayout(5, 5));
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        p.add(l, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(42);
        t.setFont(BODY);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(32, 35, 40));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(new Font("SansSerif", Font.BOLD, 11));
        h.setPreferredSize(new Dimension(0, 38));
        h.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
    }

    public static JLabel muted(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(MUTED);
        l.setFont(BODY);
        return l;
    }

    public static String money(long cents) {
        return gym.util.Money.format(cents);
    }
}
