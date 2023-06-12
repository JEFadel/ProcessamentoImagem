import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final String SOURCE_FILE = "./resources/many-flowers.jpg";
    public static final String DESTINATION_FILE = "./out/many-flowers.jpg";

    public static void main(String[] args) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(SOURCE_FILE));
        BufferedImage resultImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Medir tempo sem threads
        long startTime = System.currentTimeMillis();
        recolorSingleThreaded(originalImage, resultImage);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Tempo de execução sem threads: " + executionTime + " ms");

        // Medir tempo com uma thread
        startTime = System.currentTimeMillis();
        recolorMultiThreaded(originalImage, resultImage, 1);
        endTime = System.currentTimeMillis();
        executionTime = endTime - startTime;
        System.out.println("Tempo de execução com 1 thread: " + executionTime + " ms");

        // Medir tempo com duas threads
        startTime = System.currentTimeMillis();
        recolorMultiThreaded(originalImage, resultImage, 2);
        endTime = System.currentTimeMillis();
        executionTime = endTime - startTime;
        System.out.println("Tempo de execução com 2 threads: " + executionTime + " ms");

        // Medir tempo com quatro threads
        startTime = System.currentTimeMillis();
        recolorMultiThreaded(originalImage, resultImage, 4);
        endTime = System.currentTimeMillis();
        executionTime = endTime - startTime;
        System.out.println("Tempo de execução com 4 threads: " + executionTime + " ms");

        // Gerar gráfico de número de threads x tempo
        generateThreadTimeGraph(originalImage, resultImage, 1, 8, 1);

        // Fixar o número de threads em 4 e variar o tamanho da imagem
        generateResolutionSpeedupGraph(originalImage, resultImage, 4, 1.0, 0.1, 0.1);
    }

    public static void recolorSingleThreaded(BufferedImage originalImage, BufferedImage resultImage) {
        recolorImage(originalImage, resultImage, 0, 0, originalImage.getWidth(), originalImage.getHeight());
    }

    public static void recolorMultiThreaded(BufferedImage originalImage, BufferedImage resultImage, int numThreads) {
        List<ImageProcessor> threads = new ArrayList<>();
        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();
        int regionWidth = imageWidth / numThreads;
        int regionHeight = imageHeight / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int startX = i * regionWidth;
            int endX = startX + regionWidth;
            if (i == numThreads - 1) {
                // A última thread processa até o final da imagem para cobrir eventuais divisões com resto
                endX = imageWidth;
            }

            ImageProcessor thread = new ImageProcessor(originalImage, resultImage, startX, 0, endX, imageHeight);
            threads.add(thread);
        }

        // Inicia as threads
        for (ImageProcessor thread : threads) {
            thread.start();
        }

        // Aguarda a conclusão das threads
        try {
            for (ImageProcessor thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void recolorImage(BufferedImage originalImage, BufferedImage resultImage, int leftCorner, int topCorner,
                                    int width, int height) {
        for (int x = leftCorner; x < leftCorner + width && x < originalImage.getWidth(); x++) {
            for (int y = topCorner; y < topCorner + height && y < originalImage.getHeight(); y++) {
                recolorPixel(originalImage, resultImage, x, y);
            }
        }
    }

    public static void recolorPixel(BufferedImage originalImage, BufferedImage resultImage, int x, int y) {
        int rgb = originalImage.getRGB(x, y);

        int red = getRed(rgb);
        int green = getGreen(rgb);
        int blue = getBlue(rgb);

        int newRed;
        int newGreen;
        int newBlue;

        if (isShadeOfGray(red, green, blue)) {
            newRed = Math.min(255, red + 10);
            newGreen = Math.max(0, green - 80);
            newBlue = Math.max(0, blue - 20);
        } else {
            newRed = red;
            newGreen = green;
            newBlue = blue;
        }

        int newRGB = createRGBFromColors(newRed, newGreen, newBlue);
        setRGB(resultImage, x, y, newRGB);
    }


    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }


    public static void setRGB(BufferedImage image, int x, int y, int rgb) {
        image.getRaster().setDataElements(x, y, image.getColorModel().getDataElements(rgb, null));
    }

    public static boolean isShadeOfGray(int red, int green, int blue) {
        return Math.abs(red - green) < 30 && Math.abs(red - blue) < 30 && Math.abs(green - blue) < 30;
    }

    public static int createRGBFromColors(int red, int green, int blue) {
        int rgb = 0;
        rgb |= blue;
        rgb |= green << 8;
        rgb |= red << 16;
        rgb |= 0xFF000000;
        return rgb;
    }

    public static int getRed(int rgb) {
        return (rgb & 0x00FF0000) >> 16;
    }

    public static int getGreen(int rgb) {
        return (rgb & 0x0000FF00) >> 8;
    }

    public static int getBlue(int rgb) {
        return rgb & 0x000000FF;
    }

    public static void generateThreadTimeGraph(BufferedImage originalImage, BufferedImage resultImage, int startThreads,
                                               int endThreads, int step) {
        System.out.println("Número de threads x Tempo de execução:");

        for (int numThreads = startThreads; numThreads <= endThreads; numThreads += step) {
            long startTime = System.currentTimeMillis();
            recolorMultiThreaded(originalImage, resultImage, numThreads);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.println("Threads: " + numThreads + ", Tempo: " + executionTime + " ms");
        }
    }

    public static void generateResolutionSpeedupGraph(BufferedImage originalImage, BufferedImage resultImage,
                                                      int numThreads, double startResolution, double endResolution,
                                                      double stepResolution) {
        System.out.println("Speed-up x Resolução:");

        for (double resolution = startResolution; resolution >= endResolution; resolution -= stepResolution) {
            int newWidth = (int) (originalImage.getWidth() * resolution);
            int newHeight = (int) (originalImage.getHeight() * resolution);
            BufferedImage resizedImage = resizeImage(originalImage, newWidth, newHeight);

            BufferedImage resizedResultImage = new BufferedImage(resizedImage.getWidth(), resizedImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            long singleThreadStartTime = System.currentTimeMillis();
            recolorSingleThreaded(resizedImage, resizedResultImage);
            long singleThreadEndTime = System.currentTimeMillis();
            long singleThreadExecutionTime = singleThreadEndTime - singleThreadStartTime;

            long multiThreadStartTime = System.currentTimeMillis();
            recolorMultiThreaded(resizedImage, resizedResultImage, numThreads);
            long multiThreadEndTime = System.currentTimeMillis();
            long multiThreadExecutionTime = multiThreadEndTime - multiThreadStartTime;

            double speedup = (double) singleThreadExecutionTime / multiThreadExecutionTime;
            System.out.println("Resolução: " + resolution + ", Speed-up: " + speedup);
        }
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int newWidth, int newHeight) {
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        return resizedImage;
    }
}

class ImageProcessor extends Thread {
    private BufferedImage originalImage;
    private BufferedImage resultImage;
    private int leftCorner;
    private int topCorner;
    private int width;
    private int height;

    public ImageProcessor(BufferedImage originalImage, BufferedImage resultImage, int leftCorner, int topCorner,
                          int width, int height) {
        this.originalImage = originalImage;
        this.resultImage = resultImage;
        this.leftCorner = leftCorner;
        this.topCorner = topCorner;
        this.width = width;
        this.height = height;
    }

    @Override
    public void run() {
        Main.recolorImage(originalImage, resultImage, leftCorner, topCorner, width, height);
    }
}
