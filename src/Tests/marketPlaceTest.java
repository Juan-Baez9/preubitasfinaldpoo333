package Tests;

import Cliente.Cliente;
import log.EntradaLog;
import manager.BoletaMasterSystem;
import marketPlace.ContraOferta;
import marketPlace.EstadoContraOferta;
import marketPlace.EstadoOferta;
import marketPlace.OfertaMarketPlace;

import Cliente.Administrador;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración sobre {@link BoletaMasterSystem} para validar los flujos
 * principales del Marketplace descritos en las historias de usuario.
 */
public class marketPlaceTest {

    private Path tempDir;
    private BoletaMasterSystem sistema;
    private Cliente vendedorPrincipal;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("marketplace-test");
        copiarDatosIniciales("usuarios.json");
        copiarDatosIniciales("eventos.json");
        copiarDatosIniciales("tiquetes.json");
        copiarDatosIniciales("paquetes.json");
        copiarDatosIniciales("marketplace_ofertas.json");
        copiarDatosIniciales("marketplace_log.json");

        sistema = new BoletaMasterSystem(
                tempDir.resolve("usuarios.json"),
                tempDir.resolve("eventos.json"),
                tempDir.resolve("tiquetes.json"),
                tempDir.resolve("paquetes.json"),
                tempDir.resolve("marketplace_ofertas.json"),
                tempDir.resolve("marketplace_log.json"));
        sistema.cargarDatos();

        vendedorPrincipal = sistema.autenticarCliente("cli01", "cli01").orElseThrow();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (sistema != null) {
            sistema = null;
        }
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @Test
    void publicarOfertaRegistro() {
        int logPrevio = sistema.getLogSistema().getEntradas().size();
        Cliente vendedor = sistema.autenticarCliente("cli03", "cli03").orElseThrow();
        assertFalse(sistema.obtenerOfertasPorVendedor(vendedor).stream()
                .anyMatch(oferta -> oferta.getTiquetes().stream().anyMatch(t -> t.getIdTiquete() == 103)));

        OfertaMarketPlace nuevaOferta = sistema.publicarOferta(vendedor, List.of(103), 150000.0);

        assertEquals(EstadoOferta.ACTIVA, nuevaOferta.getEstado());
        assertTrue(sistema.buscarOferta(nuevaOferta.getId()).isPresent());
        assertTrue(vendedor.poseeTiquete(103));

        List<EntradaLog> logActual = sistema.getLogSistema().getEntradas();
        assertEquals(logPrevio + 1, logActual.size());
        EntradaLog ultima = logActual.get(logActual.size() - 1);
        assertEquals("OFERTA", ultima.getTipo());
        assertTrue(ultima.getDescripcion().contains(vendedor.getLogin()));
        assertTrue(ultima.getDescripcion().contains(nuevaOferta.getId()));
    }

    @Test
    void crearYAceptarContraofertaActualizacion() {
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-100").orElseThrow();
        Cliente comprador = sistema.autenticarCliente("cli03", "cli03").orElseThrow();
        double saldoVendedorInicial = vendedorPrincipal.getSaldo();
        double saldoCompradorInicial = comprador.getSaldo();
        int logPrevio = sistema.getLogSistema().getEntradas().size();

        ContraOferta contra = sistema.crearContraoferta(comprador, oferta.getId(), 150000.0);
        assertEquals(EstadoContraOferta.PENDIENTE, contra.getEstado());
        assertEquals(logPrevio + 1, sistema.getLogSistema().getEntradas().size());

        sistema.aceptarContraoferta(vendedorPrincipal, oferta.getId(), contra.getId());

        assertEquals(EstadoOferta.VENDIDA, oferta.getEstado());
        assertEquals(EstadoContraOferta.ACEPTADA, contra.getEstado());
        assertEquals(saldoVendedorInicial + 150000.0, vendedorPrincipal.getSaldo(), 0.01);
        assertEquals(saldoCompradorInicial - 150000.0, comprador.getSaldo(), 0.01);
        assertTrue(comprador.poseeTiquete(101));
        assertFalse(vendedorPrincipal.poseeTiquete(101));

        List<EntradaLog> logActual = sistema.getLogSistema().getEntradas();
        assertEquals(logPrevio + 3, logActual.size());
        EntradaLog transaccion = logActual.get(logActual.size() - 1);
        assertEquals("TRANSACCION", transaccion.getTipo());
        assertTrue(transaccion.getDescripcion().contains(oferta.getId()));
    }

