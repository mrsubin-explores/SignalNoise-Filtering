/**
 * Digital filters used by the simulator.
 * Each method reads the 'in' array and writes the result into the separate 'out' array.
 */
public class Filters {

    /** Centered moving average: y[n] = mean of window around n. */
    static void movingAverage(double[] in, double[] out, int w) {
        int h = w / 2;
        for (int i = 0; i < in.length; i++) {
            double sum = 0; int cnt = 0;
            for (int k = i - h; k <= i + h; k++) if (k >= 0 && k < in.length) { sum += in[k]; cnt++; }
            out[i] = sum / cnt;
        }
    }

    /** Median filter (optional second filter). */
    static void median(double[] in, double[] out, int w) {
        int h = w / 2;
        for (int i = 0; i < in.length; i++) {
            int lo = Math.max(0, i - h), hi = Math.min(in.length - 1, i + h);
            double[] win = java.util.Arrays.copyOfRange(in, lo, hi + 1);
            java.util.Arrays.sort(win);
            out[i] = win[win.length / 2];
        }
    }
}
