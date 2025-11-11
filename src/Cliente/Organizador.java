package Cliente;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import eventos.Evento;
import eventos.Localidad;
import eventos.Oferta;
import eventos.TipoEvento;
import eventos.Venue;
import tiquetes.Tiquete;
import tiquetes.Transaccion;


/**
 * Representa al organizador (promotor) de eventos en la plataforma BoletaMaster.
 * <p>
 * Responsabilidades de dominio:
 * <ul>
 *   <li>Crear eventos y registrarlos en un {@link eventos.Venue} aprobado.</li>
 *   <li>Definir y asignar localidades a los eventos.</li>
 *   <li>Crear ofertas (descuento porcentual, ventana de vigencia) sobre localidades.</li>
 *   <li>Gestionar cortesías (tiquetes con precio 0) y consultar sus finanzas.</li>
 * </ul>
 * Hereda credenciales y saldo de {@link Cliente.Usuario}.
 */
public class Organizador extends Usuario {
    private final String idOrganizador;
    private double finanzas;
    private final List<Evento> eventos;
    private final List<Tiquete> cortesias = new ArrayList<>();
    
    /**
     * Crea un organizador con credenciales, saldo inicial, identificador y arreglo de eventos.
     *
     * @param login          login del organizador (heredado de {@code Usuario}).
     * @param password       password del organizador (heredado de {@code Usuario}).
     * @param nombre         nombre del organizador (heredado de {@code Usuario}).
     * @param saldo          saldo virtual inicial (heredado de {@code Usuario}).
     * @param idOrganizador  identificador único del organizador.
     * @param eventos        arreglo inicial de eventos (puede ser {@code null} o contener posiciones vacías).
     * @param finanzas 
     */
	public Organizador(String login, String password, String nombre, double saldo, String idOrganizador,
			Evento[] eventos) {
		this(login, password, nombre, saldo, idOrganizador, 0.0, eventos);
	}

	public Organizador(String login, String password, String nombre, double saldo, String idOrganizador,
			double finanzasIniciales, Evento[] eventosIniciales) {
		super(login, password, nombre, saldo);
		this.idOrganizador = Objects.requireNonNull(idOrganizador, "El identificador del organizador es obligatorio");
		this.finanzas = finanzasIniciales;
		this.eventos = new ArrayList<>();
		if (eventosIniciales != null) {
			for (Evento e : eventosIniciales) {
				if (e != null) {
					this.eventos.add(e);
				}
			}
		}
	}

	public List<Tiquete> getCortesias() {
        return new ArrayList<>(cortesias);
    }
	
	/**
	 * Agrega un tiquete de cortesía al organizador.
	 * <p>
	 * Efectos: el tiquete queda registrado en la lista interna de cortesías del organizador.
	 *
	 * @param t tiquete a marcar como cortesía (no debe ser {@code null}).
	 * @return {@code true} si el tiquete fue agregado; {@code false} si {@code t} era {@code null}.
	 */
    public boolean agregarCortesia(Tiquete t) {
        if (t == null) return false;
        return cortesias.add(t);
    }

    public double getFinanzas() {
        return finanzas;
    }

    public void setFinanzas(double finanzas) {
        if (finanzas < 0) {
            throw new IllegalArgumentException("Las finanzas no pueden ser negativas");
        }
        this.finanzas = finanzas;
    }

    public String getIdOrganizador() {
        return idOrganizador;
    }

