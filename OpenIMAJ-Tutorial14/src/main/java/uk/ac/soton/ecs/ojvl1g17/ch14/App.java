package uk.ac.soton.ecs.ojvl1g17.ch14;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.time.Timer;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.openimaj.util.parallel.partition.RangePartitioner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException {
        VFSGroupDataset<MBFImage> allImages = Caltech101.getImages(ImageUtilities.MBFIMAGE_READER);
        GroupedDataset<String, ListDataset<MBFImage>, MBFImage> images = GroupSampler.sample(allImages, 8, false);
        final List<MBFImage> output = new ArrayList<MBFImage>();
        final ResizeProcessor resize = new ResizeProcessor(200);

        // VERSION 1: Unparallelised
//        Timer t1 = Timer.timer();
//        for (ListDataset<MBFImage> clzImages : images.values()) {
//            MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);
//
//            for (MBFImage i : clzImages) {
//                MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
//                tmp.fill(RGBColour.WHITE);
//
//                MBFImage small = i.process(resize).normalise();
//                int x = (200 - small.getWidth()) / 2;
//                int y = (200 - small.getHeight()) / 2;
//                tmp.drawImage(small, x, y);
//
//                current.addInplace(tmp);
//            }
//            current.divideInplace((float) clzImages.size());
//            output.add(current);
//        }
//        System.out.println("Time for Version 1: " + t1.duration() + "ms");

        // VERSION 2: Parallelised Inner Loop w/ forEach
//        Timer t1 = Timer.timer();
//        for (ListDataset<MBFImage> clzImages : images.values()) {
//            final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);
//
//            Parallel.forEach(clzImages, new Operation<MBFImage>() {
//                public void perform(MBFImage i) {
//                    final MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
//                    tmp.fill(RGBColour.WHITE);
//
//                    final MBFImage small = i.process(resize).normalise();
//                    final int x = (200 - small.getWidth()) / 2;
//                    final int y = (200 - small.getHeight()) / 2;
//                    tmp.drawImage(small, x, y);
//
//                    synchronized (current) {
//                        current.addInplace(tmp);
//                    }
//                }
//            });
//            current.divideInplace((float) clzImages.size());
//            output.add(current);
//        }
//        System.out.println("Time for Version 2: " + t1.duration() + "ms");

        // VERSION 3: Parallelised Inner Loop w/ forEachPartitioned
//        Timer t1 = Timer.timer();
//        for (ListDataset<MBFImage> clzImages : images.values()) {
//            final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);
//
//            Parallel.forEachPartitioned(new RangePartitioner<MBFImage>(clzImages), new Operation<Iterator<MBFImage>>() {
//                public void perform(Iterator<MBFImage> it) {
//                    MBFImage tmpAccum = new MBFImage(200, 200, 3);
//                    MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
//
//                    while (it.hasNext()) {
//                        final MBFImage i = it.next();
//                        tmp.fill(RGBColour.WHITE);
//
//                        final MBFImage small = i.process(resize).normalise();
//                        final int x = (200 - small.getWidth()) / 2;
//                        final int y = (200 - small.getHeight()) / 2;
//                        tmp.drawImage(small, x, y);
//                        tmpAccum.addInplace(tmp);
//                    }
//                    synchronized (current) {
//                        current.addInplace(tmpAccum);
//                    }
//                }
//            });
//            current.divideInplace((float) clzImages.size());
//            output.add(current);
//        }
//        System.out.println("Time for Version 3: " + t1.duration() + "ms");

        // VERSION 4: Exercise 14.1.1 - Parallelised Outer Loop
        Timer t1 = Timer.timer();
        Parallel.forEachPartitioned(new RangePartitioner<ListDataset<MBFImage>>(images.values()), new Operation<Iterator<ListDataset<MBFImage>>>() {
            public void perform(Iterator<ListDataset<MBFImage>> it) {
                MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

                while (it.hasNext()) {
                    ListDataset<MBFImage> nxt = it.next();
                    for (MBFImage i : nxt) {
                        MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                        tmp.fill(RGBColour.WHITE);

                        MBFImage small = i.process(resize).normalise();
                        int x = (200 - small.getWidth()) / 2;
                        int y = (200 - small.getHeight()) / 2;
                        tmp.drawImage(small, x, y);

                        current.addInplace(tmp);
                    }
                    current.divideInplace((float) nxt.size());
                    output.add(current);
                }
            }
        });
        System.out.println("Time for Version 4: " + t1.duration() + "ms");
        DisplayUtilities.display("Images", output);


        // 1 Unparallelised: 15538ms
        // 2 Parallelised Inner Loop w/ forEach: 6648ms
        // 3 Parallelised Inner Loop w/ forEachPartitioned: 7009ms
        //      Interestingly this took longer, the tutorial suggests it will use more memory, so I tried varying the
        //      JVM Heap size (1024 vs 2048 vs 4096) in case it was a memory bottleneck, but none of these values
        //      resulted in it being faster.

        // EXERCISE 14.1.1:
        // 4 Parallelised Outer Loop (Ex 14.1.1): 9147ms
        //      Using a parallelised outer loop was slower on testing, this may be because fewer threads were
        //      being used at a given time.
        //      I noticed we have 8 groups, which I observed to have anywhere from 42-800 images per group.
        //      Perhaps parallelising on the processing of each group (which could create up to 80 threads simultaneously)
        //      is more effective than parallelising on the groups themselves (which creates up to 8 threads only).

    }
}
