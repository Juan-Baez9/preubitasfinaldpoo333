package console;

import Cliente.Cliente;
import manager.BoletaMasterSystem;
import marketPlace.ContraOferta;
import marketPlace.OfertaMarketPlace;
import marketPlace.EstadoContraOferta;
import tiquetes.Tiquete;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Interfaz de línea de comandos para clientes de la plataforma.
 * Permite autenticarse, gestionar ofertas de reventa, contraofertar y comprar.
 */
public final class ClienteApp {

    private ClienteApp() {}

    public static void main(String[] args) {
        BoletaMasterSystem sistema = BoletaMasterSystem.desdeDirectorio("data");
        sistema.cargarDatos();
        try (Scanner scanner = new Scanner(System.in)) {
            Cliente cliente = autenticar(scanner, sistema);
            if (cliente == null) {
                System.out.println("Credenciales inválidas. Finalizando aplicación.");
                return;
            }
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();
                try {
                    switch (opcion) {
                        case "1" -> listarTiquetes(cliente);
                        case "2" -> publicarOferta(scanner, sistema, cliente);
                        case "3" -> cancelarOferta(scanner, sistema, cliente);
                        case "4" -> mostrarOfertasActivas(sistema, cliente);
                        case "5" -> comprarOferta(scanner, sistema, cliente);
                        case "6" -> contraofertar(scanner, sistema, cliente);
                        case "7" -> mostrarContraofertasPendientes(sistema, cliente);
                        case "8" -> aceptarContraoferta(scanner, sistema, cliente);
                        case "9" -> rechazarContraoferta(scanner, sistema, cliente);
                        case "10" -> System.out.printf("Saldo disponible: %.2f%n", cliente.getSaldo());
                        case "0" -> salir = true;
                        default -> System.out.println("Opción no válida.");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } finally {
            sistema.guardarDatos();
        }
    }

    private static Cliente autenticar(Scanner scanner, BoletaMasterSystem sistema) {
        System.out.print("Login: ");
        String login = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();
        return sistema.autenticarCliente(login, password).orElse(null);
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("=== Menú Cliente ===");
        System.out.println("1. Ver mis tiquetes");
        System.out.println("2. Publicar oferta");
        System.out.println("3. Cancelar oferta");
        System.out.println("4. Ver ofertas activas");
        System.out.println("5. Comprar oferta");
        System.out.println("6. Contraofertar");
        System.out.println("7. Ver contraofertas pendientes");
        System.out.println("8. Aceptar contraoferta");
        System.out.println("9. Rechazar contraoferta");
        System.out.println("10. Consultar saldo");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void listarTiquetes(Cliente cliente) {
        List<Tiquete> tiquetes = cliente.verTiquetes();
        if (tiquetes.isEmpty()) {
            System.out.println("No tiene tiquetes disponibles.");
            return;
        }
        System.out.println("Mis tiquetes:");
        for (Tiquete tiquete : tiquetes) {
            String evento = tiquete.getEvento() == null ? "(sin evento)" : tiquete.getEvento().getNombre();
            System.out.printf("- ID %d | Evento: %s | Estado: %s%n", tiquete.getIdTiquete(), evento, tiquete.getEstado());
        }
    }

    private static void publicarOferta(Scanner scanner, BoletaMasterSystem sistema, Cliente cliente) {
        System.out.print("Ingrese el precio inicial: ");
        double precio = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Ingrese los IDs de tiquetes separados por coma: ");
        String linea = scanner.nextLine().trim();
        List<Integer> ids = parsearEnteros(linea);
        OfertaMarketPlace oferta = sistema.publicarOferta(cliente, ids, precio);
        System.out.printf("Oferta %s creada con éxito.%n", oferta.getId());
    }

    private static void cancelarOferta(Scanner scanner, BoletaMasterSystem sistema, Cliente cliente) {
        mostrarOfertasDeVendedor(sistema, cliente);
        System.out.print("Ingrese el ID de la oferta a cancelar: ");
        String ofertaId = scanner.nextLine().trim();
        sistema.cancelarOfertaPorVendedor(cliente, ofertaId);
        System.out.println("Oferta cancelada.");
    }

    private static void mostrarOfertasActivas(BoletaMasterSystem sistema, Cliente cliente) {
        List<OfertaMarketPlace> ofertas = sistema.obtenerOfertasActivas().stream()
                .filter(o -> !o.getVendedor().equals(cliente))
                .collect(Collectors.toList());
        if (ofertas.isEmpty()) {
            System.out.println("No hay ofertas activas disponibles.");
            return;
        }
        System.out.println("Ofertas activas:");
        for (OfertaMarketPlace oferta : ofertas) {
            String eventos = oferta.getTiquetes().stream()
                    .map(t -> t.getEvento() == null ? "(sin evento)" : t.getEvento().getNombre())
                    .distinct()
                    .collect(Collectors.joining(", "));
            System.out.printf("- ID %s | Vendedor: %s | Precio: %.2f | Eventos: %s%n",
                    oferta.getId(), oferta.getVendedor().getLogin(), oferta.getPrecioInicial(), eventos);
        }
    }

    private static void comprarOferta(Scanner scanner, BoletaMasterSystem sistema, Cliente cliente) {
        mostrarOfertasActivas(sistema, cliente);
        System.out.print("Ingrese el ID de la oferta a comprar: ");
        String ofertaId = scanner.nextLine().trim();
        sistema.comprarOferta(cliente, ofertaId);
        System.out.println("Compra realizada con éxito.");
    }

    private static void contraofertar(Scanner scanner, BoletaMasterSystem sistema, Cliente cliente) {
        mostrarOfertasActivas(sistema, cliente);
        System.out.print("Ingrese el ID de la oferta a contraofertar: ");
        String ofertaId = scanner.nextLine().trim();
        System.out.print("Ingrese el valor de la contraoferta: ");
        double monto = Double.parseDouble(scanner.nextLine().trim());
        sistema.crearContraoferta(cliente, ofertaId, monto);
        System.out.println("Contraoferta enviada.");
    }

    private static void mostrarContraofertasPendientes(BoletaMasterSystem sistema, Cliente cliente) {
        Map<OfertaMarketPlace, List<ContraOferta>> pendientes = sistema.contraofertasPendientes(cliente);
        if (pendientes.isEmpty()) {
            System.out.println("No tiene contraofertas pendientes.");
            return;
        }
        for (Map.Entry<OfertaMarketPlace, List<ContraOferta>> entry : pendientes.entrySet()) {
            OfertaMarketPlace oferta = entry.getKey();
            System.out.printf("Oferta %s:%n", oferta.getId());
            for (ContraOferta contra : entry.getValue()) {
                System.out.printf("  - ID %s | Comprador: %s | Monto: %.2f%n",
                        contra.getId(), contra.getComprador().getLogin(), contra.getMonto());
            }
        }
    }

    private static void aceptarContraoferta(Scanner scanner, BoletaMasterSystem sistema, Cliente cliente) {
        mostrarContraofertasPendientes(sistema, cliente);
        System.out.print("ID de la oferta: ");
        String ofertaId = scanner.nextLine().trim();
        System.out.print("ID de la contraoferta: ");
        String contraId = scanner.nextLine().trim();
        sistema.aceptarContraoferta(cliente, ofertaId, contraId);
        System.out.println("Contraoferta aceptada y venta concretada.");
    }

    private static void rechazarContraoferta(Scanner scanner, BoletaMasterSystem sistema, Cliente cliente) {
        mostrarContraofertasPendientes(sistema, cliente);
        System.out.print("ID de la oferta: ");
        String ofertaId = scanner.nextLine().trim();
        System.out.print("ID de la contraoferta: ");
        String contraId = scanner.nextLine().trim();
        sistema.rechazarContraoferta(cliente, ofertaId, contraId);
        System.out.println("Contraoferta rechazada.");
    }

    private static void mostrarOfertasDeVendedor(BoletaMasterSystem sistema, Cliente cliente) {
        List<OfertaMarketPlace> ofertas = sistema.obtenerOfertasPorVendedor(cliente);
        if (ofertas.isEmpty()) {
            System.out.println("No tiene ofertas publicadas.");
            return;
        }
        for (OfertaMarketPlace oferta : ofertas) {
            System.out.printf("- ID %s | Estado: %s | Precio: %.2f | Contraofertas pendientes: %d%n",
                    oferta.getId(), oferta.getEstado(), oferta.getPrecioInicial(),
                    oferta.getContraofertas().stream().filter(c -> c.getEstado() == EstadoContraOferta.PENDIENTE).count());
        }
    }

    private static List<Integer> parsearEnteros(String linea) {
        if (linea.isBlank()) {
            return List.of();
        }
        String[] partes = linea.split(",");
        List<Integer> numeros = new ArrayList<>();
        for (String parte : partes) {
            numeros.add(Integer.parseInt(parte.trim()));
        }
        return numeros;
    }
}