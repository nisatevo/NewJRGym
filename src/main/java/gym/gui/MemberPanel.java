package gym.gui;

import gym.database.DatabaseManager;
import gym.model.Member;
import gym.util.ImageStore;
import gym.util.Validation;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

public class MemberPanel extends JPanel {
    private final JTextField name = Theme.field(), age = Theme.field(), phone = Theme.field(), email = Theme.field(),
            address = Theme.field(), search = Theme.field();
    private final JComboBox<String> gender = Theme.combo("Male", "Female", "Other");
    private final JLabel photo = new JLabel("No photo", SwingConstants.CENTER);
    private String photoPath = "";
    private int selectedId = -1;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Name", "Age", "Gender", "Phone", "Email", "Membership" }, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public MemberPanel() {
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(10, 28, 24, 28));
        setLayout(new BorderLayout(16, 16));
        JPanel top = Theme.flat();
        top.setLayout(new BorderLayout());
        top.add(Theme.title("Members"), BorderLayout.WEST);
        top.add(Theme.muted("Add photos, keep contact details clean, and protect payment history."), BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JPanel main = Theme.flat();
        main.setLayout(new BorderLayout(16, 16));
        main.add(form(), BorderLayout.NORTH);
        main.add(tableArea(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        load();
    }

    private JPanel form() {
        JPanel card = Theme.panel();
        card.setLayout(new BorderLayout(14, 0));
        JPanel fields = new JPanel(new GridLayout(2, 4, 12, 12));
        fields.setOpaque(false);
        fields.add(Theme.fieldBox("FULL NAME", name));
        fields.add(Theme.fieldBox("AGE", age));
        fields.add(Theme.fieldBox("GENDER", gender));
        fields.add(Theme.fieldBox("PHONE", phone));
        fields.add(Theme.fieldBox("EMAIL", email));
        fields.add(Theme.fieldBox("ADDRESS", address));
        JButton choose = Theme.button("Choose photo");
        choose.addActionListener(e -> choosePhoto());
        fields.add(Theme.fieldBox("PROFILE PHOTO", choose));
        JPanel actions = Theme.flat();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton add = Theme.primary("Add Member");
        JButton update = Theme.button("Update");
        JButton del = Theme.button("Delete");
        JButton clear = Theme.button("Clear");
        add.addActionListener(e -> add());
        update.addActionListener(e -> update());
        del.addActionListener(e -> delete());
        clear.addActionListener(e -> clear());
        actions.add(add);
        actions.add(update);
        actions.add(del);
        actions.add(clear);
        JPanel left = Theme.flat();
        left.setLayout(new BorderLayout(12, 10));
        left.add(fields, BorderLayout.CENTER);
        left.add(actions, BorderLayout.SOUTH);
        photo.setPreferredSize(new Dimension(112, 112));
        photo.setForeground(Theme.MUTED);
        photo.setBorder(new LineBorder(Theme.BORDER, 1, true));
        card.add(left, BorderLayout.CENTER);
        card.add(photo, BorderLayout.EAST);
        return card;
    }

    private JPanel tableArea() {
        JPanel p = Theme.panel();
        p.setLayout(new BorderLayout(10, 10));
        JPanel searchBar = Theme.flat();
        searchBar.setLayout(new BorderLayout(8, 0));
        searchBar.add(Theme.muted("Search"), BorderLayout.WEST);
        searchBar.add(search, BorderLayout.CENTER);
        JButton go = Theme.button("Search");
        go.addActionListener(e -> load());
        searchBar.add(go, BorderLayout.EAST);
        p.add(searchBar, BorderLayout.NORTH);
        Theme.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> fill());
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void choosePhoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                int id = selectedId > 0 ? selectedId : DatabaseManager.nextMemberId();
                photoPath = ImageStore.copy(fc.getSelectedFile().toPath(), "member", id);
                setPreview(photoPath);
            } catch (Exception e) {
                error(e);
            }
        }
    }

    private void setPreview(String path) {
        try {
            if (path == null || path.isBlank()) {
                photo.setText("No photo");
                photo.setIcon(null);
                return;
            }
            BufferedImage im = ImageIO.read(Paths.get(path).toFile());
            Image scaled = im.getScaledInstance(110, 110, Image.SCALE_SMOOTH);
            photo.setText("");
            photo.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            photo.setIcon(null);
            photo.setText("Photo");
        }
    }

    private Member read() {
        String n = Validation.name(name.getText(), "Name");
        int a = Validation.age(age.getText());
        String ph = Validation.phone(phone.getText());
        String em = Validation.email(email.getText());
        String ad = Validation.address(address.getText());
        return new Member(selectedId > 0 ? selectedId : 0, n, a, (String) gender.getSelectedItem(), ph, em, ad,
                photoPath.isBlank() ? null : photoPath);
    }

    private void add() {
        try {
            Member x = read();
            DatabaseManager.addMember(x);
            success("Member added successfully.");
            clear();
            load();
        } catch (Exception e) {
            error(e);
        }
    }

    private void update() {
        try {
            if (selectedId < 1)
                throw new IllegalArgumentException("Select a member from the table first.");
            DatabaseManager.updateMember(read());
            success("Member updated successfully.");
            clear();
            load();
        } catch (Exception e) {
            error(e);
        }
    }

    private void delete() {
        try {
            if (selectedId < 1)
                throw new IllegalArgumentException("Select a member first.");
            if (JOptionPane.showConfirmDialog(this,
                    "Delete member #" + selectedId + "? Financial records cannot be deleted with the member.",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            DatabaseManager.deleteMember(selectedId);
            clear();
            load();
            success("Member deleted.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void fill() {
        int r = table.getSelectedRow();
        if (r < 0)
            return;
        try {
            selectedId = (int) model.getValueAt(r, 0);
            Member m = DatabaseManager.getMember(selectedId);
            if (m == null)
                return;
            name.setText(m.getName());
            age.setText(String.valueOf(m.getAge()));
            gender.setSelectedItem(m.getGender());
            phone.setText(m.getPhone());
            email.setText(m.getEmail());
            address.setText(m.getAddress());
            photoPath = m.getPhotoPath() == null ? "" : m.getPhotoPath();
            setPreview(photoPath);
        } catch (Exception e) {
            error(e);
        }
    }

    private void clear() {
        selectedId = -1;
        name.setText("");
        age.setText("");
        phone.setText("");
        email.setText("");
        address.setText("");
        gender.setSelectedIndex(0);
        photoPath = "";
        photo.setIcon(null);
        photo.setText("No photo");
        table.clearSelection();
    }

    private void load() {
        try {
            model.setRowCount(0);
            for (Member m : DatabaseManager.getMembers(search.getText().trim())) {
                var active = DatabaseManager.getActiveMembership(m.getId());
                model.addRow(new Object[] { m.getId(), m.getName(), m.getAge(), m.getGender(), m.getPhone(),
                        m.getEmail(), active == null ? "No active plan" : active.getPlan() });
            }
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
