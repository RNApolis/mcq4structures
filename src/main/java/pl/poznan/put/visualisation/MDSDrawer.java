package pl.poznan.put.visualisation;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.LineMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.twod.Euclidean2D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.stat.StatUtils;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.algorithms.convexhull.ConvexHullFunction;
import org.jzy3d.maths.algorithms.convexhull.GrahamScan;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;
import pl.poznan.put.clustering.hierarchical.Cluster;
import pl.poznan.put.clustering.hierarchical.Clusterer;
import pl.poznan.put.clustering.hierarchical.HierarchicalClusterMerge;
import pl.poznan.put.clustering.hierarchical.HierarchicalClustering;
import pl.poznan.put.clustering.hierarchical.Linkage;
import pl.poznan.put.constant.Colors;
import pl.poznan.put.datamodel.ColoredNamedPoint;
import pl.poznan.put.datamodel.NamedPoint;
import pl.poznan.put.types.DistanceMatrix;
import pl.poznan.put.utility.svg.SVGHelper;

public final class MDSDrawer {
  public interface ColorProvider {
    Color getColor(int index);
  }

  public interface NameProvider {
    String getName(int index);
  }

  private static final int DESIRED_WIDTH = 640;
  private static final int CIRCLE_DIAMETER = 10;
  private static final int MAX_CLUSTER_NAME = 64;
  private static final ColorProvider COLOR_PROVIDER = index -> Color.BLACK;
  private static final NameProvider NAME_PROVIDER = index -> "";

  private MDSDrawer() {
    super();
  }

  public static SVGDocument scale2DAndVisualizePoints(final DistanceMatrix distanceMatrix) {
    return MDSDrawer.scale2DAndVisualizePoints(
        distanceMatrix, MDSDrawer.COLOR_PROVIDER, MDSDrawer.NAME_PROVIDER);
  }

