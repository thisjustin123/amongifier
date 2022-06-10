package com.thisjustin.amongifier;

import java.io.*;
import java.awt.Point;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;

import java.util.*;

public class Amongifier {

    /**
     * 0 = Starting
     * 1 = Cutting out face
     * 2 = Formatting image
     * 3 = Pasting image
     * 4 = Extrapolation
     * 5 = Noise 1
     * 6 = Smooth
     * 7 = Border Blend
     * 8 = Noise 2
     */
    private int stage = 0;
    private double progress = 0;

    /**
     * @deprecated
     */
    private LinkedList<Point> remainingGreenPixels = new LinkedList<>();

    /**
     * If a pixel is in this set, it is a part of the face. Does NOT include border,
     * for example.
     */
    private HashSet<Point> faceSet = new HashSet<>();
    private HashSet<Point> greenSet = new HashSet<>();
    private HashSet<Point> backpackSet = new HashSet<>();
    private HashSet<Point> blueSet = new HashSet<>();
    private HashSet<Point> extrapolatedPixels = new HashSet<>();
    private Queue<Point> definitePixels = new ArrayDeque<>();
    private BufferedImage pastedTemplate;
    private boolean threeEighths = false;

    // Higher is better, but gets much more costly. Good number is 12.
    private static int pointsRectWidth = 12;
    private static int pointsRectHeight = 12;

    // Higher is better, but gets much more costly. Good number is 15.
    private static int borderBlendRadius = 15;

    private HashSet<Point> originalGreenSet = new HashSet<>();
    private HashSet<Point> originalFaceSet = new HashSet<>();

    private static final String INPUTFILE_STRING = "data/susmine.jpg";
    private static final String OUTPUTFILE_STRING = "data/chris_amongified.png";

    private static boolean FORCE_ASPECT_RATIO = false;

    /** Scales to the cut out, properly formatted image, not the raw one. */
    private static double midPointX = .5, midPointY = .5;;

    public Amongifier() {

    }

    public Amongifier(int pointsRectRadius, int bbRad, boolean forceAspectRatio, double midx, double midy) {
        pointsRectWidth = pointsRectRadius;
        pointsRectHeight = pointsRectRadius;
        borderBlendRadius = bbRad;
        FORCE_ASPECT_RATIO = forceAspectRatio;
        midPointX = midx;
        midPointY = midy;
    }

