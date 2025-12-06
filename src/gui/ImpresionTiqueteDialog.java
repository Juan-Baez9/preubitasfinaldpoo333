package gui;

import manager.BoletaMasterSystem;
import tiquetes.Tiquete;
import gui.qr.QrCode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImpresionTiqueteDialog extends JDialog {

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
        qrLabel.setBackground(new Color(12, 27, 58));
        qrLabel.setBorder(BorderFactory.createLineBorder(new Color(247, 204, 64), 2));
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
        String fechaEvento = tiquete.getEvento() != null && tiquete.getEvento().getFecha() != null
        		  ? tiquete.getEvento().getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/D";
        return String.format("Evento:%s\nID:%d\nF.Evento:%s\nF.Expedición:%s",
                tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "(sin evento)",
                tiquete.getIdTiquete(),
                fechaEvento,
                fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    private boolean mostrarQr(String contenido) {
        try {
            QrCode qr = QrCode.encodeText(contenido, QrCode.Ecc.MEDIUM);
            int escala = 6;
            int borde = 4;
            int tamano = (qr.size + borde * 2) * escala;
            BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tamano, tamano);
            g.setColor(Color.BLACK);
            for (int y = 0; y < qr.size; y++) {
                for (int x = 0; x < qr.size; x++) {
                    if (qr.getModule(x, y)) {
                        g.fillRect((x + borde) * escala, (y + borde) * escala, escala, escala);
                    }
                }
            }
            g.dispose();
            qrLabel.setText(null);
            qrLabel.setIcon(new ImageIcon(img));
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