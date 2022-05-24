import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;

public class Transparentify {

    private static final String INPUTFILE_STRING = "Among_Template_BackpackWhite.png";
    private static final String OUTPUTFILE_STRING = "Among_Template_Transparent.png";

    public static void main(String[] args) {
        try {
            BufferedImage image = ImageIO.read(new File(INPUTFILE_STRING));
            transparentify(image, OUTPUTFILE_STRING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes in an image with a white background, then returns a new image with the
     * background removed.
     * 
     * @param image Image to transparentify.
     * @return Transparent background version of the image.
     */
    public static BufferedImage transparentify(BufferedImage image) {
        boolean[][] pixels = new boolean[image.getWidth()][image.getHeight()];

        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        BufferedImage bufferedImage2 = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        // Create a graphics which can be used to draw into the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
        Graphics2D g2d2 = bufferedImage2.createGraphics();
        g2d2.setBackground(new Color(255, 255, 255, 0));
        // g2d2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT));
        g2d2.setColor(new Color(255, 255, 255, 0));
        g2d2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int clr = image.getRGB(i, j);
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;
                Color c = new Color(clr, true);
                double whiteness = ((red) / 255.0 + (green) / 255.0 + (blue) / 255.0) / 3.0;
                g2d.setColor(new Color(red, green, blue));
                g2d.drawRect(i, j, 1, 1);

                if (whiteness >= .9 || c.getAlpha() == 0) {
                    pixels[i][j] = true;
                }
            }
        }

        // g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT));
        g2d.setColor(new Color(255, 0, 0, 255));
        HashSet<Point> transparentPixels = new HashSet<>();
        // g2d.setBackground(new Color(50, 255, 255, 0));

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                if (!pixels[i][j]) {
                    break;
                }
                transparentPixels.add(new Point(i, j));
            }
        }

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = image.getHeight() - 1; j >= 0; j--) {
                if (!pixels[i][j]) {
                    break;
                }
                transparentPixels.add(new Point(i, j));
            }
        }

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                if (!pixels[i][j]) {
                    break;
                }
                transparentPixels.add(new Point(i, j));
            }
        }

        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = image.getWidth() - 1; i >= 0; i--) {
                if (!pixels[i][j]) {
                    break;
                }
                transparentPixels.add(new Point(i, j));
            }
        }

        // Make actual image
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                if (!transparentPixels.contains(new Point(i, j))) {
                    int clr = image.getRGB(i, j);
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;

                    g2d2.setColor(new Color(red, green, blue, 255));
                    g2d2.drawRect(i, j, 1, 1);
                }
            }
        }

        g2d.dispose();
        g2d2.dispose();

        return bufferedImage2;
    }

    /**
     * Takes in an image with a white background, then returns a new image with the
     * background removed. Also writes an image file of the transparent image.
     * 
     * @param image Image to transparentify.
     * @param outputFileString The name of of the file to write.
     * @return Transparent background version of the image.
     */
    public static BufferedImage transparentify(BufferedImage image, String outputFileString) {
        try {
            BufferedImage i = transparentify(image);
            
            if (outputFileString != null) {
                File file = new File(outputFileString);
                ImageIO.write(i, "png", file);
            }

            return i;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
