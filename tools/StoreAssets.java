import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Play ストア用の画像を作る。使い方: java tools/StoreAssets.java store/ */
public class StoreAssets {
    static final Color OCEAN = new Color(0x006A9E);

    /** app/src/main/res/drawable/ic_launcher_foreground.xml と同じ図形 (108x108 の座標系)。 */
    static void drawFish(Graphics2D g, double cx, double cy, double scale, double s) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-54, -54); // 108x108 の中心を原点に合わせる
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
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            double y = 72 + i * 9;
            g.setColor(i == 0 ? Color.WHITE : new Color(255, 255, 255, 153));
            Path2D w = new Path2D.Double();
            w.moveTo(31, y);
            w.quadTo(39, y - 8, 47, y);
            w.quadTo(55, y + 8, 63, y);
            w.quadTo(71, y - 8, 79, y);
            g.draw(w);
        }
        g.setTransform(old);
    }

    static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "store");
        out.mkdirs();

        // ストア用アイコン 512x512 (角丸はPlay側で付くので全面塗り)
        BufferedImage icon = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(icon);
        g.setColor(OCEAN);
        g.fillRect(0, 0, 512, 512);
        drawFish(g, 256, 256, 512 / 108.0 * 1.3, 1);
        g.dispose();
        ImageIO.write(icon, "png", new File(out, "icon-512.png"));
        System.out.println("wrote " + new File(out, "icon-512.png"));
    }
}
