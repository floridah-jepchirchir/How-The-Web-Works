import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("SPECTRE");
            GamePanel panel = new GamePanel();

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.add(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