  public static SVGDocument scale2DAndVisualizePoints(
      final DistanceMatrix distanceMatrix,
      final ColorProvider colorProvider,
      final NameProvider nameProvider) {
    final double[][] originalDistanceMatrix = distanceMatrix.getMatrix();
    final double[][] scaledXYMatrix = MDS.multidimensionalScaling(originalDistanceMatrix, 2);

    final SVGDocument document = SVGHelper.emptyDocument();
    final SVGGraphics2D graphics = new SVGGraphics2D(document);
    graphics.setFont(new Font("monospaced", Font.PLAIN, 10));

    final Rectangle2D bounds = MDSDrawer.calculateBounds(scaledXYMatrix);

    for (int i = 0; i < scaledXYMatrix.length; i++) {
      scaledXYMatrix[i][0] =
          (scaledXYMatrix[i][0] - bounds.getX()) * (MDSDrawer.DESIRED_WIDTH / bounds.getWidth());
      scaledXYMatrix[i][1] =
          (scaledXYMatrix[i][1] - bounds.getY()) * (MDSDrawer.DESIRED_WIDTH / bounds.getHeight());
    }

    for (int i = 0; i < scaledXYMatrix.length; i++) {
      final double x = scaledXYMatrix[i][0];
      final double y = scaledXYMatrix[i][1];
      graphics.setColor(colorProvider.getColor(i));
      graphics.draw(
          new Ellipse2D.Double(x, y, MDSDrawer.CIRCLE_DIAMETER, MDSDrawer.CIRCLE_DIAMETER));
    }

    final Map<Color, List<Integer>> colorMap = new HashMap<>();
    for (int i = 0; i < scaledXYMatrix.length; i++) {
      final Color color = colorProvider.getColor(i);
      if (!colorMap.containsKey(color)) {
        colorMap.put(color, new ArrayList<>());
      }
      colorMap.get(color).add(i);
    }

    if (colorMap.size() > 1) {
      final LineMetrics lineMetrics = SVGHelper.getLineMetrics(graphics);
      final float lineHeight = lineMetrics.getHeight();
      float legendHeight = 0.0f;

      for (final Map.Entry<Color, List<Integer>> entry : colorMap.entrySet()) {
        final List<Integer> indices = entry.getValue();
        assert !indices.isEmpty();

        final int first = indices.get(0);
        final String clusterName = nameProvider.getName(first);
        final String nameAbbreviated =
            StringUtils.abbreviate(clusterName, MDSDrawer.MAX_CLUSTER_NAME);

        graphics.setColor(entry.getKey());
        graphics.draw(
            new Ellipse2D.Double(
                MDSDrawer.DESIRED_WIDTH + MDSDrawer.CIRCLE_DIAMETER,
                legendHeight,
                MDSDrawer.CIRCLE_DIAMETER,
                MDSDrawer.CIRCLE_DIAMETER));
        graphics.drawString(
            nameAbbreviated,
            (float) (MDSDrawer.DESIRED_WIDTH + (MDSDrawer.CIRCLE_DIAMETER * 2.5)),
            (legendHeight + lineHeight) - (MDSDrawer.CIRCLE_DIAMETER / 2.0f));
        legendHeight += lineHeight;

        if (indices.size() == 1) {
          continue;
        }
        if (indices.size() == 2) {
          final int p1 = indices.get(0);
          final double x1 = scaledXYMatrix[p1][0] + (MDSDrawer.CIRCLE_DIAMETER / 2.0);
          final double y1 = scaledXYMatrix[p1][1] + (MDSDrawer.CIRCLE_DIAMETER / 2.0);
          final int p2 = indices.get(1);
          final double x2 = scaledXYMatrix[p2][0] + (MDSDrawer.CIRCLE_DIAMETER / 2.0);
          final double y2 = scaledXYMatrix[p2][1] + (MDSDrawer.CIRCLE_DIAMETER / 2.0);
          graphics.draw(new Line2D.Double(x1, y1, x2, y2));
          continue;
        }

        final Deque<Coord2d> convexHull = MDSDrawer.calculateConvexHull(scaledXYMatrix, indices);
        final double[] is = new double[convexHull.size()];
        final double[] xs = new double[convexHull.size()];
        final double[] ys = new double[convexHull.size()];
        int j = 0;

        for (final Coord2d coord : convexHull) {
          is[j] = j;
          xs[j] = coord.x;
          ys[j] = coord.y;
          j++;
        }

        final UnivariateInterpolator interpolator = new SplineInterpolator();
        final UnivariateFunction functionX = interpolator.interpolate(is, xs);
        final UnivariateFunction functionY = interpolator.interpolate(is, ys);

        final Path2D.Double path = new Path2D.Double();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
          final double cx1 = functionX.value(i - 0.6666666666666666);
          final double cy1 = functionY.value(i - 0.6666666666666666);
          final double cx2 = functionX.value(i - 0.3333333333333333);
          final double cy2 = functionY.value(i - 0.3333333333333333);
          path.curveTo(cx1, cy1, cx2, cy2, xs[i], ys[i]);
        }

        graphics.draw(path);
      }
    }

    final SVGSVGElement rootElement = document.getRootElement();
    graphics.getRoot(rootElement);

