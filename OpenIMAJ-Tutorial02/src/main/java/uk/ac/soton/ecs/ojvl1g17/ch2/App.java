package uk.ac.soton.ecs.ojvl1g17.ch2;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Ellipse;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
        //Load an image
        MBFImage image = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/sinaface.jpg"));
        ImageManager imgDisplay = new ImageManager(image);

        // Print the colourSpace of the image
        System.out.println("Image colourSpace: " + image.colourSpace);
    }

    /**
     * Inner class providing functionality for changing between images
     */
    private static class ImageManager {
        private MBFImage originalImage;
        private JFrame imageFrame;
        private int frameCount = 0;

        ImageManager(MBFImage firstImg) {
            this.originalImage = firstImg;
            this.imageFrame = DisplayUtilities.displayName(originalImage, "Tutorial2 Window, middle-mouse click to advance");

            // It didn't work if I just added the listener to the imageFrame.
            // I copied this from some methods that added listeners within the DisplayUtilities class
            imageFrame.getContentPane().getComponent(0).addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // We use Middle Mouse to advance because the other buttons are already reserved for other useful stuff
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        frameCount++;
                        nextImage();
                    }
                }
            });
        }

        private void nextImage() {
            System.out.println("Next Image");
            switch(frameCount) {
                case 0:
                    break;
                case 1:
                    DisplayUtilities.updateNamed("Tutorial2 Window, middle-mouse click to advance", originalImage.getBand(0), "Red Band");
                    break;
                case 2:
                    DisplayUtilities.updateNamed("Tutorial2 Window, middle-mouse click to advance", blueGreenExercise(), "Blue, Green Bands = Black");
                    break;
                case 3:
                    DisplayUtilities.updateNamed("Tutorial2 Window, middle-mouse click to advance", originalImage.processInplace(new CannyEdgeDetector()), "Edges Detected");
                    break;
                case 4:
                    DisplayUtilities.updateNamed("Tutorial2 Window, middle-mouse click to advance", shapeDrawingExercise(), "Speech bubble");
                    break;
                default:
                    System.exit(0);
            }
        }

        private MBFImage blueGreenExercise() {
            // Image processing, we set the image's blue and green pixels to black.
            MBFImage image = originalImage.clone();
            for (int y=0; y<originalImage.getHeight(); y++) {
                for(int x=0; x<originalImage.getWidth(); x++) {
                    image.getBand(1).pixels[y][x] = 0;
                    image.getBand(2).pixels[y][x] = 0;
                }
            }
            return image;
        }

        private MBFImage shapeDrawingExercise() {
            MBFImage image = originalImage.clone();
            image.drawShapeFilled(new Ellipse(700f, 450f, 22f, 12f, 0f), RGBColour.RED);
            image.drawShapeFilled(new Ellipse(700f, 450f, 20f, 10f, 0f), RGBColour.WHITE);

            image.drawShapeFilled(new Ellipse(650f, 425f, 27f, 14f, 0f), RGBColour.RED);
            image.drawShapeFilled(new Ellipse(650f, 425f, 25f, 12f, 0f), RGBColour.WHITE);

            image.drawShapeFilled(new Ellipse(600f, 380f, 32f, 17f, 0f), RGBColour.RED);
            image.drawShapeFilled(new Ellipse(600f, 380f, 30f, 15f, 0f), RGBColour.WHITE);

            image.drawShapeFilled(new Ellipse(500f, 300f, 102f, 72f, 0f), RGBColour.RED);
            image.drawShapeFilled(new Ellipse(500f, 300f, 100f, 70f, 0f), RGBColour.WHITE);
            image.drawText("OpenIMAJ is", 425, 300, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
            image.drawText("Awesome", 425, 330, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
            return image;
        }
    }

}

