package log;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una entrada en el log del sistema con trazabilidad completa.
 * Cada registro almacena la fecha y hora en la que sucedió el evento,
 * el tipo de operación y una descripción detallada.
 */
public final class EntradaLog {
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

    @Override
    public String toString() {
        return "EntradaLog{" +
                "fechaHora=" + fechaHora +
                ", tipo='" + tipo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntradaLog that)) return false;
        return fechaHora.equals(that.fechaHora)
                && tipo.equals(that.tipo)
                && descripcion.equals(that.descripcion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fechaHora, tipo, descripcion);
    }
}