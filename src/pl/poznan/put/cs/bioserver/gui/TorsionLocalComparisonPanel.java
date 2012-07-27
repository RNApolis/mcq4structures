package pl.poznan.put.cs.bioserver.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Structure;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import pl.poznan.put.cs.bioserver.comparison.IncomparableStructuresException;
import pl.poznan.put.cs.bioserver.comparison.TorsionLocalComparison;

/**
 * A panel which is a graphical interface to a local comparison measure based on
 * torsion angle representation.
 * 
 * @author Tomasz Żok (tzok[at]cs.put.poznan.pl)
 */
public class TorsionLocalComparisonPanel extends JPanel {
    /**
     * Subpanel containing file list and another panel with all options.
     */
    private class ControlPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        // /////////////////////////////////////////////////////////////////////
        // fields
        InstructionsPanel instructionsPanel;
        JList<String> list;
        DefaultListModel<String> listModel;
        OptionsPanel optionsPanel;

        // /////////////////////////////////////////////////////////////////////
        // constructors
        public ControlPanel() {
            super(new BorderLayout());

            listModel = new DefaultListModel<>();
            list = new JList<>(listModel);
            optionsPanel = new OptionsPanel();
            instructionsPanel = new InstructionsPanel();

            add(list, BorderLayout.EAST);
            add(optionsPanel, BorderLayout.CENTER);
            add(instructionsPanel, BorderLayout.SOUTH);

            list.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE)
                        listModel.remove(list.getSelectedIndex());
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    // do nothing
                }

                @Override
                public void keyTyped(KeyEvent e) {
                    // do nothing
                }
            });

            final JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "PDB file format", "pdb", "pdb1", "ent", "brk", "gz"));

            optionsPanel.addFile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    /*
                     * when user clicks on "Add file", then check if new file
                     * can be added first
                     */
                    if (listModel.size() == 2) {
                        JOptionPane.showMessageDialog(null,
                                "Only two structures are allowed for"
                                        + " local comparison measures",
                                "Maximum number of structures reached",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    /*
                     * when user chooses a file, then try to parse it
                     */
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        try {
                            String path = file.getCanonicalPath();
                            if (pdbManager.addStructure(path))
                                listModel.addElement(path);
                            else
                                JOptionPane.showMessageDialog(null,
                                        "Specified file is not a "
                                                + "valid PDB file",
                                        "Invalid PDB file",
                                        JOptionPane.ERROR_MESSAGE);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null,
                                    "Failed to add file " + file.toString(),
                                    "Problem with file access",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        /*
                         * if that was the first file added, then update the
                         * list of chains
                         */
                        loadChainsNames(listModel.size() - 1);
                        if (listModel.size() == 2)
                            instructionsPanel
                                    .setInstruction(InstructionsPanel.INSTRUCTION_SELECT_CHAIN);
                    }
                }

                private void loadChainsNames(int index) {
                    DefaultComboBoxModel<String> model = null;
                    JComboBox<String> comboBox = null;
                    if (index == 0) {
                        model = optionsPanel.comboBoxModelFirst;
                        comboBox = optionsPanel.comboBoxFirst;
                    } else {
                        model = optionsPanel.comboBoxModelSecond;
                        comboBox = optionsPanel.comboBoxSecond;
                    }

                    Vector<String> vector = new Vector<>();
                    vector.add(listModel.getElementAt(index));
                    Structure[] structures = pdbManager.getStructures(vector);
                    model.removeAllElements();
                    for (Chain c : structures[0].getChains())
                        model.addElement(c.getChainID());
                    comboBox.setSelectedIndex(0);
                }
            });
        }
    }

    /**
     * Subpanel containing instructions for the user.
     */
    private class InstructionsPanel extends JPanel {
        public static final int INSTRUCTION_ADD_FILE = 0;
        public static final int INSTRUCTION_SELECT_CHAIN = 1;
        // /////////////////////////////////////////////////////////////////////
        // fields
        private static final long serialVersionUID = 1L;
        private final String[] instructions = {
                "Click \"Add file\" to select exactly two structures to compare",
                "<html><ol><li>From the dropdown combo box select chain to compare</li>"
                        + "<li>Select comparison mode: \"Amino acids\" or "
                        + "\"Nucleotides\"</li>"
                        + "<li>Select group names to compare and plot.<br>"
                        + "MCQ stands for Mean of Circular Quantities - an average "
                        + "value for each group</li></html>" };
        private final JLabel instructionsLabel;

        // /////////////////////////////////////////////////////////////////////
        // constructors
        public InstructionsPanel() {
            super();
            instructionsLabel = new JLabel(
                    instructions[InstructionsPanel.INSTRUCTION_ADD_FILE]);
            instructionsLabel.setFont(new Font(Font.DIALOG, Font.BOLD
                    | Font.ITALIC, 12));
            add(instructionsLabel);
        }

        // /////////////////////////////////////////////////////////////////
        // methods
        /**
         * Sets text containing instructions for user to take.
         * 
         * @param index
         *            Index of instruction in the set.
         */
        public void setInstruction(int index) {
            instructionsLabel.setText(instructions[index]);
        }
    }

    /**
     * Subpanel containing all buttons and boxes concerning local comparison
     * measure options.
     */
    private class OptionsPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        JButton addFile;
        JCheckBox[][] angleChoiceChecks;
        // /////////////////////////////////////////////////////////////////////
        // fields
        JComboBox<String> comboBoxFirst, comboBoxSecond;
        DefaultComboBoxModel<String> comboBoxModelFirst, comboBoxModelSecond;
        JButton compare;
        JRadioButton[] groupChoiceRadios;

        // /////////////////////////////////////////////////////////////////////
        // constructors
        public OptionsPanel() {
            setLayout(new GridBagLayout());
            addFile = new JButton("Add file");
            compare = new JButton("Compare");

            comboBoxModelFirst = new DefaultComboBoxModel<>();
            comboBoxModelSecond = new DefaultComboBoxModel<>();
            comboBoxFirst = new JComboBox<>(comboBoxModelFirst);
            comboBoxSecond = new JComboBox<>(comboBoxModelSecond);

            groupChoiceRadios = new JRadioButton[] {
                    new JRadioButton("Amino acids", true),
                    new JRadioButton("Nucleotides", false) };
            ButtonGroup group = new ButtonGroup();
            group.add(groupChoiceRadios[0]);
            group.add(groupChoiceRadios[1]);

            angleChoiceChecks = new JCheckBox[2][];
            angleChoiceChecks[0] = new JCheckBox[] { new JCheckBox("phi"),
                    new JCheckBox("psi"), new JCheckBox("omega"),
                    new JCheckBox("MCQ") };
            angleChoiceChecks[1] = new JCheckBox[] { new JCheckBox("alpha"),
                    new JCheckBox("beta"), new JCheckBox("gamma"),
                    new JCheckBox("delta"), new JCheckBox("zeta"),
                    new JCheckBox("epsilon"), new JCheckBox("chi"),
                    new JCheckBox("P"), new JCheckBox("MCQ") };
            for (JCheckBox b : angleChoiceChecks[1])
                b.setEnabled(false);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.fill = GridBagConstraints.BOTH;
            add(addFile, c);

            c.gridx = 4;
            add(compare, c);

            c.gridx = 2;
            c.gridy = 1;
            c.gridwidth = 4;
            add(comboBoxFirst, c);

            c.gridy = 2;
            add(comboBoxSecond, c);

            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 1;
            for (JRadioButton b : groupChoiceRadios) {
                add(b, c);
                c.gridy++;
            }

            c.gridx = 1;
            c.gridy = 3;
            for (JCheckBox b : angleChoiceChecks[0]) {
                add(b, c);
                c.gridx++;
            }

            c.gridx = 1;
            c.gridy = 4;
            for (JCheckBox b : angleChoiceChecks[1]) {
                add(b, c);
                c.gridx++;
            }

            ActionListener radioActionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < 2; ++i)
                        if (groupChoiceRadios[i].isSelected()) {
                            for (JCheckBox b : angleChoiceChecks[i])
                                b.setEnabled(true);
                            for (JCheckBox b : angleChoiceChecks[i ^ 1])
                                b.setEnabled(false);
                            break;
                        }
                }
            };
            groupChoiceRadios[0].addActionListener(radioActionListener);
            groupChoiceRadios[1].addActionListener(radioActionListener);
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // fields
    private static final long serialVersionUID = 1L;
    private JPanel chartPanel;
    ControlPanel controlPanel;
    PdbManager pdbManager;

    // /////////////////////////////////////////////////////////////////////////
    // constructors
    public TorsionLocalComparisonPanel(final PdbManager manager) {
        super(new BorderLayout());
        pdbManager = manager;
        chartPanel = new JPanel();

        controlPanel = new ControlPanel();
        add(controlPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        controlPanel.optionsPanel.compare
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        /*
                         * check structure count
                         */
                        if (controlPanel.listModel.size() != 2) {
                            JOptionPane.showMessageDialog(null,
                                    "You need exactly two structures"
                                            + " to compare them locally",
                                    "Incorrect number of structures "
                                            + "to compare",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        /*
                         * get specified structures
                         */
                        String[] names = new String[2];
                        int[] indices = new int[2];
                        names[0] = controlPanel.listModel.get(0);
                        indices[0] = controlPanel.optionsPanel.comboBoxFirst
                                .getSelectedIndex();
                        names[1] = controlPanel.listModel.get(1);
                        indices[1] = controlPanel.optionsPanel.comboBoxSecond
                                .getSelectedIndex();

                        Structure[] structures = manager.getStructures(Arrays
                                .asList(names));
                        Chain[] chains = new Chain[2];
                        for (int i = 0; i < 2; ++i)
                            chains[i] = structures[i].getChain(indices[i]);
                        /*
                         * compare them
                         */
                        double[][][] compare = null;
                        try {
                            compare = new TorsionLocalComparison().compare(
                                    chains[0], chains[1]);
                        } catch (IncomparableStructuresException e) {
                            JOptionPane.showMessageDialog(null, e.getMessage(),
                                    "Error during structure comparison",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        /*
                         * read options from GUI
                         */
                        int type = controlPanel.optionsPanel.groupChoiceRadios[0]
                                .isSelected() ? 0 : 1;
                        /*
                         * read angles that have to be plotted
                         */
                        Vector<Integer> anglesToShow = new Vector<>();
                        int i = 0;
                        for (JCheckBox b : controlPanel.optionsPanel.angleChoiceChecks[type]) {
                            if (b.isSelected())
                                anglesToShow.add(i);
                            i++;
                        }
                        /*
                         * prepare dataset with points
                         */
                        DefaultXYDataset dataset = new DefaultXYDataset();
                        for (int angle : anglesToShow) {
                            double[] x = new double[compare[type].length];
                            double[] y = new double[compare[type].length];
                            for (i = 0; i < compare[type].length; ++i) {
                                x[i] = i + 1;
                                y[i] = compare[type][i][angle];
                            }
                            dataset.addSeries(
                                    controlPanel.optionsPanel.angleChoiceChecks[type][angle]
                                            .getText(), new double[][] { x, y });
                        }
                        /*
                         * draw a plot and replace the previous one
                         */
                        TickUnitSource tickUnitSource = NumberAxis
                                .createIntegerTickUnits();
                        NumberTickUnit tickUnit = (NumberTickUnit) tickUnitSource
                                .getCeilingTickUnit(5);
                        NumberAxis xAxis = new NumberAxis();
                        xAxis.setLabel("Group index");
                        xAxis.setTickUnit(tickUnit);

                        NumberAxis yAxis = new NumberAxis();
                        yAxis.setAutoRange(false);
                        yAxis.setRange(0, Math.PI);
                        yAxis.setLabel("Distance [rad]");

                        XYPlot plot = new XYPlot(dataset, xAxis, yAxis,
                                new DefaultXYItemRenderer());
                        remove(1);
                        add(new ChartPanel(new JFreeChart(plot)));
                        SwingUtilities
                                .updateComponentTreeUI(TorsionLocalComparisonPanel.this);
                    }
                });
    }
}
