package Tests;

import Cliente.Administrador;
import Cliente.Cliente;
import eventos.Evento;
import eventos.Localidad;
import eventos.TipoEvento;
import eventos.Venue;
import manager.BoletaMasterState;
import manager.MarketplaceService;
import marketPlace.ContraOferta;
import marketPlace.EstadoOferta;
import marketPlace.OfertaMarketPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tiquetes.TiqueteBasico;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link MarketplaceService} sobre un estado controlado
 * para validar reglas de negocio clave del marketplace.
 */
class marketPlaceUnitTest {

    private BoletaMasterState state;
    private MarketplaceService service;
    private Cliente vendedor;
    private Cliente comprador;
    private TiqueteBasico tiqueteVendedor;

    @BeforeEach
    void setUp() {
        state = new BoletaMasterState();
        Administrador admin = new Administrador(0.0, "ADM200", new ArrayList<>(),
                "admin", "admin", "Administrador", 0.0);
        state.setAdministrador(admin);

        vendedor = new Cliente("seller", "pass", "Vendedor", 100_000.0, "CLI920");
        comprador = new Cliente("buyer", "pass", "Comprador", 400_000.0, "CLI921");
        state.getClientesPorLogin().put(vendedor.getLogin(), vendedor);
        state.getClientesPorLogin().put(comprador.getLogin(), comprador);

        Venue venue = new Venue("VEN920", "Coliseo", "Bogotá", 1000, new ArrayList<>());
        admin.aprobarVenue(venue, true);
        Evento evento = new Evento(admin, "EVT920", "Concierto", LocalDate.now().plusDays(20),
                LocalTime.of(20, 0), "PROGRAMADO", TipoEvento.CONCIERTO, venue, null, null, new ArrayList<>());
        Localidad localidad = new Localidad(null, new ArrayList<>(), "General", 90_000.0, true, 500);

        tiqueteVendedor = new TiqueteBasico(vendedor, 9201, 90_000.0, 5_000.0, 1_000.0,
                "EMITIDO", localidad, evento, 12, true);
        vendedor.agregarTiquete(tiqueteVendedor);
        state.getTiquetesPorId().put(tiqueteVendedor.getIdTiquete(), tiqueteVendedor);

        service = new MarketplaceService(state);
    }

    @Test
    void publicarOfertaRechazaTiqueteQueNoPerteneceAlVendedor() {
        TiqueteBasico ajeno = new TiqueteBasico(comprador, 9202, 85_000.0, 4_000.0, 1_000.0,
                "EMITIDO", tiqueteVendedor.getLocalidad(), tiqueteVendedor.getEvento(), 13, true);
        state.getTiquetesPorId().put(ajeno.getIdTiquete(), ajeno);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.publicarOferta(vendedor, List.of(ajeno.getIdTiquete()), 120_000.0));

        assertTrue(ex.getMessage().contains("no pertenece"));
    }

    @Test
    void excepcionTiqueteEnDosOfertas() {
        OfertaMarketPlace oferta = service.publicarOferta(vendedor, List.of(tiqueteVendedor.getIdTiquete()), 120_000.0);

        assertEquals(oferta.getId(), state.getTiqueteEnOferta().get(tiqueteVendedor.getIdTiquete()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.publicarOferta(vendedor, List.of(tiqueteVendedor.getIdTiquete()), 130_000.0));

        assertTrue(ex.getMessage().contains("ya está incluido"));
    }

    @Test
    void excepcionContraOfertaPropioVendedor() {
        OfertaMarketPlace oferta = service.publicarOferta(vendedor, List.of(tiqueteVendedor.getIdTiquete()), 110_000.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearContraoferta(vendedor, oferta.getId(), 105_000.0));

        assertTrue(ex.getMessage().contains("propia oferta"));
    }

    @Test
    void aceptarContraoferta() {
        OfertaMarketPlace oferta = service.publicarOferta(vendedor, List.of(tiqueteVendedor.getIdTiquete()), 110_000.0);
        ContraOferta contra = service.crearContraoferta(comprador, oferta.getId(), 100_000.0);
        double saldoVendedorInicial = vendedor.getSaldo();
        double saldoCompradorInicial = comprador.getSaldo();

        service.aceptarContraoferta(vendedor, oferta.getId(), contra.getId());

        assertEquals(EstadoOferta.VENDIDA, oferta.getEstado());
        assertEquals(saldoVendedorInicial + 100_000.0, vendedor.getSaldo(), 0.01);
        assertEquals(saldoCompradorInicial - 100_000.0, comprador.getSaldo(), 0.01);
        assertEquals(comprador, tiqueteVendedor.getCliente());
        assertFalse(state.getTiqueteEnOferta().containsKey(tiqueteVendedor.getIdTiquete()));
    }
}
