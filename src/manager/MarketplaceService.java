package manager;

import Cliente.Administrador;
import Cliente.Cliente;
import log.LogSistema;
import marketPlace.ContraOferta;
import marketPlace.EstadoOferta;
import marketPlace.OfertaMarketPlace;
import marketPlace.EstadoContraOferta;
import tiquetes.Tiquete;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsula toda la lógica del marketplace de reventa.
 */
public class MarketplaceService {

    private final BoletaMasterState state;
    private final LogSistema logSistema;

    public MarketplaceService(BoletaMasterState state) {
        this.state = Objects.requireNonNull(state, "state");
        this.logSistema = state.getLogSistema();
    }

    public List<OfertaMarketPlace> obtenerOfertasActivas() {
        return state.getOfertasPorId().values().stream()
                .filter(o -> o.getEstado() == EstadoOferta.ACTIVA)
                .collect(Collectors.toList());
    }

    public List<OfertaMarketPlace> obtenerOfertasPorVendedor(Cliente vendedor) {
        return state.getOfertasPorId().values().stream()
                .filter(o -> o.getVendedor().equals(vendedor))
                .collect(Collectors.toList());
    }

    public List<OfertaMarketPlace> obtenerTodasLasOfertas() {
        return new ArrayList<>(state.getOfertasPorId().values());
    }

    public Optional<OfertaMarketPlace> buscarOferta(String ofertaId) {
        return Optional.ofNullable(state.getOfertasPorId().get(ofertaId));
    }

    public Map<OfertaMarketPlace, List<ContraOferta>> contraofertasPendientes(Cliente vendedor) {
        Map<OfertaMarketPlace, List<ContraOferta>> result = new LinkedHashMap<>();
        for (OfertaMarketPlace oferta : obtenerOfertasPorVendedor(vendedor)) {
            List<ContraOferta> pendientes = oferta.getContraofertas().stream()
                    .filter(c -> c.getEstado() == EstadoContraOferta.PENDIENTE)
                    .collect(Collectors.toList());
            if (!pendientes.isEmpty()) {
                result.put(oferta, pendientes);
            }
        }
        return result;
    }

   public synchronized OfertaMarketPlace publicarOferta(Cliente vendedor, List<Integer> tiquetesIds, double precioInicial) {
        if (vendedor == null) {
            throw new IllegalArgumentException("Se requiere un vendedor");
        }
        if (tiquetesIds == null || tiquetesIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un tiquete");
        }
        if (precioInicial <= 0) {
            throw new IllegalArgumentException("El precio debe ser positivo");
        }
        Map<Integer, Tiquete> tiquetes = state.getTiquetesPorId();
        Set<Integer> deluxe = state.getTiquetesDeluxe();
        Map<Integer, String> tiqueteEnOferta = state.getTiqueteEnOferta();
        List<Tiquete> tiquetesOferta = new ArrayList<>();
        for (Integer id : tiquetesIds) {
            Tiquete tiquete = tiquetes.get(id);
            if (tiquete == null) {
                throw new IllegalArgumentException("No existe el tiquete " + id);
            }
            if (!Objects.equals(tiquete.getCliente(), vendedor)) {
                throw new IllegalArgumentException("El tiquete " + id + " no pertenece al vendedor");
            }
            if (tiquete.isImpreso()) {
                throw new IllegalArgumentException("El tiquete " + id + " ya fue impreso y no puede publicarse en el marketplace");
            }
            if (deluxe.contains(id)) {
                throw new IllegalArgumentException("El tiquete " + id + " pertenece a un paquete Deluxe y no puede revenderse");
            }
            if (tiqueteEnOferta.containsKey(id)) {
                throw new IllegalArgumentException("El tiquete " + id + " ya está incluido en otra oferta");
            }
            tiquetesOferta.add(tiquete);
        }
        OfertaMarketPlace oferta = new OfertaMarketPlace(vendedor, tiquetesOferta, precioInicial);
        state.getOfertasPorId().put(oferta.getId(), oferta);
        for (Tiquete tiquete : tiquetesOferta) {
            tiqueteEnOferta.put(tiquete.getIdTiquete(), oferta.getId());
        }
        logSistema.registrar("OFERTA", String.format("%s publicó la oferta %s por %.2f",
                vendedor.getLogin(), oferta.getId(), precioInicial));
        return oferta;
    }

