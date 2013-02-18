package pl.poznan.put.cs.bioserver.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.lang3.StringUtils;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.ResidueNumber;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureImpl;
import org.biojava.bio.structure.align.gui.jmol.JmolPanel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jmol.api.JmolViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.poznan.put.cs.bioserver.alignment.AlignmentOutput;
import pl.poznan.put.cs.bioserver.alignment.OutputAlignSeq;
import pl.poznan.put.cs.bioserver.alignment.SequenceAligner;
import pl.poznan.put.cs.bioserver.alignment.StructureAligner;
import pl.poznan.put.cs.bioserver.comparison.ComparisonListener;
import pl.poznan.put.cs.bioserver.comparison.GlobalComparison;
import pl.poznan.put.cs.bioserver.comparison.MCQ;
import pl.poznan.put.cs.bioserver.comparison.RMSD;
import pl.poznan.put.cs.bioserver.comparison.TorsionLocalComparison;
import pl.poznan.put.cs.bioserver.helper.Helper;
import pl.poznan.put.cs.bioserver.helper.PdbManager;
import pl.poznan.put.cs.bioserver.torsion.AngleDifference;
import pl.poznan.put.cs.bioserver.visualisation.MDS;
import pl.poznan.put.cs.bioserver.visualisation.MDSPlot;

import com.csvreader.CsvWriter;

