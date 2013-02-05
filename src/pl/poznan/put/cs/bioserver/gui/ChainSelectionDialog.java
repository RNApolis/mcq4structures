package pl.poznan.put.cs.bioserver.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.poznan.put.cs.bioserver.helper.PdbManager;

public class ChainSelectionDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    protected static final Logger LOGGER = LoggerFactory
            .getLogger(ChainSelectionDialog.class);
    public File[] selectedStructures;
    public Chain[][] selectedChains;
    public DefaultComboBoxModel<File> modelLeft;
    public DefaultComboBoxModel<File> modelRight;

    public ChainSelectionDialog(JFrame owner) {
        super(owner, true);

        modelLeft = new DefaultComboBoxModel<>();
        final JComboBox<File> comboLeft = new JComboBox<>(modelLeft);
        final JPanel panelChainsLeft = new JPanel();
        panelChainsLeft.setLayout(new BoxLayout(panelChainsLeft,
                BoxLayout.Y_AXIS));
        panelChainsLeft.setBorder(BorderFactory
                .createTitledBorder("Available chains:"));
        final JPanel panelLeft = new JPanel();
        panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
        panelLeft.add(comboLeft);
        panelLeft.add(new JScrollPane(panelChainsLeft));

        modelRight = new DefaultComboBoxModel<>();
        final JComboBox<File> comboRight = new JComboBox<>(modelRight);
        final JPanel panelChainsRight = new JPanel();
        panelChainsRight.setLayout(new BoxLayout(panelChainsRight,
                BoxLayout.Y_AXIS));
        panelChainsRight.setBorder(BorderFactory
                .createTitledBorder("Available chains:"));
        JPanel panelRight = new JPanel();
        panelRight.setLayout(new BoxLayout(panelRight, BoxLayout.Y_AXIS));
        panelRight.add(comboRight);
        panelRight.add(new JScrollPane(panelChainsRight));

        JPanel panelBoth = new JPanel();
        panelBoth.setLayout(new GridLayout(1, 2));
        panelBoth.add(panelLeft);
        panelBoth.add(panelRight);

        JButton buttonOk = new JButton("OK");
        JButton buttonCancel = new JButton("Cancel");
        JPanel panelButtons = new JPanel();
        panelButtons.add(buttonOk);
        panelButtons.add(buttonCancel);

        setLayout(new BorderLayout());
        add(panelBoth, BorderLayout.CENTER);
        add(panelButtons, BorderLayout.SOUTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = new Dimension(screenSize.width / 4,
                screenSize.height / 4);
        panelChainsLeft.setPreferredSize(size);
        panelChainsRight.setPreferredSize(size);

        pack();
        int width = getPreferredSize().width;
        int height = getPreferredSize().height;

        int x = screenSize.width - width;
        int y = screenSize.height - height;
        setSize(width, height);
        setLocation(x / 2, y / 2);

        setTitle("Chain selection dialog");

        ActionListener actionListenerCombo = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel panelReference;

                JComboBox<File> source = (JComboBox<File>) e.getSource();
                if (source.equals(comboLeft)) {
                    panelReference = panelChainsLeft;
                } else {
                    panelReference = panelChainsRight;
                }
                panelReference.removeAll();

                File file = (File) source.getSelectedItem();
                if (file == null) {
                    return;
                }
                Structure structure = PdbManager.getStructure(file);
                if (structure == null) {
                    return;
                }
                for (Chain chain : structure.getChains()) {
                    panelReference.add(new JCheckBox(chain.getChainID()));
                }
                panelReference.revalidate();
            }
        };
        comboLeft.addActionListener(actionListenerCombo);
        comboRight.addActionListener(actionListenerCombo);

        buttonOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                @SuppressWarnings("rawtypes")
                JComboBox[] combos = new JComboBox[] { comboLeft, comboRight };
                JPanel[] panels = new JPanel[] { panelChainsLeft,
                        panelChainsRight };

                selectedStructures = new File[2];
                selectedChains = new Chain[2][];
                for (int i = 0; i < 2; i++) {
                    File pdb = (File) combos[i].getSelectedItem();
                    selectedStructures[i] = pdb;

                    List<Chain> list = new ArrayList<>();
                    Structure structure = PdbManager.getStructure(pdb);
                    for (Component component : panels[i].getComponents()) {
                        if (component instanceof JCheckBox) {
                            if (((JCheckBox) component).isSelected()) {
                                String chainId = ((JCheckBox) component)
                                        .getText();
                                try {
                                    list.add(structure.getChainByPDB(chainId));
                                } catch (StructureException e) {
                                    ChainSelectionDialog.LOGGER
                                            .error("Failed to read chain "
                                                    + chainId
                                                    + " from structure: " + pdb,
                                                    e);
                                }
                            }
                        }
                    }
                    selectedChains[i] = list.toArray(new Chain[list.size()]);
                }
                dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedChains = null;
                dispose();
            }
        });
    }
}
