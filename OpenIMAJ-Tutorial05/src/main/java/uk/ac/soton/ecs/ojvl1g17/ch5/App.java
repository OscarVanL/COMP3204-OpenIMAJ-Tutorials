package uk.ac.soton.ecs.ojvl1g17.ch5;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.*;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.LMedS;
import org.openimaj.math.model.fit.RANSAC;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
        MBFImage query = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
        MBFImage target = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/target.jpg"));
        ImageManager imgDisplay = new ImageManager(query, target);
    }

    /*
     * Inner class providing functionality for changing between images for each part of this tutorial.
     */
    private static class ImageManager {
        private MBFImage query;
        private MBFImage target;
        private int frameCount = 0;

        ImageManager(MBFImage query, MBFImage target) {
            this.query = query;
            this.target = target;

            DisplayUtilities.createNamedWindow("Tutorial5", "Tutorial5", true);
            JFrame imageFrame = DisplayUtilities.displayName(query, "Tutorial5");

            // Create middle mouse click listener to advance between images from different parts of the tutorial
            // It didn't work if I just added the listener to the imageFrame.
            imageFrame.getContentPane().getComponent(0).addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // I use Middle Mouse to advance because the other buttons are already reserved for other useful stuff
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        frameCount++;
                        nextImage();
                    }
                }
            });
            }

        private void nextImage() {
            System.out.println("Next image");
            DoGSIFTEngine engine = new DoGSIFTEngine();
            LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
            LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());
            RobustAffineTransformEstimator robustAffineModelFitter = new RobustAffineTransformEstimator(50.0, 1500,
                    new RANSAC.PercentageInliersStoppingCondition(0.5));
            switch(frameCount) {
                case 0:
                    //Difference-of-Gaussian SIFT Engine:
                    LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<>(80);
                    matcher.setModelFeatures(queryKeypoints);
                    matcher.findMatches(targetKeypoints);

                    MBFImage basicMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
                    DisplayUtilities.updateNamed("Tutorial5", basicMatches, "Difference-of-Gaussian SIFT Engine");
                    break;
                case 1:
                    //RANSAC Model Fitted, used to find Affine Transforms
                    matcher = new ConsistentLocalFeatureMatcher2d<>(
                            new FastBasicKeypointMatcher<>(8), robustAffineModelFitter);

                    matcher.setModelFeatures(queryKeypoints);
                    matcher.findMatches(targetKeypoints);

                    final MBFImage consistentMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                            RGBColour.RED);

                    DisplayUtilities.updateNamed("Tutorial5", consistentMatches, "RAMSAC Model w/ Affine Transforms");
                    break;
                case 2:
                    //Exercise 5.1.1 : Different Matchers
                    // BasicTwoWayMatcher matcher used instead of ConsistentLocalFeatureMatcher2d:
                    //I think this is not discriminative enough, there are too many erroneous points.
                    matcher = new BasicTwoWayMatcher<>();

                    matcher.setModelFeatures(queryKeypoints);
                    matcher.findMatches(targetKeypoints);

                    MBFImage basicTwoWayMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                            RGBColour.RED);

                    DisplayUtilities.updateNamed("Tutorial5", basicTwoWayMatches,  "BasicTwoWayMatcher");
                    break;
                case 3:
                    //Exercise 5.1.2: Different models
                    // The RobustHomographyEstimator using LMedS seems to match points that were missed by the RobustAffineTransformEstimator
                    // But it also picked up a few anomalous results
                    // These could be eliminated using a smaller threshold value in the FastBasicKeypointMatcher (6 instead of 8)
                    RobustHomographyEstimator homographyEstimator = new RobustHomographyEstimator(0.75, HomographyRefinement.SINGLE_IMAGE_TRANSFER);
                    matcher = new ConsistentLocalFeatureMatcher2d<>(
                            new FastBasicKeypointMatcher<>(6), homographyEstimator);
                    matcher.setModelFeatures(queryKeypoints);
                    matcher.findMatches(targetKeypoints);

                    final MBFImage homographyMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                            RGBColour.RED);

                    DisplayUtilities.updateNamed("Tutorial5", homographyMatches, "RobustHomography model w/ ConsistentLocalFeatureMatcher2d");
                    break;
                default:
                    System.exit(0);
            }
        }
    }
}
