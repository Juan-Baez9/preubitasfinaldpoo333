package gui;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Genera imágenes decorativas y consistentes para eventos y pantallas de la app
 * sin requerir archivos externos. Produce gradientes suaves con textos
 * legibles, de modo que cada evento tenga su propia identidad visual.
 */
public class ImagenEventoFactory {

    private static final Map<String, Color[]> PALETA_EVENTOS = new HashMap<>();
    private static final Path BASE_IMAGENES = Path.of("data", "imagenes");


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
        BufferedImage personalizada = cargarPersonalizada(clave, width, height);
        if (personalizada != null) {
            return new ImageIcon(personalizada);
        }
        Color[] colores = PALETA_EVENTOS.getOrDefault(clave, coloresAleatorios(clave));
        return crearLamina(nombreEvento == null ? "Evento" : nombreEvento,
                "Disfrútalo sin filas", width, height, colores[0], colores[1]);
    }
    /**
     * Permite que el usuario coloque imágenes personalizadas en {@code data/imagenes}.
     * El archivo debe llamarse igual al evento en minúsculas y sin acentos/espacios
     * (por ejemplo: {@code concierto los alpes -> concierto-los-alpes.jpg}).
     */
    private static BufferedImage cargarPersonalizada(String nombreEvento, int width, int height) {
        if (nombreEvento == null || nombreEvento.isBlank()) {
            return null;
        }
        String base = nombreEvento
                .toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            return null;
        }
        String[] extensiones = {"png", "jpg", "jpeg"};
        for (String ext : extensiones) {
            Path posible = BASE_IMAGENES.resolve(base + "." + ext);
            if (Files.exists(posible)) {
                try {
                    BufferedImage original = ImageIO.read(posible.toFile());
                    return escalar(original, width, height);
                } catch (IOException ignored) {
                    // Si falla la lectura, se usa la imagen generada por defecto
                }
            }
        }
        return null;
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
    private static BufferedImage escalar(BufferedImage original, int width, int height) {
        int ow = original.getWidth();
        int oh = original.getHeight();
        double escala = Math.min((double) width / ow, (double) height / oh);
        int nw = (int) (ow * escala);
        int nh = (int) (oh * escala);
        BufferedImage destino = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 27, 58));
        g.fillRoundRect(0, 0, width, height, 16, 16);
        g.drawImage(original, (width - nw) / 2, (height - nh) / 2, nw, nh, null);
        g.dispose();
        return destino;
    }
}