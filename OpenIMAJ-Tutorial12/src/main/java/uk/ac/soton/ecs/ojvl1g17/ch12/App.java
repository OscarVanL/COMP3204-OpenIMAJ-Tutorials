package uk.ac.soton.ecs.ojvl1g17.ch12;

import de.bwaldvogel.liblinear.SolverType;
import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.experiment.dataset.sampling.GroupedUniformRandomisedSampler;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.ml.kernel.HomogeneousKernelMap;
import org.openimaj.util.pair.IntFloatPair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
        String CachePath = "C:/Users/Oscar/IdeaProjects/OpenIMAJ/OpenIMAJ-Tutorial12/cache/";

        GroupedDataset<String, VFSListDataset<Caltech101.Record<FImage>>, Caltech101.Record<FImage>> allData =
                Caltech101.getData(ImageUtilities.FIMAGE_READER);
        GroupedDataset<String, ListDataset<Caltech101.Record<FImage>>, Caltech101.Record<FImage>> data =
                GroupSampler.sample(allData, 5, false);
        GroupedRandomSplitter<String, Caltech101.Record<FImage>> splits =
                new GroupedRandomSplitter<>(data, 15, 0, 15);

        DenseSIFT dsift = new DenseSIFT(5, 7);
        PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<>(dsift, 6f, 7);

        HardAssigner<byte[], float[], IntFloatPair> assigner;
        // Try loading the HardAssigner from cache, if it doesn't exist, create a new one.
        try {
            assigner = IOUtils.readFromFile(new File(CachePath + "HardAssigner"));
            System.out.println("HardAssigner loaded from Cache");
        } catch (IOException e) {
            System.out.println("HardAssigner not found cached, creating new HardAssigner and writing for future use");
            assigner = trainQuantiser(GroupedUniformRandomisedSampler.sample(splits.getTrainingDataset(), 30), pdsift);
            IOUtils.writeToFile(assigner, new File(CachePath + "HardAssigner"));
            System.out.println("Written successfully");
        }


        FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> extractor = new PHOWExtractor(pdsift, assigner);

        //// Exercise 12.1.1
        HomogeneousKernelMap homogeneousKernelMap = new HomogeneousKernelMap(HomogeneousKernelMap.KernelType.Chi2, HomogeneousKernelMap.WindowType.Rectangular);
        // What effect does this have on performance?
        // I benchmarked each of the tutorial's Feature Extractors by timing the .train() function:
        // 1. Ordinary FeatureExtractor (as in tutorial): 57.9 seconds
        // 2. Homogeneous Kernel FeatureExtractor (12.1.1): 71.6
        // 3. Homogeneous Kernel w/ DiskCachingFeatureExtractor and HardAssigner Caching: 36.8 seconds (execution 1 - since nothing's cached yet), 23.4 (execution 2) Accuracy: 0.880
        // To summarise, adding the Homogeneous Kernel to the FeatureExtractor made it take longer to execute, however we
        // can speed up repeated executions by using DiskCachingFeatureExtractor to cache the FeatureExtractor and save and load the FeatureExtractor from file.
        FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> homogeneousExtractor = homogeneousKernelMap.createWrappedExtractor(extractor);

        //// EXERCISE 12.1.2
        // Create a Disk Cached feature extractor from the homogeneous feature extractor
        DiskCachingFeatureExtractor<DoubleFV, Caltech101.Record<FImage>> diskCachedHomogeneousExtractor = new DiskCachingFeatureExtractor<>(new File(CachePath), homogeneousExtractor);

        LiblinearAnnotator<Caltech101.Record<FImage>, String> ann = new LiblinearAnnotator<>(
                diskCachedHomogeneousExtractor, LiblinearAnnotator.Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, 1.0, 0.00001);

        long startTime = System.nanoTime();
        ann.train(splits.getTrainingDataset());
        long endTime = System.nanoTime();
        System.out.println("Training time (ms):" + (endTime - startTime) / 1000000);

        ClassificationEvaluator<CMResult<String>, String, Caltech101.Record<FImage>> eval =
                new ClassificationEvaluator<>(
                        ann, splits.getTestDataset(), new CMAnalyser<Caltech101.Record<FImage>, String>(CMAnalyser.Strategy.SINGLE));

        Map<Caltech101.Record<FImage>, ClassificationResult<String>> guesses = eval.evaluate();
        CMResult<String> result = eval.analyse(guesses);

        System.out.println("Result: " + result);
        //// EXERCISE 12.1.3
        // All classes (uncached): 1043 seconds
        // All classes (cached): 484 seconds
        // Visual words = 600: 424 seconds (Accuracy: 0.565)
        // DenseSIFT step-size = 3: 316 seconds (Accuracy: 0.576)
        // Using PyramidSpatialAggregator with [2,4] blocks: Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: 3606
    }


    static HardAssigner<byte[], float[], IntFloatPair> trainQuantiser(Dataset<Caltech101.Record<FImage>> sample, PyramidDenseSIFT<FImage> pdsift)
    {
        List<LocalFeatureList<ByteDSIFTKeypoint>> allkeys = new ArrayList<>();

        for (Caltech101.Record<FImage> rec : sample) {
            FImage img = rec.getImage();

            pdsift.analyseImage(img);
            allkeys.add(pdsift.getByteKeypoints(0.005f));
        }

        if (allkeys.size() > 10000)
            allkeys = allkeys.subList(0, 10000);

        ByteKMeans km = ByteKMeans.createKDTreeEnsemble(300);
        DataSource<byte[]> datasource = new LocalFeatureListDataSource<>(allkeys);
        ByteCentroidsResult result = km.cluster(datasource);

        return result.defaultHardAssigner();
    }

    static class PHOWExtractor implements FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> {
        PyramidDenseSIFT<FImage> pdsift;
        HardAssigner<byte[], float[], IntFloatPair> assigner;

        public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift, HardAssigner<byte[], float[], IntFloatPair> assigner)
        {
            this.pdsift = pdsift;
            this.assigner = assigner;
        }

        public DoubleFV extractFeature(Caltech101.Record<FImage> object) {
            FImage image = object.getImage();
            pdsift.analyseImage(image);

            BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<byte[]>(assigner);

            BlockSpatialAggregator<byte[], SparseIntFV> spatial = new BlockSpatialAggregator<byte[], SparseIntFV>(
                    bovw, 2, 2);

            return spatial.aggregate(pdsift.getByteKeypoints(0.015f), image.getBounds()).normaliseFV();
        }
    }
}
