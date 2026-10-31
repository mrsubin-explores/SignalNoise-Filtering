import javax.swing.*;
import java.awt.*;

/** Simple waveform plot drawn with Java 2D. */
public class PlotPanel extends JPanel {
    final String title; final Color color;
    double[] data = new double[0]; double range = 1, duration = 1;
    PlotPanel(String title, Color color) {
        this.title = title; this.color = color;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    }
    void setData(double[] d, double range, double duration) { this.data = d; this.range = range; this.duration = duration; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        // margins keep titles and axis labels clear of the waveform
        int left = 50, top = 24, bottom = h - 20, right = w - 10;
        int plotW = right - left;
        double half = (bottom - top) / 2.0, midY = (top + bottom) / 2.0;
        FontMetrics fm = g.getFontMetrics();

        g.setColor(Color.DARK_GRAY);
        g.drawString(title, left, 16);

        // vertical gridlines: 10 equal time divisions
        g.setColor(new Color(230, 230, 230));
        for (int i = 0; i <= 10; i++) {
            int x = left + (int) Math.round(i * plotW / 10.0);
            g.drawLine(x, top, x, bottom);
        }
        // horizontal gridlines with amplitude labels: +range, +range/2, 0, -range/2, -range
        for (int i = 0; i <= 4; i++) {
            int y = (int) Math.round(midY - (2 - i) * half / 2.0);
            double val = (2 - i) * range / 2.0;
            g.setColor(i == 2 ? Color.GRAY : new Color(230, 230, 230));
            g.drawLine(left, y, right, y);
            String s = i == 2 ? "0 V" : String.format("%+.1f V", val);
            g.setColor(Color.DARK_GRAY);
            g.drawString(s, left - 4 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
        }
        // time axis: tick labels every 2 divisions from 0 to the duration
        for (int i = 0; i <= 10; i += 2) {
            String label = String.format("%.1f", duration * i / 10.0) + (i == 10 ? " s" : "");
            int tx = left + (int) Math.round(i * plotW / 10.0);
            int lx = Math.max(2, Math.min(w - fm.stringWidth(label) - 2, tx - fm.stringWidth(label) / 2));
            g.drawLine(tx, bottom, tx, bottom + 4);
            g.drawString(label, lx, h - 4);
        }

        if (data.length < 2) return;
        g.setColor(color); g.setStroke(new BasicStroke(1.6f));
        double scale = half / range;
        int px = left, py = (int) Math.round(midY - data[0] * scale);
        for (int i = 1; i < data.length; i++) {
            int x = left + (int) ((long) i * plotW / (data.length - 1));
            int y = (int) Math.round(midY - data[i] * scale);
            g.drawLine(px, py, x, y);
            px = x; py = y;
        }
    }
}
