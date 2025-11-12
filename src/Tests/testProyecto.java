package Tests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import Cliente.Administrador;
import Cliente.Cliente;
import Cliente.Organizador;
import eventos.Evento;
import eventos.Localidad;
import eventos.Oferta;
import eventos.TipoEvento;
import eventos.Venue;
import tiquetes.Tiquete;
import tiquetes.TiqueteBasico;
import tiquetes.TiqueteMultiple;
import tiquetes.TiqueteTemporada;
import tiquetes.Transaccion;


public class testProyecto {

    
    private Administrador nuevoAdmin() {
        return new Administrador(
                0.0,                          // ganancias iniciales
                "ADM-1",                      // idAdministrador
                new ArrayList<>(),            // venues aprobados
                "admin", "secret", "Admin",   // credenciales/nombre
                0.0                           // saldo (Usuario)
        );
    }

    private Organizador nuevoOrganizador() {
        
        return new Organizador("org", "pass", "Organizador", 0.0, "ORG-1", 0.0, null);
    }

    private Cliente nuevoCliente(String id) {
   
        return new Cliente("cli_"+id, "1234", "Cliente "+id, 0.0, id);
    }

    private Venue nuevoVenueVacio() {
       
        return new Venue("V-1", "Coliseo", "Bogotá", 1000, new ArrayList<>());
    }

    private Evento nuevoEventoBasico(Administrador admin, Organizador org, Venue venue) {
        // Evento(adm, id, nombre, fecha, hora, estado, tipo, venue, oferta, organizador, tiquetes)
        return new Evento(
                admin, "E-1", "Mi Evento",
                LocalDate.of(2026, 1, 10),
                LocalTime.of(20, 0),
                "Programado",
                TipoEvento.CONCIERTO,
                venue,
                null,               
                org,
                new ArrayList<>()   
        );
    }

    private Localidad nuevaLocalidadBasica() {
               return new Localidad(
                null, new ArrayList<>(), "VIP", 120_000.0, true, 200
        );
    }

    private TiqueteBasico nuevoTiqueteBasico(Cliente c, Evento e, Localidad l, int id, int asiento) {
        
        return new TiqueteBasico(c, id, 100_000.0, 0.0, 0.0, "Emitido", l, e, asiento, true);
    }

    // ===============================================================
    // RF1. Registrarse como cliente
    // ===============================================================
    @Test
    void RF1_registrarse_cliente() {
        Cliente c = nuevoCliente("C-1");
        assertNotNull(c);
        assertEquals(0.0, c.consultarSaldo(), 1e-9, "Saldo inicial debe ser 0");
        assertTrue(c.autenticar("cli_C-1", "1234"), "Debe autenticarse con sus credenciales");
    }

    // ===============================================================
    // RF2. Comprar tiquete (solo creación/registro de objetos)
    // ===============================================================
    @Test
    void RF2_comprar_tiquete() {
        Administrador admin = nuevoAdmin();
        Organizador org = nuevoOrganizador();
        Venue venue = nuevoVenueVacio();
        admin.aprobarVenue(venue, true);

        Evento evento = nuevoEventoBasico(admin, org, venue);
        Localidad loc = nuevaLocalidadBasica();

        Cliente cliente = nuevoCliente("C-2");
        TiqueteBasico t = nuevoTiqueteBasico(cliente, evento, loc, 101, 1);

   
        cliente.agregarTiquete(t);
        loc.agregarTiquete(t);

        assertTrue(cliente.poseeTiquete(101));
        assertEquals(cliente, t.getCliente());
        assertEquals(evento, t.getEvento());
        assertEquals(loc, t.getLocalidad());
    }

    // ===============================================================
    // RF3. Crear evento
    // ===============================================================
    @Test
    void RF3_crear_evento() {
        Administrador admin = nuevoAdmin();
        Organizador org = nuevoOrganizador();
        Venue venue = nuevoVenueVacio();

        admin.aprobarVenue(venue, true); 
        var fecha = LocalDate.of(2026, 5, 5);
        var hora  = LocalTime.of(19, 30);

        Evento creado = org.crearEvento(
                admin, "E-2", "Festival", fecha, hora,
                "Programado", TipoEvento.CULTURAL, venue, null, org, new ArrayList<>()
        );

        assertNotNull(creado);
        assertTrue(venue.tieneEventoEnFecha(fecha), "El venue debe registrar el evento en esa fecha");
        assertTrue(org.getEventos().contains(creado), "El organizador debe registrar el nuevo evento");
    }

