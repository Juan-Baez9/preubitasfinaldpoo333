package Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eventos.Evento;
import tiquetes.PaqueteDeluxe;
import tiquetes.Tiquete;

/**
 * Representa al cliente/comprador de la plataforma BoletaMaster.
 * <p>
 * Responsabilidades principales:
 * <ul>
 *   <li>Poseer y gestionar sus {@link tiquetes.Tiquete} (alta/baja en su colección).</li>
 *   <li>Transferir un tiquete a otro {@code Cliente}, validando credenciales del emisor.</li>
 *   <li>Recibir reembolsos a su saldo virtual (heredado de {@link Cliente.Usuario}).</li>
 * </ul>
 * <p>
 * Nota de dominio: las políticas de transferibilidad (p. ej., “Deluxe no se transfiere”)
 * deben ser validadas por la capa que orquesta el caso de uso antes de invocar a este objeto.
 */
public class Cliente extends Usuario {
    private final String idCliente;
    private final List<Tiquete> tiquetes;

    
    /**
     * Crea un cliente con credenciales y saldo inicial.
     *
     * @param login     login del cliente (heredado de {@code Usuario}).
     * @param password  password del cliente (heredado de {@code Usuario}).
     * @param nombre    nombre del cliente (heredado de {@code Usuario}).
     * @param saldo     saldo virtual inicial (heredado de {@code Usuario}).
     * @param idCliente identificador único del cliente (obligatorio).
     * @throws NullPointerException si {@code idCliente} es {@code null}.
     */
    public Cliente(String login, String password, String nombre, double saldo, String idCliente) {
        super(login, password, nombre, saldo);
        this.idCliente = Objects.requireNonNull(idCliente, "El identificador del cliente es obligatorio");
        this.tiquetes = new ArrayList<>();
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        throw new UnsupportedOperationException("El identificador del cliente es inmutable");
    }

    public ArrayList<Tiquete> getTiquetes() {
        return new ArrayList<>(tiquetes);
    }

    public List<Tiquete> verTiquetes() {
        return new ArrayList<>(tiquetes);
    }

    public void setTiquetes(ArrayList<Tiquete> tiquetesNuevos) {
        this.tiquetes.clear();
        if (tiquetesNuevos != null) {
            this.tiquetes.addAll(tiquetesNuevos);
        }
    }
    /**
     * Agrega un {@link tiquetes.Tiquete} a la colección del cliente.
     *
     * @param tiquete tiquete a agregar (obligatorio).
     * @throws NullPointerException si {@code tiquete} es {@code null}.
     */
    public void agregarTiquete(Tiquete tiquete) {
        tiquetes.add(Objects.requireNonNull(tiquete, "El tiquete es obligatorio"));
    }
    
    /**
     * Elimina un {@link tiquetes.Tiquete} de la colección del cliente.
     *
     * @param tiquete tiquete a eliminar.
     * @return {@code true} si el tiquete estaba en la colección y fue eliminado;
     *         {@code false} en caso contrario.
     */
    public boolean eliminarTiquete(Tiquete tiquete) {
        return tiquetes.remove(tiquete);
    }
    /**
     * Transfiere un tiquete identificado por {@code idTiquete} desde este cliente (emisor)
     * hacia un {@code receptor}, validando la contraseña del emisor.
     * <p>
     * Comportamiento:
     * <ul>
     *   <li>Si {@code receptor} es {@code null}, la transferencia falla.</li>
     *   <li>Si la contraseña no coincide con la del emisor, la transferencia falla.</li>
     *   <li>Si el tiquete no pertenece al emisor o no existe, la transferencia falla.</li>
     *   <li>Si tiene éxito, el tiquete se remueve del emisor, se agrega al receptor
     *       y se actualiza su propietario via {@code tiquete.setCliente(receptor)}.</li>
     * </ul>
     * <p>
     * Nota de dominio: las restricciones específicas tales como que (“Deluxe no transferible”,
     * tope de paquete, vencimientos, etc.) deben validarse en la capa de aplicación
     * antes de invocar este método.
     *
     * @param receptor   cliente destinatario de la transferencia.
     * @param idTiquete  identificador del tiquete a transferir.
     * @param password   contraseña del emisor para validar la operación.
     * @return {@code true} si la transferencia se realizó; {@code false} en caso contrario.
     */
    public boolean transferirTiquete(Cliente receptor, int idTiquete, String password) {
        if (receptor == null) {
            return false;
        }
        if (!this.getPassword().equals(password)) {
            return false;
        }
        Tiquete tiqueteATransferir = null;
        for (Tiquete t : tiquetes) {
            if (t.getIdTiquete() == idTiquete) {
                tiqueteATransferir = t;
                break;
            }
        }
        if (tiqueteATransferir == null) {
            return false;
        }
        tiquetes.remove(tiqueteATransferir);
        receptor.agregarTiquete(tiqueteATransferir);
        tiqueteATransferir.setCliente(receptor);
        return true;
    }

    public boolean poseeTiquete(int idTiquete) {
        for (Tiquete t : tiquetes) {
            if (t.getIdTiquete() == idTiquete) {
                return true;
            }
        }
        return false;
    }
}