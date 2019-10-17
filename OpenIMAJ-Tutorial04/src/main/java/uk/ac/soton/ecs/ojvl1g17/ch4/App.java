package uk.ecs.soton.ecs.ojvl1g17;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
        URL[] imageURLs = new URL[] {
                new URL( "http://openimaj.org/tutorial/figs/hist1.jpg" ),
                new URL( "http://openimaj.org/tutorial/figs/hist2.jpg" ),
                new URL( "http://openimaj.org/tutorial/figs/hist3.jpg" )
        };

        List<MultidimensionalHistogram> histograms = new ArrayList<>();
        HistogramModel model = new HistogramModel(4, 4, 4);

        for( URL u : imageURLs ) {
            model.estimateModel(ImageUtilities.readMBF(u));
            histograms.add( model.histogram.clone() );
        }

        int similarId1 = 0;
        int similarId2 = 0;
        double bestDistance = 100;
        List<Double> euclideans = new ArrayList<>();
        List<Double> sumSquares = new ArrayList<>();
        List<Double> correlations = new ArrayList<>();
        List<Double> chiSquares = new ArrayList<>();
        List<Double> intersections = new ArrayList<>();
        for( int i = 0; i < histograms.size(); i++ ) {
            for( int j = i; j < histograms.size(); j++ ) {
                if (i != j) {
                    double euclideanDistance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.EUCLIDEAN );
                    euclideans.add(euclideanDistance);
                    double sumSquareDistance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.SUM_SQUARE );
                    sumSquares.add(sumSquareDistance);
                    double correlation = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.CORRELATION );
                    correlations.add(correlation);
                    double chiSquare = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.CHI_SQUARE );
                    chiSquares.add(chiSquare);
                    double intersection = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.INTERSECTION );
                    intersections.add(intersection);

                    if (euclideanDistance < bestDistance) {
                        bestDistance = euclideanDistance;
                        similarId1 = i;
                        similarId2 = j;
                    }
                }
            }
        }



        //Exercise 4.1.1:
        // Images 1 and 2 are most similar, as indicated by the histogram Euclidian distance metric.
        // This does match what I'd expect, as these look most similar to me too.
        System.out.println("Displaying the two most similar images...");
        DisplayUtilities.display(ImageUtilities.readMBF(imageURLs[similarId1]));
        DisplayUtilities.display(ImageUtilities.readMBF(imageURLs[similarId2]));

        //Exercise 4.1.2:
        // Different measures can tell us different things about the images in different ways.
        // Euclidian distance is a distance measure, so smaller values indicate similarity
        // whereas with Intersection a higher value indicates similarity.
        printDistances(euclideans, "Euclidean Distance", 3);
        printDistances(sumSquares, "Sum Square Distance",3 );
        printDistances(correlations, "Correlation", 3); //Interestingly, this measure suggests that images 1 and 3 have most correlation, which I'd attest.
        printDistances(chiSquares, "Chi Square", 3);
        printDistances(intersections, "Intersection", 3);
    }

    private static void printDistances(List<Double> distances, String metricName, int pictures) {
        int count = 0;
        for(int i=0; i<pictures; i++) {
            for (int j=0; j<pictures; j++) {
                if (i != j && count != pictures) {
                    System.out.println(metricName + " between " + (i+1) + " and " + (j+1) + " : " + distances.get(count));
                    count++;
                }
            }
        }
        System.out.println();
    }
}
