package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Genera imágenes decorativas y consistentes para eventos y pantallas de la app
 * sin requerir archivos externos. Produce gradientes suaves con textos
 * legibles, de modo que cada evento tenga su propia identidad visual.
 */
public class ImagenEventoFactory {

    private static final Map<String, Color[]> PALETA_EVENTOS = new HashMap<>();

    static {
        PALETA_EVENTOS.put("concierto los alpes", new Color[]{new Color(0x0B3C5D), new Color(0x3A7BD5)});
        PALETA_EVENTOS.put("festival andino", new Color[]{new Color(0x5C258D), new Color(0x4389A2)});
        PALETA_EVENTOS.put("final liga capital", new Color[]{new Color(0x0B486B), new Color(0xF56217)});
        PALETA_EVENTOS.put("obra teatro del sol", new Color[]{new Color(0x2C3E50), new Color(0xFD746C)});
    }

    private ImagenEventoFactory() {}

    public static ImageIcon crearBannerLogin(int width, int height) {
        return crearLamina("BoletaMaster", "Entradas listas para ti", width, height,
                new Color(0x0B1C3D), new Color(0x1E5F74));
    }

    public static ImageIcon crearImagenEvento(String nombreEvento, int width, int height) {
        String clave = nombreEvento == null ? "evento" : nombreEvento.toLowerCase();
        Color[] colores = PALETA_EVENTOS.getOrDefault(clave, coloresAleatorios(clave));
        return crearLamina(nombreEvento == null ? "Evento" : nombreEvento,
                "Disfrútalo sin filas", width, height, colores[0], colores[1]);
    }

    private static Color[] coloresAleatorios(String semilla) {
        Random r = new Random(semilla.hashCode());
        Color base = new Color(100 + r.nextInt(120), 80 + r.nextInt(120), 120 + r.nextInt(120));
        Color contraste = new Color(Math.min(255, base.getRed() + 60),
                Math.min(255, base.getGreen() + 30),
                Math.min(255, base.getBlue() + 40));
        return new Color[]{base, contraste};
    }

    private static ImageIcon crearLamina(String titulo, String subtitulo, int width, int height,
                                         Color inicio, Color fin) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gradiente = new GradientPaint(0, 0, inicio, width, height, fin);
        g.setPaint(gradiente);
        g.fill(new RoundRectangle2D.Double(0, 0, width, height, 28, 28));

        g.setColor(new Color(255, 255, 255, 70));
        g.fillOval(width - 160, -40, 240, 200);
        g.fillOval(-60, height - 160, 220, 200);

        g.setColor(new Color(255, 255, 255, 110));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(10, 10, width - 20, height - 20, 22, 22);

        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, Math.min(30, width * 0.08f)));
        g.drawString(titulo, 24, height / 2 - 8);

        g.setColor(new Color(255, 255, 255, 210));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, Math.min(18, width * 0.05f)));
        g.drawString(subtitulo, 24, height / 2 + 22);

        g.dispose();
        return new ImageIcon(img);
    }
}