    @Test
    void cancelarOfertaPorVendedorCambiarEstadoTiquete() {
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-101").orElseThrow();
        Cliente vendedor = sistema.autenticarCliente("cli02", "cli02").orElseThrow();
        int logPrevio = sistema.getLogSistema().getEntradas().size();

        sistema.cancelarOfertaPorVendedor(vendedor, oferta.getId());

        assertEquals(EstadoOferta.CANCELADA_VENDEDOR, oferta.getEstado());
        assertEquals(logPrevio + 1, sistema.getLogSistema().getEntradas().size());
        EntradaLog ultima = sistema.getLogSistema().getEntradas().get(logPrevio);
        assertEquals("OFERTA", ultima.getTipo());
        assertTrue(ultima.getDescripcion().contains(oferta.getId()));
        assertTrue(vendedor.poseeTiquete(102));
        assertTrue(vendedor.poseeTiquete(201));
    }

    @Test
    void comprarOfertaDirectaTransSaldo() {
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-101").orElseThrow();
        Cliente comprador = sistema.autenticarCliente("cli04", "cli04").orElseThrow();
        double saldoVendedorInicial = oferta.getVendedor().getSaldo();
        double saldoCompradorInicial = comprador.getSaldo();
        int logPrevio = sistema.getLogSistema().getEntradas().size();

        sistema.comprarOferta(comprador, oferta.getId());

        assertEquals(EstadoOferta.VENDIDA, oferta.getEstado());
        assertEquals(saldoVendedorInicial + oferta.getPrecioInicial(), oferta.getVendedor().getSaldo(), 0.01);
        assertEquals(saldoCompradorInicial - oferta.getPrecioInicial(), comprador.getSaldo(), 0.01);
        assertTrue(comprador.poseeTiquete(102));
        assertTrue(comprador.poseeTiquete(201));
        assertEquals(logPrevio + 1, sistema.getLogSistema().getEntradas().size());
        EntradaLog transaccion = sistema.getLogSistema().getEntradas().get(logPrevio);
        assertEquals("TRANSACCION", transaccion.getTipo());
        assertTrue(transaccion.getDescripcion().contains(oferta.getId()));
    }

    @Test
    void obtenerOfertasActivas() {
        List<OfertaMarketPlace> activas = sistema.obtenerOfertasActivas();

        assertFalse(activas.isEmpty(), "Debe haber ofertas activas de ejemplo");
        assertTrue(activas.stream().allMatch(o -> o.getEstado() == EstadoOferta.ACTIVA));
        assertTrue(activas.stream().noneMatch(o -> o.getId().equals("OFER-102")),
                "Ofertas canceladas no deben aparecer entre las activas");
    }

    @Test
    void publicarOfertaNoDeluxe() {
        Cliente vendedor = sistema.autenticarCliente("cli04", "cli04").orElseThrow();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sistema.publicarOferta(vendedor, List.of(302), 180000.0));

