/**
 * Signal-quality measurements: how much signal survives a filter, and the signal-to-noise ratio.
 */
public class SignalMetrics {

    /** Fraction of the clean signal that survives the filter (1 = unchanged, 0 = removed, negative = inverted). */
    static double signalGain(double[] ref, double[] out, int skip) {
        double num = 0, den = 0;
        for (int i = skip; i < ref.length - skip; i++) { num += out[i] * ref[i]; den += ref[i] * ref[i]; }
        return den == 0 ? 1 : num / den;
    }

    /** SNR (dB) of 'test' relative to the clean reference. */
    static double snr(double[] ref, double[] test, int skip) {
        double sig = 0, err = 0;
        for (int i = skip; i < ref.length - skip; i++) {
            sig += ref[i] * ref[i];
            double d = test[i] - ref[i];
            err += d * d;
        }
        if (err == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(sig / err);
    }
}
