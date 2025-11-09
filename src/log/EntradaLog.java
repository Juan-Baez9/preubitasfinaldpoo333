package log;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una entrada del log del sistema de reventa.
 * Cada registro conserva el instante en que ocurrió, un tipo y una descripción.
 */
public class EntradaLog {
    private final LocalDateTime fechaHora;
    private final String tipo;
    private final String descripcion;

    public EntradaLog(LocalDateTime fechaHora, String tipo, String descripcion) {
        this.fechaHora = Objects.requireNonNull(fechaHora, "La fecha y hora son obligatorias");
        this.tipo = Objects.requireNonNull(tipo, "El tipo es obligatorio");
        this.descripcion = Objects.requireNonNull(descripcion, "La descripción es obligatoria");
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}