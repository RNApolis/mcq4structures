package pl.poznan.put.cs.bioserver.torsion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biojava.bio.structure.Group;

import pl.poznan.put.cs.bioserver.helper.UniTypeQuadruplet;

/**
 * Dihedral angles for nucleic acid group (nucleotide).
 * 
 * @author Tomasz Zok (tzok[at]cs.put.poznan.pl)
 */
public final class NucleotideDihedral implements AngleType {
    /** All names of angles in a nucleotide. */
    private enum AngleName {
        ALPHA,
        BETA,
        CHI,
        DELTA,
        EPSILON,
        ETA,
        ETA_PRIM,
        GAMMA,
        TAU0,
        TAU1,
        TAU2,
        TAU3,
        TAU4,
        THETA,
        THETA_PRIM,
        ZETA
    }

    private static List<AngleType> angles = Arrays.asList(new AngleType[] {
            new NucleotideDihedral(AngleName.ALPHA),
            new NucleotideDihedral(AngleName.BETA),
            new NucleotideDihedral(AngleName.GAMMA),
            new NucleotideDihedral(AngleName.DELTA),
            new NucleotideDihedral(AngleName.EPSILON),
            new NucleotideDihedral(AngleName.ZETA),
            new NucleotideDihedral(AngleName.CHI),
            new NucleotideDihedral(AngleName.TAU0),
            new NucleotideDihedral(AngleName.TAU1),
            new NucleotideDihedral(AngleName.TAU2),
            new NucleotideDihedral(AngleName.TAU3),
            new NucleotideDihedral(AngleName.TAU4),
            new NucleotideDihedral(AngleName.ETA),
            new NucleotideDihedral(AngleName.THETA),
            new NucleotideDihedral(AngleName.ETA_PRIM),
            new NucleotideDihedral(AngleName.THETA_PRIM) });
    private static List<String> atoms = Arrays.asList(new String[] {
            NucleotideDihedral.C1P, NucleotideDihedral.C2,
            NucleotideDihedral.C2P, NucleotideDihedral.C3P,
            NucleotideDihedral.C4, NucleotideDihedral.C4P,
            NucleotideDihedral.C5P, NucleotideDihedral.N1,
            NucleotideDihedral.N9, NucleotideDihedral.O3P,
            NucleotideDihedral.O4P, NucleotideDihedral.O5P,
            NucleotideDihedral.P });
    private static final String C1P = " C1'";
    private static final String C2 = " C2 ";
    private static final String C2P = " C2'";
    private static final String C3P = " C3'";
    private static final String C4 = " C4 ";
    private static final String C4P = " C4'";
    private static final String C5P = " C5'";
    private static Map<AngleName, UniTypeQuadruplet<String>> mapAngleToAtoms;
    private static Map<AngleName, UniTypeQuadruplet<Integer>> mapAngleToRules;
    private static final String N1 = " N1 ";
    private static final String N9 = " N9 ";

    private static final String O3P = " O3'";

    private static final String O4P = " O4'";

    private static final String O5P = " O5'";
    private static final String P = " P  ";

