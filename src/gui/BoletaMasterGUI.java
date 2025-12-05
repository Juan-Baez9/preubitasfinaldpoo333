package gui;

import Cliente.Cliente;
import manager.BoletaMasterSystem;
import marketPlace.ContraOferta;
import marketPlace.OfertaMarketPlace;
import marketPlace.EstadoContraOferta;
import tiquetes.Tiquete;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class BoletaMasterGUI extends JFrame {

    private final BoletaMasterSystem sistema;
    private Cliente clienteActual;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private DefaultListModel<Tiquete> tiqueteModel;
    private JLabel detalleTiquete;
    private JButton imprimirBtn;

    private DefaultListModel<OfertaMarketPlace> ofertasActivasModel;
    private DefaultListModel<OfertaMarketPlace> misOfertasModel;
    private DefaultListModel<ContraOferta> contraofertasModel;
    private JTextField valorContraOfertaField;
    private JTextField valorNuevaOfertaField;
    private JList<Tiquete> listaPublicables;

    public BoletaMasterGUI() {
        super("BoletaMaster - Plataforma gráfica");
        this.sistema = BoletaMasterSystem.desdeDirectorio("data");
        this.sistema.cargarDatos();
        initUI();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sistema.guardarDatos();
                dispose();
            }
        });
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        content.add(crearPanelLogin(), "login");
        content.add(crearPanelCliente(), "cliente");
        cardLayout.show(content, "login");
    }

    private JPanel crearPanelLogin() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(new Color(8, 20, 48));

        JLabel hero = new JLabel(ImagenEventoFactory.crearBannerLogin(360, 360));
        hero.setHorizontalAlignment(SwingConstants.CENTER);
        hero.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(hero, BorderLayout.WEST);

        JPanel tarjeta = new JPanel(new GridBagLayout());
        tarjeta.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        tarjeta.setBackground(new Color(255, 255, 255, 230));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Bienvenido a BoletaMaster", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        titulo.setForeground(new Color(10, 40, 90));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        tarjeta.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        tarjeta.add(new JLabel("Usuario"), gbc);
        JTextField usuarioField = new JTextField();
        gbc.gridx = 1;
        tarjeta.add(usuarioField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        tarjeta.add(new JLabel("Contraseña"), gbc);
        JPasswordField passField = new JPasswordField();
        gbc.gridx = 1;
        tarjeta.add(passField, gbc);

        JButton ingresar = new JButton("Ingresar como cliente");
        ingresar.addActionListener(e -> {
            String login = usuarioField.getText().trim();
            String pass = new String(passField.getPassword());
            clienteActual = sistema.autenticarCliente(login, pass).orElse(null);
            if (clienteActual == null) {
                JOptionPane.showMessageDialog(this, "Credenciales inválidas", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                refrescarDatosCliente();
                cardLayout.show(content, "cliente");
            }
        });
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        tarjeta.add(ingresar, gbc);

        panel.add(tarjeta, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelCliente() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("Panel de cliente", SwingConstants.LEFT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(titulo, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Mis tiquetes", crearTabTiquetes());
        tabs.addTab("Marketplace", crearTabMarketplace());
        tabs.addTab("Mis ofertas", crearTabMisOfertas());
        tabs.addTab("Publicar oferta", crearTabPublicar());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTabTiquetes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        tiqueteModel = new DefaultListModel<>();
        JList<Tiquete> lista = new JList<>(tiqueteModel);
        lista.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String evento = value.getEvento() != null ? value.getEvento().getNombre() : "(sin evento)";
            String label = "#" + value.getIdTiquete() + " - " + evento;
            if (value.isImpreso()) {
                label += " [IMPRESO]";
            }
            JLabel lbl = new JLabel(label);
            if (isSelected) {
                lbl.setOpaque(true);
                lbl.setBackground(new Color(220, 235, 255));
            }
            return lbl;
        });
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalleTiquete(lista.getSelectedValue());
            }
        });
        panel.add(new JScrollPane(lista), BorderLayout.WEST);

        detalleTiquete = new JLabel("Seleccione un tiquete para ver detalles", SwingConstants.CENTER);
        detalleTiquete.setVerticalAlignment(SwingConstants.TOP);
        detalleTiquete.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(detalleTiquete, BorderLayout.CENTER);

        imprimirBtn = new JButton("Imprimir boleta");
        imprimirBtn.addActionListener(e -> {
            Tiquete seleccionado = lista.getSelectedValue();
            if (seleccionado != null) {
                abrirImpresion(seleccionado);
            }
        });
        panel.add(imprimirBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearTabMarketplace() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        ofertasActivasModel = new DefaultListModel<>();
        JList<OfertaMarketPlace> lista = new JList<>(ofertasActivasModel);
        lista.setCellRenderer((l, v, i, s, f) -> {
            String eventos = v.getTiquetes().stream()
                    .map(t -> t.getEvento() != null ? t.getEvento().getNombre() : "(sin evento)")
                    .distinct().collect(Collectors.joining(", "));
            String texto = String.format("%s - $%.2f - Vendedor: %s", v.getId(), v.getPrecioInicial(), v.getVendedor().getLogin());
            JLabel lbl = new JLabel(texto + " | " + eventos);
            if (s) { lbl.setOpaque(true); lbl.setBackground(new Color(220, 235, 255)); }
            return lbl;
        });
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new GridLayout(3, 2, 8, 8));
        JButton comprar = new JButton("Comprar oferta seleccionada");
        comprar.addActionListener(e -> {
            OfertaMarketPlace oferta = lista.getSelectedValue();
            if (oferta == null) return;
            try {
                sistema.comprarOferta(clienteActual, oferta.getId());
                refrescarDatosCliente();
                JOptionPane.showMessageDialog(this, "Compra realizada con éxito", "Compra", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        acciones.add(comprar);

        valorContraOfertaField = new JTextField();
        acciones.add(new JLabel("Valor para contraoferta"));
        acciones.add(valorContraOfertaField);
        JButton contraBtn = new JButton("Enviar contraoferta");
        contraBtn.addActionListener(e -> {
            OfertaMarketPlace oferta = lista.getSelectedValue();
            if (oferta == null) return;
            try {
                double monto = Double.parseDouble(valorContraOfertaField.getText());
                sistema.crearContraoferta(clienteActual, oferta.getId(), monto);
                valorContraOfertaField.setText("");
                JOptionPane.showMessageDialog(this, "Contraoferta enviada", "Marketplace", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        acciones.add(contraBtn);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearTabMisOfertas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        misOfertasModel = new DefaultListModel<>();
        JList<OfertaMarketPlace> listaOfertas = new JList<>(misOfertasModel);
        listaOfertas.setCellRenderer((l, v, i, s, f) -> new JLabel(v.getId() + " - Estado: " + v.getEstado() + " - $" + v.getPrecioInicial()));
        panel.add(new JScrollPane(listaOfertas), BorderLayout.WEST);

        contraofertasModel = new DefaultListModel<>();
        JList<ContraOferta> listaContra = new JList<>(contraofertasModel);
        listaContra.setCellRenderer((l, v, i, s, f) -> new JLabel(v.getId() + " - " + v.getComprador().getLogin() + " $" + v.getMonto() + " (" + v.getEstado() + ")"));
        panel.add(new JScrollPane(listaContra), BorderLayout.CENTER);

        listaOfertas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarContraofertas(listaOfertas.getSelectedValue());
            }
        });

        JPanel acciones = new JPanel(new GridLayout(1, 3, 8, 8));
        JButton cancelar = new JButton("Cancelar oferta");
        cancelar.addActionListener(e -> {
            OfertaMarketPlace oferta = listaOfertas.getSelectedValue();
            if (oferta == null) return;
            try {
                sistema.cancelarOfertaPorVendedor(clienteActual, oferta.getId());
                refrescarDatosCliente();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton aceptar = new JButton("Aceptar contraoferta");
        aceptar.addActionListener(e -> {
            OfertaMarketPlace oferta = listaOfertas.getSelectedValue();
            ContraOferta contra = listaContra.getSelectedValue();
            if (oferta == null || contra == null) return;
            try {
                sistema.aceptarContraoferta(clienteActual, oferta.getId(), contra.getId());
                refrescarDatosCliente();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton rechazar = new JButton("Rechazar contraoferta");
        rechazar.addActionListener(e -> {
            OfertaMarketPlace oferta = listaOfertas.getSelectedValue();
            ContraOferta contra = listaContra.getSelectedValue();
            if (oferta == null || contra == null) return;
            try {
                sistema.rechazarContraoferta(clienteActual, oferta.getId(), contra.getId());
                refrescarDatosCliente();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        acciones.add(cancelar);
        acciones.add(aceptar);
        acciones.add(rechazar);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearTabPublicar() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        listaPublicables = new JList<>(new DefaultListModel<>());
        listaPublicables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(listaPublicables), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Precio inicial"));
        valorNuevaOfertaField = new JTextField();
        form.add(valorNuevaOfertaField);
        JButton publicar = new JButton("Publicar oferta");
        publicar.addActionListener(e -> publicarOferta());
        form.add(new JLabel());
        form.add(publicar);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void mostrarDetalleTiquete(Tiquete t) {
        if (t == null) {
            detalleTiquete.setText("Seleccione un tiquete");
            imprimirBtn.setEnabled(false);
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtImp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String evento = t.getEvento() != null ? t.getEvento().getNombre() : "(sin evento)";
        String fecha = t.getEvento() != null && t.getEvento().getFecha() != null ? fmt.format(t.getEvento().getFecha()) : "N/D";
        String fechaImp = t.getFechaImpresion() != null ? fmtImp.format(t.getFechaImpresion()) : "Pendiente";
        StringBuilder sb = new StringBuilder("<html><h2>" + evento + "</h2>");
        sb.append("<p>ID: ").append(t.getIdTiquete()).append("</p>");
        sb.append("<p>Estado: ").append(t.getEstado()).append(t.isImpreso() ? " (IMPRESO)" : "").append("</p>");
        sb.append("<p>Fecha evento: ").append(fecha).append("</p>");
        sb.append("<p>Fecha impresión: ").append(fechaImp).append("</p>");
        sb.append("</html>");
        detalleTiquete.setText(sb.toString());
        imprimirBtn.setEnabled(true);
    }

    private void refrescarDatosCliente() {
        if (clienteActual == null) return;
        tiqueteModel.clear();
        for (Tiquete t : clienteActual.verTiquetes()) {
            tiqueteModel.addElement(t);
        }
        ofertasActivasModel.clear();
        for (OfertaMarketPlace o : sistema.obtenerOfertasActivas()) {
            if (!o.getVendedor().equals(clienteActual)) {
                ofertasActivasModel.addElement(o);
            }
        }
        misOfertasModel.clear();
        for (OfertaMarketPlace o : sistema.obtenerOfertasPorVendedor(clienteActual)) {
            misOfertasModel.addElement(o);
        }
        actualizarPublicables();
    }

    private void cargarContraofertas(OfertaMarketPlace oferta) {
        contraofertasModel.clear();
        if (oferta == null) return;
        for (ContraOferta c : oferta.getContraofertas()) {
            if (c.getEstado() == EstadoContraOferta.PENDIENTE) {
                contraofertasModel.addElement(c);
            }
        }
    }

    private void actualizarPublicables() {
        DefaultListModel<Tiquete> model = (DefaultListModel<Tiquete>) listaPublicables.getModel();
        model.clear();
        for (Tiquete t : clienteActual.verTiquetes()) {
            if (!t.isImpreso()) {
                model.addElement(t);
            }
        }
    }

    private void publicarOferta() {
        List<Tiquete> seleccion = listaPublicables.getSelectedValuesList();
        if (seleccion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un tiquete", "Marketplace", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double precio = Double.parseDouble(valorNuevaOfertaField.getText());
            List<Integer> ids = seleccion.stream().map(Tiquete::getIdTiquete).collect(Collectors.toList());
            sistema.publicarOferta(clienteActual, ids, precio);
            valorNuevaOfertaField.setText("");
            refrescarDatosCliente();
            JOptionPane.showMessageDialog(this, "Oferta publicada", "Marketplace", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirImpresion(Tiquete tiquete) {
        ImpresionTiqueteDialog dialog = new ImpresionTiqueteDialog(this, sistema, tiquete, () -> {
            refrescarDatosCliente();
            mostrarDetalleTiquete(tiquete);
        });
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BoletaMasterGUI gui = new BoletaMasterGUI();
            gui.setVisible(true);
        });
    }
}