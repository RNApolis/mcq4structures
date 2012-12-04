package pl.poznan.put.cs.bioserver.alignment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Element;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureImpl;
import org.biojava.bio.structure.align.StructureAlignment;
import org.biojava.bio.structure.align.ce.CeMain;
import org.biojava.bio.structure.align.model.AFPChain;

import pl.poznan.put.cs.bioserver.helper.Helper;

/**
 * A class that allows to computer structural alignment.
 * 
 * @author tzok
 */
public final class StructureAligner {
    private static final Logger LOGGER = Logger
            .getLogger(StructureAligner.class);
    private static Map<AlignmentInput, AlignmentOutput> cache = new HashMap<>();

    /**
     * Align structurally two chains.
     * 
     * @param c1
     *            First chain.
     * @param c2
     *            Second chain
     * @return An object with all information about computed alignment.
     * @throws StructureException
     *             If there were problems during alignment computation.
     */
    public static AlignmentOutput align(Chain c1, Chain c2)
            throws StructureException {
        StructureImpl s1 = new StructureImpl(c1);
        StructureImpl s2 = new StructureImpl(c2);
        return StructureAligner.align(s1, s2);
    }

    /**
     * Align structurally two structures.
     * 
     * @param s1
     *            First structure.
     * @param s2
     *            Second structure.
     * @return An object with all information about computed alignment.
     * @throws StructureException
     *             If there were problems during alignment computation.
     */
    public static AlignmentOutput align(Structure s1, Structure s2)
            throws StructureException {
        /*
         * Check if alignment was made before
         */
        AlignmentInput input = new AlignmentInput(s1, s2);
        if (StructureAligner.cache.containsKey(input)) {
            StructureAligner.LOGGER.info("Reusing alignment data from cache");
            return StructureAligner.cache.get(input);
        }

        Atom[][] atoms = new Atom[2][];
        Structure[] structures = new Structure[] { s1, s2 };

        Set<Atom> changedAtoms = new HashSet<>();
        for (int j = 0; j < 2; j++) {
            List<Atom> list = Helper.getAtomArray(structures[j], new String[] {
                    "P", "CA" });
            atoms[j] = list.toArray(new Atom[list.size()]);

            for (int i = 0; i < atoms[j].length; i++) {
                Atom atom = atoms[j][i];
                if (atom.getElement().equals(Element.P)) {
                    atom.setName("CA");
                    atom.setFullName(" CA ");
                    changedAtoms.add(atom);
                }
            }
        }

        StructureAlignment alignment = new CeMain();
        AFPChain align = alignment.align(atoms[0], atoms[1]);

        for (Atom atom : changedAtoms) {
            atom.setName("P");
            atom.setFullName(" P  ");
        }
        AlignmentOutput result = new AlignmentOutput(align, s1, s2, atoms);
        StructureAligner.cache.put(input, result);
        return result;
    }

    private StructureAligner() {
    }
}
