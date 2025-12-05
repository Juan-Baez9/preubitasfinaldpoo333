package tiquetes;

import java.util.Objects;

import Cliente.Cliente;
import eventos.Evento;
import eventos.Localidad;
/**
 * Clase base abstracta para todos los tiquetes de BoletaMaster.
 * <p>
 * Un tiquete pertenece a un {@link eventos.Evento}, está asociado a una
 * {@link eventos.Localidad} y puede tener un {@link Cliente.Cliente} como propietario.
 * Conserva su estado y los componentes de precio:
 * <ul>
 *   <li>precio base</li>
 *   <li>cargo porcentual por servicio</li>
 *   <li>cargo fijo de emisión</li>
 * </ul>
 * <p>
 * Proporciona utilidades comunes como el cálculo del valor total a pagar.
 */
public abstract class Tiquete {
    private int idTiquete;
    private double precio;
    private double cargoServicio;
    private double cargoEmision;
    private String estado;
    private boolean impreso;
    private Localidad localidad;
    private Evento evento;
    private Cliente cliente;
    /**
     * Construye un tiquete con sus datos de contexto y valores económicos.
     *
     * @param cliente        propietario inicial del tiquete (puede ser {@code null} si se asignará luego).
     * @param idTiquete      identificador único del tiquete.
     * @param precio         precio base (debe ser coherente con las reglas de negocio).
     * @param cargoServicio  cargo porcentual/valor por servicio (no negativo).
     * @param cargoEmision   cargo fijo de emisión (no negativo).
     * @param estado         estado del tiquete (obligatorio).
     * @param localidad      localidad asociada (puede ser {@code null} si se define más adelante).
     * @param evento         evento al que pertenece el tiquete (obligatorio).
     *
     * @throws NullPointerException si {@code estado} o {@code evento} son {@code null}.
     */
    protected Tiquete(Cliente cliente, int idTiquete, double precio, double cargoServicio, double cargoEmision,
            String estado, Localidad localidad, Evento evento) {
        this.cliente = cliente;
        this.idTiquete = idTiquete;
        this.precio = precio;
        this.cargoServicio = cargoServicio;
        this.cargoEmision = cargoEmision;
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.localidad = localidad;
        this.evento = Objects.requireNonNull(evento, "El evento es obligatorio");
        this.impreso = false;
    }


    public int getIdTiquete() {
        return idTiquete;
    }

    public void setIdTiquete(int idTiquete) {
        this.idTiquete = idTiquete;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        if (precio < 0) {
            throw new IllegalArgumentException("El precio debe ser positivo");
        }
        this.precio = precio;
    }

    public double getCargoServicio() {
        return cargoServicio;
    }

    public void setCargoServicio(double cargoServicio) {
        if (cargoServicio < 0) {
            throw new IllegalArgumentException("El cargo de servicio debe ser positivo");
        }
        this.cargoServicio = cargoServicio;
    }

    public double getCargoEmision() {
        return cargoEmision;
    }

    public void setCargoEmision(double cargoEmision) {
        if (cargoEmision < 0) {
            throw new IllegalArgumentException("El cargo de emisión debe ser positivo");
        }
        this.cargoEmision = cargoEmision;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
    }

    public boolean isImpreso() {
        return impreso;
    }

    public void setImpreso(boolean impreso) {
        this.impreso = impreso;
    }

    public void marcarImpreso() {
        this.impreso = true;
    }

    public Localidad getLocalidad() {
        return localidad;
    }

    public void setLocalidad(Localidad localidad) {
        this.localidad = Objects.requireNonNull(localidad, "La localidad es obligatoria");
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = Objects.requireNonNull(evento, "El evento es obligatorio");
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
    /**
     * Calcula el valor total del tiquete como suma del precio base,
     * el cargo por servicio y el cargo de emisión.
     *
     * @return total a pagar por el tiquete.
     */
    public double calcularValorTotal() {
        return precio + cargoServicio + cargoEmision;
    }
}