        assertTrue(ex.getMessage().contains("Deluxe"));
        assertTrue(sistema.obtenerOfertasPorVendedor(vendedor).stream()
                .noneMatch(o -> o.getTiquetes().stream().anyMatch(t -> t.getIdTiquete() == 302)));
    }

    @Test
    void excepcionContraOfertaSinSaldo() {
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-101").orElseThrow();
        Cliente comprador = sistema.autenticarCliente("cli03", "cli03").orElseThrow();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sistema.crearContraoferta(comprador, oferta.getId(), 300000.0));

        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
        assertEquals(EstadoOferta.ACTIVA, oferta.getEstado());
    }

    @Test
    void rechazarContraofertaActualizarEstado() {
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-100").orElseThrow();
        int logPrevio = sistema.getLogSistema().getEntradas().size();

        sistema.rechazarContraoferta(vendedorPrincipal, oferta.getId(), "CO-200");

        ContraOferta contra = oferta.buscarContraoferta("CO-200").orElseThrow();
        assertEquals(EstadoContraOferta.RECHAZADA, contra.getEstado());
        EntradaLog entrada = sistema.getLogSistema().getEntradas().get(logPrevio);
        assertEquals("CONTRAOFERTA", entrada.getTipo());
        assertTrue(entrada.getDescripcion().contains("rechazó"));
    }

    @Test
    void contraofertasPendientesVendedor() {
        Map<OfertaMarketPlace, List<ContraOferta>> pendientes = sistema.contraofertasPendientes(vendedorPrincipal);

        assertEquals(1, pendientes.size());
        Map.Entry<OfertaMarketPlace, List<ContraOferta>> entry = pendientes.entrySet().iterator().next();
        assertEquals("OFER-100", entry.getKey().getId());
        assertTrue(entry.getValue().stream().allMatch(c -> c.getEstado() == EstadoContraOferta.PENDIENTE));
    }

    @Test
    void obtenerOfertasPorVendedor() {
        Cliente vendedorSecundario = sistema.autenticarCliente("cli02", "cli02").orElseThrow();

        List<OfertaMarketPlace> ofertasVendedor = sistema.obtenerOfertasPorVendedor(vendedorSecundario);

        assertFalse(ofertasVendedor.isEmpty());
        assertTrue(ofertasVendedor.stream().allMatch(o -> o.getVendedor().equals(vendedorSecundario)));
    }

    @Test
    void excepcionComprarOfertaSaldoInsuficiente() {
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-101").orElseThrow();
        Cliente comprador = sistema.autenticarCliente("cli03", "cli03").orElseThrow();
        double saldoInicial = comprador.getSaldo();

        assertThrows(IllegalArgumentException.class, () -> sistema.comprarOferta(comprador, oferta.getId()));

        assertEquals(saldoInicial, comprador.getSaldo(), 0.01);
        assertEquals(EstadoOferta.ACTIVA, oferta.getEstado());
    }

    @Test
    void cancelarOfertaPorAdministrador() {
        Administrador admin = sistema.getAdministrador();
        OfertaMarketPlace oferta = sistema.buscarOferta("OFER-100").orElseThrow();
        int logPrevio = sistema.getLogSistema().getEntradas().size();

        sistema.cancelarOfertaPorAdministrador(admin, oferta.getId());

        assertEquals(EstadoOferta.CANCELADA_ADMIN, oferta.getEstado());
        EntradaLog entrada = sistema.getLogSistema().getEntradas().get(logPrevio);
        assertEquals("OFERTA", entrada.getTipo());
        assertTrue(entrada.getDescripcion().contains("eliminó"));
    }

    @Test
    void consultarLogValido() {
        Administrador admin = sistema.getAdministrador();

        Set<Integer> tiquetesEnOferta = new HashSet<>();
        sistema.obtenerOfertasPorVendedor(vendedorPrincipal)
                .forEach(oferta -> oferta.getTiquetes()
                        .forEach(t -> tiquetesEnOferta.add(t.getIdTiquete())));

        int tiqueteDisponible = vendedorPrincipal.verTiquetes().stream()
                .mapToInt(t -> t.getIdTiquete())
                .filter(id -> !tiquetesEnOferta.contains(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El vendedor principal no tiene tiquetes disponibles para la prueba"));

        sistema.publicarOferta(vendedorPrincipal, List.of(tiqueteDisponible), 120000.0);

        List<EntradaLog> entradas = sistema.getLogSistema().consultar(admin, admin.getLogin(), admin.getPassword());

        assertFalse(entradas.isEmpty());
    }

    @Test
    void exepcionConsultarLogCredencialInvalida() {
        Administrador admin = sistema.getAdministrador();

        assertThrows(SecurityException.class,
                () -> sistema.getLogSistema().consultar(admin, admin.getLogin(), "claveErrada"));
        assertThrows(SecurityException.class,
                () -> sistema.getLogSistema().consultar(null, "ronny", "ronny"));
    }

    private void copiarDatosIniciales(String nombreArchivo) throws IOException {
        Path origen = Path.of("data").resolve(nombreArchivo);
        Path destino = tempDir.resolve(nombreArchivo);
        Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
    }
}