class MainWindow extends JFrame {
    private static final String TITLE = "MCQ4Structures: computing similarity of 3D RNA / protein structures";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MainWindow.class);

    private static final char CSV_DELIMITER = ';';

    private static final String CARD_GLOBAL = "CARD_GLOBAL";
    private static final String CARD_LOCAL = "CARD_LOCAL";
    private static final String CARD_ALIGN_SEQ = "CARD_ALIGN_SEQ";
    private static final String CARD_ALIGN_STRUC = "CARD_ALIGN_STRUC";

    private static Component getCurrentCard(JPanel panel) {
        for (Component component : panel.getComponents()) {
            if (component.isVisible()) {
                return component;
            }
        }
        return null;
    }

    private JFileChooser chooserSaveFile;
    private PdbManagerDialog managerDialog;
    private StructureSelectionDialog structureDialog;
    private ChainSelectionDialog chainDialog;
    private TorsionAnglesSelectionDialog torsionDialog;

    private String[] resultGlobalNames;
    private double[][] resultGlobalMatrix;
    private Map<String, List<AngleDifference>> resultLocal;
    private String resultAlignStruc;
    private String resultAlignSeq;

    public MainWindow() {
        super();

        chooserSaveFile = new JFileChooser();
        managerDialog = PdbManagerDialog.getInstance(this);
        managerDialog.setVisible(true);
        structureDialog = StructureSelectionDialog.getInstance(this);
        chainDialog = ChainSelectionDialog.getInstance(this);
        torsionDialog = TorsionAnglesSelectionDialog.getInstance(this);

        /*
         * Create menu
         */
        JMenuItem itemOpen = new JMenuItem("Open structure(s)",
                loadIcon("/toolbarButtonGraphics/general/Open16.gif"));
        final JMenuItem itemSave = new JMenuItem("Save results",
                loadIcon("/toolbarButtonGraphics/general/Save16.gif"));
        itemSave.setEnabled(false);
        final JCheckBoxMenuItem checkBoxManager = new JCheckBoxMenuItem(
                "View structure manager", true);
        JMenuItem itemExit = new JMenuItem("Exit");
        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuFile.add(itemOpen);
        menuFile.add(itemSave);
        menuFile.addSeparator();
        menuFile.add(checkBoxManager);
        menuFile.addSeparator();
        menuFile.add(itemExit);

        final JRadioButtonMenuItem radioMcq = new JRadioButtonMenuItem("MCQ",
                true);
        radioMcq.setEnabled(false);
        final JRadioButtonMenuItem radioRmsd = new JRadioButtonMenuItem("RMSD",
                false);
        radioRmsd.setEnabled(false);
        ButtonGroup group = new ButtonGroup();
        group.add(radioMcq);
        group.add(radioRmsd);

        final JMenuItem itemSelectStructures = new JMenuItem(
                "Select structures to compare");
        final JMenuItem itemComputeGlobal = new JMenuItem(
                "Compute distance matrix");
        itemComputeGlobal.setEnabled(false);
        final JMenuItem itemVisualise = new JMenuItem("Visualise results");
        itemVisualise.setEnabled(false);
        final JMenuItem itemCluster = new JMenuItem("Cluster results");
        itemCluster.setEnabled(false);
        JMenu menuGlobal = new JMenu("Global distance");
        menuGlobal.add(itemSelectStructures);
        menuGlobal.addSeparator();
        menuGlobal.add(new JLabel("    Distance measure:"));
        menuGlobal.add(radioMcq);
        menuGlobal.add(radioRmsd);
        menuGlobal.addSeparator();
        menuGlobal.add(itemComputeGlobal);
        menuGlobal.add(itemVisualise);
        menuGlobal.add(itemVisualise);
        menuGlobal.add(itemCluster);

        final JMenuItem itemSelectChainsCompare = new JMenuItem(
                "Select chains to compare");
        final JMenuItem itemSelectTorsion = new JMenuItem(
                "Select torsion angles");
        itemSelectTorsion.setEnabled(false);
        final JMenuItem itemComputeLocal = new JMenuItem(
                "Compute distance vector");
        itemComputeLocal.setEnabled(false);
        JMenu menuLocal = new JMenu("Local distance");
        menuLocal.add(itemSelectChainsCompare);
        menuLocal.add(itemSelectTorsion);
        menuLocal.add(itemComputeLocal);

        JMenu menuDistance = new JMenu("Distance computation");
        menuDistance.setMnemonic(KeyEvent.VK_D);
        menuDistance.add(menuGlobal);
        menuDistance.addSeparator();
        menuDistance.add(menuLocal);

        final JRadioButtonMenuItem radioAlignGlobal = new JRadioButtonMenuItem(
                "Global", true);
        radioAlignGlobal.setEnabled(false);
        final JRadioButtonMenuItem radioAlignLocal = new JRadioButtonMenuItem(
                "Local", false);
        radioAlignLocal.setEnabled(false);
        ButtonGroup groupAlign = new ButtonGroup();
        groupAlign.add(radioAlignGlobal);
        groupAlign.add(radioAlignLocal);

        final JMenuItem itemSelectChainsAlignSeq = new JMenuItem(
                "Select chains to align");
        final JMenuItem itemComputeAlignSeq = new JMenuItem("Compute alignment");
        itemComputeAlignSeq.setEnabled(false);
        JMenu menuAlignSeq = new JMenu("Sequence alignment");
        menuAlignSeq.add(itemSelectChainsAlignSeq);
        menuAlignSeq.addSeparator();
        menuAlignSeq.add(new JLabel("    Alignment type:"));
        menuAlignSeq.add(radioAlignGlobal);
        menuAlignSeq.add(radioAlignLocal);
        menuAlignSeq.addSeparator();
        menuAlignSeq.add(itemComputeAlignSeq);

        final JMenuItem itemSelectChainsAlignStruc = new JMenuItem(
                "Select chains to align");
        final JMenuItem itemComputeAlignStruc = new JMenuItem(
                "Compute alignment");
        itemComputeAlignStruc.setEnabled(false);
        JMenu menuAlignStruc = new JMenu("3D structure alignment");
        menuAlignStruc.add(itemSelectChainsAlignStruc);
        menuAlignStruc.add(itemComputeAlignStruc);

        JMenu menuAlignment = new JMenu("Alignment");
        menuAlignment.setMnemonic(KeyEvent.VK_A);
        menuAlignment.add(menuAlignSeq);
        menuAlignment.addSeparator();
        menuAlignment.add(menuAlignStruc);

        JMenuItem itemGuide = new JMenuItem("Quick guide");
        itemGuide.setMnemonic(KeyEvent.VK_Q);
        itemGuide.setMaximumSize(new Dimension(
                itemGuide.getPreferredSize().width,
                itemGuide.getMaximumSize().height));

        JMenuItem itemAbout = new JMenuItem("About");
        itemAbout.setMnemonic(KeyEvent.VK_O);
        itemAbout.setMaximumSize(new Dimension(
                itemAbout.getPreferredSize().width,
                itemAbout.getMaximumSize().height));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuFile);
        menuBar.add(menuDistance);
        menuBar.add(menuAlignment);
        menuBar.add(itemGuide);
        menuBar.add(itemAbout);

        setJMenuBar(menuBar);

        /*
         * Create panel with global comparison results
         */
        JPanel panel;
        final JLabel labelInfoGlobal = new JLabel(
                "Global comparison results: distance matrix");
        final JTable tableMatrix = new JTable();
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        final JPanel panelResultsGlobal = new JPanel(new BorderLayout());
        panel = new JPanel();
        panel.add(labelInfoGlobal);
        panelResultsGlobal.add(panel, BorderLayout.NORTH);
        panelResultsGlobal.add(new JScrollPane(tableMatrix),
                BorderLayout.CENTER);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Progress in computing:"));
        panel.add(progressBar);

        /*
         * Create panel with local comparison results
         */
        final JLabel labelInfoLocal = new JLabel(
                "Local comparison results: distance plot");
        final JPanel panelLocalPlot = new JPanel(new GridLayout(1, 1));

        final JPanel panelResultsLocal = new JPanel(new BorderLayout());
        panel = new JPanel();
        panel.add(labelInfoLocal);
        panelResultsLocal.add(panel, BorderLayout.NORTH);
        panelResultsLocal.add(panelLocalPlot, BorderLayout.CENTER);

        /*
         * Create panel with sequence alignment
         */
        final JLabel labelInfoAlignSeq = new JLabel(
                "Sequence alignment results");
        final JTextArea textAreaAlignSeq = new JTextArea();
        textAreaAlignSeq.setEditable(false);
        textAreaAlignSeq.setFont(new Font("Monospaced", Font.PLAIN, 20));

        final JPanel panelResultsAlignSeq = new JPanel(new BorderLayout());
        panel = new JPanel();
        panel.add(labelInfoAlignSeq);
        panelResultsAlignSeq.add(panel, BorderLayout.NORTH);
        panelResultsAlignSeq.add(new JScrollPane(textAreaAlignSeq),
                BorderLayout.CENTER);

        /*
         * Create panel with structure alignment
         */
        JPanel panelAlignStrucInfo = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        panelAlignStrucInfo.add(new JLabel("Whole structures (Jmol view)"),
                constraints);
        constraints.gridx++;
        constraints.weightx = 0;
        final JLabel labelInfoAlignStruc = new JLabel(
                "3D structure alignment results");
        panelAlignStrucInfo.add(labelInfoAlignStruc, constraints);
        constraints.gridx++;
        constraints.weightx = 0.5;
        panelAlignStrucInfo.add(new JLabel("Aligned fragments (Jmol view)"),
                constraints);

        final JmolPanel panelJmolLeft = new JmolPanel();
        panelJmolLeft.executeCmd("background lightgrey; save state state_init");
        final JmolPanel panelJmolRight = new JmolPanel();
        panelJmolRight.executeCmd("background darkgray; save state state_init");

        final JPanel panelResultsAlignStruc = new JPanel(new BorderLayout());
        panelResultsAlignStruc.add(panelAlignStrucInfo, BorderLayout.NORTH);
        panel = new JPanel(new GridLayout(1, 2));
        panel.add(panelJmolLeft);
        panel.add(panelJmolRight);
        panelResultsAlignStruc.add(panel, BorderLayout.CENTER);

        /*
         * Create card layout
         */
        final CardLayout layoutCards = new CardLayout();
        final JPanel panelCards = new JPanel();
        panelCards.setLayout(layoutCards);
        panelCards.add(new JPanel());
        panelCards.add(panelResultsGlobal, MainWindow.CARD_GLOBAL);
        panelCards.add(panelResultsLocal, MainWindow.CARD_LOCAL);
        panelCards.add(panelResultsAlignSeq, MainWindow.CARD_ALIGN_SEQ);
        panelCards.add(panelResultsAlignStruc, MainWindow.CARD_ALIGN_STRUC);

        setLayout(new BorderLayout());
        add(panelCards, BorderLayout.CENTER);

        /*
         * Set window properties
         */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(MainWindow.TITLE);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension size = toolkit.getScreenSize();
        setSize(size.width * 3 / 4, size.height * 3 / 4);
        setLocation(size.width / 8, size.height / 8);

        /*
         * Set action listeners
         */
        managerDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                checkBoxManager.setSelected(false);
            }
        });

        itemOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File[] files = PdbFileChooser.getSelectedFiles(MainWindow.this);
                for (File f : files) {
                    if (PdbManager.loadStructure(f) != null) {
                        PdbManagerDialog.MODEL.addElement(f);
                    }
                }
            }
        });

        itemSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component current = MainWindow.getCurrentCard(panelCards);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-DD-HH-mm");
                Date now = new Date();

                File proposedName = null;
                if (current.equals(panelResultsGlobal)) {
                    proposedName = new File(sdf.format(now) + "-global.csv");
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append(PdbManager
                            .getStructureName(chainDialog.selectedStructures[0]));
                    builder.append('-');
                    builder.append(PdbManager
                            .getStructureName(chainDialog.selectedStructures[1]));

                    if (current.equals(panelResultsLocal)) {
                        proposedName = new File(sdf.format(now) + "-local-"
                                + builder.toString() + ".csv");
                    } else if (current.equals(panelResultsAlignSeq)) {
                        proposedName = new File(sdf.format(now) + "-alignseq-"
                                + builder.toString() + ".txt");
                    } else { // current.equals(panelResultsAlignStruc)
                        proposedName = new File(sdf.format(now)
                                + "-alignstruc-" + builder.toString() + ".pdb");
                    }
                }
                chooserSaveFile.setSelectedFile(proposedName);

                int chosenOption = chooserSaveFile
                        .showSaveDialog(MainWindow.this);
                if (chosenOption != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                if (current.equals(panelResultsGlobal)) {
                    saveResultsGlobalComparison(chooserSaveFile
                            .getSelectedFile());
                } else if (current.equals(panelResultsLocal)) {
                    saveResultsLocalComparison(chooserSaveFile
                            .getSelectedFile());
                } else if (current.equals(panelResultsAlignSeq)) {
                    try (FileOutputStream stream = new FileOutputStream(
                            chooserSaveFile.getSelectedFile())) {
                        stream.write(resultAlignSeq.getBytes("UTF-8"));
                    } catch (IOException e1) {
                        MainWindow.LOGGER.error(
                                "Failed to save aligned sequences", e1);
                        JOptionPane.showMessageDialog(MainWindow.this,
                                e1.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else { // current.equals(panelResultsAlignStruc)
                    try (FileOutputStream stream = new FileOutputStream(
                            chooserSaveFile.getSelectedFile())) {
                        stream.write(resultAlignStruc.getBytes("UTF-8"));
                    } catch (IOException e1) {
                        MainWindow.LOGGER.error(
                                "Failed to save PDB of aligned structures", e1);
                        JOptionPane.showMessageDialog(MainWindow.this,
                                e1.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        checkBoxManager.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                managerDialog.setVisible(checkBoxManager.isSelected());
            }
        });

        itemExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(MainWindow.this,
                        WindowEvent.WINDOW_CLOSING));
            }
        });

        itemSelectStructures.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /*
                 * Add new structures to the "all" section of structures
                 * selection dialog
                 */
                Enumeration<File> elements = PdbManagerDialog.MODEL.elements();
                while (elements.hasMoreElements()) {
                    File path = elements.nextElement();
                    if (!structureDialog.modelAll.contains(path)
                            && !structureDialog.modelSelected.contains(path)) {
                        structureDialog.modelAll.addElement(path);
                    }
                }
                /*
                 * Remove from "all" section these structures, that were removed
                 * in PDB manager dialog
                 */
                elements = structureDialog.modelAll.elements();
                while (elements.hasMoreElements()) {
                    File path = elements.nextElement();
                    if (PdbManager.getStructure(path) == null) {
                        structureDialog.modelAll.removeElement(path);
                    }
                }
                /*
                 * Remove from "selected" section these structures, that were
                 * removed in PDB manager dialog
                 */
                elements = structureDialog.modelSelected.elements();
                while (elements.hasMoreElements()) {
                    File path = elements.nextElement();
                    if (PdbManager.getStructure(path) == null) {
                        structureDialog.modelSelected.removeElement(path);
                    }
                }
                /*
                 * Show dialog
                 */
                structureDialog.setVisible(true);
                if (structureDialog.chosenOption == StructureSelectionDialog.OK
                        && structureDialog.selectedStructures != null) {
                    if (structureDialog.selectedStructures.size() < 2) {
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "You need to select at least two structures",
                                "Information", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    tableMatrix.setModel(new MatrixTableModel(new String[0],
                            new double[0][]));
                    layoutCards.show(panelCards, CARD_GLOBAL);

                    itemSave.setEnabled(false);
                    radioMcq.setEnabled(true);
                    radioRmsd.setEnabled(true);
                    itemComputeGlobal.setEnabled(true);
                }
            }
        });

        itemComputeGlobal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final GlobalComparison comparison;
                if (radioMcq.isSelected()) {
                    comparison = new MCQ();
                } else { // radioRmsd.isSelected() == true
                    comparison = new RMSD();
                }

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Structure[] structures = PdbManager
                                .getSelectedStructures(structureDialog.selectedStructures);

                        resultGlobalNames = PdbManager
                                .getSelectedStructuresNames(structureDialog.selectedStructures);
                        resultGlobalMatrix = comparison.compare(structures,
                                new ComparisonListener() {
                                    @Override
                                    public void stateChanged(long all,
                                            long completed) {
                                        progressBar.setMaximum((int) all);
                                        progressBar.setValue((int) completed);
                                    }
                                });

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                MatrixTableModel model = new MatrixTableModel(
                                        resultGlobalNames, resultGlobalMatrix);
                                tableMatrix.setModel(model);

                                itemSave.setEnabled(true);
                                itemSave.setText("Save results (CSV)");
                                itemCluster.setEnabled(true);
                                itemVisualise.setEnabled(true);

                                labelInfoGlobal.setText("Global comparison "
                                        + "results: distance matrix for "
                                        + (radioMcq.isSelected() ? "MCQ"
                                                : "RMSD"));
                            }
                        });
                    }
                });
                thread.start();
            }
        });

        itemVisualise.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MatrixTableModel model = (MatrixTableModel) tableMatrix
                        .getModel();
                String[] names = model.getNames();
                double[][] values = model.getValues();

                for (double[] value : values) {
                    for (double element : value) {
                        if (Double.isNaN(element)) {
                            JOptionPane.showMessageDialog(MainWindow.this,
                                    "Cannot visualize, because some "
                                            + "of the structures were "
                                            + "incomparable", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }

                double[][] mds = MDS.multidimensionalScaling(values, 2);
                if (mds == null) {
                    JOptionPane.showMessageDialog(null,
                            "Cannot visualise specified structures in 2D",
                            "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                MDSPlot plot = new MDSPlot(mds, names);
                plot.setVisible(true);
            }
        });

        itemCluster.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                MatrixTableModel model = (MatrixTableModel) tableMatrix
                        .getModel();
                String[] names = model.getNames();
                double[][] values = model.getValues();

                for (double[] value : values) {
                    for (double element : value) {
                        if (Double.isNaN(element)) {
                            JOptionPane.showMessageDialog(MainWindow.this,
                                    "Cannot cluster, because some "
                                            + "of the structures were "
                                            + "incomparable", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }

                ClusteringDialog dialogClustering = new ClusteringDialog(names,
                        values);
                dialogClustering.setVisible(true);
            }
        });

        ActionListener actionListenerSelectChains = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                chainDialog.modelLeft.removeAllElements();
                chainDialog.modelRight.removeAllElements();
                Enumeration<File> elements = PdbManagerDialog.MODEL.elements();
                while (elements.hasMoreElements()) {
                    File path = elements.nextElement();
                    chainDialog.modelLeft.addElement(path);
                    chainDialog.modelRight.addElement(path);
                }

                chainDialog.setVisible(true);
                if (chainDialog.chosenOption == ChainSelectionDialog.OK
                        && chainDialog.selectedStructures != null
                        && chainDialog.selectedChains != null) {
                    for (int i = 0; i < 2; i++) {
                        if (chainDialog.selectedChains[i].length == 0) {
                            String message = "No chains specified for structure: "
                                    + chainDialog.selectedStructures[i];
                            JOptionPane.showMessageDialog(MainWindow.this,
                                    message, "Information",
                                    JOptionPane.INFORMATION_MESSAGE);
                            chainDialog.selectedStructures = null;
                            chainDialog.selectedChains = null;
                            return;
                        }
                    }

                    Object source = arg0.getSource();
                    if (source.equals(itemSelectChainsCompare)) {
                        panelLocalPlot.removeAll();
                        panelLocalPlot.revalidate();
                        layoutCards.show(panelCards, CARD_LOCAL);

                        itemSelectTorsion.setEnabled(true);
                        radioAlignGlobal.setEnabled(false);
                        radioAlignLocal.setEnabled(false);
                        itemComputeAlignSeq.setEnabled(false);
                        itemComputeAlignStruc.setEnabled(false);
                    } else if (source.equals(itemSelectChainsAlignSeq)) {
                        if (chainDialog.selectedChains[0].length != 1
                                || chainDialog.selectedChains[1].length != 1) {
                            JOptionPane.showMessageDialog(MainWindow.this,
                                    "For sequence alignment, you need "
                                            + "to select a single chain "
                                            + "from each structure",
                                    "Information",
                                    JOptionPane.INFORMATION_MESSAGE);
                            chainDialog.selectedStructures = null;
                            chainDialog.selectedChains = null;
                            return;
                        }

                        textAreaAlignSeq.setText("");
                        layoutCards.show(panelCards, CARD_ALIGN_SEQ);

                        itemSelectTorsion.setEnabled(false);
                        radioAlignGlobal.setEnabled(true);
                        radioAlignLocal.setEnabled(true);
                        itemComputeAlignSeq.setEnabled(true);
                        itemComputeAlignStruc.setEnabled(false);
                    } else { // source.equals(itemSelectChainsAlignStruc)
                        panelJmolLeft.executeCmd("restore state state_init");
                        panelJmolRight.executeCmd("restore state state_init");
                        layoutCards.show(panelCards, CARD_ALIGN_STRUC);

                        itemSelectTorsion.setEnabled(false);
                        radioAlignGlobal.setEnabled(false);
                        radioAlignLocal.setEnabled(false);
                        itemComputeAlignSeq.setEnabled(false);
                        itemComputeAlignStruc.setEnabled(true);
                    }
                }
            }
        };
        itemSelectChainsCompare.addActionListener(actionListenerSelectChains);
        itemSelectChainsAlignSeq.addActionListener(actionListenerSelectChains);
        itemSelectChainsAlignStruc
                .addActionListener(actionListenerSelectChains);

        itemSelectTorsion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                torsionDialog.setVisible(true);
                itemComputeLocal.setEnabled(true);
            }
        });

        itemComputeLocal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                layoutCards.show(panelCards, MainWindow.CARD_LOCAL);

                final Structure[] structures = new Structure[2];
                for (int i = 0; i < 2; i++) {
                    structures[i] = new StructureImpl();
                    // FIXME: NPE after hitting Cancel on chain selection
                    structures[i].setChains(Arrays
                            .asList(chainDialog.selectedChains[i]));
                }

                try {
                    resultLocal = TorsionLocalComparison.compare(structures[0],
                            structures[1], false);
                } catch (StructureException e1) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                            e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Set<ResidueNumber> set = new HashSet<>();
                for (String angle : torsionDialog.selectedNames) {
                    if (!resultLocal.containsKey(angle)) {
                        continue;
                    }
                    for (AngleDifference ad : resultLocal.get(angle)) {
                        set.add(ad.getResidue());
                    }
                }
                List<ResidueNumber> list = new ArrayList<>(set);
                Collections.sort(list);

                DefaultXYDataset dataset = new DefaultXYDataset();
                for (String angle : torsionDialog.selectedNames) {
                    if (!resultLocal.containsKey(angle)) {
                        continue;
                    }
                    List<AngleDifference> diffs = resultLocal.get(angle);
                    Collections.sort(diffs);

                    double[] x = new double[diffs.size()];
                    double[] y = new double[diffs.size()];
                    for (int i = 0; i < diffs.size(); i++) {
                        AngleDifference ad = diffs.get(i);
                        x[i] = list.indexOf(ad.getResidue());
                        y[i] = ad.getDifference();
                    }
                    dataset.addSeries(angle, new double[][] { x, y });
                }

                NumberAxis xAxis = new TorsionAxis(resultLocal);
                xAxis.setLabel("Residue");
                NumberAxis yAxis = new NumberAxis();
                yAxis.setAutoRange(false);
                yAxis.setRange(0, Math.PI);
                yAxis.setLabel("Distance [rad]");
                XYPlot plot = new XYPlot(dataset, xAxis, yAxis,
                        new DefaultXYItemRenderer());

                panelLocalPlot.removeAll();
                panelLocalPlot.add(new ChartPanel(new JFreeChart(plot)));
                panelLocalPlot.revalidate();

                itemSave.setEnabled(true);
                itemSave.setText("Save results (CSV)");

                File[] pdbs = new File[] { chainDialog.selectedStructures[0],
                        chainDialog.selectedStructures[1] };
                String[] names = new String[] {
                        PdbManager.getStructureName(pdbs[0]),
                        PdbManager.getStructureName(pdbs[1]) };
                labelInfoLocal.setText("Local comparison results: distance "
                        + "plot for " + names[0] + " and " + names[1]);
            }
        });

        itemComputeAlignSeq.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                layoutCards.show(panelCards, MainWindow.CARD_ALIGN_SEQ);

                Chain chains[] = new Chain[] {
                        chainDialog.selectedChains[0][0],
                        chainDialog.selectedChains[1][0] };

                boolean isRNA = Helper.isNucleicAcid(chains[0]);
                if (isRNA != Helper.isNucleicAcid(chains[1])) {
                    String message = "Structures meant to be aligned "
                            + "represent different molecule types!";
                    MainWindow.LOGGER.error(message);
                    JOptionPane.showMessageDialog(null, message, "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                OutputAlignSeq alignment = SequenceAligner.align(chains[0],
                        chains[1], radioAlignGlobal.isSelected());
                resultAlignSeq = alignment.toString();
                textAreaAlignSeq.setText(resultAlignSeq);

                itemSave.setEnabled(true);
                itemSave.setText("Save results (TXT)");

                File[] pdbs = new File[] { chainDialog.selectedStructures[0],
                        chainDialog.selectedStructures[1] };
                String[] names = new String[] {
                        PdbManager.getStructureName(pdbs[0]),
                        PdbManager.getStructureName(pdbs[1]) };
                labelInfoAlignSeq.setText("Sequence alignment results for "
                        + names[0] + " and " + names[1]);
            }
        });

        itemComputeAlignStruc.addActionListener(new ActionListener() {
            private Thread thread;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (thread != null && thread.isAlive()) {
                    JOptionPane.showMessageDialog(null,
                            "Alignment calculation underway!", "Information",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                layoutCards.show(panelCards, MainWindow.CARD_ALIGN_STRUC);

                final Structure[] structures = new Structure[2];
                for (int i = 0; i < 2; i++) {
                    structures[i] = new StructureImpl();
                    structures[i].setChains(Arrays
                            .asList(chainDialog.selectedChains[i]));
                }

                boolean isRNA = Helper.isNucleicAcid(structures[0]);
                if (isRNA != Helper.isNucleicAcid(structures[1])) {
                    String message = "Structures meant to be aligned "
                            + "represent different molecule types!";
                    MainWindow.LOGGER.error(message);
                    JOptionPane.showMessageDialog(null, message, "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                labelInfoAlignStruc.setText("Processing");
                final Timer timer = new Timer(250, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        String text = labelInfoAlignStruc.getText();
                        int count = StringUtils.countMatches(text, ".");
                        if (count < 5) {
                            labelInfoAlignStruc.setText(text + ".");
                        } else {
                            labelInfoAlignStruc.setText("Processing");
                        }
                    }
                });
                timer.start();

                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Helper.normalizeAtomNames(structures[0]);
                            Helper.normalizeAtomNames(structures[1]);

                            AlignmentOutput output = StructureAligner.align(
                                    structures[0], structures[1]);
                            final Structure[] aligned = output.getStructures();

                            SwingUtilities.invokeLater(new Runnable() {
                                private static final String JMOL_SCRIPT = "frame 0.0; "
                                        + "cartoon only; "
                                        + "select model=1.1; color green; "
                                        + "select model=1.2; color red; ";

                                @Override
                                public void run() {
                                    StringBuilder builder = new StringBuilder();
                                    builder.append("MODEL        1                                                                  \n");
                                    builder.append(aligned[0].toPDB());
                                    builder.append("ENDMDL                                                                          \n");
                                    builder.append("MODEL        2                                                                  \n");
                                    builder.append(aligned[1].toPDB());
                                    builder.append("ENDMDL                                                                          \n");
                                    resultAlignStruc = builder.toString();

                                    JmolViewer viewer = panelJmolLeft
                                            .getViewer();
                                    viewer.openStringInline(builder.toString());
                                    panelJmolLeft.executeCmd(JMOL_SCRIPT);

                                    builder = new StringBuilder();
                                    builder.append("MODEL        1                                                                  \n");
                                    builder.append(aligned[2].toPDB());
                                    builder.append("ENDMDL                                                                          \n");
                                    builder.append("MODEL        2                                                                  \n");
                                    builder.append(aligned[3].toPDB());
                                    builder.append("ENDMDL                                                                          \n");

                                    viewer = panelJmolRight.getViewer();
                                    viewer.openStringInline(builder.toString());
                                    panelJmolRight.executeCmd(JMOL_SCRIPT);

                                    itemSave.setEnabled(true);
                                    itemSave.setText("Save results (PDB)");
                                }
                            });
                        } catch (StructureException e1) {
                            MainWindow.LOGGER.error(
                                    "Failed to align structures", e1);
                            JOptionPane.showMessageDialog(getParent(),
                                    e1.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        } finally {
                            timer.stop();

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    File[] pdbs = new File[] {
                                            chainDialog.selectedStructures[0],
                                            chainDialog.selectedStructures[1] };
                                    String[] names = new String[] {
                                            PdbManager
                                                    .getStructureName(pdbs[0]),
                                            PdbManager
                                                    .getStructureName(pdbs[1]) };
                                    labelInfoAlignStruc.setText("3D structure "
                                            + "alignments results for "
                                            + names[0] + " and " + names[1]);
                                }
                            });
                        }
                    }
                });
                thread.start();
            }
        });

        itemGuide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                QuickGuideDialog dialog = new QuickGuideDialog(MainWindow.this);
                dialog.setVisible(true);
            }
        });

        itemAbout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog dialog = new AboutDialog(MainWindow.this);
                dialog.setVisible(true);
            }
        });
    }

    private ImageIcon loadIcon(String name) {
        URL resource = getClass().getResource(name);
        if (resource == null) {
            MainWindow.LOGGER.error("Failed to load icon: " + name);
            return null;
        }
        return new ImageIcon(resource);
    }

    private void saveResultsGlobalComparison(File outputFile) {
        try (FileOutputStream stream = new FileOutputStream(outputFile)) {
            CsvWriter writer = new CsvWriter(stream, MainWindow.CSV_DELIMITER,
                    Charset.forName("UTF-8"));
            /*
             * Print header
             */
            int length = resultGlobalNames.length;
            writer.write("");
            for (int i = 0; i < length; i++) {
                writer.write(resultGlobalNames[i]);
            }
            writer.endRecord();
            /*
             * Print each value in the matrix
             */
            for (int i = 0; i < length; i++) {
                writer.write(resultGlobalNames[i]);
                for (int j = 0; j < length; j++) {
                    writer.write(Double.toString(resultGlobalMatrix[i][j]));
                }
                writer.endRecord();
            }
            writer.close();
        } catch (IOException e) {
            MainWindow.LOGGER.error(
                    "Failed to save results from global comparison", e);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveResultsLocalComparison(File outputFile) {
        /*
         * Reverse information: [angleName -> angleValue(residue)] into:
         * [residue -> angleValue(angleName)]
         */
        SortedMap<String, Map<String, Double>> map = new TreeMap<>();
        Set<String> setAngleNames = new LinkedHashSet<>();
        for (Entry<String, List<AngleDifference>> pair : resultLocal.entrySet()) {
            String angleName = pair.getKey();
            boolean isAnyNotNaN = false;
            for (AngleDifference ad : pair.getValue()) {
                ResidueNumber residue = ad.getResidue();
                String residueName = String.format("%s:%03d",
                        residue.getChainId(), residue.getSeqNum());
                if (!map.containsKey(residueName)) {
                    map.put(residueName, new LinkedHashMap<String, Double>());
                }
                Map<String, Double> angleValues = map.get(residueName);
                double difference = ad.getDifference();
                angleValues.put(angleName, difference);
                if (!Double.isNaN(difference)) {
                    isAnyNotNaN = true;
                }
            }
            if (isAnyNotNaN) {
                setAngleNames.add(angleName);
            }
        }

        try (FileOutputStream stream = new FileOutputStream(outputFile)) {
            CsvWriter writer = new CsvWriter(stream, MainWindow.CSV_DELIMITER,
                    Charset.forName("UTF-8"));
            /*
             * Write header
             */
            writer.write("");
            for (String angleName : setAngleNames) {
                writer.write(angleName);
            }
            writer.endRecord();
            /*
             * Write a record for each residue
             */
            for (String residueName : map.keySet()) {
                writer.write(residueName);
                Map<String, Double> mapAngles = map.get(residueName);
                for (String angleName : setAngleNames) {
                    if (mapAngles.containsKey(angleName)) {
                        String angleValue = Double.toString(mapAngles
                                .get(angleName));
                        writer.write(angleValue);
                    } else {
                        writer.write("");
                    }
                }
                writer.endRecord();
            }
            writer.close();
        } catch (IOException e) {
            MainWindow.LOGGER.error(
                    "Failed to save results from local comparison", e);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
