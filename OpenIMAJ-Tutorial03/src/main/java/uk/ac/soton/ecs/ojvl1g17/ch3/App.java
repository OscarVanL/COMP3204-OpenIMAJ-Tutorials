package uk.ac.soton.ecs.ojvl1g17.ch3;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.connectedcomponent.GreyscaleConnectedComponentLabeler;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.pixel.Pixel;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.image.segmentation.FelzenszwalbHuttenlocherSegmenter;
import org.openimaj.image.segmentation.SegmentationUtilities;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.ml.clustering.FloatCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.FloatKMeans;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
        App segmentationApp = new App();

        //Open an image I took of the University campus
    	MBFImage input = ImageUtilities.readMBF(new URL("https://i.imgur.com/3Q3lXif.jpg"));
        //Convert from RGB colourspace into LAB colour space
        //This allows the euclidean distance calculated between colours to be more representative of the perceived
        //distance in colour.
        input = ColourSpace.convert(input, ColourSpace.CIE_Lab);

        //segmentationApp.doKMeansSegmentation(input, false);
        segmentationApp.doFHSegmentation(input, true);

    }

    private void doKMeansSegmentation(MBFImage input, boolean pointLabels) {
        //Instantiate KMeans algorithm
        final FloatKMeans cluster = FloatKMeans.createExact(2);
        //Process image data to format required by FloatKMeans

        final float[][] imageData = input.getPixelVectorNative(new float[input.getWidth() * input.getHeight()][3]);

        //Result of K-Means
        final FloatCentroidsResult result = cluster.cluster(imageData);
        final float[][] centroids = result.centroids; //Get K-Means centroids from results
        for (float[] fs : centroids) {
            System.out.println(Arrays.toString(fs));
        }

        final HardAssigner<float[],?,?> assigner = result.defaultHardAssigner();

        // Exercise 3.1.1
        final MBFImage finalInput = input;
        input.processInplace(new PixelProcessor<Float[]>() {
            public Float[] processPixel(Float[] pixel) {
                //Convert Object version of Float to primitive
                int centroid = assigner.assign(floatObjToPrim(pixel));
                return floatPrimToObj(centroids[centroid]);
            }
            // Handles converting the float[] list to Float[]
            private Float[] floatPrimToObj(float[] prim) {
                Float[] pixelObj = new Float[prim.length];
                for (int i=0; i<prim.length; i++) {
                    pixelObj[i] = prim[i];
                }
                return pixelObj;
            }
            // Handles converting the Float[] list to float[]
            private float[] floatObjToPrim(Float[] obj) {
                float[] pixelPrimitive = new float[obj.length];
                for (int i=0; i<obj.length; i++) {
                    pixelPrimitive[i] = obj[i]
                    ;
                }
                return pixelPrimitive;
            }
        });

        input = ColourSpace.convert(input, ColourSpace.RGB);

        GreyscaleConnectedComponentLabeler labeler = new GreyscaleConnectedComponentLabeler();
        //Flattens our MBFImage to merge colours into grey values, as GreyscaleConnectedComponentLabeler only takes FImage
        List<ConnectedComponent> components = labeler.findComponents(input.flatten());


        if (pointLabels) {
            int i = 0;

            for (ConnectedComponent comp : components) {
                //Only display point number for sufficiently large ConnectedComponents
                if (comp.calculateArea() < 500)
                    continue;
                input.drawText("Point: " + (i++), comp.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 20);
            }
        }


        DisplayUtilities.display(input);
    }

    private void doFHSegmentation(MBFImage input, boolean printLabels) {


        FelzenszwalbHuttenlocherSegmenter segmenter = new FelzenszwalbHuttenlocherSegmenter(0.5f, 500f / 255f, 500);
        List<ConnectedComponent> components = segmenter.segment(input);

        if (printLabels) {
            int i = 0;
            for (ConnectedComponent comp : components) {
                //Only display point number for sufficiently large ConnectedComponents
                if (comp.calculateArea() < 500)
                    continue;
                input.drawText("Point: " + (i++), comp.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 20);
            }
        }


        MBFImage out = SegmentationUtilities.renderSegments(input, components);
        DisplayUtilities.display(out);
    }
}