    public void setIdOrganizador(String idOrganizador) {
        throw new UnsupportedOperationException("El identificador del organizador es inmutable");
    }
    public List<Evento> getEventos() {
        return new ArrayList<>(eventos);
    }
    
    
    /**
     * Crea un {@link eventos.Evento} asociado a un {@link eventos.Venue} aprobado por el administrador.
     * <p>
     * Reglas de dominio validadas:
     * <ul>
     *   <li>El venue debe estar aprobado por el administrador.</li>
     *   <li>El venue no puede tener otro evento programado en la misma fecha.</li>
     * </ul>
     * Efectos colaterales:
     * <ul>
     *   <li>Se instancia el evento y se registra en el venue ({@code venue.registrarEvento(evento)}).</li>
     * </ul>
     *
     * @param administrador administrador responsable de la aprobación del venue (obligatorio).
     * @param idEvento      identificador del evento.
     * @param nombre        nombre del evento.
     * @param fecha         fecha del evento (obligatoria).
     * @param hora          hora del evento (obligatoria).
     * @param estado        estado inicial del evento (p. ej., "PROGRAMADO").
     * @param tipoEvento    tipo de evento (musical, cultural, deportivo, religioso).
     * @param venue         venue donde se realizará el evento (obligatorio y aprobado).
     * @param oferta        oferta inicial (opcional; puede ser {@code null}).
     * @param organizador   organizador (this u otro que actúe como responsable).
     * @param tiquetes      colección inicial de tiquetes del evento (opcional).
     * @return el evento creado y ya registrado en el venue.
     *
     * @throws NullPointerException     si {@code administrador}, {@code venue}, {@code fecha} o {@code hora} son {@code null}.
     * @throws IllegalArgumentException si el venue no está aprobado o si ya tiene un evento en la fecha indicada.
     */
	public Evento crearEvento(Administrador administrador, String idEvento, String nombre, LocalDate fecha, LocalTime hora,
            String estado, TipoEvento tipoEvento, Venue venue, Oferta oferta, Organizador organizador,
            ArrayList<Tiquete> tiquetes) {
        Objects.requireNonNull(administrador, "El administrador es obligatorio");
        Objects.requireNonNull(venue, "El venue es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        Objects.requireNonNull(hora, "La hora es obligatoria");
        if (!administrador.getVenuesAprobados().contains(venue)) {
            throw new IllegalArgumentException("El venue no ha sido aprobado por el administrador");
        }
        if (venue.tieneEventoEnFecha(fecha)) {
            throw new IllegalArgumentException("El venue ya tiene un evento programado en la fecha indicada");
        }
        Evento evento = new Evento(administrador, idEvento, nombre, fecha, hora, estado, tipoEvento, venue, oferta,
                organizador, tiquetes);
        
        venue.registrarEvento(evento);
        registrarEvento(evento);
        return evento;
    }
	/**
	 * Define y asigna localidades a un evento en un venue.
	 * <p>
	 * Reglas de dominio:
	 * <ul>
	 *   <li>Las localidades se configuran por evento (no son inherentes al venue).</li>
	 *   <li>Todas las boletas de una misma localidad tienen el mismo precio.</li>
	 *   <li>Si la localidad es numerada, cada tiquete tiene un asiento único.</li>
	 * </ul>
	 * Efectos colaterales:
	 * <ul>
	 *   <li>Se añade una localidad construida con los parámetros recibidos a la lista del venue.</li>
	 *   <li>Se asocia el venue al evento ({@code evento.asociarVenue(venue)}).</li>
	 * </ul>
	 *
	 * @param venue       venue al que se le configurarán localidades (obligatorio).
	 * @param evento      evento al que se asocia el venue/localidades (obligatorio).
	 * @param nombre      nombre de la localidad.
	 * @param precio      precio base de la localidad (uniforme para sus tiquetes).
	 * @param numerada    si la localidad es numerada.
	 * @param cupos       capacidad de la localidad.
	 * @param localidades lista de localidades existentes a la que se añadirá la nueva (puede ser {@code null}).
	 *
	 * @throws NullPointerException si {@code venue} o {@code evento} son {@code null}.
	 */
    public void asignarLocalidades(Venue venue, Evento evento, String nombre, double precio, boolean numerada, int cupos,
            ArrayList<Localidad> localidades) {
        Objects.requireNonNull(venue, "El venue es obligatorio");
        Objects.requireNonNull(evento, "El evento es obligatorio");
        ArrayList<Localidad> definidas = localidades != null ? localidades : new ArrayList<>();
        Localidad localidad = new Localidad(null, new ArrayList<>(), nombre, precio, numerada, cupos);
        definidas.add(localidad);
        venue.setLocalidades(definidas);
        evento.asociarVenue(venue);
    }
    
    /**
     * Crea una {@link eventos.Oferta} sobre una {@link eventos.Localidad} de un evento.
     * <p>
     * La oferta aplica un descuento porcentual dentro de una ventana de tiempo definida (según la lógica de {@code Oferta}).
     * Efectos colaterales: la oferta queda asociada al evento mediante {@code evento.setOferta(oferta)}.
     *
     * @param idLocalidad id interno de la localidad en la cual aplica la oferta.
     * @param nombre      nombre de la oferta.
     * @param precioBase  precio base de referencia para la oferta (si aplica a la lógica de la clase Oferta).
     * @param numerada    indicador de numeración (si corresponde al modelo de Oferta/Localidad).
     * @param localidad   localidad objetivo (obligatoria).
     * @param evento      evento objetivo (obligatorio).
     * @return la oferta creada.
     *
     * @throws NullPointerException si {@code localidad} o {@code evento} son {@code null}.
     */
    public Oferta crearOferta(String idLocalidad, String nombre, double precioBase, boolean numerada, Localidad localidad,
            Evento evento) {
        Objects.requireNonNull(localidad, "La localidad es obligatoria");
        Objects.requireNonNull(evento, "El evento es obligatorio");
        Oferta oferta = new Oferta(idLocalidad, nombre, precioBase, numerada, localidad, evento);
        evento.setOferta(oferta);
        return oferta;
    }
    
    /**
     * Marca un tiquete como cortesía (precio 0) dentro del contexto de una transacción.
     * <p>
     * Efectos:
     * <ul>
     *   <li>El tiquete recibe precio 0 y estado "CORTESIA".</li>
     *   <li>No altera otros campos de la transacción; devuelve la misma referencia recibida.</li>
     * </ul>
     *
     * @param transaccion transacción relacionada (obligatoria).
     * @param tiquete     tiquete a marcar como cortesía (obligatorio).
     * @return la misma transacción recibida.
     *
     * @throws NullPointerException si {@code transaccion} o {@code tiquete} son {@code null}.
     */
    public Transaccion comprarComoCortesia(Transaccion transaccion, Tiquete tiquete) {
        Objects.requireNonNull(transaccion, "La transacción es obligatoria");
        Objects.requireNonNull(tiquete, "El tiquete es obligatorio");
        tiquete.setPrecio(0);
        tiquete.setEstado("CORTESIA");
        return transaccion;
    }
    /**
     * Retorna el valor de finanzas del organizador, previa autenticación.
     *
     * @param login    login del organizador que consulta.
     * @param password password del organizador que consulta.
     * @return valor de {@code finanzas}.
     * @throws SecurityException si las credenciales son inválidas.
     */
    public double consultarFinanzas(String login, String password) {
        if (!autenticar(login, password)) {
            throw new SecurityException("Credenciales inválidas");
        }
        return finanzas;
    }
    /**
     * Registra un evento en el arreglo interno de eventos del organizador.
     * <p>
     * Estrategia: intenta ocupar la primera posición libre; si no hay, crea
     * un nuevo arreglo con espacio adicional y copia los elementos.
     *
     * @param e evento a registrar (puede ser {@code null}, aunque no se sugiere en la práctica).
     */
    public void registrarEvento(Evento e) {
    	if (e != null) {
            eventos.add(e);
        }
    }
}