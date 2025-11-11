package log;

import Cliente.Administrador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Log centralizado del marketplace de reventa.
 * Solo el administrador autenticado puede consultarlo.
 */
public class LogSistema {
    private final List<EntradaLog> entradas = new ArrayList<>();

    /**
     * Registra un suceso en el log con la fecha y hora actual.
     *
     * @param tipo        categoría del evento (p. ej., "OFERTA", "CONTRAOFERTA").
     * @param descripcion descripción detallada del suceso.
     */
    
    public EntradaLog registrar(String tipo, String descripcion) {
        EntradaLog entrada = new EntradaLog(
                LocalDateTime.now(),
                Objects.requireNonNull(tipo, "El tipo es obligatorio"),
                Objects.requireNonNull(descripcion, "La descripción es obligatoria"));
        entradas.add(entrada);
        return entrada;
    }

    /**
     * Consulta las entradas del log. Requiere autenticación del administrador.
     *
     * @param administrador administrador que solicita la consulta.
     * @param login         login suministrado.
     * @param password      contraseña suministrada.
     * @return lista inmutable de entradas del log.
     * @throws SecurityException si las credenciales son inválidas o el administrador es nulo.
     */
    public List<EntradaLog> consultar(Administrador administrador, String login, String password) {
        if (administrador == null || !administrador.autenticar(login, password)) {
            throw new SecurityException("Acceso no autorizado al log");
        }
        return Collections.unmodifiableList(new ArrayList<>(entradas));
    }

    /**
     * Retorna una copia de las entradas para propósitos internos (p. ej., pruebas o persistencia).
     *
     * @return lista de entradas.
     */
    public List<EntradaLog> getEntradas() {
        return Collections.unmodifiableList(new ArrayList<>(entradas));
    }

    /**
     * Reemplaza las entradas del log por una colección dada (usado en persistencia).
     */
    public void reemplazarEntradas(List<EntradaLog> nuevas) {
        entradas.clear();
        if (nuevas != null) {
            entradas.addAll(nuevas);
        }
    }


    /**
     * Vacía por completo el log. Útil para restablecer el estado de la aplicación
     * entre cargas de datos.
     */
    public void limpiar() {
        entradas.clear();
    }
}