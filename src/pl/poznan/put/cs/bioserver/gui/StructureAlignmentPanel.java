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

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.jmol.util.Logger;

import pl.poznan.put.cs.bioserver.alignment.StructureAligner;
import pl.poznan.put.cs.bioserver.helper.Helper;

public class StructureAlignmentPanel extends JPanel {
    public class ButtonPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        JButton buttonAddFile;
        JButton buttonAlign;

        public ButtonPanel() {
            super();
            buttonAddFile = new JButton("Add file");
            buttonAlign = new JButton("Align");

            add(buttonAddFile);
            add(buttonAlign);
        }
    }

    public class PdbPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        DefaultListModel<String> listModel;
        JList<String> list;
        DefaultComboBoxModel<String> comboBoxModelFirst, comboBoxModelSecond;
        JComboBox<String> comboBoxFirst, comboBoxSecond;

        public PdbPanel() {
            super();

            listModel = new DefaultListModel<>();
            list = new JList<>(listModel);
            comboBoxModelFirst = new DefaultComboBoxModel<>();
            comboBoxModelSecond = new DefaultComboBoxModel<>();
            comboBoxFirst = new JComboBox<>(comboBoxModelFirst);
            comboBoxSecond = new JComboBox<>(comboBoxModelSecond);

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 2;
            add(list, c);
            c.gridx++;
            c.gridheight--;
            add(comboBoxFirst, c);
            c.gridy++;
            add(comboBoxSecond, c);

            list.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        int index = list.getSelectedIndex();
                        if (index == 0)
                            comboBoxModelFirst.removeAllElements();
                        else
                            comboBoxModelSecond.removeAllElements();
                        listModel.remove(index);
                        refreshComboBoxes();
                    }
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
        }
    }

    public class SettingsPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        ButtonPanel buttonPanel;
        PdbPanel pdbPanel;

        public SettingsPanel() {
            super(new BorderLayout());
            buttonPanel = new ButtonPanel();
            pdbPanel = new PdbPanel();

            add(buttonPanel, BorderLayout.NORTH);
            add(pdbPanel, BorderLayout.SOUTH);
        }
    }

    private static final long serialVersionUID = 1L;
    final JFileChooser chooser = new JFileChooser();
    PdbManager pdbManager;
    JTextArea textArea;
    SettingsPanel settingsPanel;

    public StructureAlignmentPanel(PdbManager m) {
        super(new BorderLayout());
        pdbManager = m;

        settingsPanel = new SettingsPanel();
        textArea = new JTextArea();

        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        textArea.setEditable(false);

        add(settingsPanel, BorderLayout.NORTH);
        add(textArea, BorderLayout.CENTER);

        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "PDB file format", "pdb", "pdb1", "ent", "brk", "gz"));
        chooser.setMultiSelectionEnabled(true);

        settingsPanel.buttonPanel.buttonAddFile
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                            return;
                        for (File f : chooser.getSelectedFiles())
                            if (!addFile(f))
                                break;
                    }
                });

        settingsPanel.buttonPanel.buttonAlign
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (settingsPanel.pdbPanel.listModel.size() != 2) {
                            warning();
                            return;
                        }

                        Structure[] structures = pdbManager
                                .getStructures(settingsPanel.pdbPanel.listModel
                                        .elements());
                        Chain chains[] = new Chain[2];
                        chains[0] = structures[0]
                                .getChain(settingsPanel.pdbPanel.comboBoxFirst
                                        .getSelectedIndex());
                        chains[1] = structures[1]
                                .getChain(settingsPanel.pdbPanel.comboBoxSecond
                                        .getSelectedIndex());

                        boolean isRNA = Helper.isNucleicAcid(chains[0]);
                        if (isRNA != Helper.isNucleicAcid(chains[1])) {
                            String message = "Structures meant to be aligned "
                                    + "represent different molecule types!";
                            Logger.error(message);
                            JOptionPane.showMessageDialog(null, message,
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        Helper.normalizeAtomNames(chains[0]);
                        Helper.normalizeAtomNames(chains[1]);

                        try {
                            Chain[] aligned = StructureAligner.align(chains[0],
                                    chains[1]);
                        } catch (StructureException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                });
    }

    boolean addFile(File path) {
        if (settingsPanel.pdbPanel.listModel.size() >= 2) {
            warning();
            return false;
        }
        String absolutePath = path.getAbsolutePath();
        pdbManager.addStructure(absolutePath);
        settingsPanel.pdbPanel.listModel.addElement(absolutePath);

        refreshComboBoxes();
        return true;
    }

    void refreshComboBoxes() {
        settingsPanel.pdbPanel.comboBoxModelFirst.removeAllElements();
        settingsPanel.pdbPanel.comboBoxModelSecond.removeAllElements();

        Structure[] structures = pdbManager
                .getStructures(settingsPanel.pdbPanel.listModel.elements());
        for (int i = 0; i < settingsPanel.pdbPanel.listModel.getSize(); ++i)
            for (Chain c : structures[i].getChains())
                if (i == 0)
                    settingsPanel.pdbPanel.comboBoxModelFirst.addElement(c
                            .getChainID());
                else
                    settingsPanel.pdbPanel.comboBoxModelSecond.addElement(c
                            .getChainID());

    }

    void warning() {
        JOptionPane.showMessageDialog(this,
                "You must have exactly two molecules", "Warning",
                JOptionPane.WARNING_MESSAGE);
    }
}
