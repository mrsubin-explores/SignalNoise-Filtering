import javax.swing.*;
import java.awt.*;

/**
 * Signal Noise & Filtering Simulator
 * Generates a sine wave, adds artificial noise, applies a digital filter,
 * and shows Original / Noisy / Filtered plots with SNR before and after.
 */
public class SignalNoiseSimulator extends JFrame {

    static final int MAX_HALF_WINDOW = 15;   // largest window is 31 samples -> 15 neighbours each side

    // sampling rate and duration are user-controlled (sliders); number of samples n = fs * duration
    double fs = 500;          // sampling rate in Hz
    double duration = 1.0;    // seconds shown
    int n = 500;              // number of samples

    double[] original = new double[n];
    double[] noise = new double[n];     // unit noise, scaled by slider
    double[] noisy = new double[n];
    double[] filtered = new double[n];
    double[] cleanFiltered = new double[n]; // filter applied to the noise-free signal, to measure signal loss

    JSlider freqSlider = new JSlider(1, 20, 5);
    JSlider ampSlider = new JSlider(1, 50, 10);     // /10 -> 0.1..5.0
    JSlider noiseSlider = new JSlider(0, 100, 30);  // /50 -> 0..2.0
    JSlider windowSlider = new JSlider(1, 16, 4);   // value v -> window 2v-1 (1,3,5,...,31), always odd
    JSlider rateSlider = new JSlider(100, 2000, 500);  // sampling rate in Hz
    JSlider durSlider = new JSlider(5, 50, 10);        // /10 -> 0.5..5.0 s
    JComboBox<String> filterBox = new JComboBox<>(new String[]{"Moving Average", "Median"});
    JCheckBox edgeBox = new JCheckBox("Include edge samples in SNR");
    javax.swing.border.TitledBorder snrBorder;
    JLabel freqL = new JLabel(), ampL = new JLabel(), noiseL = new JLabel(), winL = new JLabel(), rateL = new JLabel(), durL = new JLabel();
    JLabel snrBefore = new JLabel("SNR before: -");
    JLabel snrAfter = new JLabel("SNR after: -");
    JLabel improvement = new JLabel("Improvement: -");
    JLabel warnL = new JLabel(" ");
    PlotPanel p1, p2, p3;
    SignalGenerator generator = new SignalGenerator();

