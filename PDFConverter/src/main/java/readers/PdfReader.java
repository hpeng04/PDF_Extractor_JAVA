package readers;

import app.ProgressDialog;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import utils.AtomicDoubleBackedByAtomicLong;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PdfReader extends Reader {
    private final int TEXT_LINE = 2;
    private final int WORD = 3;

    private ConcurrentHashMap<Integer, List<Word>> pagesWithInfo;

    // Thread-local Tesseract instances to avoid expensive initialization per image
    private static final ThreadLocal<ITesseract> THREAD_LOCAL_TESSERACT = ThreadLocal.withInitial(() -> {
        ITesseract reader = new Tesseract();
        new Reader().configureTesseract(reader);
        return reader;
    });

    public PdfReader() {
        super();
    }

    public void setAllContent(Map<Integer, BufferedImage> imgs, ProgressDialog dialog, AtomicInteger progress, int numFiles) throws TesseractException {
        if (imgs.isEmpty()) {
            this.pagesWithInfo = new ConcurrentHashMap<>();
            return;
        }

        // Optimize thread pool size - don't create more threads than images
        int threadPoolSize = Math.min(Runtime.getRuntime().availableProcessors(), imgs.size());
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        ConcurrentHashMap<Integer, List<Word>> resultsWithDetail = new ConcurrentHashMap<>();
        AtomicDoubleBackedByAtomicLong p = new AtomicDoubleBackedByAtomicLong(progress.get());
        
        // Pre-calculate progress increment to avoid repeated division
        final double progressIncrement = 30.0 / (numFiles * imgs.size());
        
        // Counter for batched progress updates
        final AtomicInteger processedCount = new AtomicInteger(0);
        final int progressUpdateInterval = Math.max(1, imgs.size() / 20); // Update every 5% of images

        try {
            // Use CompletableFuture for better exception handling and cleaner code
            List<CompletableFuture<Void>> futures = imgs.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    try {
                        // Use thread-local Tesseract instance
                        ITesseract reader = THREAD_LOCAL_TESSERACT.get();
                        List<Word> ocrResultDetailed = reader.getWords(entry.getValue(), TEXT_LINE);
                        resultsWithDetail.put(entry.getKey(), ocrResultDetailed);
                        
                        // Batch progress updates to reduce UI overhead
                        int processed = processedCount.incrementAndGet();
                        if (processed % progressUpdateInterval == 0 || processed == imgs.size()) {
                            p.addAndGet(progressIncrement * progressUpdateInterval);
                            dialog.updateProgress(p.get());
                        }
                    } catch (Exception e) {
                        // Convert to runtime exception to propagate through CompletableFuture
                        throw new RuntimeException("OCR processing failed for image " + entry.getKey(), e);
                    }
                }, executor))
                .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allTasks.join();

            // Handle any remaining progress updates
            int finalProcessed = processedCount.get();
            if (finalProcessed % progressUpdateInterval != 0) {
                p.addAndGet(progressIncrement * (finalProcessed % progressUpdateInterval));
                dialog.updateProgress(p.get());
            }

            progress.set((int) p.get());
            
        } catch (Exception e) {
            // Handle CompletableFuture exceptions
            Throwable cause = e.getCause();
            if (cause instanceof TesseractException) {
                throw (TesseractException) cause;
            }
            throw new TesseractException("OCR processing failed", e);
        } finally {
            // Proper executor shutdown
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("Thread pool did not terminate gracefully");
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        this.pagesWithInfo = resultsWithDetail;
    }

    public ConcurrentHashMap<Integer, List<Word>> getPagesWithInfo() {
        return this.pagesWithInfo;
    }
    
    /**
     * Clean up thread-local resources when done (call this when shutting down)
     */
    public static void cleanupThreadLocalResources() {
        THREAD_LOCAL_TESSERACT.remove();
    }
}