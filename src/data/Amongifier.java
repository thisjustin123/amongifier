import java.io.*;
import java.awt.Point;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
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
    private static HashSet<Point> faceSet = new HashSet<>();
    private static HashSet<Point> greenSet = new HashSet<>();
    private static HashSet<Point> backpackSet = new HashSet<>();
    private static HashSet<Point> blueSet = new HashSet<>();
    private static HashSet<Point> extrapolatedPixels = new HashSet<>();
    private static Queue<Point> definitePixels = new ArrayDeque<>();
    private static BufferedImage pastedTemplate;
    private static boolean threeEighths = false;

    private static final boolean forceThreeEights = true;

    // Higher is better, but gets much more costly.
    private static final int pointsRectWidth = 12;
    private static final int pointsRectHeight = 12;

    private static HashSet<Point> originalGreenSet = new HashSet<>();
    private static HashSet<Point> originalFaceSet = new HashSet<>();

    private static final String INPUTFILE_STRING = "Anthony_TestCase.png";
    private static final String OUTPUTFILE_STRING = "am_noSmooth.png";

    private static final boolean FORCE_ASPECT_RATIO = false;

    public static void main(String[] args) {
        try {
            BufferedImage image = ImageIO.read(new File(INPUTFILE_STRING));
            image = format(image);
            amongify(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage format(BufferedImage faceImage) {
        faceImage = Transparentify.transparentify(faceImage);
        int desiredWidth = 570, desiredHeight = 766;

        double multFactor = 1.0;
        int newWidth = desiredWidth, newHeight = desiredHeight;
        // Image is too small in both dimensions
        if (!FORCE_ASPECT_RATIO) {

            if (faceImage.getWidth() < desiredWidth && faceImage.getHeight() < desiredHeight) {
                multFactor = Math.min(((double) (desiredWidth)) / faceImage.getWidth(),
                        ((double) (desiredHeight)) / faceImage.getHeight());
            }
            // Image is too tall
            else if (faceImage.getWidth() < desiredWidth) {
                multFactor = ((double) desiredHeight) / faceImage.getHeight();
            }
            // Image is too wide
            else if (faceImage.getHeight() < desiredHeight) {
                multFactor = ((double) desiredWidth) / faceImage.getWidth();
            }
            // Image is too big in both dimensions
            else {
                multFactor = Math.min(((double) (desiredWidth)) / faceImage.getWidth(),
                        ((double) (desiredHeight)) / faceImage.getHeight());
            }

            multFactor = Math.max(((double) (desiredWidth)) / faceImage.getWidth(),
                    ((double) (desiredHeight)) / faceImage.getHeight());

            newWidth = (int) Math.round(faceImage.getWidth() * multFactor);
            newHeight = (int) Math.round(faceImage.getHeight() * multFactor);
        }

        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        BufferedImage newImageFinal = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        Graphics2D gFinal = newImageFinal.createGraphics();
        gFinal.setBackground(new Color(0, 0, 0, 0));

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.drawImage(faceImage, 0, 0, newWidth, newHeight, 0, 0, faceImage.getWidth(),
                faceImage.getHeight(), null);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        // g.setColor(new Color(0, 0, 0, 255));
        for (int i = 0; i < newImage.getWidth(); i++) {
            for (int j = 0; j < newImage.getHeight(); j++) {
                Color faceColor = getColorAt(newImage, i, j, true);
                gFinal.setColor(faceColor);

                if (faceColor.getAlpha() == 255) {
                    gFinal.drawRect(i, j, 0, 0);
                }
            }
        }

        g.dispose();
        /*
         * try {
         * File file = new File("ChrisfaceScaled.png");
         * ImageIO.write(newImageFinal, "png", file);
         * } catch (IOException e) {
         * }
         */

        return newImageFinal;
    }

    /**
     * Takes in a properly formatted image for a face, then returns an amongified
     * version of
     * it.
     * 
     * @param faceImage The image to amongify.
     */
    public static BufferedImage amongify(BufferedImage faceImage) throws IOException {
        BufferedImage template = ImageIO.read(new File("Among_Template_BackpackTransparent.png"));
        double ratioX = .58;
        double ratioY = .38;

        Graphics2D g2d = template.createGraphics();

        Point midPixel = new Point((int) (template.getWidth() * ratioX), (int) (template.getHeight() * ratioY));

        System.out.println("Image width: " + faceImage.getWidth() + ", Image height: " + faceImage.getHeight());
        int num = 0;
        int num3 = 0;
        for (int i = 0; i < faceImage.getWidth(); i++) {
            for (int j = 0; j < faceImage.getHeight(); j++) {
                Color faceColor = getColorAt(faceImage, i, j, true);

                if (faceColor.getAlpha() != 0) {
                    num++;
                }
                if (faceColor.getAlpha() == 255) {
                    num3++;
                }
            }
        }
        System.out.println("Num non-transparent pixels: " + num + ", " + num3);
        assert num == num3;

        for (int i = 0; i < template.getWidth(); i++) {
            for (int j = 0; j < template.getHeight(); j++) {
                if (getColorAt(template, i, j, false).getGreen() == 255) {
                    greenSet.add(new Point(i, j));
                }
            }
        }

        for (int i = 0; i < template.getWidth(); i++) {
            for (int j = 0; j < template.getHeight(); j++) {
                if (getColorAt(template, i, j, false).getBlue() == 255) {
                    blueSet.add(new Point(i, j));
                }
            }
        }

        num = 0;
        int faceX = 0, faceY = 0, num2 = 0;
        for (int i = midPixel.x - faceImage.getWidth() / 2; i < midPixel.x + faceImage.getWidth() / 2; i++) {
            faceY = 0;
            for (int j = midPixel.y - faceImage.getHeight() / 2; j < midPixel.y + faceImage.getHeight() / 2; j++) {
                if (i < template.getWidth() && i >= 0 && j < template.getHeight() && j >= 0
                        && getColorAt(template, i, j, false).getGreen() == 255) {
                    Color faceColor = getColorAt(faceImage, faceX, faceY, true);

                    g2d.setColor(faceColor);
                    g2d.drawRect(i, j, 0, 0);
                    num++;

                    if (faceColor.getAlpha() != 0) {
                        faceSet.add(new Point(i, j));
                        greenSet.remove(new Point(i, j));
                    }
                } else {
                    num2++;
                }

                faceY++;
            }
            faceX++;
        }
        System.out.println("Looped from: i = " + (midPixel.x - faceImage.getWidth() / 2) + " to "
                + (midPixel.x + faceImage.getWidth() / 2));
        System.out.println("Looped from: j = " + (midPixel.y - faceImage.getHeight() / 2) + " to "
                + (midPixel.y + faceImage.getHeight() / 2));
        System.out.println("Pasted # pixels: " + num);
        System.out.println("Non-pasted # pixels: " + num2);
        System.out.println("faceY = " + faceY);

        originalGreenSet = (HashSet<Point>) greenSet.clone();
        originalFaceSet = (HashSet<Point>) faceSet.clone();

        for (Point p : greenSet) {
            remainingGreenPixels.add(p);
        }

        pastedTemplate = template;
        System.out.println("Pasting Complete.");

        extrapolate();

        for (int i = 0; i < 1; i++) {
            addNoise(.02);
            smooth(0);
            blendBorder(15);
            addNoise(.02);
        }

        g2d.dispose();

        File file = new File(OUTPUTFILE_STRING);
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
        int lastPrint = greenSet.size();
        int badLoops = 0;

        System.out.println("Starting body extrapolation with " + greenSet.size() + " total pixels");

        // Prepare definitePixels
        for (Point p : greenSet) {
            if (threeEightsOrMore(p)) {
                definitePixels.add(p);
            }
        }

        // Extrapolate green set
        while (greenSet.size() > 0) {
            Point nextPoint = definitePixels.poll();

            for (Point p : greenSet) {
                if (nextPoint != null)
                    break;

                if (numFilledPixels(p) >= 1 && badLoops >= 2 || threeEightsOrMore(p)) {
                    nextPoint = p;
                    if (threeEighths)
                        threeEighths = false;
                }
            }

            if (nextPoint != null) {
                // System.out.println("Filling in pixel (" + nextPoint.x + ", " + nextPoint.y +
                // ") with color "
                // + averageColor(nextPoint) + ". " + greenSet.size() + " pixels remaining.");

                // System.out.println(definitePixels.size() + " " + greenSet.size() +
                // nextPoint.toString());

                badLoops = 0;

                if (lastPrint - greenSet.size() >= 100000) {
                    System.out.println(greenSet.size() + " pixels remaining.");
                    lastPrint = greenSet.size();
                }
                g2d.setColor(averageColor(nextPoint));
                g2d.drawRect(nextPoint.x, nextPoint.y, 0, 0);

                faceSet.add(nextPoint);
                greenSet.remove(nextPoint);
                extrapolatedPixels.add(nextPoint);

                ArrayList<Point> attemptPoint = getAllAdjacentPoints(nextPoint);
                definitePixels.addAll(attemptPoint);
            }

            else {
                badLoops++;

                // If it reaches this point, that's fine! Just have to search for 3/8 next time.
                threeEighths = true;

                if (badLoops > 10) {
                    System.out.println("Stuck with " + greenSet.size() + " pixels remaining.");
                }
            }
        }

        // Fill in blue space
        for (int j = 0; j < pastedTemplate.getHeight(); j++) {
            int blueIndex = -1;
            for (int i = 0; i < pastedTemplate.getWidth(); i++) {
                if (blueSet.contains(new Point(i, j))) {
                    blueIndex = i;
                }
            }
            if (blueIndex >= 0) {
                // Find first face pixel, copy color over
                Color copiedColor = new Color(0, 0, 0);
                for (int i = blueIndex; i < pastedTemplate.getWidth(); i++) {
                    if (faceSet.contains(new Point(i, j))) {
                        copiedColor = getColorAt(pastedTemplate, i, j, false);
                        break;
                    }
                }
                g2d.setColor(copiedColor);
                backpackSet.add(new Point(blueIndex, j));
                blueSet.remove(new Point(blueIndex, j));
                g2d.drawRect(blueIndex, j, 0, 0);
            }
        }
        badLoops = 0;

        lastPrint = blueSet.size();

        System.out.println("Starting backpack extrapolation with " + blueSet.size() + " total pixels...");

        // Prepare definitePixels
        definitePixels.clear();
        for (Point p : greenSet) {
            if (threeEightsOrMore(p)) {
                definitePixels.add(p);
            }
        }

        // Extrapolate blue set
        while (blueSet.size() > 0) {
            Point nextPoint = definitePixels.poll();

            for (Point p : blueSet) {
                if (nextPoint != null)
                    break;

                if (numFilledPixels(p) >= 1 && badLoops >= 2 || threeEightsOrMore(p)) {
                    nextPoint = p;
                    if (threeEighths)
                        threeEighths = false;
                }
            }

            if (nextPoint != null) {
                badLoops = 0;

                if (lastPrint - blueSet.size() >= 25000) {
                    System.out.println(blueSet.size() + " pixels remaining.");
                    lastPrint = blueSet.size();
                }
                g2d.setColor(averageColor(nextPoint));
                g2d.drawRect(nextPoint.x, nextPoint.y, 0, 0);

                backpackSet.add(nextPoint);
                blueSet.remove(nextPoint);
                extrapolatedPixels.add(nextPoint);

                ArrayList<Point> attemptPoint = getAllAdjacentPoints(nextPoint);
                definitePixels.addAll(attemptPoint);
            }

            else {
                badLoops++;

                // If it reaches this point, that's fine! Just have to search for 3/8 next time.
                threeEighths = true;

                if (badLoops > 10) {
                    System.out.println("Stuck with " + greenSet.size() + " pixels remaining.");
                }
            }
        }

        g2d.dispose();
    }

    /**
     * Attempts to slightly smooth out the (previously) green area with
     * mass-weighted averages.
     * 
     * @param conservation A number from 0 to 1 dictating by how much the
     *                     mass-weighted average should be weighted. 1 means no
     *                     change, 0 means very changed.
     */
    private static void smooth(double conservation) {
        System.out.println("Smoothing with conservation " + conservation + "...");
        Graphics2D g2d = pastedTemplate.createGraphics();
        HashMap<Point, Color> colorMap = new HashMap<>();

        HashSet<Point> smoothSet = new HashSet<>();
        smoothSet.addAll(originalGreenSet);
        smoothSet.addAll(backpackSet);

        // Add face boundary pixels to smooth set. A face boundary pixel is any pixel
        // that is within [some amount] of pixels from a pixel that is immediately
        // adjacent to a green pixel, including those pixels themselves.
        for (Point p : originalGreenSet) {
            Point[] pointsOfInterest = {
                    new Point(p.x - 1, p.y),
                    new Point(p.x + 1, p.y),
                    new Point(p.x, p.y - 1),
                    new Point(p.x, p.y + 1)
            };

            for (Point potentialPoint : pointsOfInterest) {
                if (originalFaceSet.contains(potentialPoint)) {
                    // Add corresponding points to smoothSet.
                    ArrayList<Point> pointsToAdd = new ArrayList<>();
                    Collections.addAll(pointsToAdd, Helper.pointsRectangle(potentialPoint, 8, 8));
                    pointsToAdd.removeIf((aPoint) -> !faceSet.contains(aPoint));
                    smoothSet.addAll(pointsToAdd);
                    smoothSet.add(potentialPoint);
                }
            }
        }

        System.out.println("Pixels to smooth: " + smoothSet.size());
        for (Point p : smoothSet) {
            Color avg = averageColor(p);
            Color oldColor = getColorAt(pastedTemplate, p.x, p.y, false);

            Color newColor = Helper.colorInterp(avg, oldColor, conservation);

            colorMap.put(p, newColor);
        }

        for (Point p : colorMap.keySet()) {
            g2d.setColor(colorMap.get(p));
            g2d.drawRect(p.x, p.y, 0, 0);
        }
        g2d.dispose();
    }

    /**
     * Attempts to slightly smooth out the (previously) green area with
     * mass-weighted averages. Takes in a set of GOOD points, each that has its own
     * conservation.
     * 
     * @param conservation A number from 0 to 1 dictating by how much the
     *                     mass-weighted average should be weighted. 1 means no
     *                     change, 0 means very changed.
     */
    private static void smooth(Map<Point, Double> smoothMap) {
        System.out.println("Smoothing custom...");
        Graphics2D g2d = pastedTemplate.createGraphics();
        HashMap<Point, Color> colorMap = new HashMap<>();

        System.out.println("Pixels to smooth: " + smoothMap.keySet().size());
        for (Point p : smoothMap.keySet()) {
            double conservation = Helper.clamp(smoothMap.get(p), 0, 1);
            Color avg = averageColor(p);
            Color oldColor = getColorAt(pastedTemplate, p.x, p.y, false);

            Color newColor = Helper.colorInterp(avg, oldColor, conservation);

            colorMap.put(p, newColor);
        }

        for (Point p : colorMap.keySet()) {
            g2d.setColor(colorMap.get(p));
            g2d.drawRect(p.x, p.y, 0, 0);
        }
        g2d.dispose();
    }

    /**
     * Attempts to blend the border of the face.
     * 
     * @param radius The radius of the face that should be blended. Not technically
     *               a radius... but shhh.... I'm not smart enough for that...
     */
    private static void blendBorder(int radius) {
        // Point -> Conservation
        HashMap<Point, Double> smoothMap = new HashMap<>();

        for (Point p : originalGreenSet) {
            Point[] pointsOfInterest = {
                    new Point(p.x - 1, p.y),
                    new Point(p.x + 1, p.y),
                    new Point(p.x, p.y - 1),
                    new Point(p.x, p.y + 1)
            };

            for (Point potentialPoint : pointsOfInterest) {
                // NOTE: potentialPoint should only ever be a point DIRECTLY on the face border.
                if (originalFaceSet.contains(potentialPoint)) {
                    // Add corresponding points to smoothMap.
                    ArrayList<Point> pointsToAdd = new ArrayList<>();
                    Collections.addAll(pointsToAdd,
                            Helper.pointsRectangle(potentialPoint, radius, radius));
                    pointsToAdd.removeIf((aPoint) -> !faceSet.contains(aPoint));

                    for (Point foundPoint : pointsToAdd) {
                        double conservation = Math.sqrt(
                                Math.pow(foundPoint.getX() - p.x, 2) + Math.pow(foundPoint.getY() - p.y, 2)) / radius;
                        Double currentConservation = smoothMap.get(foundPoint);
                        if (currentConservation == null)
                            smoothMap.put(foundPoint, conservation);
                        else if (conservation < currentConservation)
                            smoothMap.put(foundPoint, conservation);
                    }
                    smoothMap.put(potentialPoint, 0.0);
                }
            }
        }

        extrapolatedPixels.addAll(smoothMap.keySet());

        smooth(smoothMap);
    }

    private static void addNoise(double degree) {
        System.out.println("Adding noise...");

        double[][] noiseMatrix = new double[pastedTemplate.getWidth()][pastedTemplate.getHeight()];
        OpenSimplexNoise noise = new OpenSimplexNoise();
        Graphics2D g2d = pastedTemplate.createGraphics();

        for (int y = 0; y < pastedTemplate.getHeight(); y++) {
            for (int x = 0; x < pastedTemplate.getWidth(); x++) {
                noiseMatrix[x][y] = ((noise.eval(x / (pastedTemplate.getHeight() / 400),
                        y / (pastedTemplate.getHeight() / 400), 0.0)) + 1) / 2;
            }
        }

        /*
         * long red = 0;
         * long green = 0;
         * long blue = 0;
         * int colors = 0;
         * for (Point p : originalFaceSet) {
         * Color c = getColorAt(pastedTemplate, p.x, p.y, false);
         * red += c.getRed();
         * green += c.getGreen();
         * blue += c.getBlue();
         * colors++;
         * }
         * Color averageColor = new Color((int) (red / colors), (int) (green / colors),
         * (int) (blue / colors));
         */
        Color averageColor = new Color(255, 255, 255);

        HashSet<Point> noiseSet = new HashSet<Point>();
        noiseSet.addAll(extrapolatedPixels);

        for (Point p : noiseSet) {

            Color oldColor = getColorAt(pastedTemplate, p.x, p.y, false);
            int r = Helper.clamp((int) (Math.round(oldColor.getRed() * (1 - degree))
                    + Math.round(averageColor.getRed() * degree * noiseMatrix[p.x][p.y])), 0, 255);
            int g = Helper.clamp((int) (Math.round(oldColor.getGreen() * (1 - degree))
                    + Math.round(averageColor.getGreen() * degree * noiseMatrix[p.x][p.y])), 0, 255);
            int b = Helper.clamp((int) (Math.round(oldColor.getBlue() * (1 - degree))
                    + Math.round(averageColor.getBlue() * degree * noiseMatrix[p.x][p.y])), 0, 255);

            Color weightedColor = new Color(r, g, b);
            g2d.setColor(weightedColor);
            g2d.drawRect(p.x, p.y, 0, 0);
        }

        g2d.dispose();
    }

    /**
     * Returns the number of adjacent filled pixels in the 4 immediately adjacent
     * pixels.
     * 
     * @param p
     * @return
     */
    private static int numFilledPixels(Point p) {
        Point[] pointsOfInterest = {
                new Point(p.x - 1, p.y),
                new Point(p.x + 1, p.y),
                new Point(p.x, p.y - 1),
                new Point(p.x, p.y + 1)
        };

        int num = 0;
        for (Point point : pointsOfInterest) {
            if (faceSet.contains(point) || backpackSet.contains(point))
                num++;
        }
        return num;
    }

    /**
     * Returns the average color of the surrounding tiles.
     * 
     * @param p The point to get an average color around
     * @return A color object--the average color.
     */
    private static Color averageColor(Point p) {
        ArrayList<Color> colors = new ArrayList<Color>();
        Point[] pointsOfInterest = getPointsOfInterest(p);
        for (Point point : pointsOfInterest) {
            if (faceSet.contains(point) || backpackSet.contains(point)) {
                colors.add(getColorAt(pastedTemplate, point.x, point.y, false));
            }
        }

        assert colors.size() > 0;
        Color averageColor;
        double red = 0;
        double green = 0;
        double blue = 0;
        for (Color c : colors) {
            red += c.getRed();
            green += c.getGreen();
            blue += c.getBlue();
        }
        averageColor = new Color(
                (int) Math.round(red / colors.size()),
                (int) Math.round(green / colors.size()),
                (int) Math.round(blue / colors.size()));
        return averageColor;
    }

    /**
     * Attempts to find new working points around the input p from the tiles
     * immediately north, east, south, and west.
     * (Order: West -> East -> South -> North)
     * 
     * @param p The point whose adjacent points to check.
     * @return A list of points corresponding to points found, null if no point is
     *         found.
     */
    private static ArrayList<Point> getAllAdjacentPoints(Point p) {
        Point[] pointsOfInterest = {
                new Point(p.x - 1, p.y - 1),
                new Point(p.x + 1, p.y - 1),
                new Point(p.x - 1, p.y + 1),
                new Point(p.x + 1, p.y + 1),
                new Point(p.x - 1, p.y),
                new Point(p.x + 1, p.y),
                new Point(p.x, p.y - 1),
                new Point(p.x, p.y + 1)
        };

        ArrayList<Point> goodPoints = new ArrayList<>();
        for (Point point : pointsOfInterest) {
            if ((greenSet.contains(point) || blueSet.contains(point)) && threeEightsOrMore(p)
                    && !definitePixels.contains(point)) {
                goodPoints.add(point);
            }
        }
        return goodPoints;
    }

    private static Point[] getPointsOfInterest(Point p) {
        Point[] normal = {
                new Point(p.x - 1, p.y),
                new Point(p.x + 1, p.y),
                new Point(p.x, p.y - 1),
                new Point(p.x, p.y + 1)
        };
        Point[] more = {
                new Point(p.x - 1, p.y - 1),
                new Point(p.x + 1, p.y - 1),
                new Point(p.x - 1, p.y + 1),
                new Point(p.x + 1, p.y + 1),
                new Point(p.x - 1, p.y),
                new Point(p.x + 1, p.y),
                new Point(p.x, p.y - 1),
                new Point(p.x, p.y + 1)
        };
        Point[] evenMore = Helper.pointsRectangle(p, pointsRectWidth, pointsRectHeight);

        return evenMore;// forceThreeEights || threeEighths ? more : normal;
    }

    private static int minPointCount() {
        return forceThreeEights || threeEighths ? 3 : 2;
    }

    private static boolean threeEightsOrMore(Point p) {
        Point[] pointsOfInterest = {
                new Point(p.x - 1, p.y - 1),
                new Point(p.x + 1, p.y - 1),
                new Point(p.x - 1, p.y + 1),
                new Point(p.x + 1, p.y + 1),
                new Point(p.x - 1, p.y),
                new Point(p.x + 1, p.y),
                new Point(p.x, p.y - 1),
                new Point(p.x, p.y + 1)
        };

        int num = 0;
        for (Point point : pointsOfInterest) {
            if (faceSet.contains(point) || backpackSet.contains(point))
                num++;
        }
        return num >= 3;
    }
}
