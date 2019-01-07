package com.instantolap.charts.impl.axis;

import com.instantolap.charts.ScaleAxis;
import com.instantolap.charts.ScaleAxisListener;
import com.instantolap.charts.impl.util.ArrayHelper;
import com.instantolap.charts.renderer.*;

import java.util.ArrayList;
import java.util.List;


public abstract class BasicScaleAxisImpl extends BasicAxisImpl implements ScaleAxis {

  private final List<ScaleAxisListener> listeners = new ArrayList<>();
  protected Double userMin, userMax;
  protected String[] measures = new String[0];
  protected Double min, max;
  protected Double tick, userTick;
  protected int minTickSize = 25, maxLineCount = Integer.MAX_VALUE;
  protected int size;
  protected boolean vertical;
  protected int[] grids;
  protected double[] gridPositions;
  protected int[] gridLines;
  protected String[] texts;
  protected int neededWidth;
  private boolean isZoomEnabled = true;
  private double zoomStep = 1.2;

  public BasicScaleAxisImpl() {
    setTitleRotation(270);
  }

  @Override
  public void clearMeasures() {
    measures = new String[0];
  }

  @Override
  public void addMeasures(String... newMeasures) {
    for (String m : newMeasures) {
      if (m != null) {
        measures = ArrayHelper.add(measures, m);
      }
    }
  }

  @Override
  public Double getMin() {
    return min;
  }

  @Override
  public void setMin(Double min) {
    this.userMin = min;
  }

  @Override
  public Double getMax() {
    return max;
  }

  @Override
  public void setMax(Double max) {
    this.userMax = max;
  }

  @Override
  public int getMinTickSize() {
    return minTickSize;
  }

  @Override
  public void setMinTickSize(int minTickSize) {
    this.minTickSize = minTickSize;
  }

  @Override
  public int getMaxLineCount() {
    return maxLineCount;
  }

  @Override
  public void setMaxLineCount(int maxLineCount) {
    this.maxLineCount = maxLineCount;
  }

  @Override
  public Double getTick() {
    return tick;
  }

  @Override
  public void setTick(Double stepSize) {
    this.tick = stepSize;
  }

  @Override
  public Double getUserTick() {
    return userTick;
  }

  @Override
  public void setUserTick(Double stepSize) {
    this.userTick = stepSize;
  }

  @Override
  public int getPosition(double v) {
    int pos = getRadius(v);
    if (vertical) {
      pos = size - pos;
    }
    return pos;
  }

  @Override
  public int getRadius(double v) {
    if (min == null || max == null) {
      return 0;
    } else {
      return (int) ((v - min) / (max - min) * size);
    }
  }

  @Override
  public void enableZoom(boolean isZoomEnabled) {
    this.isZoomEnabled = isZoomEnabled;
  }

  @Override
  public boolean isZoomEnabled() {
    return isZoomEnabled;
  }

  @Override
  public double getZoomStep() {
    return zoomStep;
  }

  @Override
  public void setZoomStep(double step) {
    this.zoomStep = step;
  }

  @Override
  public void zoom(double zoom, double center) {
    final double v = (min + (max - min) * center);
    userMin = v + (min - v) * zoom;
    userMax = v + (max - v) * zoom;

    fireMinMaxUpdate();
  }

  @Override
  public void translate(double shift) {
    userMin = min + shift;
    userMax = max + shift;

    fireMinMaxUpdate();
  }

  @Override
  public void addListener(ScaleAxisListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ScaleAxisListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void setGridPositions(double[] gridPositions) {
    this.gridPositions = gridPositions;
  }

  protected String getMeasure() {
    return measures[0];
  }

  @Override
  public int[] getGrid() {
    return grids;
  }

  @Override
  public int[] getCenteredGrid() {
    return grids;
  }

  @Override
  public int[] getGridLines() {
    if (gridLines != null) {
      return gridLines;
    }
    return grids;
  }

  @Override
  public String[] getTexts() {
    return texts;
  }

  @Override
  public int getNeededSize() {
    return neededWidth;
  }

  @Override
  public int getSize() {
    return size;
  }

  protected void adjustTicksAndBorders(int size) {
    if (Double.isNaN(min) || Double.isNaN(max)) {
      return;
    }

    int minTickSize = getMinTickSize();
    final int maxLineCount = getMaxLineCount();
    minTickSize = Math.max(minTickSize, size / maxLineCount);

    if (userTick != null) {
      tick = userTick;
      adjustMinMax();
    } else {
      // find stepsize (max 10 times, avoid endless loop)
      boolean newTick = true;
      for (int n = 0; newTick && n < 10; n++) {
        tick = findBestScale(min, max, size, minTickSize);
        newTick = adjustMinMax();
      }
    }
  }

  private boolean adjustMinMax() {
    boolean newTick = false;

    // adjust bounds
    if (userMin == null) {
      if (min % tick != 0) {
        if (min >= 0) {
          min -= min % tick;
        } else {
          min -= tick + (min % tick);
        }
        newTick = true;
      }
    }

    if (userMax == null) {
      if (max % tick != 0) {
        if (max >= 0) {
          max += tick - (max % tick);
        } else {
          // TODO
        }
        newTick = true;
      }
    }
    return newTick;
  }

  protected abstract double findBestScale(Double min, Double max, int size, int minTickSize);

  @Override
  public void render(final Renderer r, final int x, final int y,
    final int width, final int height, boolean isCentered,
    boolean flip, ChartFont font)
  {
    super.render(r, x, y, width, height, isCentered, flip, font);

    // add mouse listener for zoom
    if (isZoomEnabled) {
      r.addMouseListener(x, y, width, height,
        (ChartMouseWheelListener) (mx, my, v) -> {
          if (isVertical()) {
            doZoom(r, v, my, y, height);
          } else {
            doZoom(r, v, mx, x, width);
          }
        }
      );

      r.addMouseListener(
        x, y, width, height, (ChartMouseDragListener) (dx, dy) -> doDrag(r, dx, dy));
    }

  }

  @Override
  public boolean isVertical() {
    return vertical;
  }

  public void doZoom(final Renderer r, int wheelMotion, int x, int startX, int width) {
    try {
      final double f = (double) (x - startX) / (width);
      if (wheelMotion < 0) {
        zoom(1 / getZoomStep(), f);
      } else {
        zoom(getZoomStep(), f);
      }
      r.fireRepaint(true);
    }
    catch (ChartException ignored) {
    }
  }

  public void doDrag(Renderer r, int dx, int dy) {
    try {
      if (isVertical()) {
        final double f = (max - min) / size;
        translate(-dy * f);
      } else {
        final double f = (max - min) / size;
        translate(dx * f);
      }
      r.fireRepaint(true);
    }
    catch (Exception ignored) {
    }
  }

  private void fireMinMaxUpdate() {
    for (ScaleAxisListener l : listeners) {
      l.onTranslate(userMin, userMax);
    }
  }

}