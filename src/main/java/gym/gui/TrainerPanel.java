package gym.gui;

import gym.database.DatabaseManager;
import gym.model.Trainer;
import gym.util.ImageStore;
import gym.util.Money;
import gym.util.Validation;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

public class TrainerPanel extends JPanel {
    private final JTextField name = Theme.field(), age = Theme.field(), specialization = Theme.field(),
            phone = Theme.field(), email = Theme.field(), pay = Theme.field();
    private final JLabel photo = new JLabel("No photo", SwingConstants.CENTER);
    private String photoPath = "";
    private int selectedId = -1;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Trainer", "Specialization", "Phone", "Email", "Fixed Monthly Pay" }, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public TrainerPanel() {
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(10, 28, 24, 28));
        setLayout(new BorderLayout(16, 16));
        JPanel h = Theme.flat();
        h.setLayout(new BorderLayout());
        h.add(Theme.title("Trainers"), BorderLayout.WEST);
        h.add(Theme.muted("Fixed monthly pay is used automatically when paying trainers."), BorderLayout.EAST);
        add(h, BorderLayout.NORTH);
        JPanel center = Theme.flat();
        center.setLayout(new BorderLayout(16, 16));
        center.add(form(), BorderLayout.NORTH);
        center.add(tableArea(), BorderLayout.CENTER);
        add(center);
        load();
    }

    private JPanel form() {
        JPanel c = Theme.panel();
        c.setLayout(new BorderLayout(14, 0));
        JPanel f = new JPanel(new GridLayout(2, 4, 12, 12));
        f.setOpaque(false);
        f.add(Theme.fieldBox("FULL NAME", name));
        f.add(Theme.fieldBox("AGE", age));
        f.add(Theme.fieldBox("SPECIALIZATION", specialization));
        f.add(Theme.fieldBox("PHONE", phone));
        f.add(Theme.fieldBox("EMAIL", email));
        f.add(Theme.fieldBox("FIXED MONTHLY PAY", pay));
        JButton photoBtn = Theme.button("Choose photo");
        photoBtn.addActionListener(e -> choosePhoto());
        f.add(Theme.fieldBox("PROFILE PHOTO", photoBtn));
        JPanel actions = Theme.flat();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton add = Theme.primary("Add Trainer"), del = Theme.button("Delete"), clear = Theme.button("Clear");
        add.addActionListener(e -> add());
        del.addActionListener(e -> delete());
        clear.addActionListener(e -> clear());
        actions.add(add);
        actions.add(del);
        actions.add(clear);
        JPanel left = Theme.flat();
        left.setLayout(new BorderLayout(8, 8));
        left.add(f, BorderLayout.CENTER);
        left.add(actions, BorderLayout.SOUTH);
        photo.setPreferredSize(new Dimension(112, 112));
        photo.setBorder(new LineBorder(Theme.BORDER, 1, true));
        c.add(left, BorderLayout.CENTER);
        c.add(photo, BorderLayout.EAST);
        return c;
    }

    private JPanel tableArea() {
        JPanel p = Theme.panel();
        p.setLayout(new BorderLayout());
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
                int id = selectedId > 0 ? selectedId : DatabaseManager.nextTrainerId();
                photoPath = ImageStore.copy(fc.getSelectedFile().toPath(), "trainer", id);
                preview(photoPath);
            } catch (Exception e) {
                error(e);
            }
        }
    }

    private void preview(String p) {
        try {
            BufferedImage im = ImageIO.read(Paths.get(p).toFile());
            photo.setText("");
            photo.setIcon(new ImageIcon(im.getScaledInstance(110, 110, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            photo.setIcon(null);
            photo.setText("Photo");
        }
    }

    private Trainer read() {
        String n = Validation.name(name.getText(), "Name");
        int a = Validation.age(age.getText());
        String sp = Validation.name(specialization.getText(), "Specialization");
        String ph = Validation.phone(phone.getText());
        String em = Validation.email(email.getText());
        long amount = Money.parseCents(pay.getText());
        return new Trainer(0, n, a, sp, ph, em, amount, photoPath.isBlank() ? null : photoPath);
    }

    private void add() {
        try {
            DatabaseManager.addTrainer(read());
            success("Trainer added successfully.");
            clear();
            load();
        } catch (Exception e) {
            error(e);
        }
    }

    private void delete() {
        try {
            if (selectedId < 1)
                throw new IllegalArgumentException("Select a trainer first.");
            if (JOptionPane.showConfirmDialog(this, "Delete trainer #" + selectedId + "? Payment history is protected.",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            DatabaseManager.deleteTrainer(selectedId);
            clear();
            load();
            success("Trainer deleted.");
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
            for (Trainer t : DatabaseManager.getTrainers())
                if (t.getId() == selectedId) {
                    name.setText(t.getName());
                    age.setText(String.valueOf(t.getAge()));
                    specialization.setText(t.getSpecialization());
                    phone.setText(t.getPhone());
                    email.setText(t.getEmail());
                    pay.setText(java.math.BigDecimal.valueOf(t.getMonthlyPayCents(), 2).toPlainString());
                    photoPath = t.getPhotoPath() == null ? "" : t.getPhotoPath();
                    if (!photoPath.isBlank())
                        preview(photoPath);
                    break;
                }
        } catch (Exception e) {
            error(e);
        }
    }

    private void clear() {
        selectedId = -1;
        name.setText("");
        age.setText("");
        specialization.setText("");
        phone.setText("");
        email.setText("");
        pay.setText("");
        photoPath = "";
        photo.setIcon(null);
        photo.setText("No photo");
        table.clearSelection();
    }

    private void load() {
        try {
            model.setRowCount(0);
            for (Trainer t : DatabaseManager.getTrainers())
                model.addRow(new Object[] { t.getId(), t.getName(), t.getSpecialization(), t.getPhone(), t.getEmail(),
                        Money.format(t.getMonthlyPayCents()) });
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