    public synchronized void cancelarOfertaPorVendedor(Cliente vendedor, String ofertaId) {
        OfertaMarketPlace oferta = validarOfertaDeVendedor(vendedor, ofertaId);
        if (oferta.getEstado() != EstadoOferta.ACTIVA) {
            throw new IllegalStateException("La oferta no está activa");
        }
        oferta.setEstado(EstadoOferta.CANCELADA_VENDEDOR);
        liberarTiquetes(oferta);
        logSistema.registrar("OFERTA", String.format("%s canceló la oferta %s",
                vendedor.getLogin(), oferta.getId()));
    }

    public synchronized void cancelarOfertaPorAdministrador(Administrador admin, String ofertaId) {
        Administrador administrador = state.getAdministrador();
        if (administrador == null || admin == null || !administrador.equals(admin)) {
            throw new SecurityException("Administrador no autorizado");
        }
        OfertaMarketPlace oferta = state.getOfertasPorId().get(ofertaId);
        if (oferta == null) {
            throw new IllegalArgumentException("No existe la oferta");
        }
        if (oferta.getEstado() != EstadoOferta.ACTIVA) {
            throw new IllegalStateException("La oferta no está activa");
        }
        oferta.setEstado(EstadoOferta.CANCELADA_ADMIN);
        liberarTiquetes(oferta);
        logSistema.registrar("OFERTA", String.format(
                "El administrador eliminó la oferta %s del vendedor %s",
                oferta.getId(), oferta.getVendedor().getLogin()));
    }