    private static Set<Character> setPyrimidines;
    static {
        NucleotideDihedral.mapAngleToAtoms = new HashMap<>();
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.ALPHA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.O3P,
                        NucleotideDihedral.P, NucleotideDihedral.O5P,
                        NucleotideDihedral.C5P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.BETA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.P,
                        NucleotideDihedral.O5P, NucleotideDihedral.C5P,
                        NucleotideDihedral.C4P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.GAMMA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.O5P,
                        NucleotideDihedral.C5P, NucleotideDihedral.C4P,
                        NucleotideDihedral.C3P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.DELTA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C5P,
                        NucleotideDihedral.C4P, NucleotideDihedral.C3P,
                        NucleotideDihedral.O3P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.EPSILON,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C4P,
                        NucleotideDihedral.C3P, NucleotideDihedral.O3P,
                        NucleotideDihedral.P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.ZETA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C3P,
                        NucleotideDihedral.O3P, NucleotideDihedral.P,
                        NucleotideDihedral.O5P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.CHI,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.O4P,
                        NucleotideDihedral.C1P, NucleotideDihedral.N9,
                        NucleotideDihedral.C4 }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU0,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C4P,
                        NucleotideDihedral.O4P, NucleotideDihedral.C1P,
                        NucleotideDihedral.C2P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU1,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.O4P,
                        NucleotideDihedral.C1P, NucleotideDihedral.C2P,
                        NucleotideDihedral.C3P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU2,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C1P,
                        NucleotideDihedral.C2P, NucleotideDihedral.C3P,
                        NucleotideDihedral.C4P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU3,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C2P,
                        NucleotideDihedral.C3P, NucleotideDihedral.C4P,
                        NucleotideDihedral.O4P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU4,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C3P,
                        NucleotideDihedral.C4P, NucleotideDihedral.O4P,
                        NucleotideDihedral.C1P }));

        NucleotideDihedral.mapAngleToAtoms.put(AngleName.ETA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C4P,
                        NucleotideDihedral.P, NucleotideDihedral.C4P,
                        NucleotideDihedral.P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.THETA,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.P,
                        NucleotideDihedral.C4P, NucleotideDihedral.P,
                        NucleotideDihedral.C4P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.ETA_PRIM,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.C1P,
                        NucleotideDihedral.P, NucleotideDihedral.C1P,
                        NucleotideDihedral.P }));
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.THETA_PRIM,
                new UniTypeQuadruplet<>(new String[] { NucleotideDihedral.P,
                        NucleotideDihedral.C1P, NucleotideDihedral.P,
                        NucleotideDihedral.C1P }));

        NucleotideDihedral.mapAngleToRules = new HashMap<>();
        NucleotideDihedral.mapAngleToRules.put(AngleName.ALPHA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 1, 1, 1 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.BETA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.GAMMA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.DELTA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.EPSILON,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 1 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.ZETA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 1, 1 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.CHI,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU0,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU1,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU2,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU3,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU4,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 0, 0 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.ETA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 1, 1, 2 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.THETA,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 1, 1 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.ETA_PRIM,
                new UniTypeQuadruplet<>(new Integer[] { 0, 1, 1, 2 }));
        NucleotideDihedral.mapAngleToRules.put(AngleName.THETA_PRIM,
                new UniTypeQuadruplet<>(new Integer[] { 0, 0, 1, 1 }));

        NucleotideDihedral.setPyrimidines = new HashSet<>();
        NucleotideDihedral.setPyrimidines.addAll(Arrays.asList(new Character[] {
                'C', 'U', 'Y' }));
    }

    public static List<AngleType> getAngles() {
        return NucleotideDihedral.angles;
    }

    public static List<String> getUsedAtoms() {
        return NucleotideDihedral.atoms;
    }

    private AngleName angleName;

    private NucleotideDihedral(AngleName angleName) {
        this.angleName = angleName;
    }

    @Override
    public String getAngleName() {
        return angleName.toString();
    }

    @Override
    public UniTypeQuadruplet<String> getAtomNames(Group residue) {
        if (angleName.equals(AngleName.CHI)) {
            String pdbName = residue.getPDBName();
            char last = pdbName.charAt(pdbName.length() - 1);
            if (NucleotideDihedral.setPyrimidines.contains(last)) {
                return new UniTypeQuadruplet<>(new String[] {
                        NucleotideDihedral.O4P, NucleotideDihedral.C1P,
                        NucleotideDihedral.N1, NucleotideDihedral.C2 });
            }
        }
        return NucleotideDihedral.mapAngleToAtoms.get(angleName);
    }

    @Override
    public UniTypeQuadruplet<Integer> getGroupRule() {
        return NucleotideDihedral.mapAngleToRules.get(angleName);
    }
}
