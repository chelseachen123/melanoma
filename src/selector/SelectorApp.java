package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import selector.SelectionModel.SelectionState;
import scissors.ScissorsSelectionModel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Arrays;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;

    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add status bar
        statusLabel = new JLabel();
        frame.add(statusLabel, BorderLayout.PAGE_END);


        // Add image component with scrollbars
        imgPanel = new ImagePanel();
        JScrollPane scrollPane = new JScrollPane(imgPanel);
        frame.add(scrollPane);


        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        frame.add(makeControlPanel(), BorderLayout.WEST);

        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));

        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        undoButton = new JButton("Undo");
        resetButton = new JButton("Reset");
        finishButton = new JButton("Finish");
        cancelButton = new JButton("Cancel");

        controlPanel.add(undoButton);
        controlPanel.add(resetButton);
        controlPanel.add(finishButton);
        controlPanel.add(cancelButton);

        // Controller: Attach listeners
        undoButton.addActionListener(e -> model.undo());
        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());
        cancelButton.addActionListener(e -> model.cancelProcessing());

        String[] models = {"Point-to-point", "Intelligent scissors: gray", "Intelligent scissors: "
                + "color"};
        JComboBox modelsBox = new JComboBox(models);
        modelsBox.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String selectedModel = (String)cb.getSelectedItem();
                SelectionModel currentModel = getSelectionModel();
                if (selectedModel.equals("Point-to-point")) {
                    setSelectionModel(new PointToPointSelectionModel(currentModel));
                } else if (selectedModel.equals("Intelligent scissors: gray")) {
                    setSelectionModel(new ScissorsSelectionModel("CrossGradMono", currentModel));
                } else if (selectedModel.equals("Intelligent scissors: color")) {
                    setSelectionModel(new ScissorsSelectionModel("CrossGradMulti", currentModel));
                }
            }
        });
        controlPanel.add(modelsBox);

        return controlPanel;
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include:
     * * "state": Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            reflectSelectionState(model.state());
            if (model.state() == PROCESSING) {
                processingProgress.setIndeterminate(true);
            } else {
                processingProgress.setIndeterminate(false);
                processingProgress.setValue(0);
            }
        } else if ("progress".equals(evt.getPropertyName())) {
            processingProgress.setValue((int) evt.getNewValue());
            processingProgress.setIndeterminate(false);
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());

        if (state == NO_SELECTION) {
            cancelButton.setEnabled(false);
            undoButton.setEnabled(false);
            undoItem.setEnabled(false);
            resetButton.setEnabled(false);
            finishButton.setEnabled(false);
            saveItem.setEnabled(false);
        } else if (state == SELECTING) {
            cancelButton.setEnabled(false);
            undoButton.setEnabled(true);
            undoItem.setEnabled(true);
            resetButton.setEnabled(true);
            finishButton.setEnabled(true);
            saveItem.setEnabled(false);
        } else if (state == SELECTED) {
            cancelButton.setEnabled(false);
            undoButton.setEnabled(true);
            undoItem.setEnabled(true);
            resetButton.setEnabled(true);
            finishButton.setEnabled(false);
            saveItem.setEnabled(true);
        } else if (state == PROCESSING) {
            cancelButton.setEnabled(true);
            undoButton.setEnabled(false);
            undoItem.setEnabled(false);
            resetButton.setEnabled(true);
            finishButton.setEnabled(false);
            saveItem.setEnabled(false);
        }
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());

        // New in A6: Listen for "progress" events
        model.addPropertyChangeListener("progress", this);
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));


        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            BufferedImage img = null;
            try {
                img = ImageIO.read(file);
                if (img == null) {
                    throw new IOException();
                }
                this.setImage(img);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not read image at " + file,
                        "Unsupported image format",
                        JOptionPane.ERROR_MESSAGE);
                openImage();
            }
        }
    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String name = file.getName();

            if (!name.endsWith(".png")) {
                file = new File(name + ".png");
            }

            if (file.isFile()) {
                int dialogButton = JOptionPane.YES_NO_CANCEL_OPTION;
                int dialogResult = JOptionPane.showConfirmDialog(imgPanel, "Overwrite existing file?",
                        "File with same name already exists", dialogButton);
                if (dialogResult == 0) {
                    try {
                        model.saveSelection(new FileOutputStream(file));
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null,
                                "Could not read image at " + file,
                                "Unsupported image format",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else if (dialogResult == 2){
                    saveSelection();
                }
            } else {
                try {
                    model.saveSelection(new FileOutputStream(file));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                            "Could not read image at " + file,
                            "Unsupported image format",
                            JOptionPane.ERROR_MESSAGE);
                }
            }


        }
    }

    /**
     * Analyzes the path between two points in an image using intelligent scissors algorithm
     * @param imagePath Path to the image file
     * @param start Starting point
     * @param end End point
     * @return The PolyLine representing the path between the points
     */
    public static PolyLine analyzePath(String imagePath, Point start, Point end) throws IOException {
        // Load the image
        BufferedImage img = ImageIO.read(new File(imagePath));
        if (img == null) {
            throw new IOException("Could not load image: " + imagePath);
        }
        float C = 250.0f;
        float FACTOR = (259 * (C + 255)) / (255 * (259 - C));

        // Increase contrast
        BufferedImage contrastedImg = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
            Color color = new Color(img.getRGB(x, y));
            // Increase contrast by expanding color range around middle gray with a factor of FACTOR
            int r = Math.min(255, Math.max(0, (int)(((color.getRed() - 128) * FACTOR) + 128)));
            int g = Math.min(255, Math.max(0, (int)(((color.getGreen() - 128) * FACTOR) + 128)));
            int b = Math.min(255, Math.max(0, (int)(((color.getBlue() - 128) * FACTOR) + 128)));
            contrastedImg.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        // Create a selection model using the color-aware weighing function
        ScissorsSelectionModel model = new ScissorsSelectionModel("CrossGradMulti", true);
        model.setImage(contrastedImg);

        // Add the start point
        model.addPoint(start);

        // Wait for processing to complete (model will be in PROCESSING state initially)
        while (model.state() == SelectionState.PROCESSING) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Get the path to the end point
        PolyLine path = model.liveWire(end);
        
        return path;
    }


    public static void main(String[] args) {
        // Can still keep the GUI version in an if block
        if (args.length == 0) {
            SwingUtilities.invokeLater(() -> {
                // Original GUI code
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                } catch (Exception ignored) {}
                SelectorApp app = new SelectorApp();
                app.start();
            });
        } else {
            // Programmatic usage example
            try {
                if (args[0].equals("test")) {
                    String csvPath = "HAM10000_metadata.csv";
                    String imageDirPath = "HAM10000_images/HAM10000_images/";
                    
                    // Read all lines into a list
                    java.util.List<String> allLines = new java.util.ArrayList<>();
                    try (BufferedReader initialReader = new BufferedReader(new FileReader(csvPath))) {
                        initialReader.readLine(); // Skip header
                        String line;
                        while ((line = initialReader.readLine()) != null) {
                            allLines.add(line);
                        }
                    }
                    
                    // Shuffle the lines
                    java.util.Collections.shuffle(allLines);
                    
                    int truePositives = 0;
                    int trueNegatives = 0;
                    int falsePositives = 0; 
                    int falseNegatives = 0;
                    
                    int iterations = 0;
                    int melCount = 0;
                    for (String currentLine : allLines) {
                        String[] parts = currentLine.split(",");
                        String diagnosis = parts[2];
                        if (diagnosis.equals("mel")) melCount++;
                        if (iterations >= 10 && melCount >= 50) break;
                        String imageId = parts[1];
                        
                        // Only process if diagnosis is "nv" or "mel"
                        if (diagnosis.equals("nv") || diagnosis.equals("mel")) {
                            iterations++;
                            String imagePath = imageDirPath + imageId + ".jpg";
                            
                            try {
                                BufferedImage img = ImageIO.read(new File(imagePath));
                                int width = img.getWidth();
                                int height = img.getHeight();
                                Point start = new Point(width*1/2, height/2);
                                Point end = new Point(width*2/3, height/2);

                                PolyLine path = analyzePath(imagePath, start, end);

                                // Calculate ratio
                                int minY = Integer.MAX_VALUE;
                                int maxY = Integer.MIN_VALUE;
                                for (int y : path.ys()) {
                                    minY = Math.min(minY, y);
                                    maxY = Math.max(maxY, y);
                                }
                                double ratio = (maxY - minY) / (double)height;
                                
                                // Make prediction - using a lower threshold to maximize sensitivity
                                String prediction = ratio < 0.003 | ratio > 0.05 ? "nv" : "mel";
                                
                                // Update confusion matrix
                                if (diagnosis.equals("mel") && prediction.equals("mel")) {
                                    truePositives++;
                                } else if (diagnosis.equals("nv") && prediction.equals("nv")) {
                                    trueNegatives++;
                                } else if (diagnosis.equals("nv") && prediction.equals("mel")) {
                                    falsePositives++;
                                } else if (diagnosis.equals("mel") && prediction.equals("nv")) {
                                    falseNegatives++;
                                }
                                
                                // Print to console
                                System.out.printf("%s: Actual=%s, Predicted=%s, Ratio=%.3f%n", 
                                    imageId, diagnosis, prediction, ratio);
                                    
                                // Save ratio to appropriate file based on diagnosis
                                String filename = diagnosis + "_ratios.txt";
                                try (FileWriter fw = new FileWriter(filename, true)) {
                                    fw.write(String.format("%s,%.3f%n", imageId, ratio));
                                } catch (IOException e) {
                                    System.err.println("Error writing to " + filename + ": " + e.getMessage());
                                }
                                
                            } catch (IOException e) {
                                System.err.println("Error processing " + imagePath + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    // Calculate metrics
                    int total = truePositives + trueNegatives + falsePositives + falseNegatives;
                    double accuracy = (double)(truePositives + trueNegatives) / total;
                    double sensitivity = (double)truePositives / (truePositives + falseNegatives);
                    double specificity = (double)trueNegatives / (trueNegatives + falsePositives);
                    
                    System.out.printf("%nMetrics:%n");
                    System.out.printf("Accuracy: %.2f%% (%d/%d correct)%n", 
                        accuracy * 100, (truePositives + trueNegatives), total);
                    System.out.printf("Sensitivity: %.2f%%%n", sensitivity * 100);
                    System.out.printf("Specificity: %.2f%%%n", specificity * 100);
                    
                } else {
                    String imagePath = args[0];
                    BufferedImage img = ImageIO.read(new File(imagePath));
                    int width = img.getWidth();
                    int height = img.getHeight();
                    Point start = new Point(width/4, height/2);
                    Point end = new Point(width*3/4, height/2);
                    
                    PolyLine path = analyzePath(imagePath, start, end);
                    
                    int minY = Integer.MAX_VALUE;
                    int maxY = Integer.MIN_VALUE;
                    for (int y : path.ys()) {
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                    double heightRatio = (maxY - minY) / (double)height;
                    System.out.printf("Height ratio: %.3f%n", heightRatio);
                }
            } catch (IOException e) {
                System.err.println("Error processing image: " + e.getMessage());
            }
        }
    }
}
