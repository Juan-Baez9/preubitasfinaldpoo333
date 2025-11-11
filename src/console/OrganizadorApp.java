package console;

import Cliente.Organizador;
import eventos.Evento;
import manager.BoletaMasterSystem;

import java.util.Collection;
import java.util.Scanner;

/**
 * Interfaz de consola para organizadores.
 */
public final class OrganizadorApp {

    private OrganizadorApp() {}

    public static void main(String[] args) {
        BoletaMasterSystem sistema = BoletaMasterSystem.desdeDirectorio("data");
        sistema.cargarDatos();
        try (Scanner scanner = new Scanner(System.in)) {
            Organizador organizador = autenticar(scanner, sistema);
            if (organizador == null) {
                System.out.println("Credenciales inválidas. Finalizando aplicación.");
                return;
            }
            String login = organizador.getLogin();
            String password = organizador.getPassword();
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();
                try {
                    switch (opcion) {
                        case "1" -> listarEventos(sistema, organizador);
                        case "2" -> mostrarFinanzas(organizador, login, password);
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

    private static Organizador autenticar(Scanner scanner, BoletaMasterSystem sistema) {
        System.out.print("Login organizador: ");
        String login = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();
        return sistema.autenticarOrganizador(login, password).orElse(null);
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("=== Menú Organizador ===");
        System.out.println("1. Listar mis eventos");
        System.out.println("2. Consultar finanzas");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void listarEventos(BoletaMasterSystem sistema, Organizador organizador) {
        Collection<Evento> eventos = sistema.getEventosOrganizador(organizador);
        if (eventos.isEmpty()) {
            System.out.println("No tiene eventos asociados.");
            return;
        }
        System.out.println("Eventos:");
        for (Evento evento : eventos) {
            String venue = evento.getVenue() == null ? "(sin venue)" : evento.getVenue().getNombre();
            System.out.printf("- %s | Fecha: %s | Hora: %s | Venue: %s | Estado: %s%n",
                    evento.getNombre(), evento.getFecha(), evento.getHora(), venue, evento.getEstado());
        }
    }

    private static void mostrarFinanzas(Organizador organizador, String login, String password) {
        double finanzas = organizador.consultarFinanzas(login, password);
        System.out.printf("Finanzas disponibles: %.2f%n", finanzas);
    }
}