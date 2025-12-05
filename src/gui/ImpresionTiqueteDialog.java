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
        setSize(900, 500);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 246, 252));

        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(main, BorderLayout.CENTER);

        JLabel banner = new JLabel("Boleta lista para impresión", SwingConstants.CENTER);
        banner.setFont(banner.getFont().deriveFont(Font.BOLD, 18f));
        banner.setForeground(new Color(0, 70, 110));
        add(banner, BorderLayout.NORTH);

        JLabel imagenEvento = new JLabel();
        imagenEvento.setHorizontalAlignment(SwingConstants.CENTER);
        imagenEvento.setVerticalAlignment(SwingConstants.CENTER);
        imagenEvento.setOpaque(true);
        imagenEvento.setBackground(new Color(223, 234, 245));
        imagenEvento.setBorder(BorderFactory.createLineBorder(new Color(180, 200, 220)));
        ImageIcon img = ImagenEventoFactory.crearImagenEvento(
                tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "Evento",
                420, 260);
        imagenEvento.setIcon(img);
        main.add(imagenEvento, BorderLayout.CENTER);

        JPanel lateralTexto = new JPanel();
        lateralTexto.setLayout(new BoxLayout(lateralTexto, BoxLayout.Y_AXIS));
        lateralTexto.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lateralTexto.setBackground(new Color(250, 252, 255));
        infoLabel.setVerticalAlignment(SwingConstants.TOP);
        infoLabel.setText(construirTexto());
        lateralTexto.add(infoLabel);
        main.add(lateralTexto, BorderLayout.WEST);

        JPanel lateralQr = new JPanel(new BorderLayout());
        lateralQr.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lateralQr.setBackground(new Color(250, 252, 255));
        qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qrLabel.setVerticalAlignment(SwingConstants.CENTER);
        qrLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));
        lateralQr.add(qrLabel, BorderLayout.CENTER);
        main.add(lateralQr, BorderLayout.EAST);

        JButton imprimirBtn = new JButton("Imprimir");
        imprimirBtn.addActionListener(e -> procesarImpresion(imprimirBtn));
        imprimirBtn.setEnabled(!tiquete.isImpreso());
        add(imprimirBtn, BorderLayout.SOUTH);

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
        StringBuilder sb = new StringBuilder("<html><h2 style='color:#0c4b75'>BoletaMaster</h2>");
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
        mostrarQr(construirContenidoQr(fechaImpresion));
        sistema.marcarTiqueteImpreso(tiquete, fechaImpresion);
        infoLabel.setText(construirTexto());
        boton.setEnabled(false);
        if (onPrinted != null) {
            onPrinted.run();
        }
    }

    private String construirContenidoQr(LocalDateTime fecha) {
        String fechaEvento = tiquete.getEvento() != null && tiquete.getEvento().getFecha() != null
                ? tiquete.getEvento().getFecha().toString() : "N/D";
        return String.format("Evento:%s\nID:%d\nF.Evento:%s\nF.Expedición:%s",
                tiquete.getEvento() != null ? tiquete.getEvento().getNombre() : "(sin evento)",
                tiquete.getIdTiquete(),
                fechaEvento,
                fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    private void mostrarQr(String contenido) {
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
        qrLabel.setIcon(new ImageIcon(img));
    }
}