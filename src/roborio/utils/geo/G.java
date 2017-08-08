package roborio.utils.geo;

import roborio.utils.colors.Gradient;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public class G {
    private static final Gradient   DANGER_GRADIENT;
    private static final Gradient   SAFE_GRADIENT;

    static {
        DANGER_GRADIENT = new Gradient(new Gradient.GradientColor[]{
                new Gradient.GradientColor(Color.WHITE, 0),
                new Gradient.GradientColor(Color.YELLOW, 0.5),
                new Gradient.GradientColor(Color.RED, 1)
        });

        SAFE_GRADIENT = new Gradient(new Gradient.GradientColor[]{
                new Gradient.GradientColor(Color.GREEN, 0),
                new Gradient.GradientColor(Color.YELLOW, 0.5),
                new Gradient.GradientColor(Color.RED, 1)
        });
    }

    private Graphics2D g;
    private LinkedList<Color> colorStack;

    public G(Graphics2D g) {
        this.g = g;
        colorStack = new LinkedList<Color>();
        colorStack.add(g.getColor());
    }

    public void setColor(Color color) {
        colorStack.clear();
        colorStack.push(color);
        g.setColor(color);
    }

    public void pushColor(Color color) {
        colorStack.push(color);
        g.setColor(color);
    }

    public void popColor() {
        if(colorStack.size() <= 1)
            throw new IllegalStateException();

        colorStack.pop();
        g.setColor(colorStack.peek());
    }

    public Shape getCircleShape(Point center, double radius) {
        return new Ellipse2D.Double(center.x - radius, center.y - radius, radius*2, radius*2);
    }

    public void drawCircle(Point center, double radius) {
        g.draw(getCircleShape(center, radius));
    }

    public void drawCircle(Point center, double radius, Color color) {
        pushColor(color);
        drawCircle(center, radius);
        popColor();
    }

    public Shape getRectShape(Point pivot, double sizeX, double sizeY) {
        return new Rectangle2D.Double(pivot.x, pivot.y, sizeX, sizeY);
    }

    public Shape getSquareShape(Point pivot, double size) {
        return getRectShape(pivot, size, size);
    }

    public Shape getCenteredSquareShape(Point center, double size) {
        return getSquareShape(new Point(center.x - size * 0.5, center.y - size * 0.5), size);
    }

    public void drawPoint(Point point, double width) {
        g.draw(getCenteredSquareShape(point, width));
    }

    public void drawPoint(Point point, double width, Color color) {
        pushColor(color);
        drawPoint(point, width);
        popColor();
    }

    public Shape getLine(Point a, Point b) {
        Line2D.Double line = new Line2D.Double(a.to2D(), b.to2D());
        return line;
    }

    public void drawLine(Point a, Point b) {
        g.draw(getLine(a, b));
    }

    public void drawLine(Point a, Point b, Color color) {
        pushColor(color);
        drawLine(a, b);
        popColor();
    }

    public static Color getDangerColor(double alpha) {
        return DANGER_GRADIENT.evaluate(alpha);
    }

    public static Color getSafeColor(double alpha) { return SAFE_GRADIENT.evaluate(alpha); }

    public void drawString(Point p, String s) {
        g.drawString(s, (float) p.x, (float) p.y);
    }
}
