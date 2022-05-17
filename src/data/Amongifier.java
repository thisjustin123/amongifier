import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;

public class Amongifier {

    /**
     * @deprecated
     */
    private static LinkedList<Point> remainingGreenPixels = new LinkedList<>();

    /**
     * If a pixel is in this set, it is a part of the face. Does NOT include border,
     * for example.
     */
    private static HashSet<Point> facePixels = new HashSet<>();
    private static HashSet<Point> remainingSet = new HashSet<>();
    private static BufferedImage pastedTemplate;
    private static BufferedImage face;

    public static void main(String[] args) {
        try {
            BufferedImage image = ImageIO.read(new File("Anthony_FinalTestCase.png"));
            amongify(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes in a properly formatted image for a face, then returns an amongified
     * version of
     * it.
     * 
     * @param faceImage The image to amongify.
     */
    public static BufferedImage amongify(BufferedImage faceImage) throws IOException {
        face = faceImage;
        BufferedImage template = ImageIO.read(new File("Among_Template_Backpack.png"));
        double ratioX = .58;
        double ratioY = .38;

        Graphics2D g2d = template.createGraphics();

        Point midPixel = new Point((int) (template.getWidth() * ratioX), (int) (template.getHeight() * ratioY));

        System.out.println(midPixel.y - faceImage.getHeight() / 2);
        System.out.println(midPixel.y + faceImage.getHeight() / 2);

        for (int i = 0; i < template.getWidth(); i++) {
            for (int j = 0; j < template.getHeight(); j++) {
                if (getColorAt(template, i, j, false).getGreen() == 255) {
                    remainingSet.add(new Point(i, j));
                }
            }
        }

        int faceX = 0, faceY = 0;
        for (int i = midPixel.x - faceImage.getWidth() / 2; i < midPixel.x + faceImage.getWidth() / 2; i++) {
            faceY = 0;
            for (int j = midPixel.y - faceImage.getHeight() / 2; j < midPixel.y + faceImage.getHeight() / 2; j++) {
                if (getColorAt(template, i, j, false).getGreen() == 255) {
                    Color faceColor = getColorAt(faceImage, faceX, faceY, true);

                    g2d.setColor(faceColor);
                    g2d.drawRect(i, j, 1, 1);

                    if (faceColor.getAlpha() != 0) {
                        facePixels.add(new Point(i, j));
                        remainingSet.remove(new Point(i, j));
                    }
                }

                faceY++;
            }
            faceX++;
        }

        for (Point p : remainingSet) {
            remainingGreenPixels.add(p);
        }

        pastedTemplate = template;
        extrapolate();

        g2d.dispose();

        File file = new File("testAmongify.png");
        ImageIO.write(template, "png", file);

        return template;
    }

    /**
     * Returns the color of the image at a specified pixel.
     * 
     * @param image
     * @param x
     * @param y
     * @return The color at the pixel (x,y)
     */
    private static Color getColorAt(BufferedImage image, int x, int y, boolean useAlpha) {
        int clr = image.getRGB(x, y);

        return new Color(clr, useAlpha);
    }

    /**
     * Fills in the remaining green space of the image.
     * 
     * @param image
     */
    private static void extrapolate() {
        Graphics2D g2d = pastedTemplate.createGraphics();
        int lastPrint = remainingSet.size();
        while (remainingSet.size() > 0) {
            Point nextPoint = null;
            for (Point p : remainingSet) {
                if (numFilledPixels(p) >= 2) {
                    nextPoint = p;
                    break;
                }
            }

            if (nextPoint != null) {
                //System.out.println("Filling in pixel (" + nextPoint.x + ", " + nextPoint.y + ") with color "
                //        + averageColor(nextPoint) + ". " + remainingSet.size() + " pixels remaining.");

                if (lastPrint - remainingSet.size() >= 1000) {
                    System.out.println(remainingSet.size() + " pixels remaining.");
                    lastPrint = remainingSet.size();
                }
                g2d.setColor(averageColor(nextPoint));
                g2d.drawRect(nextPoint.x, nextPoint.y, 1, 1);

                facePixels.add(nextPoint);
                remainingSet.remove(nextPoint);
            }

            else {
                System.out.println("Bad :(");
            }
        }
    }

    private static int numFilledPixels(Point p) {
        int num = 0;
        if (facePixels.contains(new Point(p.x, p.y - 1))) {
            num++;
        }
        if (facePixels.contains(new Point(p.x, p.y + 1))) {
            num++;
        }
        if (facePixels.contains(new Point(p.x - 1, p.y))) {
            num++;
        }
        if (facePixels.contains(new Point(p.x + 1, p.y))) {
            num++;
        }
        return num;
    }

    private static Color averageColor(Point p) {
        ArrayList<Color> colors = new ArrayList<Color>();
        if (facePixels.contains(new Point(p.x, p.y - 1))) {
            colors.add(getColorAt(pastedTemplate, p.x, p.y - 1, false));
        }
        if (facePixels.contains(new Point(p.x, p.y + 1))) {
            colors.add(getColorAt(pastedTemplate, p.x, p.y + 1, false));
        }
        if (facePixels.contains(new Point(p.x - 1, p.y))) {
            colors.add(getColorAt(pastedTemplate, p.x - 1, p.y, false));
        }
        if (facePixels.contains(new Point(p.x + 1, p.y))) {
            colors.add(getColorAt(pastedTemplate, p.x + 1, p.y, false));
        }
        assert colors.size() > 0;
        Color averageColor;
        int red = 0;
        int green = 0;
        int blue = 0;
        for (Color c : colors) {
            red += c.getRed();
            green += c.getGreen();
            blue += c.getBlue();
        }
        averageColor = new Color(red / colors.size(), green / colors.size(), blue / colors.size());
        return averageColor;
    }
}
