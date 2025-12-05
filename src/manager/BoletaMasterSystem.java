package manager;

import Cliente.Administrador;
import java.util.function.Supplier;
import Cliente.Cliente;
import Cliente.Organizador;
import eventos.Evento;
import log.LogSistema;
import marketPlace.ContraOferta;
import marketPlace.OfertaMarketPlace;
import tiquetes.Tiquete;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Fachada principal del sistema BoletaMaster.
 * <p>
 * La clase delega la persistencia y la lógica del marketplace a componentes
 * especializados para reducir el acoplamiento y facilitar la evolución del
 * sistema.
 */
public class BoletaMasterSystem {

    private final JsonDataStore dataStore;
    private BoletaMasterState state;
    private MarketplaceService marketplaceService;

    public BoletaMasterSystem(Path usuariosPath,
                              Path eventosPath,
                              Path tiquetesPath,
                              Path paquetesPath,
                              Path ofertasPath,
                              Path logPath) {
        this(new JsonDataStore(usuariosPath, eventosPath, tiquetesPath, paquetesPath, ofertasPath, logPath));
    }

    private BoletaMasterSystem(JsonDataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    public static BoletaMasterSystem desdeDirectorio(String dataDir) {
        return new BoletaMasterSystem(JsonDataStore.fromDirectory(dataDir));
    }

    public synchronized void cargarDatos() {
        state = dataStore.load();
        marketplaceService = new MarketplaceService(state);
    }

    public synchronized void guardarDatos() {
        if (state == null) {
            return;
        }
        dataStore.save(state);
    }

    public Administrador getAdministrador() {
        return requireState().getAdministrador();
    }

    public Optional<Administrador> autenticarAdministrador(String login, String password) {
        Administrador administrador = getAdministrador();
        if (administrador != null && administrador.autenticar(login, password)) {
            return Optional.of(administrador);
        }
        return Optional.empty();
    }

    public Optional<Cliente> autenticarCliente(String login, String password) {
        Cliente cliente = requireState().getClientesPorLogin().get(login);
        if (cliente != null && cliente.autenticar(login, password)) {
            return Optional.of(cliente);
        }
        return Optional.empty();
    }

    public Optional<Organizador> autenticarOrganizador(String login, String password) {
        Organizador organizador = requireState().getOrganizadoresPorLogin().get(login);
        if (organizador != null && organizador.autenticar(login, password)) {
            return Optional.of(organizador);
        }
        return Optional.empty();
    }

    public List<Tiquete> obtenerTiquetesCliente(Cliente cliente) {
        return cliente.verTiquetes();
    }

    public List<OfertaMarketPlace> obtenerOfertasActivas() {
        return marketplace().obtenerOfertasActivas();
    }

    public List<OfertaMarketPlace> obtenerOfertasPorVendedor(Cliente vendedor) {
        return marketplace().obtenerOfertasPorVendedor(vendedor);
    }

    public List<OfertaMarketPlace> obtenerTodasLasOfertas() {
        return marketplace().obtenerTodasLasOfertas();
    }

    public Optional<OfertaMarketPlace> buscarOferta(String ofertaId) {
        return marketplace().buscarOferta(ofertaId);
    }

    public Map<OfertaMarketPlace, List<ContraOferta>> contraofertasPendientes(Cliente vendedor) {
        return marketplace().contraofertasPendientes(vendedor);
    }

    public OfertaMarketPlace publicarOferta(Cliente vendedor, List<Integer> tiquetesIds, double precioInicial) {
    	 return ejecutarYGuardar(() -> marketplace().publicarOferta(vendedor, tiquetesIds, precioInicial));
    }

    public void cancelarOfertaPorVendedor(Cliente vendedor, String ofertaId) {
    	ejecutarYGuardar(() -> marketplace().cancelarOfertaPorVendedor(vendedor, ofertaId));
    }

    public void cancelarOfertaPorAdministrador(Administrador admin, String ofertaId) {
    	 ejecutarYGuardar(() -> marketplace().cancelarOfertaPorAdministrador(admin, ofertaId));
    }

    public ContraOferta crearContraoferta(Cliente comprador, String ofertaId, double monto) {
        return ejecutarYGuardar(() -> marketplace().crearContraoferta(comprador, ofertaId, monto));
    }

    public void rechazarContraoferta(Cliente vendedor, String ofertaId, String contraofertaId) {
    	ejecutarYGuardar(() -> marketplace().rechazarContraoferta(vendedor, ofertaId, contraofertaId));
    }

    public void aceptarContraoferta(Cliente vendedor, String ofertaId, String contraofertaId) {
    	ejecutarYGuardar(() -> marketplace().aceptarContraoferta(vendedor, ofertaId, contraofertaId));
    }

    public void comprarOferta(Cliente comprador, String ofertaId) {
    	ejecutarYGuardar(() -> marketplace().comprarOferta(comprador, ofertaId));
    }

    public LogSistema getLogSistema() {
        return requireState().getLogSistema();
    }

    public Collection<Evento> getEventosOrganizador(Organizador organizador) {
        if (organizador == null) {
            return Collections.emptyList();
        }
        return organizador.getEventos();
    }
    
    public void marcarTiqueteImpreso(Tiquete tiquete, LocalDateTime fechaImpresion) {
        ejecutarYGuardar(() -> {
            tiquete.setFechaImpresion(fechaImpresion);
            tiquete.marcarImpreso();
        });
    }



    private MarketplaceService marketplace() {
        if (marketplaceService == null) {
            throw new IllegalStateException("Debe cargar los datos antes de usar el marketplace");
        }
        return marketplaceService;
    }

    private BoletaMasterState requireState() {
        if (state == null) {
            throw new IllegalStateException("Debe cargar los datos del sistema primero");
        }
        return state;
    }
    private void ejecutarYGuardar(Runnable accion) {
        Objects.requireNonNull(accion, "accion").run();
        guardarDatos();
    }

    private <T> T ejecutarYGuardar(Supplier<T> accion) {
        T resultado = Objects.requireNonNull(accion, "accion").get();
        guardarDatos();
        return resultado;
    }
}