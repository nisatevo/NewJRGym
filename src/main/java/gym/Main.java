package gym;

import gym.database.DatabaseManager;
import gym.gui.LoginFrame;
import gym.gui.Theme;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.initialize();
        Theme.setup();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
