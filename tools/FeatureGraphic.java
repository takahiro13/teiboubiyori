import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Play ストア用フィーチャーグラフィック (1024x500) を作る。
 * 使い方: java tools/FeatureGraphic.java <bold-ttf-path> store/
 */
public class FeatureGraphic {
    static final Color OCEAN = new Color(0x006A9E);
    static final Color OCEAN_DARK = new Color(0x004B70);
    static final Color WAVE_LIGHT = new Color(255, 255, 255, 200);
    static final Color WAVE_DIM = new Color(255, 255, 255, 110);

    // app/src/main/res/drawable/ic_launcher_foreground.xml と同じ図形 (108x108 の座標系)。StoreAssets.java と同じ。
    static void drawFish(Graphics2D g, double cx, double cy, double scale) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-54, -54);
        g.setColor(Color.WHITE);
        Path2D body = new Path2D.Double();
        body.moveTo(32, 46);
        body.curveTo(32, 38, 43, 33, 52, 33);
        body.curveTo(61, 33, 69, 39, 71, 46);
        body.curveTo(69, 53, 61, 59, 52, 59);
        body.curveTo(43, 59, 32, 54, 32, 46);
        body.closePath();
        g.fill(body);
        Path2D tail = new Path2D.Double();
        tail.moveTo(69, 46); tail.lineTo(83, 35); tail.lineTo(83, 57); tail.closePath();
        g.fill(tail);
        g.setColor(OCEAN);
        g.fill(new Ellipse2D.Double(41 - 2.4, 43.5 - 2.4, 4.8, 4.8));
        g.setTransform(old);
    }

    static void drawWaveLine(Graphics2D g, double y, double width, Color c, float strokeWidth) {
        g.setColor(c);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D w = new Path2D.Double();
        double step = 48;
        w.moveTo(-step, y);
        double x = -step;
        boolean up = true;
        w.moveTo(x, y);
        while (x < width + step) {
            double nx = x + step;
            double cy1 = y + (up ? -18 : 18);
            w.quadTo(x + step / 2, cy1, nx, y);
            x = nx;
            up = !up;
        }
        g.draw(w);
    }

    static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    public static void main(String[] args) throws Exception {
        Font boldBase = Font.createFont(Font.TRUETYPE_FONT, new File(args[0]));
        File out = new File(args.length > 1 ? args[1] : "store");
        out.mkdirs();

        int W = 1024, H = 500;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // 背景: 縦グラデーション (アイコンと同じ OCEAN を基調に、少し深みを出す)
        g.setPaint(new GradientPaint(0, 0, OCEAN, 0, H, OCEAN_DARK));
        g.fillRect(0, 0, W, H);

        // 波の装飾 (下部、アイコンと同じモチーフ)
        drawWaveLine(g, H - 46, W, WAVE_DIM, 10f);
        drawWaveLine(g, H - 20, W, WAVE_LIGHT, 12f);

        // 魚アイコン (左寄り)
        drawFish(g, 170, 230, 2.55);

        // タイトル
        Font title = boldBase.deriveFont(92f);
        g.setFont(title);
        g.setColor(Color.WHITE);
        int titleX = 330;
        int titleBaseline = 195;
        g.drawString("堤防日和", titleX, titleBaseline);

        // タグライン
        Font sub = boldBase.deriveFont(34f);
        g.setFont(sub);
        g.setColor(new Color(255, 255, 255, 235));
        g.drawString("港の天気・風・波・潮を", titleX, titleBaseline + 62);
        g.drawString("行く前にひと目で", titleX, titleBaseline + 108);

        g.dispose();
        File f = new File(out, "feature-graphic-1024x500.png");
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f + " (" + f.length() / 1024 + " KB)");
    }
}
