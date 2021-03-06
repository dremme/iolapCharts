package com.instantolap.charts.impl.chart;

import com.instantolap.charts.*;
import com.instantolap.charts.impl.axis.*;
import com.instantolap.charts.impl.canvas.RoundCanvasImpl;
import com.instantolap.charts.impl.content.SampleValueRenderer;
import com.instantolap.charts.impl.data.Theme;
import com.instantolap.charts.renderer.*;


public class RoundChartImpl extends BasicRoundChartImpl {

  private final ValueAxisImpl scaleAxis;
  private final SampleAxisImpl sampleAxis;
  private final RoundCanvasImpl canvas;
  private final boolean scaleOutside;

  public RoundChartImpl(Theme theme, boolean scaleOutside) {
    super(theme);

    this.scaleOutside = scaleOutside;
    if (scaleOutside) {
      this.scaleAxis = new RoundValueAxisImpl(theme);
      this.sampleAxis = new SampleAxisImpl(theme, 1);
    } else {
      this.scaleAxis = new ValueAxisImpl(theme);
      this.sampleAxis = new RoundSampleAxisImpl(theme, 0);
    }
    scaleAxis.enableZoom(false);
    this.canvas = new RoundCanvasImpl(theme);
  }

  @Override
  public ValueAxis getScaleAxis() {
    return scaleAxis;
  }

  @Override
  public SampleAxis getSampleAxis() {
    return sampleAxis;
  }

  @Override
  public RoundCanvas getCanvas() {
    return canvas;
  }

  @Override
  protected void buildCubes() {
    super.buildCubes();

    final Data data = getData();
    if (data == null) {
      return;
    }

    // build x-axis cube
    final Cube cube = data.getCurrentCube();
    if (sampleAxis != null) {
      sampleAxis.setData(cube);
    }

    if (scaleAxis != null) {
      scaleAxis.setData(cube);
      scaleAxis.clearMeasures();

      for (Content content : getContents()) {
        if (content instanceof SampleValueRenderer) {
          final SampleValueRenderer sampleContent = (SampleValueRenderer) content;
          sampleContent.addMeasuresToAxes(scaleAxis);
        }
      }
    }
  }

  @Override
  protected void render(double progress, double x, double y, double width, double height)
    throws ChartException {
    final Renderer r = getRenderer();

    final Data data = getData();
    final boolean isStacked = isStacked();
    final boolean isCentered = needsCenteredSampleAxis();
    final ChartFont font = getFont();

    // calc axis sizes
    final BasicAxisImpl innerAxis = scaleOutside ? sampleAxis : scaleAxis;
    final BasicAxisImpl outerAxis = scaleOutside ? scaleAxis : sampleAxis;

    final ChartColor innerBorderColor = getInnerBorder();
    ChartStroke innerBorderStroke = getInnerBorderStroke();

    double outerRadius = 0, innerRadius = 0;
    for (int n = 0; n < 2; n++) {
      outerRadius = outerAxis.getRadius(r, width, height);

      innerRadius = outerRadius;
      if (innerBorderColor != null) {
        if (innerBorderStroke != null) {
          innerRadius -= innerBorderStroke.getWidth();
        }
        innerRadius -= getInnerPadding();
      }
      innerAxis.setData(innerRadius, r, 0, isStacked, isCentered, true, 0);

      final double size =(2 * Math.PI * innerRadius);
      outerAxis.setData(size, r, 0, isStacked, isCentered, false, 0);
    }

    final double cx = x + width / 2;
    final double cy = y + height / 2;

    // paint inner (round) border
    if (innerBorderColor != null) {
      if (innerBorderStroke == null) {
        innerBorderStroke = ChartStroke.DEFAULT;
      }
      r.setStroke(innerBorderStroke);
      final double br = outerRadius - innerBorderStroke.getWidth() / 2;
      r.setColor(innerBorderColor);
      r.drawDonut(cx, cy, br, br, 0, Math.PI * 2, true);
      r.resetStroke();
    }

    // paint canvas
    canvas.render(
      progress,
      r,
      x, y,
      width, height,
      cx, cy,
      innerRadius,
      data,
      innerAxis,
      (RoundAxis) outerAxis,
      isCentered
    );

    final double innerX = cx - innerRadius;
    final double innerY = cy - innerRadius;
    final double innerSize = 2 * innerRadius;

    // paint axes
    outerAxis.setRadius(innerRadius);
    if (outerAxis.isVisible()) {
      outerAxis.render(r, innerX, innerY, innerSize, innerSize, isCentered, false, font);
    }

    if (innerAxis.isVisible()) {
      final double axisWidth = innerAxis.getNeededSize();
      innerAxis.render(
        r, cx - axisWidth, cy - innerRadius, axisWidth, height, true, true, font
      );
    }

    // paint content
    for (Content content : getContents()) {
      if (content instanceof SampleValueRenderer) {
        final SampleValueRenderer sampleContent = (SampleValueRenderer) content;
        sampleContent.render(
          progress,
          r,
          data,
          innerX, innerY,
          innerSize, innerSize,
          sampleAxis, scaleAxis,
          isStacked,
          isCentered,
          false,
          font,
          getBackground()
        );
      }
    }

    // add mouse listener for rotation
    if (getEnableRotation()) {
      r.addMouseListener(x, y, width, height, (ChartMouseWheelListener) (mx, my, v) -> {
        final RoundAxis round = (RoundAxis) outerAxis;
        round.rotate(r, v);
      });
    }
  }
}
