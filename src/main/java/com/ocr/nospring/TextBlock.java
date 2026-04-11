package com.ocr.nospring;

/**
 * OCR 辨識結果的文字區塊
 */
public class TextBlock {
    private String text;
    private double x;
    private double y;
    private double width;
    private double height;
    private double confidence;
    private float fontSize;

    public TextBlock() {}

    public TextBlock(String text, double x, double y, double width, double height, double confidence, float fontSize) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = confidence;
        this.fontSize = fontSize;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }
}
