package uk.ac.soton.ecs.ojvl1g17.ch13;

import com.jogamp.newt.Display;
import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.dataset.util.DatasetAdaptors;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.model.EigenImages;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;

import java.util.*;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {
        VFSGroupDataset<FImage> dataset =
                null;
        try {
            dataset = new VFSGroupDataset<>("zip:file:///C:/Users/Oscar/IdeaProjects/OpenIMAJ/OpenIMAJ-Tutorial13/att_faces.zip", ImageUtilities.FIMAGE_READER);
        } catch (FileSystemException e) {
            e.printStackTrace();
        }


        int nTraining = 5;
        int nTesting = 5;
        GroupedRandomSplitter<String, FImage> splits =
                new GroupedRandomSplitter<>(dataset, nTraining, 0, nTesting);
        GroupedDataset<String, ListDataset<FImage>, FImage> training = splits.getTrainingDataset();
        GroupedDataset<String, ListDataset<FImage>, FImage> testing = splits.getTestDataset();

        List<FImage> basisImages = DatasetAdaptors.asList(training);
        int nEigenvectors = 100;
        EigenImages eigen = new EigenImages(nEigenvectors);
        eigen.train(basisImages);

        List<FImage> eigenFaces = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            eigenFaces.add(eigen.visualisePC(i));
        }
        DisplayUtilities.display("EigenFaces", eigenFaces);

        Map<String, DoubleFV[]> features = new HashMap<>();
        for (final String person : training.getGroups()) {
            final DoubleFV[] fvs = new DoubleFV[nTraining];

            for (int i = 0; i < nTraining; i++) {
                final FImage face = training.get(person).get(i);
                fvs[i] = eigen.extractFeature(face);
            }
            features.put(person, fvs);
        }



        // Exercise 13.1.1: Reconstructing faces
        Random rand = new Random();
        // A little convoluted, but this gets a random face.
        List<String> imageKeys = new ArrayList<>(testing.keySet());
        List<FImage> randomFaceGroup = testing.get(imageKeys.get(rand.nextInt(imageKeys.size())));
        FImage randomFace = randomFaceGroup.get(rand.nextInt(randomFaceGroup.size()));
        // Extract the eigen features from the face, then reconstruct it, normalise it and show it.
        DoubleFV feature = eigen.extractFeature(randomFace);
        FImage reconstructed = eigen.reconstruct(feature);
        DisplayUtilities.display("Exercise 13.1.1", reconstructed.normalise());

        double threshold = 12;
        double correct = 0, incorrect = 0;
        for (String truePerson : testing.getGroups()) {
            for (FImage face : testing.get(truePerson)) {
                DoubleFV testFeature = eigen.extractFeature(face);

                String bestPerson = null;
                double minDistance = Double.MAX_VALUE;
                for (final String person : features.keySet()) {
                    for (final DoubleFV fv : features.get(person)) {
                        double distance = fv.compare(testFeature, DoubleFVComparison.EUCLIDEAN);

                        if (distance < minDistance) {
                            minDistance = distance;
                            bestPerson = person;
                        }
                    }
                }
                if (minDistance > threshold) {
                    bestPerson = "Unknown";
                }

                System.out.println("Actual: " + truePerson + "\tguess: " + bestPerson);

                if (truePerson.equals(bestPerson) && (!bestPerson.equals("Unknown"))) {
                    correct++;
                } else if ((!truePerson.equals(bestPerson)) && (!bestPerson.equals("Unknown"))) {
                    incorrect++;
                }
            }
        }

        System.out.println("Accuracy: " + (correct / (correct + incorrect)));

        // Exercise 13.1.2: Explore the effect of training set size
        // When reducing the number of training images the following occcur:
        // * The granularity of the faces increases, not as much blurring / averaging occurs
        // * There are significant accuracy decreases, 5: 0.95 accuracy, 2: 0.72 accuracy.

        // Exercise 13.1.3: Apply a threshold:
        // The 'optimal' value of the threshold depends on the application of the system.
        // * If you are in a circumstance where you require high % accuracy (few false detections), then a lower threshold value
        // will achieve this - With threshold = 5.0 I achieved accuracy of 100%. However, this is at the expense of fewer
        // classifications, loads were picked as 'Unknown'.
        // * If I was not fussed about some inaccuracies, threshold = 12.0 achieved 97.9% accuracy with only a few Unknown
        // classifications.
        // Ultimately the good threshold value is dependent on application and how tolerant the system is to inaccuracy.

    }
}
