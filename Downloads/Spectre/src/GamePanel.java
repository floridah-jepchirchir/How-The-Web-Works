import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private static final int PANEL_WIDTH = 960;
    private static final int PANEL_HEIGHT = 640;
    private static final int TIMER_DELAY_MS = 16;
    private static final int CHARACTER_DRAW_HEIGHT = 140;
    private static final int ANIMATION_SPEED = 8;
    private static final int STAGE_PADDING = 24;
    private static final int CELL_GAP = 18;
    private static final int LABEL_HEIGHT = 26;
    private static final double PLAYER_SPEED = 3.8;
    private static final int CHARACTER_COUNT = 5;

    private static final String[] CHARACTER_NAMES = {
            "Astra",
            "Man",
            "Nun",
            "Scare",
            "Whiteb"
    };

    private static final String[][] NORTH_FRAME_NAMES = {
            {"astra.png", "astra2.png", "astra3.png", "astra4.png", "astra5.png", "astra6.png"},
            {"man/man5.png", "man/man6.png", "man/man7.png", "man/man8.png"},
            {"ghost1/nun5.png", "ghost1/nun6.png", "ghost1/nun7.png", "ghost1/nun8.png"},
            {"ghost3/scare4.png", "ghost3/scare5.png", "ghost3/scare6.png"},
            {"ghost2/whiteb5.png", "ghost2/whiteb6.png", "ghost2/whiteb7.png", "ghost2/whiteb8.png"}
    };

    private static final String[][] SOUTH_FRAME_NAMES = {
            {"astra7.png", "astra8.png", "astra9.png", "astra10.png"},
            {"man/man.png", "man/man2.png", "man/man3.png", "man/man4.png"},
            {"ghost1/nun.png", "ghost1/nun2.png", "ghost1/nun3.png", "ghost1/nun4.png"},
            {"ghost3/scare.png", "ghost3/scare2.png", "ghost3/scare3.png"},
            {"ghost2/whiteb.png", "ghost2/whiteb2.png", "ghost2/whiteb3.png", "ghost2/whiteb4.png"}
    };

    private final Timer timer = new Timer(TIMER_DELAY_MS, this);
    private final BufferedImage[][] northFrames = new BufferedImage[CHARACTER_COUNT][];
    private final BufferedImage[][] southFrames = new BufferedImage[CHARACTER_COUNT][];
    private final Rectangle[] movementBounds = new Rectangle[CHARACTER_COUNT];
    private final double[] xPositions = new double[CHARACTER_COUNT];
    private final double[] yPositions = new double[CHARACTER_COUNT];
    private final double[] velocityX = new double[CHARACTER_COUNT];
    private final double[] velocityY = new double[CHARACTER_COUNT];
    private final boolean[] facingNorth = new boolean[CHARACTER_COUNT];
    private final int[] frameIndices = new int[CHARACTER_COUNT];
    private final int[] frameCounters = new int[CHARACTER_COUNT];
    private final boolean[] initializedInBounds = new boolean[CHARACTER_COUNT];

    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private int selectedActorIndex;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        loadCharacters();
        timer.start();

        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void loadCharacters() {
        for (int index = 0; index < CHARACTER_COUNT; index++) {
            northFrames[index] = loadFrames(NORTH_FRAME_NAMES[index]);
            southFrames[index] = loadFrames(SOUTH_FRAME_NAMES[index]);
            movementBounds[index] = new Rectangle();
            facingNorth[index] = true;
            frameIndices[index] = 0;
            frameCounters[index] = 0;
            velocityX[index] = 0;
            velocityY[index] = 0;
            initializedInBounds[index] = false;
        }
    }

    private BufferedImage[] loadFrames(String[] fileNames) {
        return Arrays.stream(fileNames)
                .map(this::loadImage)
                .filter(image -> image != null)
                .toArray(BufferedImage[]::new);
    }

    private BufferedImage loadImage(String fileName) {
        String normalizedName = fileName.replace("\\", "/");
        try {
            URL imageResource = GamePanel.class.getResource("/images/" + normalizedName);
            if (imageResource != null) {
                return ImageIO.read(imageResource);
            }

            File[] candidates = {
                    new File("images", normalizedName),
                    new File("Spectre\\src\\images\\" + normalizedName.replace("/", "\\")),
                    new File(System.getProperty("user.dir"), "images\\" + normalizedName.replace("/", "\\")),
                    new File(System.getProperty("user.dir"), "src\\images\\" + normalizedName.replace("/", "\\"))
            };

            for (File candidate : candidates) {
                if (candidate.exists()) {
                    return ImageIO.read(candidate);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load image: " + fileName);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        paintBackground(g2);
        for (int index = 0; index < CHARACTER_COUNT; index++) {
            paintCharacter(g2, index);
        }

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        for (int index = 0; index < CHARACTER_COUNT; index++) {
            Rectangle bounds = movementBounds[index];
            Color borderColor = index == selectedActorIndex
                    ? new Color(255, 220, 120)
                    : new Color(110, 110, 110);

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(index == selectedActorIndex ? 3f : 2f));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

            g2.setColor(new Color(235, 235, 235));
            g2.drawString((index + 1) + " " + CHARACTER_NAMES[index], bounds.x + 12, bounds.y + 22);
        }
    }

    private void paintCharacter(Graphics2D g2, int index) {
        BufferedImage frame = getCurrentFrame(index);
        int drawWidth = getDrawWidth(frame);

        if (frame != null) {
            g2.drawImage(frame, (int) xPositions[index], (int) yPositions[index], drawWidth, CHARACTER_DRAW_HEIGHT, null);
        } else {
            g2.setColor(Color.GRAY);
            g2.fillRect((int) xPositions[index], (int) yPositions[index], 64, 96);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        double inputX = 0;
        double inputY = 0;

        if (leftPressed) {
            inputX -= PLAYER_SPEED;
        }
        if (rightPressed) {
            inputX += PLAYER_SPEED;
        }
        if (upPressed) {
            inputY -= PLAYER_SPEED;
        }
        if (downPressed) {
            inputY += PLAYER_SPEED;
        }

        for (int index = 0; index < CHARACTER_COUNT; index++) {
            if (index == selectedActorIndex) {
                updateControlledCharacter(index, inputX, inputY);
            } else {
                updateIdleCharacter(index);
            }
        }

        repaint();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        layoutMovementBounds();
    }

    private void layoutMovementBounds() {
        int availableWidth = Math.max(PANEL_WIDTH, getWidth()) - (STAGE_PADDING * 2);
        int availableHeight = Math.max(PANEL_HEIGHT, getHeight()) - (STAGE_PADDING * 2);
        int columns = 3;
        int rows = 2;
        int cellWidth = (availableWidth - ((columns - 1) * CELL_GAP)) / columns;
        int cellHeight = (availableHeight - ((rows - 1) * CELL_GAP)) / rows;

        for (int index = 0; index < CHARACTER_COUNT; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = STAGE_PADDING + column * (cellWidth + CELL_GAP);
            int y = STAGE_PADDING + row * (cellHeight + CELL_GAP);
            movementBounds[index] = new Rectangle(x, y, cellWidth, cellHeight);
            placeCharacterInsideBounds(index);
        }
    }

    private void placeCharacterInsideBounds(int index) {
        Rectangle bounds = movementBounds[index];
        int drawWidth = getDrawWidth(getCurrentFrame(index));
        double centeredX = bounds.x + (bounds.width - drawWidth) / 2.0;
        double centeredY = bounds.y + LABEL_HEIGHT + Math.max(0, bounds.height - LABEL_HEIGHT - CHARACTER_DRAW_HEIGHT) / 2.0;

        if (!initializedInBounds[index]) {
            xPositions[index] = centeredX;
            yPositions[index] = centeredY;
            initializedInBounds[index] = true;
        } else {
            xPositions[index] = clamp(xPositions[index], bounds.x + 6, Math.max(bounds.x + 6, bounds.x + bounds.width - drawWidth - 6));
            yPositions[index] = clamp(yPositions[index], bounds.y + LABEL_HEIGHT + 4, Math.max(bounds.y + LABEL_HEIGHT + 4, bounds.y + bounds.height - CHARACTER_DRAW_HEIGHT - 6));
        }
    }

    private void updateControlledCharacter(int index, double inputX, double inputY) {
        velocityX[index] = inputX;
        velocityY[index] = inputY;
        applyMovement(index);
    }

    private void updateIdleCharacter(int index) {
        velocityX[index] = 0;
        velocityY[index] = 0;
        frameIndices[index] = 0;
        frameCounters[index] = 0;
    }

    private void applyMovement(int index) {
        Rectangle bounds = movementBounds[index];
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        xPositions[index] += velocityX[index];
        yPositions[index] += velocityY[index];

        int drawWidth = getDrawWidth(getCurrentFrame(index));
        double minX = bounds.x + 6;
        double maxX = Math.max(minX, bounds.x + bounds.width - drawWidth - 6);
        double minY = bounds.y + LABEL_HEIGHT + 4;
        double maxY = Math.max(minY, bounds.y + bounds.height - CHARACTER_DRAW_HEIGHT - 6);

        if (xPositions[index] <= minX || xPositions[index] >= maxX) {
            velocityX[index] = 0;
            xPositions[index] = clamp(xPositions[index], minX, maxX);
        }
        if (yPositions[index] <= minY || yPositions[index] >= maxY) {
            velocityY[index] = 0;
            yPositions[index] = clamp(yPositions[index], minY, maxY);
        }

        if (velocityY[index] < 0) {
            facingNorth[index] = true;
        } else if (velocityY[index] > 0) {
            facingNorth[index] = false;
        }

        boolean moving = Math.abs(velocityX[index]) > 0.01 || Math.abs(velocityY[index]) > 0.01;
        if (moving && getActiveFrames(index).length > 0) {
            frameCounters[index]++;
            if (frameCounters[index] >= ANIMATION_SPEED) {
                frameCounters[index] = 0;
                frameIndices[index] = (frameIndices[index] + 1) % getActiveFrames(index).length;
            }
        } else {
            frameIndices[index] = 0;
            frameCounters[index] = 0;
        }
    }

    private BufferedImage[] getActiveFrames(int index) {
        return facingNorth[index] ? northFrames[index] : southFrames[index];
    }

    private BufferedImage getCurrentFrame(int index) {
        return getFrameFrom(getActiveFrames(index), frameIndices[index]);
    }

    private static BufferedImage getFrameFrom(BufferedImage[] frames, int index) {
        if (frames == null || frames.length == 0) {
            return null;
        }

        BufferedImage frame = frames[index % frames.length];
        if (frame != null) {
            return frame;
        }

        for (BufferedImage fallback : frames) {
            if (fallback != null) {
                return fallback;
            }
        }

        return null;
    }

    private static int getDrawWidth(BufferedImage sprite) {
        if (sprite == null) {
            return 64;
        }
        return Math.max(1, sprite.getWidth() * CHARACTER_DRAW_HEIGHT / Math.max(1, sprite.getHeight()));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> upPressed = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> downPressed = true;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> leftPressed = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> rightPressed = true;
            case KeyEvent.VK_G -> selectedActorIndex = (selectedActorIndex + 1) % CHARACTER_COUNT;
            case KeyEvent.VK_1 -> selectedActorIndex = 0;
            case KeyEvent.VK_2 -> selectedActorIndex = 1;
            case KeyEvent.VK_3 -> selectedActorIndex = 2;
            case KeyEvent.VK_4 -> selectedActorIndex = 3;
            case KeyEvent.VK_5 -> selectedActorIndex = 4;
            default -> {
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> upPressed = false;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> downPressed = false;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> leftPressed = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> rightPressed = false;
            default -> {
            }
        }
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