    // ===============================================================
    // RF4. Consultar ganancias (organizador) 
    // ===============================================================
    @Test
    void RF4_consultar_ganancias_organizador() {
        Organizador org = nuevoOrganizador();
        assertNotNull(org);
    
        assertEquals(0.0, org.getFinanzas(), 1e-9);
    }

    // ===============================================================
    // RF5. Generar oferta
    // ===============================================================
    @Test
    void RF5_generar_oferta() {
        Administrador admin = nuevoAdmin();
        Organizador org = nuevoOrganizador();
        Venue venue = nuevoVenueVacio();
        admin.aprobarVenue(venue, true);

        Evento evento = nuevoEventoBasico(admin, org, venue);
        Localidad loc  = nuevaLocalidadBasica();

        Oferta oferta = new Oferta(
                loc,
                evento,
                15.0, 
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        loc.setOferta(oferta);

        assertNotNull(oferta);
        assertEquals(oferta, loc.getOferta());
       
    }

    // ===============================================================
    // RF6. Ver ganancias (administrador)
    // ===============================================================
    @Test
    void RF6_ver_ganancias_admin() {
        Administrador admin = nuevoAdmin();
        assertNotNull(admin);
        assertEquals(0.0, admin.getGanancias(), 1e-9);

 
        Cliente c = nuevoCliente("C-6");
        Transaccion trx = new Transaccion(1, LocalDateTime.now(), 250_000.0, c);
        assertNotNull(trx);
    }

    // ===============================================================
    // RF7. Fijar cargo fijo (emisión)
    // ===============================================================
    @Test
    void RF7_fijar_cargo_fijo() {
        Administrador admin = nuevoAdmin();

       
        Cliente c = nuevoCliente("C-7");
        Venue v = nuevoVenueVacio();
        admin.aprobarVenue(v, true);
        Organizador org = nuevoOrganizador();
        Evento e = nuevoEventoBasico(admin, org, v);
        Localidad l = nuevaLocalidadBasica();
        TiqueteBasico t = nuevoTiqueteBasico(c, e, l, 701, 1);

        admin.fijarCargoEmision(5000.0, t);

        assertEquals(5000.0, admin.getCargoEmision(), 1e-9);
        assertEquals(5000.0, t.getCargoEmision(), 1e-9);
    }

    // ===============================================================
    // RF8. Fijar cargo porcentual por tipo de evento
    // ===============================================================
    @Test
    void RF8_fijar_cargo_porcentual() {
        Administrador admin = nuevoAdmin();
        admin.fijarCargoServicio(TipoEvento.DEPORTIVO, 12.0);
        assertEquals(12.0, admin.getCargoServicio(TipoEvento.DEPORTIVO), 1e-9);
    }

    // ===============================================================
    // RF9. Reembolso de tiquete (decisión)
    // ===============================================================
    @Test
    void RF9_reembolso_tiquete() {
        Administrador admin = nuevoAdmin();
        boolean aprobado = admin.decidirReembolso("REQ-REEMB-1", true);
        boolean rechazado = admin.decidirReembolso("REQ-REEMB-2", false);

        assertTrue(aprobado);
        assertFalse(rechazado);
    }

    // ===============================================================
    // RF10. Transferir tiquete (Deluxe NO transferible se valida a otra capa,
    // aquí solo probamos la transferencia básica con password correcto)
    // ===============================================================
    @Test
    void RF10_transferir_tiquete() {
        Administrador admin = nuevoAdmin();
        Organizador org = nuevoOrganizador();
        Venue venue = nuevoVenueVacio();
        admin.aprobarVenue(venue, true);

        Evento e = nuevoEventoBasico(admin, org, venue);
        Localidad l = nuevaLocalidadBasica();

        Cliente emisor = nuevoCliente("C-10-A");
        Cliente receptor = nuevoCliente("C-10-B");

        TiqueteBasico t = nuevoTiqueteBasico(emisor, e, l, 1001, 10);
        emisor.agregarTiquete(t);

        boolean ok = emisor.transferirTiquete(receptor, 1001, "1234"); 
        assertTrue(ok);
        assertFalse(emisor.poseeTiquete(1001));
        assertTrue(receptor.poseeTiquete(1001));
        assertEquals(receptor, t.getCliente());
    }

    // ===============================================================
    // RF11. Aprobar venues propuestos
    // ===============================================================
    @Test
    void RF11_aprobar_venue() {
        Administrador admin = nuevoAdmin();
        Venue v = nuevoVenueVacio();

        admin.aprobarVenue(v, true);
        assertTrue(admin.getVenuesAprobados().contains(v));
    }

    
}
