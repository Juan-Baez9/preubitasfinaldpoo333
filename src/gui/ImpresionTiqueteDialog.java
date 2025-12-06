package gui;

import manager.BoletaMasterSystem;
import tiquetes.Tiquete;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.EnumMap;
import java.util.Map;

public class ImpresionTiqueteDialog extends JDialog {
	 private static final int QR_ESCALA = 2;
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
	@@ -156,80 +158,70 @@ public class ImpresionTiqueteDialog extends JDialog {
	        DateTimeFormatter fmtImp = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	        String evento = tiquete.getEvento() != null
	                ? tiquete.getEvento().getNombre()
	                : "(sin evento)";

	        String fechaEvento = tiquete.getEvento() != null && tiquete.getEvento().getFecha() != null
	                ? tiquete.getEvento().getFecha().format(fmtEvento)
	                : "N/D";

	        String fechaImp = fecha.format(fmtImp);

	        StringBuilder sb = new StringBuilder();
	        sb.append("Evento: ").append(evento).append("\n");
	        sb.append("ID:").append(tiquete.getIdTiquete()).append("\n");
	        sb.append("F.Evento:").append(fechaEvento).append("\n");
	        sb.append("F.Expedicion:").append(fechaImp);

	        return sb.toString();
	    }


	    private boolean mostrarQr(String contenido) {
	        try {
	            int size = 320; // tamaño en píxeles del QR

	            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
	            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
	            hints.put(EncodeHintType.MARGIN, QR_BORDE);
	            
	            QRCodeWriter writer = new QRCodeWriter();
	            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, size, size, hints);

	            BufferedImage img = convertirABufferedImage(matrix);
	            
	            Image scaledImage = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
	            ImageIcon icon = new ImageIcon(scaledImage);
	            qrLabel.setText(null);
	            qrLabel.setIcon(icon);
	            qrLabel.setPreferredSize(new Dimension(icon.getIconWidth() + 24, icon.getIconHeight() + 24));
	            qrLabel.revalidate();
	            qrLabel.repaint();

	            return true;
	        } catch (WriterException e) {
	            qrLabel.setText("QR no disponible");
	            qrLabel.setIcon(null);
	            JOptionPane.showMessageDialog(this,
	                    "No se pudo generar el código QR: " + e.getMessage(),
	                    "QR", JOptionPane.ERROR_MESSAGE);
	            return false;
	        }
	    }

	    private BufferedImage convertirABufferedImage(BitMatrix matrix) {
	    	MatrixToImageConfig config = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);
	        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, config);

	        int renderWidth = matrix.getWidth() * QR_ESCALA;
	        int renderHeight = matrix.getHeight() * QR_ESCALA;
	        BufferedImage escalada = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
	        Graphics2D g = escalada.createGraphics();
	        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	        g.drawImage(image, 0, 0, renderWidth, renderHeight, null);
	        g.dispose();

	        return escalada;
	    }

	    
	    
	 
	 
	 
	 
	 
}