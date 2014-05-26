package pl.poznan.put.matching;

import java.util.List;

import pl.poznan.put.structure.ResidueTorsionAngles;
import pl.poznan.put.torsion.ChiTorsionAngle;
import pl.poznan.put.torsion.ChiTorsionAngleType;
import pl.poznan.put.torsion.TorsionAngle;
import pl.poznan.put.utility.TorsionAngleDelta;

public class ResidueComparisonResult {
    private final ResidueTorsionAngles left;
    private final ResidueTorsionAngles right;
    private final List<TorsionAngleDelta> deltas;

    public ResidueComparisonResult(ResidueTorsionAngles left,
            ResidueTorsionAngles right, List<TorsionAngleDelta> deltas) {
        super();
        this.left = left;
        this.right = right;
        this.deltas = deltas;
    }

    public ResidueTorsionAngles getLeft() {
        return left;
    }

    public ResidueTorsionAngles getRight() {
        return right;
    }

    public TorsionAngleDelta getDelta(TorsionAngle angle) {
        for (TorsionAngleDelta delta : deltas) {
            TorsionAngle torsionAngle = delta.getTorsionAngle();

            if (angle instanceof ChiTorsionAngleType) {
                ChiTorsionAngleType type = (ChiTorsionAngleType) angle;
                if (torsionAngle instanceof ChiTorsionAngle
                        && ((ChiTorsionAngle) torsionAngle).getType() == type) {
                    return delta;
                }
            }

            if (torsionAngle.equals(angle)) {
                return delta;
            }
        }

        return null;
    }
}
