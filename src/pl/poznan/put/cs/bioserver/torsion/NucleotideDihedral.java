package pl.poznan.put.cs.bioserver.torsion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.biojava.bio.structure.Group;

/**
 * Dihedral angles for nucleic acid group (nucleotide).
 * 
 * @author Tomasz Żok (tzok[at]cs.put.poznan.pl)
 */
public class NucleotideDihedral implements AngleType {
    /** All names of angles in a nucleotide. */
    public enum AngleName {
        ALPHA, BETA, GAMMA, DELTA, EPSILON, ZETA, CHI, TAU0, TAU1, TAU2, TAU3, TAU4
    }

    private static final String C1P = " C1'";
    private static final String C2 = " C2 ";
    private static final String C2P = " C2'";
    private static final String C3P = " C3'";
    private static final String C4 = " C4 ";
    private static final String C4P = " C4'";
    private static final String C5 = " C5 ";
    private static final String C5P = " C5'";
    private static final String C6 = " C6 ";
    private static final String N1 = " N1 ";
    private static final String N3 = " N3 ";
    private static final String N9 = " N9 ";
    private static final String O2P = " O2'";
    private static final String O3P = " O3'";
    private static final String O4P = " O4'";
    private static final String O5P = " O5'";
    private static final String OP1 = " OP1";
    private static final String OP2 = " OP2";
    private static final String P = " P  ";

    /** A list of all used names. */
    public static final String[] USED_ATOMS = new String[] {
            NucleotideDihedral.C1P, NucleotideDihedral.C2,
            NucleotideDihedral.C2P, NucleotideDihedral.C3P,
            NucleotideDihedral.C4, NucleotideDihedral.C4P,
            NucleotideDihedral.C5, NucleotideDihedral.C5P,
            NucleotideDihedral.C6, NucleotideDihedral.N1,
            NucleotideDihedral.N3, NucleotideDihedral.N9,
            NucleotideDihedral.O2P, NucleotideDihedral.O3P,
            NucleotideDihedral.O4P, NucleotideDihedral.O5P,
            NucleotideDihedral.OP1, NucleotideDihedral.OP2,
            NucleotideDihedral.P };
    /** A list of all defined angles. */
    public static final AngleType[] ANGLES = new NucleotideDihedral[] {
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
            new NucleotideDihedral(AngleName.TAU4), };

    private static Map<AngleName, String[]> mapAngleToAtoms;
    private static Map<AngleName, int[]> mapAngleToRules;
    private static Set<Character> setPyrimidines;
    static {
        NucleotideDihedral.mapAngleToAtoms = new HashMap<>();
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.ALPHA, new String[] {
                NucleotideDihedral.O3P, NucleotideDihedral.P,
                NucleotideDihedral.O5P, NucleotideDihedral.C5P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.BETA, new String[] {
                NucleotideDihedral.P, NucleotideDihedral.O5P,
                NucleotideDihedral.C5P, NucleotideDihedral.C4P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.GAMMA, new String[] {
                NucleotideDihedral.O5P, NucleotideDihedral.C5P,
                NucleotideDihedral.C4P, NucleotideDihedral.C3P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.DELTA, new String[] {
                NucleotideDihedral.C5P, NucleotideDihedral.C4P,
                NucleotideDihedral.C3P, NucleotideDihedral.O3P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.EPSILON, new String[] {
                NucleotideDihedral.C4P, NucleotideDihedral.C3P,
                NucleotideDihedral.O3P, NucleotideDihedral.P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.ZETA, new String[] {
                NucleotideDihedral.C3P, NucleotideDihedral.O3P,
                NucleotideDihedral.P, NucleotideDihedral.O5P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.CHI, new String[] {
                NucleotideDihedral.O4P, NucleotideDihedral.C1P,
                NucleotideDihedral.N9, NucleotideDihedral.C4 });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU0, new String[] {
                NucleotideDihedral.C4P, NucleotideDihedral.O4P,
                NucleotideDihedral.C1P, NucleotideDihedral.C2P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU1, new String[] {
                NucleotideDihedral.O4P, NucleotideDihedral.C1P,
                NucleotideDihedral.C2P, NucleotideDihedral.C3P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU2, new String[] {
                NucleotideDihedral.C1P, NucleotideDihedral.C2P,
                NucleotideDihedral.C3P, NucleotideDihedral.C4P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU3, new String[] {
                NucleotideDihedral.C2P, NucleotideDihedral.C3P,
                NucleotideDihedral.C4P, NucleotideDihedral.O4P });
        NucleotideDihedral.mapAngleToAtoms.put(AngleName.TAU4, new String[] {
                NucleotideDihedral.C3P, NucleotideDihedral.C4P,
                NucleotideDihedral.O4P, NucleotideDihedral.C1P });

        NucleotideDihedral.mapAngleToRules = new HashMap<>();
        NucleotideDihedral.mapAngleToRules.put(AngleName.ALPHA, new int[] { 0,
                1, 1, 1 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.BETA, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.GAMMA, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.DELTA, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.EPSILON, new int[] {
                0, 0, 0, 1 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.ZETA, new int[] { 0,
                0, 1, 1 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.CHI, new int[] { 0, 0,
                0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU0, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU1, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU2, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU3, new int[] { 0,
                0, 0, 0 });
        NucleotideDihedral.mapAngleToRules.put(AngleName.TAU4, new int[] { 0,
                0, 0, 0 });

        NucleotideDihedral.setPyrimidines = new HashSet<>();
        NucleotideDihedral.setPyrimidines.addAll(Arrays.asList(new Character[] {
                'C', 'U', 'Y' }));
    }

    private AngleName angleName;

    @SuppressWarnings("javadoc")
    public NucleotideDihedral(AngleName angleName) {
        this.angleName = angleName;
    }

    @Override
    public String getAngleName() {
        return angleName.toString();
    }

    @Override
    public String[] getAtomNames(Group residue) {
        if (angleName.equals(AngleName.CHI)) {
            String pdbName = residue.getPDBName();
            char last = pdbName.charAt(pdbName.length() - 1);
            if (NucleotideDihedral.setPyrimidines.contains(last)) {
                return new String[] { NucleotideDihedral.O4P,
                        NucleotideDihedral.C1P, NucleotideDihedral.N1,
                        NucleotideDihedral.C2 };
            }
        }
        return NucleotideDihedral.mapAngleToAtoms.get(angleName);
    }

    @Override
    public int[] getGroupRule() {
        return NucleotideDihedral.mapAngleToRules.get(angleName);
    }
}
