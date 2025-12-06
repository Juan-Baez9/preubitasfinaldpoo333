package gui;

import manager.BoletaMasterSystem;
import tiquetes.Tiquete;
import gui.qr.QrCode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ImpresionTiqueteDialog extends JDialog {
	
	private static final int QR_ESCALA = 10;
    private static final int QR_BORDE = 8;

    private final Tiquete tiquete;
    private final BoletaMasterSystem sistema;
    private final Runnable onPrinted;
    private final JLabel qrLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();
    private LocalDateTime fechaImpresion;

    public ImpresionTiqueteDialog(Frame owner, BoletaMasterSystem sistema, Tiquete tiquete, Runnable onPrinted) {
        super(owner, "Imprimir boleta", true);
        this.tiquete = tiquete;
        this.sistema = sistema;
        this.onPrinted = onPrinted;
        this.fechaImpresion = tiquete.getFechaImpresion() != null ? tiquete.getFechaImpresion() : LocalDateTime.now();
        initUI();
    }

    private void initUI() {
    	setSize(980, 540);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        Color azulNoche = new Color(12, 27, 58);
        getContentPane().setBackground(azulNoche);

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        main.setBackground(azulNoche);
        add(main, BorderLayout.CENTER);

        JLabel banner = new JLabel("Boleta lista para impresión", SwingConstants.CENTER);
        banner.setFont(banner.getFont().deriveFont(Font.BOLD, 20f));
        banner.setForeground(new Color(230, 238, 255));
        banner.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
    
        add(banner, BorderLayout.NORTH);
        
        JLabel tituloEvento = new JLabel(tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "Evento", SwingConstants.CENTER);
        tituloEvento.setForeground(new Color(240, 240, 245));
        tituloEvento.setFont(tituloEvento.getFont().deriveFont(Font.BOLD, 22f));
        tituloEvento.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        add(tituloEvento, BorderLayout.SOUTH);

        JLabel imagenEvento = new JLabel();
        imagenEvento.setHorizontalAlignment(SwingConstants.CENTER);
        imagenEvento.setVerticalAlignment(SwingConstants.CENTER);
        imagenEvento.setOpaque(true);
        imagenEvento.setBackground(new Color(22, 42, 82));
        imagenEvento.setBorder(BorderFactory.createLineBorder(new Color(80, 110, 150), 2));
        ImageIcon img = ImagenEventoFactory.crearImagenEvento(
                tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "Evento",
                420, 260);
        imagenEvento.setIcon(img);
        main.add(imagenEvento, BorderLayout.CENTER);

        JPanel lateralTexto = new JPanel();
        lateralTexto.setLayout(new BoxLayout(lateralTexto, BoxLayout.Y_AXIS));
        lateralTexto.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lateralTexto.setBackground(new Color(18, 42, 84));
        infoLabel.setVerticalAlignment(SwingConstants.TOP);
        infoLabel.setText(construirTexto());
        infoLabel.setForeground(new Color(232, 238, 247));
        lateralTexto.add(infoLabel);
        lateralTexto.add(Box.createVerticalStrut(8));
        JLabel logo = new JLabel("BoletaMaster", SwingConstants.CENTER);
        logo.setForeground(new Color(247, 204, 64));
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 18f));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lateralTexto.add(logo);
        main.add(lateralTexto, BorderLayout.WEST);

        
        JPanel lateralQr = new JPanel(new BorderLayout());
        lateralQr.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lateralQr.setBackground(new Color(18, 42, 84));
        qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qrLabel.setVerticalAlignment(SwingConstants.CENTER);
        qrLabel.setOpaque(true);
        qrLabel.setPreferredSize(new Dimension(240, 240));
        qrLabel.setText("Imprime para generar QR");
        qrLabel.setForeground(new Color(200, 210, 230));
        qrLabel.setBackground(new Color(12, 27, 58));
        qrLabel.setBorder(BorderFactory.createLineBorder(new Color(247, 204, 64), 2));
        qrLabel.setPreferredSize(new Dimension(340, 340));
        lateralQr.add(qrLabel, BorderLayout.CENTER);
        main.add(lateralQr, BorderLayout.EAST);

        JButton imprimirBtn = new JButton("Imprimir");
        imprimirBtn.addActionListener(e -> procesarImpresion(imprimirBtn));
        imprimirBtn.setEnabled(!tiquete.isImpreso());
        imprimirBtn.setBackground(new Color(247, 204, 64));
        imprimirBtn.setForeground(new Color(20, 30, 48));
        imprimirBtn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        add(imprimirBtn, BorderLayout.WEST);

        if (tiquete.isImpreso()) {
            mostrarQr(construirContenidoQr(fechaImpresion));
        }
    }

    private String construirTexto() {
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtImp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String evento = tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "(sin evento)";
        String fechaEvento = tiquete.getEvento() != null && tiquete.getEvento().getFecha() != null
                ? fmtFecha.format(tiquete.getEvento().getFecha()) : "N/D";
        String fechaImp = tiquete.isImpreso() && fechaImpresion != null ? fmtImp.format(fechaImpresion) : "Pendiente";
        StringBuilder sb = new StringBuilder("<html><h2 style='color:#f7cc40'>Datos del tiquete</h2>");
        sb.append("<p><b>Evento:</b> ").append(evento).append("</p>");
        sb.append("<p><b>ID Tiquete:</b> ").append(tiquete.getIdTiquete()).append("</p>");
        sb.append("<p><b>Fecha evento:</b> ").append(fechaEvento).append("</p>");
        sb.append("<p><b>Fecha impresión:</b> ").append(fechaImp).append("</p>");
        sb.append(tiquete.isImpreso() ? "<p style='color:red'>Ya impreso</p>" : "");
        sb.append("</html>");
        return sb.toString();
    }

    private void procesarImpresion(JButton boton) {
        if (tiquete.isImpreso()) {
            JOptionPane.showMessageDialog(this, "Este tiquete ya fue impreso", "Impresión", JOptionPane.WARNING_MESSAGE);
            boton.setEnabled(false);
            return;
        }
        fechaImpresion = LocalDateTime.now();
        tiquete.setFechaImpresion(fechaImpresion);
        if (mostrarQr(construirContenidoQr(fechaImpresion))) {
            sistema.marcarTiqueteImpreso(tiquete, fechaImpresion);
            infoLabel.setText(construirTexto());
            boton.setEnabled(false);
            if (onPrinted != null) {
                onPrinted.run();
            }
        }
    }

    private String construirContenidoQr(LocalDateTime fecha) {
        DateTimeFormatter fmtEvento = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtImp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaEvento = tiquete.getEvento() != null && tiquete.getEvento().getFecha() != null
        		  ? tiquete.getEvento().getFecha().format(fmtEvento) : "N/D";
        String evento = tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "(sin evento)";
        String query = String.format("evento=%s&id=%s&fechaEvento=%s&fechaExpedicion=%s",
                URLEncoder.encode(evento, StandardCharsets.UTF_8),
                URLEncoder.encode(String.valueOf(tiquete.getIdTiquete()), StandardCharsets.UTF_8),
                URLEncoder.encode(fechaEvento, StandardCharsets.UTF_8),
                URLEncoder.encode(fecha.format(fmtImp), StandardCharsets.UTF_8));
        // El contenido se codifica como una URL completa para que la cámara del teléfono lo detecte
        // inmediatamente como enlace. La propia URL incluye todos los datos del tiquete como parámetros
        // de consulta legibles.
        return "https://boletamaster.app/ticket?" + query;
    }

    private boolean mostrarQr(String contenido) {
        try {
        	 QrCode qr = QrCode.encodeText(contenido, QrCode.Ecc.HIGH);
             int tamano = (qr.size + QR_BORDE * 2) * QR_ESCALA;
            BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tamano, tamano);
            g.setColor(Color.BLACK);
            for (int y = 0; y < qr.size; y++) {
                for (int x = 0; x < qr.size; x++) {
                    if (qr.getModule(x, y)) {
                    	g.fillRect((x + QR_BORDE) * QR_ESCALA, (y + QR_BORDE) * QR_ESCALA, QR_ESCALA, QR_ESCALA);
                    }
                }
            }
            g.dispose();
            ImageIcon icon = new ImageIcon(img);
            qrLabel.setText(null);
            qrLabel.setIcon(icon);
            qrLabel.setPreferredSize(new Dimension(icon.getIconWidth() + 16, icon.getIconHeight() + 16));
            qrLabel.revalidate();
            return true;
        } catch (Exception ex) {
            qrLabel.setText("QR no disponible");
            qrLabel.setIcon(null);
            JOptionPane.showMessageDialog(this, "No se pudo generar el código QR: " + ex.getMessage(),
                    "QR", JOptionPane.ERROR_MESSAGE);
            return false;
        	}
    	}
    }