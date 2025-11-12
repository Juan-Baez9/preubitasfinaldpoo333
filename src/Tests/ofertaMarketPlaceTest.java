package Tests;

import Cliente.Administrador;
import Cliente.Cliente;
import Cliente.Organizador;
import eventos.Evento;
import eventos.Localidad;
import eventos.TipoEvento;
import eventos.Venue;
import marketPlace.ContraOferta;
import marketPlace.EstadoContraOferta;
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
 * Pruebas unitarias de {@link OfertaMarketPlace} centradas en su estado interno
 * y en la gestión de contraofertas.
 */
class OfertaMarketPlaceTest {

    private Cliente vendedor;
    private TiqueteBasico tiquete;
    private OfertaMarketPlace oferta;

    @BeforeEach
    void setUp() {
        vendedor = new Cliente("seller", "pass", "Vendedor", 250_000.0, "CLI900");

        Administrador admin = new Administrador(0.0, "ADM900", new ArrayList<>(),
                "admin", "admin", "Administrador", 0.0);
        Venue venue = new Venue("VEN900", "Arena", "Bogotá", 5000, new ArrayList<>());
        admin.aprobarVenue(venue, true);
        Organizador organizador = new Organizador("org", "org", "Organizador", 0.0, "ORG900", 0.0, null);
        Evento evento = new Evento(admin, "EVT900", "Festival", LocalDate.now().plusDays(10),
                LocalTime.NOON, "PROGRAMADO", TipoEvento.CONCIERTO, venue, null, organizador, new ArrayList<>());
        Localidad localidad = new Localidad(null, new ArrayList<>(), "General", 120_000.0, true, 2000);

        tiquete = new TiqueteBasico(vendedor, 9001, 120_000.0, 5_000.0, 1_500.0,
                "EMITIDO", localidad, evento, 45, true);
        vendedor.agregarTiquete(tiquete);

        oferta = new OfertaMarketPlace(vendedor, List.of(tiquete), 180_000.0);
    }

    @Test
    void constructorGeneraIdYEstadoActivo() {
        assertNotNull(oferta.getId());
        assertEquals(EstadoOferta.ACTIVA, oferta.getEstado());
        assertEquals(vendedor, oferta.getVendedor());
        assertEquals(1, oferta.getTiquetes().size());
        assertEquals(tiquete, oferta.getTiquetes().get(0));
    }

    @Test
    void agregarContraoferta_creaInstanciaPendiente() {
        Cliente comprador = new Cliente("buyer", "pass", "Comprador", 500_000.0, "CLI901");

        ContraOferta contra = oferta.agregarContraoferta(comprador, 175_000.0);

        assertEquals(EstadoContraOferta.PENDIENTE, contra.getEstado());
        assertTrue(oferta.getContraofertas().contains(contra));
        assertTrue(oferta.buscarContraoferta(contra.getId()).isPresent());
    }

    @Test
    void setEstadoNoPermiteNull() {
        assertThrows(NullPointerException.class, () -> oferta.setEstado(null));
    }

    @Test
    void perteneceAlVendedor_verificaIdentidad() {
        Cliente otro = new Cliente("other", "pass", "Otro", 100_000.0, "CLI902");

        assertTrue(oferta.perteneceAlVendedor(vendedor));
        assertFalse(oferta.perteneceAlVendedor(otro));
    }
}