    public SignalNoiseSimulator() {
        super("Signal Noise & Filtering Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Controls
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBorder(BorderFactory.createTitledBorder("Controls"));
        int row = 0;
        addRow(controls, row++, "Frequency", freqSlider, freqL);
        addRow(controls, row++, "Amplitude", ampSlider, ampL);
        addRow(controls, row++, "Noise level", noiseSlider, noiseL);
        addRow(controls, row++, "Filter window", windowSlider, winL);
        addRow(controls, row++, "Sampling rate", rateSlider, rateL);
        addRow(controls, row++, "Duration", durSlider, durL);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = row; g.insets = new Insets(4, 4, 4, 4); g.anchor = GridBagConstraints.WEST;
        controls.add(new JLabel("Filter type"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        controls.add(filterBox, g);
        row++;
        JButton newNoise = new JButton("Generate New Noise");
        newNoise.addActionListener(e -> { regenerateNoise(); refreshSimulation(); });
        g.gridx = 1; g.gridy = row;
        controls.add(newNoise, g);
        row++;
        g.gridx = 1; g.gridy = row;
        controls.add(edgeBox, g);
        add(controls, BorderLayout.WEST);

        // Plots
        p1 = new PlotPanel("1. Original signal", new Color(0x1f77b4));
        p2 = new PlotPanel("2. Noisy signal", new Color(0xd62728));
        p3 = new PlotPanel("3. Filtered signal", new Color(0x2ca02c));
        JPanel plots = new JPanel(new GridLayout(3, 1, 4, 4));
        plots.add(p1); plots.add(p2); plots.add(p3);
        add(plots, BorderLayout.CENTER);

        // Results
        JPanel results = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 6));
        snrBorder = BorderFactory.createTitledBorder("Signal quality (SNR - first and last " + MAX_HALF_WINDOW + " samples excluded)");
        results.setBorder(snrBorder);
        warnL.setForeground(new Color(0xB00020));
        results.add(snrBefore); results.add(snrAfter); results.add(improvement); results.add(warnL);
        add(results, BorderLayout.SOUTH);

        javax.swing.event.ChangeListener cl = e -> refreshSimulation();
        for (JSlider s : new JSlider[]{freqSlider, ampSlider, noiseSlider, windowSlider, durSlider}) s.addChangeListener(cl);
        rateSlider.addChangeListener(e -> { updateFreqRange(); refreshSimulation(); });
        filterBox.addActionListener(e -> refreshSimulation());
        edgeBox.addActionListener(e -> refreshSimulation());

        updateFreqRange();
        regenerateNoise();
        refreshSimulation();
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    void addRow(JPanel p, int row, String name, JSlider s, JLabel valueLabel) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row; g.insets = new Insets(4, 4, 4, 4); g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; p.add(new JLabel(name), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; p.add(s, g);
        g.gridx = 2; g.fill = GridBagConstraints.NONE; valueLabel.setPreferredSize(new Dimension(140, 20)); p.add(valueLabel, g);
    }

    void regenerateNoise() {
        // uniform noise in [-1, +1]; scaled by the slider it stays within [-level, +level]
        generator.unitNoise(noise);
    }

    /** The highest selectable frequency follows the sampling rate (keeps at least 25 samples per cycle). */
    void updateFreqRange() {
        freqSlider.setMaximum(Math.max(1, (int) (rateSlider.getValue() / 25.0)));
    }

    /** Applies the filter chosen in the dropdown. */
    void applyFilter(double[] in, double[] out, int w) {
        if (filterBox.getSelectedIndex() == 0) Filters.movingAverage(in, out, w);
        else Filters.median(in, out, w);
    }

    void refreshSimulation() {
        fs = rateSlider.getValue();
        duration = durSlider.getValue() / 10.0;
        int newN = (int) Math.round(fs * duration);
        if (newN != n) {                       // sampling rate or duration changed: resize all arrays
            n = newN;
            original = new double[n]; noisy = new double[n]; filtered = new double[n];
            cleanFiltered = new double[n]; noise = new double[n];
            regenerateNoise();
        }
        double f = freqSlider.getValue();
        double a = ampSlider.getValue() / 10.0;
        double nl = noiseSlider.getValue() / 50.0;
        int w = 2 * windowSlider.getValue() - 1; // always odd, so the label shows the exact window used
        freqL.setText(f + " Hz");
        ampL.setText(String.format("%.1f V", a));
        noiseL.setText(String.format("\u00B1%.2f V", nl));
        winL.setText(w == 1 ? "1 sample (no filtering)" : w + " samples");
        rateL.setText((int) fs + " Hz");
        durL.setText(String.format("%.1f s", duration));

        // 1. generate clean signal: x(t) = A sin(2*pi*f*t)
        generator.sine(original, a, f, fs);
        // 2. add noise (original array is never modified)
        generator.addNoise(original, noise, nl, noisy);
        // 3. filter
        applyFilter(noisy, filtered, w);
        applyFilter(original, cleanFiltered, w); // same filter on the noise-free signal, to measure signal loss

        // SNR in dB = 10 log10(signal power / noise power)
        // the first and last MAX_HALF_WINDOW samples are skipped: the filter window is cut off there
        int skip = edgeBox.isSelected() ? 0 : MAX_HALF_WINDOW;   // checkbox: include the edge samples or not
        snrBorder.setTitle(edgeBox.isSelected() ? "Signal quality (SNR - all samples included)"
                : "Signal quality (SNR - first and last " + MAX_HALF_WINDOW + " samples excluded)");
        getContentPane().repaint();
        double before = SignalMetrics.snr(original, noisy, skip);
        double after = SignalMetrics.snr(original, filtered, skip);
        snrBefore.setText(fmt("SNR before", before));
        snrAfter.setText(fmt("SNR after", after));
        improvement.setText(Double.isInfinite(before) || Double.isInfinite(after) ? "Improvement: -"
                : String.format("Improvement: %+.2f dB", after - before));

        // warn when the filter itself removes more than 10% of the wanted signal (measured on the clean signal)
        double gain = SignalMetrics.signalGain(original, cleanFiltered, MAX_HALF_WINDOW);
        if (gain < 0) warnL.setText(String.format("Warning: filter inverts the signal (gain %.0f%%)", gain * 100));
        else if (gain < 0.90) warnL.setText(String.format("Warning: filter keeps only %.0f%% of the signal amplitude", gain * 100));
        else warnL.setText(" ");

        double range = Math.max(a + nl, 1.0) * 1.05; // same vertical scale on all plots
        p1.setData(original, range, duration); p2.setData(noisy, range, duration); p3.setData(filtered, range, duration);
    }

    static String fmt(String label, double v) {
        return Double.isInfinite(v) ? label + ": no noise" : String.format("%s: %.2f dB", label, v);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignalNoiseSimulator().setVisible(true));
    }
}
