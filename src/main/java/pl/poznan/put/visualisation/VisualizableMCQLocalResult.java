package pl.poznan.put.visualisation;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.analysis.IAnalysis;
import org.w3c.dom.svg.SVGDocument;
import pl.poznan.put.comparison.local.MCQLocalResult;
import pl.poznan.put.constant.Unicode;
import pl.poznan.put.interfaces.Visualizable;
import pl.poznan.put.matching.FragmentMatch;
import pl.poznan.put.matching.ResidueComparison;
import pl.poznan.put.pdb.analysis.MoleculeType;
import pl.poznan.put.pdb.analysis.PdbCompactFragment;
import pl.poznan.put.structure.secondary.formats.InvalidStructureException;
import pl.poznan.put.torsion.MasterTorsionAngleType;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class VisualizableMCQLocalResult extends MCQLocalResult implements Visualizable {
  @Override
  public final SVGDocument visualize() {
    throw new IllegalArgumentException(
        "Invalid usage, please use visualize() on FragmentMatch " + "instances");
  }

  @Override
  public final void visualize3D() {
    if (getAngleTypes().size() <= 1) {
      JOptionPane.showMessageDialog(
          null,
          "At least two torsion angle types are required for 3D visualization",
          "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      for (final FragmentMatch fragmentMatch : selectionMatch.getFragmentMatches()) {
        final PdbCompactFragment target = fragmentMatch.getTargetFragment();
        List<String> ticksY = null;
        String labelY = null;

        if (target.getMoleculeType() == MoleculeType.RNA) {
          try {
            ticksY = fragmentMatch.generateLabelsWithDotBracket();
            labelY = "Secondary structure";
          } catch (final InvalidStructureException e) {
            VisualizableMCQLocalResult.log.warn(
                "Failed to extract canonical secondary structure", e);
          }
        }

        if (ticksY == null) {
          ticksY = fragmentMatch.generateLabelsWithResidueNames();
          labelY = "ResID";
        }

        final String name = fragmentMatch.toString();
        final double[][] matrix = prepareMatrix(fragmentMatch);
        final List<String> ticksX = prepareTicksX();
        final NavigableMap<Double, String> valueTickZ = VisualizableMCQLocalResult.prepareTicksZ();
        final String labelX = "Angle type";
        final String labelZ = "Distance";
        final boolean showAllTicksX = true;
        final boolean showAllTicksY = false;

        final IAnalysis surface3d =
            new Surface3D(
                name,
                matrix,
                ticksX,
                ticksY,
                valueTickZ,
                labelX,
                labelY,
                labelZ,
                showAllTicksX,
                showAllTicksY);
        AnalysisLauncher.open(surface3d);
      }
    } catch (final Exception e) {
      final String message = "Failed to visualize in 3D";
      VisualizableMCQLocalResult.log.error(message, e);
      JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private double[][] prepareMatrix(final FragmentMatch fragmentMatch) {
    final List<MasterTorsionAngleType> angleTypes = getAngleTypes();
    final List<ResidueComparison> residueComparisons = fragmentMatch.getResidueComparisons();
    final double[][] matrix = new double[angleTypes.size()][];

    for (int i = 0; i < angleTypes.size(); i++) {
      final MasterTorsionAngleType angleType = angleTypes.get(i);
      matrix[i] = new double[residueComparisons.size()];

      for (int j = 0; j < residueComparisons.size(); j++) {
        final ResidueComparison residueComparison = residueComparisons.get(j);
        matrix[i][j] = residueComparison.getAngleDelta(angleType).getDelta().getRadians();
      }
    }

    return matrix;
  }

  private List<String> prepareTicksX() {
    final List<String> ticksX = new ArrayList<>();
    for (final MasterTorsionAngleType angleType : getAngleTypes()) {
      ticksX.add(angleType.getExportName());
    }
    return ticksX;
  }

  public static NavigableMap<Double, String> prepareTicksZ() {
    final NavigableMap<Double, String> valueTickZ = new TreeMap<>();
    valueTickZ.put(0.0, "0");

    for (double radians = Math.PI / 12.0;
        radians <= (Math.PI + 1.0e-3);
        radians += Math.PI / 12.0) {
      valueTickZ.put(radians, Math.round(Math.toDegrees(radians)) + Unicode.DEGREE);
    }

    return valueTickZ;
  }
}
