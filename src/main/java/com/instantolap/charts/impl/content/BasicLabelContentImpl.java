package com.instantolap.charts.impl.content;

import com.instantolap.charts.LabelContent;
import com.instantolap.charts.renderer.ChartColor;
import com.instantolap.charts.renderer.ChartFont;


public abstract class BasicLabelContentImpl extends BasicContentImpl implements LabelContent {

  private String text;
  private int x, y;
  private ChartFont font;
  private ChartColor color;

  @Override
  public String getText() {
    return text;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }

  @Override
  public int getX() {
    return x;
  }

  @Override
  public void setX(int x) {
    this.x = x;
  }

  @Override
  public int getY() {
    return y;
  }

  @Override
  public void setY(int y) {
    this.y = y;
  }

  @Override
  public ChartFont getFont() {
    return font;
  }

  @Override
  public void setFont(ChartFont font) {
    this.font = font;
  }

  @Override
  public ChartColor getColor() {
    return color;
  }

  @Override
  public void setColor(ChartColor color) {
    this.color = color;
  }
}