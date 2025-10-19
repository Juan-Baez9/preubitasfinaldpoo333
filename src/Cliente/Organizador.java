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

public class Organizador extends Usuario {
    private final String idOrganizador;
    private double finanzas;
    private Evento[] eventos;
    private final List<Tiquete> cortesias = new ArrayList<>();

    
    
    
    public Organizador(String login, String password, String nombre, double saldo, String idOrganizador,
			Evento[] eventos) {
		super(login, password, nombre, saldo);
		this.idOrganizador = idOrganizador;
		this.finanzas = finanzas;
		this.eventos = eventos;
	}

	public List<Tiquete> getCortesias() {
        return new ArrayList<>(cortesias);
    }
    
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

  

    
    

    public Evento[] getEventos() {
		return eventos;
	}

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
        return evento;
    }

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

    public Oferta crearOferta(String idLocalidad, String nombre, double precioBase, boolean numerada, Localidad localidad,
            Evento evento) {
        Objects.requireNonNull(localidad, "La localidad es obligatoria");
        Objects.requireNonNull(evento, "El evento es obligatorio");
        Oferta oferta = new Oferta(idLocalidad, nombre, precioBase, numerada, localidad, evento);
        evento.setOferta(oferta);
        return oferta;
    }

    public Transaccion comprarComoCortesia(Transaccion transaccion, Tiquete tiquete) {
        Objects.requireNonNull(transaccion, "La transacción es obligatoria");
        Objects.requireNonNull(tiquete, "El tiquete es obligatorio");
        tiquete.setPrecio(0);
        tiquete.setEstado("CORTESIA");
        return transaccion;
    }

    public double consultarFinanzas(String login, String password) {
        if (!autenticar(login, password)) {
            throw new SecurityException("Credenciales inválidas");
        }
        return finanzas;
    }
    public void registrarEvento(Evento e) {
        boolean registrado = false;

        
        for (int i = 0; i < eventos.length; i++) {
            if (eventos[i] == null) {    
                eventos[i] = e;          
                registrado = true;       
                break;                  
            }
        }

        
        if (!registrado) {
            Evento[] nuevo = new Evento[eventos.length + 1]; 

          
            for (int i = 0; i < eventos.length; i++) {
                nuevo[i] = eventos[i];
            }

            
            nuevo[eventos.length] = e;

            
            eventos = nuevo;
        }
    }
