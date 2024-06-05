package readers;

import app.ProgressDialog;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import utils.AtomicDoubleBackedByAtomicLong;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PdfReader extends Reader {
    private final int TEXT_LINE = 2;
    private final int WORD = 3;

    private ConcurrentHashMap<Integer, List<Word>> pagesWithInfo;

    public PdfReader() {
        super();
    }

    public void setAllContent(Map<Integer, BufferedImage> imgs, ProgressDialog dialog, AtomicInteger progress, int numFiles) throws TesseractException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ConcurrentHashMap<Integer, List<Word>> resultsWithDetail = new ConcurrentHashMap<>();

        AtomicDoubleBackedByAtomicLong p = new AtomicDoubleBackedByAtomicLong(progress.get());
        try {
            imgs.forEach((key, value) -> executor.submit(() -> {
                ITesseract reader = new Reader().tessReader;
                List<Word> ocrResultDetailed = reader.getWords(value, TEXT_LINE);
                resultsWithDetail.put(key, ocrResultDetailed);
                p.addAndGet(30.0 / (numFiles * imgs.size()));
                dialog.updateProgress(p.get());
            }));

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            progress.set((int) p.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interrupt
        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
        this.pagesWithInfo = resultsWithDetail;
    }

    // TODO: new added
    public void setAllContent(File pdfFile, ProgressDialog dialog, AtomicInteger progress, int numFiles) throws TesseractException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ConcurrentHashMap<Integer, List<Word>> resultsWithDetail = new ConcurrentHashMap<>();

        AtomicDoubleBackedByAtomicLong p = new AtomicDoubleBackedByAtomicLong(progress.get());
        try {
            ITesseract reader = new Reader().tessReader;

            String ocrResultDetailed = reader.doOCR(pdfFile);
            System.out.println(ocrResultDetailed);

        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
        this.pagesWithInfo = resultsWithDetail;
    }


    public ConcurrentHashMap<Integer, List<Word>> getPagesWithInfo() {
        return this.pagesWithInfo;
    }
}