    public static void main(String[] args) {
        Amongifier a = new Amongifier();
        try {
            BufferedImage image = ImageIO.read(new File(INPUTFILE_STRING));
            /*Point[] points = {
                new Point(1355, 822),
                new Point( 997, 1034),
                new Point( 1001, 1924),
                new Point( 1349, 2053),
                new Point( 1741, 1800),
                new Point( 1657, 1014),
            };
            File file = new File("data/susmine_cutout.png");
            ImageIO.write(a.cutOutFace(image, points), "png", file);*/

            image = a.format(image);
            a.amongify(image);
            File file = new File(OUTPUTFILE_STRING);
            ImageIO.write(a.pastedTemplate, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {

        }
    }

    /**
     * Can be called at any time to get the status of the current Amongifier
     * operations. Outputs are formatted as follows: [Stage]|[Progress Percent]
     */
    public String getStatus() {
        return "" + stage + "|" + (int) (Math.round(progress * 100));
    }

    /**
     * Advances the publicly visible stage number for this amongifier and sets its
     * progress to 0.
     */
    private void nextStage() {
        stage++;
        progress = 0;
    }

    /**
     * Takes in an array of face points and an image with a face to cut out.
     * 
     * @param wholeImage
     * @param facePoints
     * @return wholeImage with its face cut out.
     */
    public BufferedImage cutOutFace(BufferedImage wholeImage, Point[] facePoints) {
        nextStage();
        if (facePoints.length == 0) {
            return wholeImage;
        }

        Point minXPoint = facePoints[0];
        Point minYPoint = facePoints[0];
        Point maxXPoint = facePoints[0];
        Point maxYPoint = facePoints[0];
        for (Point p : facePoints) {
            if (p.x < minXPoint.x)
                minXPoint = p;
            if (p.y < minYPoint.y)
                minYPoint = p;
            if (p.x > maxXPoint.x)
                maxXPoint = p;
            if (p.y > maxYPoint.y)
                maxYPoint = p;
        }
        int faceStartX = minXPoint.x, faceEndX = maxXPoint.x;
        int faceStartY = minYPoint.y, faceEndY = maxYPoint.y;
        wholeImage = wholeImage.getSubimage(faceStartX, faceStartY, faceEndX-faceStartX+1, faceEndY-faceStartY+1);
        for (int i = 0; i < facePoints.length; i++) {
            Point p = facePoints[i];
            p.x = p.x - faceStartX;
            p.y = p.y - faceStartY;
        }

        // An image with just large enough dimensions to contain the face bordered in black that is purely white otherwise.
        BufferedImage whiteImage = new BufferedImage(maxXPoint.x-minXPoint.x+1, maxYPoint.y-minYPoint.y+1,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D whiteG = whiteImage.createGraphics();

        whiteG.setColor(Color.WHITE);
        whiteG.fillRect(0, 0, whiteImage.getWidth(), whiteImage.getHeight());

        // Draw the black border
        whiteG.setColor(Color.BLACK);
        whiteG.setStroke(new BasicStroke(2));
        for (int i = 1; i < facePoints.length; i++) {
            Point prevPoint = facePoints[i - 1];
            Point thisPoint = facePoints[i];

            whiteG.drawLine(prevPoint.x, prevPoint.y, thisPoint.x, thisPoint.y);
        }
        whiteG.drawLine(facePoints[facePoints.length - 1].x, facePoints[facePoints.length - 1].y, facePoints[0].x,
                facePoints[0].y);
        // Postcondition: whiteImage now contains the face bordered in black.
        progress = .33;

        // Fill the non-face region of whiteImage with some dumb color, let's say BLUE.
        whiteG.setColor(Color.BLUE);
        /**
         * Contains both checked and unchecked points. This means you have to check
         * AFTER adding.
         */
        ArrayDeque<Point> pointsToCheck = new ArrayDeque<>();
        HashSet<Point> checkedPoints = new HashSet<>();
        HashSet<Point> outsideFacePoints = new HashSet<>();
        HashSet<Point> outermostBorderPoints = new HashSet<>();
        // Add all border points to pointsToCheck
        for (int i = 0; i < whiteImage.getWidth(); i++) {
            pointsToCheck.add(new Point(i, 0));
            pointsToCheck.add(new Point(i, whiteImage.getHeight() - 1));
        }
        for (int j = 1; j < whiteImage.getHeight() - 1; j++) {
            pointsToCheck.add(new Point(0, j));
            pointsToCheck.add(new Point(whiteImage.getWidth() - 1, j));
        }
        while (pointsToCheck.size() > 0) {
            Point p = pointsToCheck.pop();
            if (!checkedPoints.contains(p)) {
                if (!getColorAt(whiteImage, p.x, p.y, false).equals(Color.BLACK)) {
                    outsideFacePoints.add(p);
                    List<Point> nextPoints = Helper.adjacentPoints(p, whiteImage.getWidth(), whiteImage.getHeight());
                    pointsToCheck.addAll(nextPoints);
                } else {
                    outermostBorderPoints.add(p);
                }
            }

            checkedPoints.add(p);
        }
        // Postcondition: outsideFacePoints now contains all points that are outside the
        // face. and outermostBorderPoints contains all the... outermost face border
        // points.
        progress = .67;

        for (Point p : outsideFacePoints) {
            whiteG.fillRect(p.x, p.y, 0, 0);
        }

        Object[] tempArray = outermostBorderPoints.toArray();
        ArrayList<Point> borderPointsList = new ArrayList<>();
        for (Object o : tempArray) {
            borderPointsList.add((Point) o);
        }
        int newWidth = maxXPoint.x - minXPoint.x + 1;
        int newHeight = maxYPoint.y - minYPoint.y + 1;

        BufferedImage finalImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D finalImageG = finalImage.createGraphics();
        finalImageG.setBackground(new Color(144, 23, 4, 0));
        finalImageG.clearRect(0, 0, finalImage.getWidth(), finalImage.getHeight());
        int translatedI = 0, translatedJ = 0;
        for (int i = minXPoint.x; i <= maxXPoint.x; i++) {
            translatedJ = 0;
            for (int j = minYPoint.y; j <= maxYPoint.y; j++) {
                // If this point is part of the face...
                if (!outsideFacePoints.contains(new Point(i, j))) {
                    Color c = getColorAt(wholeImage, i, j, false);
                    finalImageG.setColor(c);
                    finalImageG.drawRect(translatedI, translatedJ, 0, 0);
                } else {

                }

                translatedJ++;
            }
            translatedI++;
        }
        progress = 1;
        return finalImage;
    }

    /**
     * Stretches the input image to a good size for the amongifier template.
     * If FORCE_ASPECT_RATIO is true, the image's aspect ratio will be ignored, and
     * this method will instead stretch the image to a tried-and-tested good aspect
     * ratio for the program.
     * 
     * @param faceImage
     * @return
     */
    public BufferedImage format(BufferedImage faceImage) {
        nextStage();
        faceImage = Transparentify.transparentify(faceImage);
        int desiredWidth = 570, desiredHeight = 766;

        double multFactor = 1.0;
        int newWidth = desiredWidth, newHeight = desiredHeight;
        if (!FORCE_ASPECT_RATIO) {
            // Image is too small in both dimensions
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

        for (int i = 0; i < newImage.getWidth(); i++) {
            for (int j = 0; j < newImage.getHeight(); j++) {
                Color faceColor = getColorAt(newImage, i, j, true);
                gFinal.setColor(faceColor);
                progress = (newImage.getHeight() * i + j) / (newImage.getWidth() * newImage.getHeight());

                if (faceColor.getAlpha() == 255) {
                    gFinal.drawRect(i, j, 0, 0);
                }
            }
        }

        g.dispose();

        return newImageFinal;
    }

    /**
     * Takes in a properly formatted image for a face, then returns an amongified
     * version of it.
     * 
     * @param faceImage The image to amongify.
     * @throws Exception
     */
    public BufferedImage amongify(BufferedImage faceImage) throws Exception {
        nextStage();
        BufferedImage template = ImageIO.read(new File("data/Among_Template_BackpackTransparent.png"));
        double ratioX = .58;
        double ratioY = .38;

        Graphics2D g2d = template.createGraphics();

        Point midPixel = new Point(
                (int) (template.getWidth() * ratioX)
                        - Math.toIntExact(Math.round((midPointX - .5) * faceImage.getWidth())),
                (int) (template.getHeight() * ratioY)
                        - Math.toIntExact(Math.round((midPointY - .5) * faceImage.getHeight())));

        System.out.println("Mid pixel: " + midPixel.toString());

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
        System.out.println("Num non-transparent pixels: " + num + ", " + num3 + " (Numbers should be the same!)");
        assert num == num3;

        progress = .33;

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

        progress = .67;

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
        progress = 1;
        System.out.println("Pasted # pixels: " + num);
        System.out.println("Non-pasted # pixels: " + num2);

        originalGreenSet = new HashSet<Point>(greenSet);
        originalFaceSet = new HashSet<Point>(faceSet);

        for (Point p : greenSet) {
            remainingGreenPixels.add(p);
        }

        pastedTemplate = template;

        System.out.println("Pasting Complete.");

        nextStage();
        extrapolate();

        for (int i = 0; i < 1; i++) {
            nextStage();
            addNoise(.02);
            nextStage();
            smooth(.1);
            nextStage();
            blendBorder(borderBlendRadius);
            nextStage();
            addNoise(.02);
        }

        g2d.dispose();

        System.out.println("Done! Sending file back.");

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
    private Color getColorAt(BufferedImage image, int x, int y, boolean useAlpha) {
        int clr = image.getRGB(x, y);

        return new Color(clr, useAlpha);
    }

    /**
     * Fills in the remaining green space of the image.
     * 
     * @param image
     * @throws Exception
     */
    private void extrapolate() throws Exception {
        Graphics2D g2d = pastedTemplate.createGraphics();
        int lastPrint = greenSet.size();
        int initialSize = greenSet.size();
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
                progress = (1 - (((double) greenSet.size()) / initialSize)) / 2;

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
                    throw new Exception(
                            "The amongifier got stuck. Something may have caused the image to paste poorly or incorrectly.");
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
        initialSize = blueSet.size();
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
                progress = ((1 - (((double) blueSet.size()) / initialSize)) / 2) + .5;

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
                    throw new Exception(
                            "The amongifier got stuck. Something may have caused the image to paste poorly or incorrectly.");
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
    private void smooth(double conservation) {
        System.out.println("Smoothing with conservation " + conservation + "...");
        Graphics2D g2d = pastedTemplate.createGraphics();
        HashMap<Point, Color> colorMap = new HashMap<>();

        HashSet<Point> smoothSet = new HashSet<>();
        smoothSet.addAll(originalGreenSet);
        smoothSet.addAll(backpackSet);

        // Add face boundary pixels to smooth set. A face boundary pixel is any pixel
        // that is within [some amount] of pixels from a pixel that is immediately
        // adjacent to a green pixel, including those pixels themselves.
        /*
         * for (Point p : originalGreenSet) {
         * Point[] pointsOfInterest = {
         * new Point(p.x - 1, p.y),
         * new Point(p.x + 1, p.y),
         * new Point(p.x, p.y - 1),
         * new Point(p.x, p.y + 1)
         * };
         * 
         * for (Point potentialPoint : pointsOfInterest) {
         * if (originalFaceSet.contains(potentialPoint)) {
         * // Add corresponding points to smoothSet.
         * ArrayList<Point> pointsToAdd = new ArrayList<>();
         * Collections.addAll(pointsToAdd, Helper.pointsRectangle(potentialPoint, 8,
         * 8));
         * pointsToAdd.removeIf((aPoint) -> !faceSet.contains(aPoint));
         * smoothSet.addAll(pointsToAdd);
         * smoothSet.add(potentialPoint);
         * }
         * }
         * }
         */

        System.out.println("Pixels to smooth: " + smoothSet.size());
        int count = 0;
        for (Point p : smoothSet) {
            Color avg = averageColor(p);
            Color oldColor = getColorAt(pastedTemplate, p.x, p.y, false);

            Color newColor = Helper.colorInterp(avg, oldColor, conservation);

            colorMap.put(p, newColor);
            count++;
            progress = (count / 2.0) / smoothSet.size();
        }
        count = 0;
        int size = colorMap.keySet().size();
        for (Point p : colorMap.keySet()) {
            g2d.setColor(colorMap.get(p));
            g2d.drawRect(p.x, p.y, 0, 0);

            count++;
            progress = (count / 2.0) / size + .5;
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
    private void smooth(Map<Point, Double> smoothMap) {
        System.out.println("Smoothing custom...");
        Graphics2D g2d = pastedTemplate.createGraphics();
        HashMap<Point, Color> colorMap = new HashMap<>();

        System.out.println("Pixels to smooth: " + smoothMap.keySet().size());
        int count = 0;
        int size = smoothMap.keySet().size();
        for (Point p : smoothMap.keySet()) {
            double conservation = Helper.clamp(smoothMap.get(p), 0, 1);
            Color avg = averageColor(p);
            Color oldColor = getColorAt(pastedTemplate, p.x, p.y, false);

            Color newColor = Helper.colorInterp(avg, oldColor, conservation);

            colorMap.put(p, newColor);
            count++;
            progress = (count / 2.0) / size;
        }

        count = 0;
        size = colorMap.keySet().size();
        for (Point p : colorMap.keySet()) {
            g2d.setColor(colorMap.get(p));
            g2d.drawRect(p.x, p.y, 0, 0);

            progress = (count / 2.0) / size + .5;
        }
        g2d.dispose();
    }

    /**
     * Attempts to blend the border of the face.
     * 
     * @param radius The radius of the face that should be blended. Not technically
     *               a radius... but shhh.... I'm not smart enough for that...
     */
    private void blendBorder(int radius) {
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

    private void addNoise(double degree) {
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
        progress = .5;

        HashSet<Point> noiseSet = new HashSet<Point>();
        noiseSet.addAll(extrapolatedPixels);

        int count = 0;
        for (Point p : noiseSet) {

            Color oldColor = getColorAt(pastedTemplate, p.x, p.y, false);
            Color noiseColor = new Color((float) noiseMatrix[p.x][p.y], (float) noiseMatrix[p.x][p.y],
                    (float) noiseMatrix[p.x][p.y]);

            Color weightedColor = Helper.colorInterp(oldColor, noiseColor, degree);
            g2d.setColor(weightedColor);
            g2d.drawRect(p.x, p.y, 0, 0);
            count++;
            progress = .5 + ((double) count) / noiseSet.size() / 2.0;
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
    private int numFilledPixels(Point p) {
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
    private Color averageColor(Point p) {
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
    private ArrayList<Point> getAllAdjacentPoints(Point p) {
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

    private Point[] getPointsOfInterest(Point p) {
        Point[] evenMore = Helper.pointsRectangle(p, pointsRectWidth, pointsRectHeight);

        return evenMore;
    }

    private boolean threeEightsOrMore(Point p) {
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