    final Rectangle2D box = SVGHelper.calculateBoundingBox(document);
    final String viewBox =
        String.format(
            Locale.US, "%f %f %f %f", box.getX(), box.getY(), box.getWidth(), box.getHeight());
    rootElement.setAttributeNS(null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, viewBox);
    rootElement.setAttributeNS(
        null, SVGConstants.SVG_WIDTH_ATTRIBUTE, Double.toString(box.getWidth()));
    rootElement.setAttributeNS(
        null, SVGConstants.SVG_HEIGHT_ATTRIBUTE, Double.toString(box.getHeight()));
    return document;
  }

  private static Deque<Coord2d> calculateConvexHull(
      final double[][] scaledXYMatrix, final List<Integer> indices) {
    final Coord2d[] points = new Coord2d[indices.size()];
    for (int i = 0; i < indices.size(); i++) {
      final int index = indices.get(i);
      points[i] =
          new Coord2d(
              scaledXYMatrix[index][0] + (MDSDrawer.CIRCLE_DIAMETER / 2.0),
              scaledXYMatrix[index][1] + (MDSDrawer.CIRCLE_DIAMETER / 2.0));
    }

    final ConvexHullFunction grahamScan = new GrahamScan();
    return grahamScan.getConvexHull(points);
  }

  private static Rectangle2D calculateBounds(final double[][] scaledXYMatrix) {
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    for (final double[] aScaledXYMatrix : scaledXYMatrix) {
      final double x = aScaledXYMatrix[0];
      final double y = aScaledXYMatrix[1];

      if (x < minX) {
        minX = x;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (y > maxY) {
        maxY = y;
      }
    }

    return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
  }

  public static SVGDocument scale2DAndVisualizePointsOld(
      final DistanceMatrix distanceMatrix, final ColorProvider colorProvider) {
    final double[][] originalDistanceMatrix = distanceMatrix.getMatrix();
    final double[][] scaledXYMatrix = MDS.multidimensionalScaling(originalDistanceMatrix, 2);
    final double[][] scaledDistanceMatrix =
        MDSDrawer.calculateScaledDistanceMatrix(originalDistanceMatrix, scaledXYMatrix);

    double maxDistance = Double.NEGATIVE_INFINITY;

    for (final double[] element : scaledDistanceMatrix) {
      maxDistance = Math.max(maxDistance, StatUtils.max(element));
    }

    final Clusterer clusterer =
        new Clusterer(distanceMatrix.getNames(), scaledDistanceMatrix, Linkage.COMPLETE);
    final HierarchicalClustering clustering = clusterer.cluster();
    final List<Cluster> clusters = Clusterer.initialClusterAssignment(distanceMatrix.getNames());
    final Map<Cluster, Vector2D> clusterCoords = new HashMap<>();
    final Map<Cluster, Set<Color>> clusterColors = new HashMap<>();

    for (int i = 0; i < clusters.size(); i++) {
      final Cluster cluster = clusters.get(i);
      clusterCoords.put(cluster, new Vector2D(scaledXYMatrix[i][0], scaledXYMatrix[i][1]));

      final Set<Color> colors = new HashSet<>();
      colors.add(colorProvider.getColor(i));
      clusterColors.put(cluster, colors);
    }

    for (final HierarchicalClusterMerge merge : clustering.getMerges()) {
      if (merge.getDistance() > (0.1 * maxDistance)) {
        break;
      }

      final Cluster left = clusters.get(merge.getLeft());
      final Cluster right = clusters.get(merge.getRight());
      final Cluster merged = Cluster.merge(left, right);
      clusters.remove(left);
      clusters.remove(right);
      clusters.add(merged);

      final Vector2D leftCoords = clusterCoords.get(left);
      final Vector2D rightCoords = clusterCoords.get(right);
      final double x = (leftCoords.getX() + rightCoords.getX()) / 2.0;
      final double y = (leftCoords.getY() + rightCoords.getY()) / 2.0;
      final Vector2D mergedCoords = new Vector2D(x, y);
      clusterCoords.put(merged, mergedCoords);

      final Set<Color> leftColors = clusterColors.get(left);
      final Set<Color> rightColors = clusterColors.get(right);
      final Set<Color> mergedColors = new HashSet<>();
      mergedColors.addAll(leftColors);
      mergedColors.addAll(rightColors);
      clusterColors.put(merged, mergedColors);
    }

    final List<NamedPoint> points = new ArrayList<>();
    for (final Cluster cluster : clusters) {
      final Set<Color> colors = clusterColors.get(cluster);
      final String name = cluster.getName();
      final Vector2D coords = clusterCoords.get(cluster);
      points.add(new ColoredNamedPoint(colors, name, coords));
    }

    return MDSDrawer.drawPoints(points);
  }

  private static double[][] calculateScaledDistanceMatrix(
      final double[][] originalDistanceMatrix, final double[][] scaledXYMatrix) {
    final double[][] scaledDistanceMatrix = new double[originalDistanceMatrix.length][];

    for (int i = 0; i < originalDistanceMatrix.length; i++) {
      scaledDistanceMatrix[i] = new double[originalDistanceMatrix.length];
    }

    for (int i = 0; i < originalDistanceMatrix.length; i++) {
      final Vector<Euclidean2D> pi = new Vector2D(scaledXYMatrix[i][0], scaledXYMatrix[i][1]);

      for (int j = i + 1; j < originalDistanceMatrix.length; j++) {
        final Vector<Euclidean2D> pj = new Vector2D(scaledXYMatrix[j][0], scaledXYMatrix[j][1]);
        scaledDistanceMatrix[i][j] = pi.distance(pj);
        scaledDistanceMatrix[j][i] = pi.distance(pj);
      }
    }
    return scaledDistanceMatrix;
  }

  private static SVGDocument drawPoints(final List<? extends NamedPoint> points) {
    final DOMImplementation dom = SVGDOMImplementation.getDOMImplementation();
    final SVGDocument document =
        (SVGDocument) dom.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
    final Element svgRoot = document.getDocumentElement();

    MDSDrawer.createAndAddLinearGradients(document, svgRoot, points);
    MDSDrawer.createAndAddTextElements(document, svgRoot, points);

    final Rectangle2D boundingBox = SVGHelper.calculateBoundingBox(document);
    svgRoot.setAttributeNS(
        null,
        SVGConstants.SVG_VIEW_BOX_ATTRIBUTE,
        String.format(
            "%s %s %s %s",
            boundingBox.getMinX(),
            boundingBox.getMinY(),
            boundingBox.getWidth(),
            boundingBox.getHeight()));
    svgRoot.setAttributeNS(
        null, SVGConstants.SVG_WIDTH_ATTRIBUTE, Double.toString(boundingBox.getWidth()));
    svgRoot.setAttributeNS(
        null, SVGConstants.SVG_HEIGHT_ATTRIBUTE, Double.toString(boundingBox.getHeight()));
    return document;
  }

  private static void createAndAddLinearGradients(
      final Document document, final Node svgRoot, final Iterable<? extends NamedPoint> points) {
    final Element defs =
        document.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_DEFS_TAG);

    for (final NamedPoint point : points) {
      if (point instanceof ColoredNamedPoint) {
        final Set<Color> colors = ((ColoredNamedPoint) point).getColors();

        if (colors.size() > 1) {
          final Element linearGradient =
              document.createElementNS(
                  SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_LINEAR_GRADIENT_TAG);
          linearGradient.setAttributeNS(
              null, SVGConstants.SVG_ID_ATTRIBUTE, StringUtils.deleteWhitespace(point.getName()));
          linearGradient.setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, "0%");
          linearGradient.setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, "0%");
          linearGradient.setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, "100%");
          linearGradient.setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, "0%");

          int i = 0;
          final int step = 100 / (colors.size() - 1);

          for (final Color color : colors) {
            final Element stop =
                document.createElementNS(
                    SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_STOP_TAG);
            stop.setAttributeNS(
                null,
                SVGConstants.SVG_OFFSET_ATTRIBUTE,
                String.format("%s%%", Integer.toString(i * step)));
            stop.setAttributeNS(
                null,
                SVGConstants.SVG_STYLE_ATTRIBUTE,
                String.format("stop-color: %s; " + "stop-opacity: 1", Colors.toSvgString(color)));
            linearGradient.appendChild(stop);
            i += 1;
          }

          defs.appendChild(linearGradient);
        }
      }
    }

    svgRoot.appendChild(defs);
  }

  private static void createAndAddTextElements(
      final Document document, final Node svgRoot, final List<? extends NamedPoint> points) {
    final double maxDistance = MDSDrawer.calculateMaxDistance(points);
    final double scale = MDSDrawer.DESIRED_WIDTH / maxDistance;

    for (final NamedPoint point : points) {
      final NamedPoint scaled = point.scalarMultiply(scale);
      final double x = scaled.getX();
      final double y = scaled.getY();

      final Element element =
          document.createElementNS(
              SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_TEXT_TAG);
      element.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, Double.toString(x));
      element.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, Double.toString(y));
      element.setAttributeNS(null, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, "middle");

      if (point instanceof ColoredNamedPoint) {
        final Set<Color> colors = ((ColoredNamedPoint) point).getColors();

        if (colors.size() == 1) {
          element.setAttributeNS(
              null, SVGConstants.SVG_FILL_ATTRIBUTE, Colors.toHexString(colors.iterator().next()));
        } else if (colors.size() > 1) {
          element.setAttributeNS(
              null,
              SVGConstants.SVG_FILL_ATTRIBUTE,
              String.format("url(#%s)", StringUtils.deleteWhitespace(point.getName())));
        }
      }

      element.setTextContent(point.getName());
      svgRoot.appendChild(element);
    }
  }

  private static double calculateMaxDistance(final List<? extends NamedPoint> points) {
    double maxDistance = 0;

    for (int i = 0; i < points.size(); i++) {
      final NamedPoint pi = points.get(i);

      for (int j = i + 1; j < points.size(); j++) {
        final NamedPoint pj = points.get(j);
        final double distance = pi.distance(pj);

        if (distance > maxDistance) {
          maxDistance = distance;
        }
      }
    }

    return maxDistance;
  }
}
