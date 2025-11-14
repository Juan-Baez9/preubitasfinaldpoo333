package Tests;

import Cliente.Administrador;
import log.EntradaLog;
import log.LogSistema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link LogSistema} enfocadas en registrar eventos
 * y controlar el acceso del administrador.
 */
class LogSistemaTest {

    private LogSistema logSistema;
    private Administrador administrador;

    @BeforeEach
    void setUp() {
        logSistema = new LogSistema();
        administrador = new Administrador(0.0, "ADM100", new ArrayList<>(),
                "admin", "admin", "Administrador", 0.0);
    }

    @Test
    void registrarAgregaEntradaInmutable() {
        EntradaLog entrada = logSistema.registrar("OFERTA", "cli01 publicó oferta");

        assertNotNull(entrada.getFechaHora());
        assertEquals("OFERTA", entrada.getTipo());
        assertEquals(1, logSistema.getEntradas().size());

        List<EntradaLog> consulta = logSistema.getEntradas();
        assertThrows(UnsupportedOperationException.class, () -> consulta.add(entrada));
    }

    @Test
    void consultarConCredencialesValidasEntregaCopia() {
        logSistema.registrar("CONTRAOFERTA", "cli02 propuso 120000");

        List<EntradaLog> entradas = logSistema.consultar(administrador, "admin", "admin");

        assertEquals(1, entradas.size());
        assertThrows(UnsupportedOperationException.class, () -> entradas.clear());
    }

    @Test
    void excepcionConsultaCredencialesInbalidas() {
        assertThrows(SecurityException.class, () -> logSistema.consultar(administrador, "admin", "otra"));
        assertThrows(SecurityException.class, () -> logSistema.consultar(null, "admin", "admin"));
    }

    @Test
    void reemplazarEntradas() {
        List<EntradaLog> preexistentes = new ArrayList<>();
        preexistentes.add(new EntradaLog(java.time.LocalDateTime.now(), "TRANSACCION", "venta previa"));

        logSistema.reemplazarEntradas(preexistentes);

        assertEquals(1, logSistema.getEntradas().size());
        assertEquals("TRANSACCION", logSistema.getEntradas().get(0).getTipo());

        logSistema.limpiar();
        assertTrue(logSistema.getEntradas().isEmpty());
    }
}
