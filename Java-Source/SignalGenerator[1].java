import java.util.Random;

/**
 * Creates the signals: the clean sine wave, the random noise, and the noisy signal.
 */
public class SignalGenerator {

    private final Random rng = new Random();

    /** Clean signal: x(t) = A sin(2*pi*f*t), sampled at fs Hz, written into 'out'. */
    public void sine(double[] out, double amplitude, double frequency, double fs) {
        for (int i = 0; i < out.length; i++) out[i] = amplitude * Math.sin(2 * Math.PI * frequency * i / fs);
    }

    /** Fills 'noise' with uniform noise in [-1, +1]; scaled by the noise level it stays within [-level, +level]. */
    public void unitNoise(double[] noise) {
        for (int i = 0; i < noise.length; i++) noise[i] = rng.nextDouble() * 2 - 1;
    }

    /** noisy = original + level * noise. The original array is never modified. */
    public void addNoise(double[] original, double[] noise, double level, double[] noisy) {
        for (int i = 0; i < noisy.length; i++) noisy[i] = original[i] + level * noise[i];
    }
}
