package uk.ac.soton.ecs.ojvl1g17.ch6;

import org.apache.avro.JsonProperties;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.dataset.BingImageDataset;
import org.openimaj.image.dataset.FlickrImageDataset;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.BingAPIToken;
import org.openimaj.util.api.auth.common.FlickrAPIToken;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.stream.Collectors;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws Exception {
        exercise1();
        exercise3();
        exercise4();
    }

    // Exercise 6.1.1, displays an image showing a randomly selected photo of each person in the dataset
    private static void exercise1() throws FileSystemException {
        VFSGroupDataset<FImage> groupedFaces =
                new VFSGroupDataset<>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);

        Random random = new Random();
        List<FImage> randomFaces = new ArrayList<>();

        // Each entrySet represents a different folder (and such, a different person's photos)
        for (Map.Entry<String, VFSListDataset<FImage>> faceSet : groupedFaces.entrySet()) {
            // Find the number of photos of that person
            int instances = groupedFaces.get(faceSet.getKey()).numInstances();
            // Pick a random FImage of that person
            final FImage randomFace = faceSet.getValue().getInstance(random.nextInt(instances));
            randomFaces.add(randomFace);
        }

        DisplayUtilities.display("Random Face", randomFaces);
    }

    // Exercise 6.1.2
    //Other types of supported file sources include Jars, Tars, Hadoop File System, HTTP/HTTPS hosted files, FTP/SFTP, even MIME emails
    //Quite versatile :)

    // Exercise 6.1.3
    //Display pictures of dogs from Bing Images
    public static void exercise3() {
        BingAPIToken bingToken = DefaultTokenFactory.get(BingAPIToken.class);
        BingImageDataset<FImage> dogs = BingImageDataset.create(ImageUtilities.FIMAGE_READER, bingToken, "dog", 5);
        DisplayUtilities.display("Dogs", dogs);
    }

    // Exercise 6.1.4
    //Display pictures of different computer scientists
    public static void exercise4() {
        BingAPIToken bingToken = DefaultTokenFactory.get(BingAPIToken.class);

        List<String> csPeople = Arrays.asList("Ada Lovelace", "Alan Turing", "Tim Berners-Lee", "Donald Knuth", "John von Neumann", "Grace Hopper", "Ken Thompson");
        //Get the images with a lambda function and put them straight into a key-backed store
        MapBackedDataset<String, BingImageDataset<FImage>, FImage> mapBacked = MapBackedDataset.of(
                        csPeople.stream()
                        .map(e -> BingImageDataset.create(ImageUtilities.FIMAGE_READER, bingToken, e, 5))
                        .collect(Collectors.toList()));

        //Sometimes if Bing returned some weird images this throws an exception, however it only seems to affect that image group.
        for (String compScientist : mapBacked.keySet()) {
                DisplayUtilities.display(compScientist, mapBacked.get(compScientist));
        }

    }
}
