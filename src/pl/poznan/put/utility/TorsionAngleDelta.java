package pl.poznan.put.utility;

import java.util.ArrayList;
import java.util.List;

import pl.poznan.put.common.MoleculeType;
import pl.poznan.put.helper.CommonNumberFormat;
import pl.poznan.put.helper.Constants;
import pl.poznan.put.helper.TorsionAnglesHelper;
import pl.poznan.put.torsion.AngleValue;
import pl.poznan.put.torsion.AverageAngle;
import pl.poznan.put.torsion.TorsionAngle;

public class TorsionAngleDelta {
    public enum State {
        TORSION_LEFT_INVALID, TORSION_RIGHT_INVALID, BOTH_INVALID, BOTH_VALID,
        DIFFERENT_CHI;
    }

    private final AngleValue torsionAngleValueLeft;
    private final AngleValue torsionAngleValueRight;
    private final State state;
    private final double delta;

    public static TorsionAngleDelta calculate(AngleValue torsion1,
            AngleValue torsion2) {
        State state;
        double delta = Double.NaN;

        if (!torsion1.isValid() && !torsion2.isValid()) {
            state = State.BOTH_INVALID;
        } else if (!torsion1.isValid() && torsion2.isValid()) {
            state = State.TORSION_LEFT_INVALID;
        } else if (torsion1.isValid() && !torsion2.isValid()) {
            state = State.TORSION_RIGHT_INVALID;
        } else {
            state = State.BOTH_VALID;
            delta = TorsionAnglesHelper.subtractTorsions(torsion1.getValue(),
                    torsion2.getValue());
        }

        return new TorsionAngleDelta(torsion1, torsion2, state, delta);
    }

    public static TorsionAngleDelta calculateChiDelta(AngleValue chiL,
            AngleValue chiR, boolean matchChiByType) {
        TorsionAngle torL = chiL.getAngle();
        TorsionAngle torR = chiR.getAngle();

        if (!matchChiByType && torL.getMoleculeType() == MoleculeType.RNA
                && !torL.equals(torR)) {
            return new TorsionAngleDelta(chiL, chiR, State.DIFFERENT_CHI,
                    Double.NaN);
        }

        return TorsionAngleDelta.calculate(chiL, chiR);
    }

    public static TorsionAngleDelta calculateAverage(MoleculeType moleculeType,
            List<TorsionAngleDelta> deltas) {
        List<AngleValue> left = new ArrayList<>();
        List<AngleValue> right = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (TorsionAngleDelta tad : deltas) {
            TorsionAngle torsionAngle = tad.getTorsionAngle();
            if (torsionAngle.getMoleculeType() != moleculeType) {
                continue;
            }

            if (tad.state == State.BOTH_VALID) {
                left.add(tad.torsionAngleValueLeft);
                right.add(tad.torsionAngleValueRight);
                values.add(tad.delta);
            } else if (tad.state == State.TORSION_LEFT_INVALID) {
                right.add(tad.torsionAngleValueRight);
            } else if (tad.state == State.TORSION_RIGHT_INVALID) {
                left.add(tad.torsionAngleValueLeft);
            }
        }

        double mcq = TorsionAnglesHelper.calculateMean(values);
        return new TorsionAngleDelta(
                AverageAngle.calculate(moleculeType, left),
                AverageAngle.calculate(moleculeType, right),
                Double.isNaN(mcq) ? State.BOTH_INVALID : State.BOTH_VALID, mcq);
    }

    public AngleValue getTorsionAngleValueLeft() {
        return torsionAngleValueLeft;
    }

    public AngleValue getTorsionAngleValueRight() {
        return torsionAngleValueRight;
    }

    public State getState() {
        return state;
    }

    public double getDelta() {
        return delta;
    }

    public TorsionAngle getTorsionAngle() {
        return torsionAngleValueLeft.getAngle();
    }

    @Override
    public String toString() {
        return "TorsionAngleDelta [torsionAngleValueLeft="
                + torsionAngleValueLeft + ", torsionAngleValueRight="
                + torsionAngleValueRight + ", state=" + state + ", delta="
                + delta + "]";
    }

    /**
     * Represent numeric value in a way external tools understand (dot as
     * fraction point and no UNICODE_DEGREE sign).
     * 
     * @return String representation of this delta object understandable by
     *         external tools.
     */
    public String toExportString() {
        switch (state) {
        case BOTH_INVALID:
            return null;
        case BOTH_VALID:
            return CommonNumberFormat.formatDouble(Math.toDegrees(delta));
        case TORSION_LEFT_INVALID:
            return "Missing atoms in left";
        case TORSION_RIGHT_INVALID:
            return "Missing atoms in right";
        case DIFFERENT_CHI:
            return "Purine/pyrimidine mismatch";
        default:
            return "Error";
        }
    }

    /**
     * Represent object as a String which will be displayed to user in the GUI.
     * 
     * @return String representation of object to be shown in the GUI.
     */
    public String toDisplayString() {
        String result = toExportString();

        if (state == State.BOTH_INVALID) {
            result = "";
        } else if (state == State.BOTH_VALID) {
            result += Constants.UNICODE_DEGREE;
        }

        return result;
    }

    private TorsionAngleDelta(AngleValue torsion1,
            AngleValue torsion2, State state, double delta) {
        super();
        this.torsionAngleValueLeft = torsion1;
        this.torsionAngleValueRight = torsion2;
        this.state = state;
        this.delta = delta;
    }
}
