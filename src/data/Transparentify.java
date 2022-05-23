import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;

public class Transparentify {

    private static final String INPUTFILE_STRING = "declan_test1.png";
    private static final String OUTPUTFILE_STRING = "declan1_transparent.png";

    public static void main(String[] args) {
        try {
            BufferedImage image = ImageIO.read(new File(INPUTFILE_STRING));
            boolean[][] pixels = new boolean[image.getWidth()][image.getHeight()];

            BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            BufferedImage bufferedImage2 = new BufferedImage(image.getWidth(), image.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            // Create a graphics which can be used to draw into the buffered image
            Graphics2D g2d = bufferedImage.createGraphics();
            Graphics2D g2d2 = bufferedImage2.createGraphics();
            g2d2.setBackground(new Color(255, 255, 255, 0));
            //g2d2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT));
            g2d2.setColor(new Color(255,255,255,0));
            g2d2.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int clr = image.getRGB(i, j);
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;
                    double whiteness = ((red) / 255.0 + (green) / 255.0 + (blue) / 255.0) / 3.0;
                    g2d.setColor(new Color(red, green, blue));
                    g2d.drawRect(i, j, 1, 1);

                    if (whiteness >= .9) {
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

                        g2d2.setColor(new Color(red,green,blue,255));
                        g2d2.drawRect(i, j, 1, 1);
                    }
                }
            }

            g2d.dispose();
            g2d2.dispose();

            File file = new File(OUTPUTFILE_STRING);
            ImageIO.write(bufferedImage2, "png", file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
