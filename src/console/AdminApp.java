package console;

import Cliente.Administrador;
import log.EntradaLog;
import manager.BoletaMasterSystem;
import marketPlace.OfertaMarketPlace;

import java.util.List;
import java.util.Scanner;

/**
 * Interfaz de consola para el administrador del sistema.
 */
public final class AdminApp {

    private AdminApp() {}

    public static void main(String[] args) {
        BoletaMasterSystem sistema = BoletaMasterSystem.desdeDirectorio("data");
        sistema.cargarDatos();
        try (Scanner scanner = new Scanner(System.in)) {
            Administrador admin = autenticar(scanner, sistema);
            if (admin == null) {
                System.out.println("Credenciales inválidas. Finalizando aplicación.");
                return;
            }
            String login = admin.getLogin();
            String password = admin.getPassword();
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();
                try {
                    switch (opcion) {
                        case "1" -> listarOfertas(sistema);
                        case "2" -> cancelarOferta(scanner, sistema, admin);
                        case "3" -> mostrarLog(sistema, admin, login, password);
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

    private static Administrador autenticar(Scanner scanner, BoletaMasterSystem sistema) {
        System.out.print("Login administrador: ");
        String login = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();
        return sistema.autenticarAdministrador(login, password).orElse(null);
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("=== Menú Administrador ===");
        System.out.println("1. Listar ofertas del marketplace");
        System.out.println("2. Eliminar oferta");
        System.out.println("3. Consultar log del marketplace");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void listarOfertas(BoletaMasterSystem sistema) {
        List<OfertaMarketPlace> ofertas = sistema.obtenerTodasLasOfertas();
        if (ofertas.isEmpty()) {
            System.out.println("No hay ofertas registradas.");
            return;
        }
        System.out.println("Ofertas registradas:");
        for (OfertaMarketPlace oferta : ofertas) {
            System.out.printf("- ID %s | Vendedor: %s | Precio: %.2f | Estado: %s%n",
                    oferta.getId(), oferta.getVendedor().getLogin(), oferta.getPrecioInicial(), oferta.getEstado());
        }
    }

    private static void cancelarOferta(Scanner scanner, BoletaMasterSystem sistema, Administrador admin) {
        listarOfertas(sistema);
        System.out.print("Ingrese el ID de la oferta a eliminar: ");
        String ofertaId = scanner.nextLine().trim();
        sistema.cancelarOfertaPorAdministrador(admin, ofertaId);
        System.out.println("Oferta eliminada.");
    }

    private static void mostrarLog(BoletaMasterSystem sistema, Administrador admin, String login, String password) {
        List<EntradaLog> entradas = sistema.getLogSistema().consultar(admin, login, password);
        if (entradas.isEmpty()) {
            System.out.println("El log está vacío.");
            return;
        }
        System.out.println("=== Log del marketplace ===");
        for (EntradaLog entrada : entradas) {
            System.out.printf("[%s] %s - %s%n", entrada.getFechaHora(), entrada.getTipo(), entrada.getDescripcion());
        }
    }
}