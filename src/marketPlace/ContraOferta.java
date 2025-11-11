package marketPlace;

import Cliente.Cliente;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa una contraoferta realizada por un comprador potencial.
 */
public class ContraOferta {
    private final String id;
    private final Cliente comprador;
    private final double monto;
    private EstadoContraOferta estado;
    private final LocalDateTime fechaCreacion;

    public ContraOferta(Cliente comprador, double monto) {
        this(UUID.randomUUID().toString(), comprador, monto, EstadoContraOferta.PENDIENTE, LocalDateTime.now());
    }

    public ContraOferta(String id, Cliente comprador, double monto, EstadoContraOferta estado, LocalDateTime fechaCreacion) {
        this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
        this.comprador = Objects.requireNonNull(comprador, "El comprador es obligatorio");
        this.monto = monto;
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria");
    }

    public String getId() {
        return id;
    }

    public Cliente getComprador() {
        return comprador;
    }

    public double getMonto() {
        return monto;
    }

    public EstadoContraOferta getEstado() {
        return estado;
    }

    public void setEstado(EstadoContraOferta estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}