package Tests;

import Cliente.Cliente;
import marketPlace.ContraOferta;
import marketPlace.EstadoContraOferta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link ContraOferta} para validar su ciclo de vida.
 */
class contraOfertaTest {

    @Test
    void constructorPorDefectoDejaEstadoPendiente() {
        Cliente comprador = new Cliente("buyer", "pass", "Comprador", 150_000.0, "CLI910");

        ContraOferta contra = new ContraOferta(comprador, 120_000.0);

        assertNotNull(contra.getId());
        assertEquals(comprador, contra.getComprador());
        assertEquals(120_000.0, contra.getMonto());
        assertEquals(EstadoContraOferta.PENDIENTE, contra.getEstado());
        assertNotNull(contra.getFechaCreacion());
    }

    @Test
    void cambiarEstadoActualizaPropiedadYValidaNull() {
        Cliente comprador = new Cliente("buyer", "pass", "Comprador", 150_000.0, "CLI911");
        ContraOferta contra = new ContraOferta(comprador, 95_000.0);

        contra.setEstado(EstadoContraOferta.ACEPTADA);
        assertEquals(EstadoContraOferta.ACEPTADA, contra.getEstado());

        assertThrows(NullPointerException.class, () -> contra.setEstado(null));
    }
}