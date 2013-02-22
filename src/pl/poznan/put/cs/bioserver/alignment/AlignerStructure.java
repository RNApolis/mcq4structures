package pl.poznan.put.cs.bioserver.alignment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Element;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.StructureAlignment;
import org.biojava.bio.structure.align.ce.CeMain;
import org.biojava.bio.structure.align.ce.CeParameters;
import org.biojava.bio.structure.align.model.AFPChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.poznan.put.cs.bioserver.helper.Helper;
import pl.poznan.put.cs.bioserver.helper.StructureManager;

/**
 * A class that allows to computer structural alignment.
 * 
 * @author tzok
 */
public final class AlignerStructure {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AlignerStructure.class);
    private static Map<AlignmentInput, AlignmentOutput> cache = new HashMap<>();

    /**
     * Align structurally two structures.
     * 
     * @param s1
     *            First structure.
     * @param s2
     *            Second structure.
     * @param description
     * @return An object with all information about computed alignment.
     * @throws StructureException
     *             If there were problems during alignment computation.
     */
    public static AlignmentOutput align(Structure s1, Structure s2,
            String description) throws StructureException {
        /*
         * Check if alignment was made before
         */
        AlignmentInput input = new AlignmentInput(s1, s2);
        if (AlignerStructure.cache.containsKey(input)) {
            AlignerStructure.LOGGER.info("Reusing alignment data from cache");
            return AlignerStructure.cache.get(input);
        }

        Atom[][] atoms = new Atom[2][];
        Structure[] structures = new Structure[] { s1, s2 };

        Set<Atom> changedAtoms = new HashSet<>();
        for (int j = 0; j < 2; j++) {
            List<Atom> list = Helper.getAtomArray(structures[j], new String[] {
                    "P", "CA" });
            atoms[j] = list.toArray(new Atom[list.size()]);
            assert atoms[j].length != 0 : "There are no P or CA atoms in: "
                    + StructureManager.getName(structures[j]);

            for (int i = 0; i < atoms[j].length; i++) {
                Atom atom = atoms[j][i];
                assert atom != null;
                if (atom.getElement().equals(Element.P)) {
                    atom.setName("CA");
                    atom.setFullName(" CA ");
                    changedAtoms.add(atom);
                }
            }
        }

        StructureAlignment alignment = new CeMain();
        CeParameters parameters = new CeParameters();
        while (true) {
            AFPChain align = alignment.align(atoms[0], atoms[1], parameters);
            if (align.getBlockRotationMatrix().length == 0) {
                int winSize = parameters.getWinSize();
                winSize--;
                if (winSize <= 0) {
                    throw new StructureException(
                            "Could not find structure alignment");
                }
                parameters.setWinSize(winSize);
                continue;
            }

            for (Atom atom : changedAtoms) {
                atom.setName("P");
                atom.setFullName(" P  ");
            }
            AlignmentOutput result = new AlignmentOutput(align, s1, s2, atoms,
                    description);
            AlignerStructure.cache.put(input, result);
            return result;
        }
    }

    private AlignerStructure() {
    }
}
