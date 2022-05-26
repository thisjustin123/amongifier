import java.awt.Point;
import java.awt.Color;

public class Helper {
    public static int clamp(int num, int min, int max) {
        return (num <= max && num >= min) ? num : ((num <= max) ? min : max);
    }

    public static double clamp(double num, double min, double max) {
        return (num <= max && num >= min) ? num : ((num <= max) ? min : max);
    }

    /**
     * Returns an array of points corresponding those contained within the
     * rectangle centered at the given point.
     * 
     * @param point
     * @param deltaX
     * @param deltaY
     * @return
     */
    public static Point[] pointsRectangle(Point p, int deltaX, int deltaY) {
        int minY = p.y - deltaY;
        int maxY = p.y + deltaY;
        int minX = p.x - deltaX;
        int maxX = p.x + deltaX;

        Point[] points = new Point[(deltaX*2+1)*(deltaY*2+1)-1];
        int ind = 0;

        for (int j = minY; j <= maxY; j++) {
            for (int i = minX; i <= maxX; i++) {
                if (!(i==p.x && j==p.y)) {
                    points[ind] = new Point(i,j);
                    ind++;
                }
            }
        }

        return points;
    }

    public static Color colorInterp(Color c1, Color c2, double fraction) {
        float fracFloat = (float) fraction;

        float[] hsv1 = new float[3];
        Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
        float[] hsv2 = new float[3];
        Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);

        float[] newHSV = {
            hsv1[0] * (1-fracFloat) + hsv2[0] * fracFloat,
            hsv1[1] * (1-fracFloat) + hsv2[1] * fracFloat,
            hsv1[2] * (1-fracFloat) + hsv2[2] * fracFloat
        };

        return Color.getHSBColor(newHSV[0], newHSV[1], newHSV[2]);
    }

}
