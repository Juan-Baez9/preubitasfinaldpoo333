package manager;

import Cliente.Administrador;
import Cliente.Cliente;
import Cliente.Organizador;
import eventos.Evento;
import log.LogSistema;
import marketPlace.OfertaMarketPlace;
import tiquetes.Tiquete;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;

/**
 * Estructura en memoria que mantiene todas las entidades cargadas del sistema.
 * <p>
 * Se encarga únicamente de almacenar el estado, delegando la lógica a servicios
 * especializados.
 */
public final class BoletaMasterState {

    private Administrador administrador;
    private final Map<String, Cliente> clientesPorLogin = new HashMap<>();
    private final Map<String, Organizador> organizadoresPorLogin = new HashMap<>();
    private final Map<String, Evento> eventosPorId = new HashMap<>();
    private final Map<Integer, Tiquete> tiquetesPorId = new HashMap<>();
    private final Map<String, OfertaMarketPlace> ofertasPorId = new HashMap<>();
    private final Map<Integer, String> tiqueteEnOferta = new HashMap<>();
    private final Set<Integer> tiquetesDeluxe = new HashSet<>();
    private final LogSistema logSistema = new LogSistema();
    private JSONArray paquetesRaw = new JSONArray();

    public Administrador getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Administrador administrador) {
        this.administrador = administrador;
    }

    public Map<String, Cliente> getClientesPorLogin() {
        return clientesPorLogin;
    }

    public Map<String, Organizador> getOrganizadoresPorLogin() {
        return organizadoresPorLogin;
    }

    public Map<String, Evento> getEventosPorId() {
        return eventosPorId;
    }

    public Map<Integer, Tiquete> getTiquetesPorId() {
        return tiquetesPorId;
    }

    public Map<String, OfertaMarketPlace> getOfertasPorId() {
        return ofertasPorId;
    }

    public Map<Integer, String> getTiqueteEnOferta() {
        return tiqueteEnOferta;
    }

    public Set<Integer> getTiquetesDeluxe() {
        return tiquetesDeluxe;
    }

    public LogSistema getLogSistema() {
        return logSistema;
    }

    public JSONArray getPaquetesRaw() {
        return paquetesRaw;
    }

    public void setPaquetesRaw(JSONArray paquetesRaw) {
        this.paquetesRaw = paquetesRaw;
    }

    void reset() {
        administrador = null;
        clientesPorLogin.clear();
        organizadoresPorLogin.clear();
        eventosPorId.clear();
        tiquetesPorId.clear();
        ofertasPorId.clear();
        tiqueteEnOferta.clear();
        tiquetesDeluxe.clear();
        paquetesRaw = new JSONArray();
        logSistema.limpiar();
    }
}