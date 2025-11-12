package marketPlace;

import Cliente.Cliente;

import tiquetes.Tiquete;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Oferta publicada por un cliente en el marketplace.
 */
public class OfertaMarketPlace {
    private final String id;
    private final Cliente vendedor;
    private final List<Tiquete> tiquetes;
    private final double precioInicial;
    private EstadoOferta estado;
    private final List<ContraOferta> contraofertas;
    private final LocalDateTime fechaCreacion;

    public OfertaMarketPlace(Cliente vendedor, List<Tiquete> tiquetes, double precioInicial) {
        this(UUID.randomUUID().toString(), vendedor, tiquetes, precioInicial, EstadoOferta.ACTIVA, new ArrayList<>(), LocalDateTime.now());
    }

    public OfertaMarketPlace(String id, Cliente vendedor, List<Tiquete> tiquetes, double precioInicial,
                             EstadoOferta estado, List<ContraOferta> contraofertas, LocalDateTime fechaCreacion) {
        this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
        this.vendedor = Objects.requireNonNull(vendedor, "El vendedor es obligatorio");
        this.tiquetes = new ArrayList<>(Objects.requireNonNull(tiquetes, "Los tiquetes son obligatorios"));
        this.precioInicial = precioInicial;
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.contraofertas = new ArrayList<>(Objects.requireNonNull(contraofertas, "Las contraofertas son obligatorias"));
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria");
    }

    public String getId() {
        return id;
    }

    public Cliente getVendedor() {
        return vendedor;
    }

    public List<Tiquete> getTiquetes() {
        return Collections.unmodifiableList(tiquetes);
    }

    public double getPrecioInicial() {
        return precioInicial;
    }

    public EstadoOferta getEstado() {
        return estado;
    }

    public void setEstado(EstadoOferta estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    public List<ContraOferta> getContraofertas() {
        return Collections.unmodifiableList(contraofertas);
    }

    public ContraOferta agregarContraoferta(Cliente comprador, double monto) {
        ContraOferta contraoferta = new ContraOferta(comprador, monto);
        contraofertas.add(contraoferta);
        return contraoferta;
    }

    public Optional<ContraOferta> buscarContraoferta(String idContraoferta) {
        return contraofertas.stream().filter(c -> c.getId().equals(idContraoferta)).findFirst();
    }

    public boolean perteneceAlVendedor(Cliente posibleVendedor) {
        return vendedor.equals(posibleVendedor);
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}