    public synchronized ContraOferta crearContraoferta(Cliente comprador, String ofertaId, double monto) {
        OfertaMarketPlace oferta = state.getOfertasPorId().get(ofertaId);
        if (comprador == null) {
            throw new IllegalArgumentException("Se requiere un comprador");
        }
        if (oferta == null) {
            throw new IllegalArgumentException("No existe la oferta");
        }
        if (oferta.getEstado() != EstadoOferta.ACTIVA) {
            throw new IllegalStateException("La oferta no está activa");
        }
        if (oferta.getVendedor().equals(comprador)) {
            throw new IllegalArgumentException("No puede contraofertar su propia oferta");
        }
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        if (comprador.getSaldo() < monto) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar la contraoferta");
        }
        ContraOferta contra = oferta.agregarContraoferta(comprador, monto);
        logSistema.registrar("CONTRAOFERTA", String.format("%s propuso %.2f sobre la oferta %s",
                comprador.getLogin(), monto, oferta.getId()));
        return contra;
    }

    public synchronized void rechazarContraoferta(Cliente vendedor, String ofertaId, String contraofertaId) {
        OfertaMarketPlace oferta = validarOfertaDeVendedor(vendedor, ofertaId);
        ContraOferta contra = oferta.buscarContraoferta(contraofertaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la contraoferta"));
        if (contra.getEstado() != EstadoContraOferta.PENDIENTE) {
            throw new IllegalStateException("La contraoferta ya fue gestionada");
        }
        contra.setEstado(EstadoContraOferta.RECHAZADA);
        logSistema.registrar("CONTRAOFERTA", String.format("%s rechazó la contraoferta %s de %s",
                vendedor.getLogin(), contra.getId(), contra.getComprador().getLogin()));
    }

    public synchronized void aceptarContraoferta(Cliente vendedor, String ofertaId, String contraofertaId) {
        OfertaMarketPlace oferta = validarOfertaDeVendedor(vendedor, ofertaId);
        if (oferta.getEstado() != EstadoOferta.ACTIVA) {
            throw new IllegalStateException("La oferta no está activa");
        }
        ContraOferta contra = oferta.buscarContraoferta(contraofertaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la contraoferta"));
        if (contra.getEstado() != EstadoContraOferta.PENDIENTE) {
            throw new IllegalStateException("La contraoferta ya fue gestionada");
        }
        validarTiquetesNoImpresos(oferta);
        Cliente comprador = contra.getComprador();
        double monto = contra.getMonto();
        comprador.usarSaldo(monto);
        vendedor.acreditarSaldo(monto);
        transferirTiquetes(oferta, comprador);
        contra.setEstado(EstadoContraOferta.ACEPTADA);
        oferta.setEstado(EstadoOferta.VENDIDA);
        logSistema.registrar("CONTRAOFERTA", String.format("%s aceptó la contraoferta %s de %s",
                vendedor.getLogin(), contra.getId(), comprador.getLogin()));
        logSistema.registrar("TRANSACCION", String.format(
                "Venta concretada por %.2f entre %s y %s (oferta %s)",
                monto, vendedor.getLogin(), comprador.getLogin(), oferta.getId()));
    }

    public synchronized void comprarOferta(Cliente comprador, String ofertaId) {
        OfertaMarketPlace oferta = state.getOfertasPorId().get(ofertaId);
        if (oferta == null) {
            throw new IllegalArgumentException("No existe la oferta");
        }
        if (oferta.getEstado() != EstadoOferta.ACTIVA) {
            throw new IllegalStateException("La oferta no está activa");
        }
        if (oferta.getVendedor().equals(comprador)) {
            throw new IllegalArgumentException("No puede comprar su propia oferta");
        }
        validarTiquetesNoImpresos(oferta);
        double monto = oferta.getPrecioInicial();
        comprador.usarSaldo(monto);
        Cliente vendedor = oferta.getVendedor();
        vendedor.acreditarSaldo(monto);
        transferirTiquetes(oferta, comprador);
        oferta.setEstado(EstadoOferta.VENDIDA);
        logSistema.registrar("TRANSACCION", String.format(
                "Compra directa por %.2f entre %s y %s (oferta %s)",
                monto, comprador.getLogin(), vendedor.getLogin(), oferta.getId()));
    }

    private void transferirTiquetes(OfertaMarketPlace oferta, Cliente nuevoPropietario) {
        Map<Integer, String> tiqueteEnOferta = state.getTiqueteEnOferta();
        for (Tiquete tiquete : oferta.getTiquetes()) {
        	if (tiquete.isImpreso()) {
                throw new IllegalStateException("El tiquete " + tiquete.getIdTiquete() + " ya fue impreso y no puede transferirse");
            }
            Cliente antiguo = tiquete.getCliente();
            if (antiguo != null) {
                antiguo.eliminarTiquete(tiquete);
            }
            nuevoPropietario.agregarTiquete(tiquete);
            tiquete.setCliente(nuevoPropietario);
            tiqueteEnOferta.remove(tiquete.getIdTiquete());
        }
    }
    
    private void validarTiquetesNoImpresos(OfertaMarketPlace oferta) {
        for (Tiquete tiquete : oferta.getTiquetes()) {
            if (tiquete.isImpreso()) {
                throw new IllegalStateException("El tiquete " + tiquete.getIdTiquete() + " ya fue impreso y no puede venderse");
            }
        }
    }

    private OfertaMarketPlace validarOfertaDeVendedor(Cliente vendedor, String ofertaId) {
        OfertaMarketPlace oferta = state.getOfertasPorId().get(ofertaId);
        if (oferta == null) {
            throw new IllegalArgumentException("No existe la oferta");
        }
        if (!oferta.perteneceAlVendedor(vendedor)) {
            throw new SecurityException("La oferta no pertenece al vendedor");
        }
        return oferta;
    }

    private void liberarTiquetes(OfertaMarketPlace oferta) {
        Map<Integer, String> tiqueteEnOferta = state.getTiqueteEnOferta();
        for (Tiquete tiquete : oferta.getTiquetes()) {
            tiqueteEnOferta.remove(tiquete.getIdTiquete());
        }
    }
}