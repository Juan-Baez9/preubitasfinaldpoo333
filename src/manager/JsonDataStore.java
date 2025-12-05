package manager;

import Cliente.Administrador;

import Cliente.Cliente;
import Cliente.Organizador;
import eventos.Evento;
import eventos.Localidad;
import eventos.Oferta;
import eventos.TipoEvento;
import eventos.Venue;
import log.EntradaLog;
import marketPlace.ContraOferta;
import marketPlace.EstadoOferta;
import marketPlace.OfertaMarketPlace;
import marketPlace.EstadoContraOferta;
import tiquetes.Tiquete;
import tiquetes.TiqueteBasico;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Gestiona la lectura y escritura de los archivos JSON del sistema.
 */
final class JsonDataStore {

    private final Path usuariosPath;
    private final Path eventosPath;
    private final Path tiquetesPath;
    private final Path paquetesPath;
    private final Path ofertasPath;
    private final Path logPath;

    JsonDataStore(Path usuariosPath,
                  Path eventosPath,
                  Path tiquetesPath,
                  Path paquetesPath,
                  Path ofertasPath,
                  Path logPath) {
        this.usuariosPath = Objects.requireNonNull(usuariosPath);
        this.eventosPath = Objects.requireNonNull(eventosPath);
        this.tiquetesPath = Objects.requireNonNull(tiquetesPath);
        this.paquetesPath = Objects.requireNonNull(paquetesPath);
        this.ofertasPath = Objects.requireNonNull(ofertasPath);
        this.logPath = Objects.requireNonNull(logPath);
    }

    static JsonDataStore fromDirectory(String dataDir) {
        Path base = PathResolver.of(dataDir);
        return new JsonDataStore(
                base.resolve("usuarios.json"),
                base.resolve("eventos.json"),
                base.resolve("tiquetes.json"),
                base.resolve("paquetes.json"),
                base.resolve("marketplace_ofertas.json"),
                base.resolve("marketplace_log.json"));
    }

    BoletaMasterState load() {
        BoletaMasterState state = new BoletaMasterState();
        state.reset();
        cargarUsuarios(state);
        cargarEventos(state);
        cargarTiquetes(state);
        cargarPaquetes(state);
        cargarLog(state);
        cargarOfertas(state);
        return state;
    }

    void save(BoletaMasterState state) {
        guardarUsuarios(state);
        guardarEventos(state);
        guardarTiquetes(state);
        guardarPaquetes(state);
        guardarLog(state);
        guardarOfertas(state);
    }

    private void cargarUsuarios(BoletaMasterState state) {
        if (!Files.exists(usuariosPath)) {
            throw new RuntimeException("No existe el archivo de usuarios: " + usuariosPath);
        }
        String raw = JsonFiles.read(usuariosPath);
        JSONObject root = new JSONObject(raw);
        JSONObject adminJson = root.getJSONObject("administrador");
        String adminLogin = adminJson.getString("login");
        String adminPassword = adminJson.optString("password", adminLogin);
        Administrador administrador = new Administrador(
                adminJson.optDouble("ganancias", 0.0),
                adminJson.getString("idAdministrador"),
                new ArrayList<>(),
                adminLogin,
                adminPassword,
                adminJson.getString("nombre"),
                adminJson.optDouble("saldo", 0.0));
        JSONObject cargos = adminJson.optJSONObject("cargosServicio");
        if (cargos != null) {
            for (String key : cargos.keySet()) {
                TipoEvento tipo = TipoEvento.valueOf(key);
                administrador.fijarCargoServicio(tipo, cargos.getDouble(key));
            }
        }
        if (adminJson.has("cargoEmision")) {
            administrador.fijarCargoEmision(adminJson.getDouble("cargoEmision"), null);
        }
        state.setAdministrador(administrador);

        JSONArray clientes = root.optJSONArray("clientes");
        if (clientes != null) {
            for (int i = 0; i < clientes.length(); i++) {
                JSONObject c = clientes.getJSONObject(i);
                String login = c.getString("login");
                Cliente cliente = new Cliente(login,
                        c.optString("password", login),
                        c.getString("nombre"),
                        c.optDouble("saldo", 0.0),
                        c.getString("idCliente"));
                state.getClientesPorLogin().put(login, cliente);
            }
        }

        JSONArray organizadores = root.optJSONArray("organizadores");
        if (organizadores != null) {
            for (int i = 0; i < organizadores.length(); i++) {
                JSONObject o = organizadores.getJSONObject(i);
                String login = o.getString("login");
                Organizador organizador = new Organizador(login,
                        o.optString("password", login),
                        o.getString("nombre"),
                        o.optDouble("saldo", 0.0),
                        o.getString("idOrganizador"),
                        o.optDouble("finanzas", 0.0),
                        new Evento[0]);
                state.getOrganizadoresPorLogin().put(login, organizador);
            }
        }
    }

    private void cargarEventos(BoletaMasterState state) {
        if (!Files.exists(eventosPath)) {
            return;
        }
        String raw = JsonFiles.read(eventosPath);
        if (raw.isBlank()) {
            return;
        }
        JSONArray eventos = new JSONArray(raw);
        Map<String, Organizador> organizadores = state.getOrganizadoresPorLogin();
        Map<String, Evento> eventosPorId = state.getEventosPorId();
        Administrador admin = state.getAdministrador();
        if (admin == null) {
            throw new IllegalStateException("No hay administrador cargado para los eventos");
        }
        for (int i = 0; i < eventos.length(); i++) {
            JSONObject e = eventos.getJSONObject(i);
            JSONObject venueJson = e.getJSONObject("venue");
            ArrayList<Localidad> localidades = new ArrayList<>();
            JSONArray locs = venueJson.optJSONArray("localidades");
            if (locs != null) {
                for (int j = 0; j < locs.length(); j++) {
                    JSONObject l = locs.getJSONObject(j);
                    localidades.add(new Localidad(null, new ArrayList<>(),
                            l.getString("nombre"),
                            l.optDouble("precioBase", 0.0),
                            l.optBoolean("numerada", false),
                            l.optInt("numeroAsientos", 0)));
                }
            }
            Venue venue = new Venue(venueJson.getString("idVenue"),
                    venueJson.getString("nombre"),
                    venueJson.optString("ubicacion", ""),
                    venueJson.optInt("capacidadMaxima", 0),
                    localidades);

            String tipo = e.optString("tipoEvento", TipoEvento.CONCIERTO.name());
            Evento evento = new Evento(
                    admin,
                    e.getString("idEvento"),
                    e.getString("nombre"),
                    LocalDate.parse(e.getString("fecha")),
                    LocalTime.parse(e.optString("hora", "00:00")),
                    e.optString("estado", "CREADO"),
                    TipoEvento.valueOf(tipo),
                    venue,
                    null,
                    null,
                    new ArrayList<>());
            venue.registrarEvento(evento);
            String organizadorLogin = e.optString("organizadorLogin", null);
            if (organizadorLogin != null) {
                Organizador organizador = organizadores.get(organizadorLogin);
                if (organizador != null) {
                    evento.setOrganizador(organizador);
                    organizador.registrarEvento(evento);
                }
            }
            if (e.has("oferta")) {
                JSONObject ofertaJson = e.getJSONObject("oferta");
                Localidad localidadOferta = buscarLocalidad(venue, ofertaJson.optString("idLocalidad", null));
                LocalDateTime inicio = parseDateTime(ofertaJson.optString("inicio", null));
                LocalDateTime fin = parseDateTime(ofertaJson.optString("fin", null));
                if (localidadOferta != null && inicio != null && fin != null) {
                    Oferta oferta = new Oferta(localidadOferta, evento,
                            ofertaJson.getDouble("porcentaje"), inicio, fin);
                    localidadOferta.setOferta(oferta);
                    evento.setOferta(oferta);
                }
            }
            eventosPorId.put(evento.getIdEvento(), evento);
        }
    }

    private void cargarTiquetes(BoletaMasterState state) {
        if (!Files.exists(tiquetesPath)) {
            return;
        }
        String raw = JsonFiles.read(tiquetesPath);
        if (raw.isBlank()) {
            return;
        }
        JSONArray arr = new JSONArray(raw);
        Map<String, Cliente> clientes = state.getClientesPorLogin();
        Map<String, Evento> eventos = state.getEventosPorId();
        Map<Integer, Tiquete> tiquetes = state.getTiquetesPorId();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.getJSONObject(i);
            Evento evento = eventos.get(t.optString("eventoId", null));
            if (evento == null) {
                continue;
            }
            String tipoTiquete = t.optString("tipo", "BASICO");
            Localidad localidad = buscarLocalidad(evento == null ? null : evento.getVenue(), t.optString("idLocalidad", null));
            Cliente propietario = null;
            String propietarioLogin = t.optString("propietarioLogin", null);
            if (propietarioLogin != null) {
                propietario = clientes.get(propietarioLogin);
            }
            Tiquete tiquete;
            if ("BASICO".equalsIgnoreCase(tipoTiquete)) {
                Integer numeroAsiento = t.has("numeroAsiento") && !t.isNull("numeroAsiento")
                        ? t.getInt("numeroAsiento") : null;
                TiqueteBasico basico = new TiqueteBasico(
                		propietario,
                        t.getInt("idTiquete"),
                        t.optDouble("precio", 0.0),
                        t.optDouble("cargoServicio", 0.0),
                        t.optDouble("cargoEmision", 0.0),
                        t.optString("estado", "CREADO"),
                        localidad,
                        evento,
                        numeroAsiento,
                        t.optBoolean("localidadNumerada", false));
                
                tiquete = basico;
            } else {
            	TiqueteBasico generico = new TiqueteBasico(
                        propietario,
                        t.getInt("idTiquete"),
                        t.optDouble("precio", 0.0),
                        t.optDouble("cargoServicio", 0.0),
                        t.optDouble("cargoEmision", 0.0),
                        t.optString("estado", "CREADO"),
                        localidad,
                        evento,
                        null,
                        t.optBoolean("localidadNumerada", false));
                tiquete = generico;
            }
            if (propietario != null) {
                propietario.agregarTiquete(tiquete);
                tiquete.setCliente(propietario);
            }
            tiquete.setImpreso(t.optBoolean("impreso", false));
            if (t.has("fechaImpresion") && !t.isNull("fechaImpresion")) {
                tiquete.setFechaImpresion(LocalDateTime.parse(t.getString("fechaImpresion")));
            }
            if (localidad != null) {
                localidad.agregarTiquete(tiquete);
            }
            if (evento != null) {
                evento.registrarTiquete(tiquete);
            }
            tiquetes.put(tiquete.getIdTiquete(), tiquete);
        }
    }

    private void cargarPaquetes(BoletaMasterState state) {
        if (!Files.exists(paquetesPath)) {
            state.setPaquetesRaw(new JSONArray());
            return;
        }
        String raw = JsonFiles.read(paquetesPath);
        if (raw.isBlank()) {
            state.setPaquetesRaw(new JSONArray());
            return;
        }
        JSONArray paquetes = new JSONArray(raw);
        state.setPaquetesRaw(paquetes);
        for (int i = 0; i < paquetes.length(); i++) {
            JSONObject p = paquetes.getJSONObject(i);
            if ("DELUXE".equalsIgnoreCase(p.optString("tipo"))) {
                JSONArray incluidos = p.optJSONArray("tiquetesIncluidos");
                if (incluidos != null) {
                    for (int j = 0; j < incluidos.length(); j++) {
                        state.getTiquetesDeluxe().add(incluidos.getInt(j));
                    }
                }
            }
        }
    }

    private void cargarLog(BoletaMasterState state) {
        if (!Files.exists(logPath)) {
            return;
        }
        String raw = JsonFiles.read(logPath);
        if (raw.isBlank()) {
            return;
        }
        JSONArray arr = new JSONArray(raw);
        List<EntradaLog> entradas = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.getJSONObject(i);
            entradas.add(new EntradaLog(
                    LocalDateTime.parse(e.getString("fechaHora")),
                    e.getString("tipo"),
                    e.getString("descripcion")));
        }
        state.getLogSistema().reemplazarEntradas(entradas);
    }

    private void cargarOfertas(BoletaMasterState state) {
        if (!Files.exists(ofertasPath)) {
            return;
        }
        String raw = JsonFiles.read(ofertasPath);
        if (raw.isBlank()) {
            return;
        }
        JSONArray arr = new JSONArray(raw);
        Map<String, Cliente> clientes = state.getClientesPorLogin();
        Map<Integer, Tiquete> tiquetes = state.getTiquetesPorId();
        Map<String, OfertaMarketPlace> ofertas = state.getOfertasPorId();
        Map<Integer, String> tiqueteEnOferta = state.getTiqueteEnOferta();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Cliente vendedor = clientes.get(o.getString("vendedorLogin"));
            if (vendedor == null) {
                continue;
            }
            List<Tiquete> tiquetesOferta = new ArrayList<>();
            JSONArray ids = o.optJSONArray("tiquetes");
            if (ids != null) {
                for (int j = 0; j < ids.length(); j++) {
                    Tiquete tiquete = tiquetes.get(ids.getInt(j));
                    if (tiquete != null) {
                        tiquetesOferta.add(tiquete);
                    }
                }
            }
            List<ContraOferta> contraofertas = new ArrayList<>();
            JSONArray contraArr = o.optJSONArray("contraofertas");
            if (contraArr != null) {
                for (int j = 0; j < contraArr.length(); j++) {
                    JSONObject c = contraArr.getJSONObject(j);
                    Cliente comprador = clientes.get(c.getString("compradorLogin"));
                    if (comprador == null) {
                        continue;
                    }
                    contraofertas.add(new ContraOferta(
                            c.getString("id"),
                            comprador,
                            c.getDouble("monto"),
                            EstadoContraOferta.valueOf(c.getString("estado")),
                            LocalDateTime.parse(c.getString("fechaCreacion"))));
                }
            }
            OfertaMarketPlace oferta = new OfertaMarketPlace(
                    o.getString("id"),
                    vendedor,
                    tiquetesOferta,
                    o.getDouble("precioInicial"),
                    EstadoOferta.valueOf(o.getString("estado")),
                    contraofertas,
                    LocalDateTime.parse(o.getString("fechaCreacion")));
            ofertas.put(oferta.getId(), oferta);
            if (oferta.getEstado() == EstadoOferta.ACTIVA) {
                for (Tiquete tiquete : tiquetesOferta) {
                    tiqueteEnOferta.put(tiquete.getIdTiquete(), oferta.getId());
                }
            }
        }
    }

    private void guardarUsuarios(BoletaMasterState state) {
        JSONObject root = new JSONObject();
        Administrador administrador = state.getAdministrador();
        if (administrador != null) {
            JSONObject admin = new JSONObject();
            admin.put("idAdministrador", administrador.getIdAdministrador());
            admin.put("saldo", administrador.getSaldo());
            admin.put("login", administrador.getLogin());
            admin.put("password", administrador.getPassword());
            admin.put("nombre", administrador.getNombre());
            admin.put("ganancias", administrador.getGanancias());
            JSONObject cargos = new JSONObject();
            for (TipoEvento tipo : TipoEvento.values()) {
                cargos.put(tipo.name(), administrador.getCargoServicio(tipo));
            }
            admin.put("cargosServicio", cargos);
            admin.put("cargoEmision", administrador.getCargoEmision());
            JSONArray venues = new JSONArray();
            for (Venue venue : administrador.getVenuesAprobados()) {
                venues.put(venue.getIdVenue());
            }
            admin.put("venuesAprobados", venues);
            root.put("administrador", admin);
        }
        JSONArray clientes = new JSONArray();
        for (Cliente cliente : state.getClientesPorLogin().values()) {
            JSONObject c = new JSONObject();
            c.put("idCliente", cliente.getIdCliente());
            c.put("saldo", cliente.getSaldo());
            c.put("login", cliente.getLogin());
            c.put("password", cliente.getPassword());
            c.put("nombre", cliente.getNombre());
            JSONArray tiquetes = new JSONArray();
            for (Tiquete tiquete : cliente.verTiquetes()) {
                tiquetes.put(tiquete.getIdTiquete());
            }
            c.put("tiquetes", tiquetes);
            clientes.put(c);
        }
        root.put("clientes", clientes);
        JSONArray organizadores = new JSONArray();
        for (Organizador organizador : state.getOrganizadoresPorLogin().values()) {
            JSONObject o = new JSONObject();
            o.put("idOrganizador", organizador.getIdOrganizador());
            o.put("saldo", organizador.getSaldo());
            o.put("login", organizador.getLogin());
            o.put("password", organizador.getPassword());
            o.put("nombre", organizador.getNombre());
            o.put("finanzas", organizador.getFinanzas());
            JSONArray eventos = new JSONArray();
            for (Evento evento : organizador.getEventos()) {
                eventos.put(evento.getIdEvento());
            }
            o.put("eventos", eventos);
            organizadores.put(o);
        }
        root.put("organizadores", organizadores);
        JsonFiles.write(usuariosPath, root.toString(2));
    }

    private void guardarEventos(BoletaMasterState state) {
        JSONArray arr = new JSONArray();
        for (Evento evento : state.getEventosPorId().values()) {
            JSONObject e = new JSONObject();
            e.put("idEvento", evento.getIdEvento());
            e.put("nombre", evento.getNombre());
            e.put("fecha", evento.getFecha().toString());
            e.put("hora", evento.getHora().toString());
            e.put("estado", evento.getEstado());
            e.put("tipoEvento", evento.getTipoEvento().name());
            e.put("organizadorLogin", evento.getOrganizador() == null ? JSONObject.NULL : evento.getOrganizador().getLogin());
            e.put("administradorLogin", evento.getAdministrador() == null ? JSONObject.NULL : evento.getAdministrador().getLogin());
            JSONArray tiquetes = new JSONArray();
            for (Tiquete tiquete : evento.getTiquetes()) {
                tiquetes.put(tiquete.getIdTiquete());
            }
            e.put("tiquetes", tiquetes);
            Venue venue = evento.getVenue();
            if (venue != null) {
                JSONObject v = new JSONObject();
                v.put("idVenue", venue.getIdVenue());
                v.put("nombre", venue.getNombre());
                v.put("ubicacion", venue.getUbicacion());
                v.put("capacidadMaxima", venue.getCapacidadMaxima());
                JSONArray locs = new JSONArray();
                for (Localidad localidad : venue.getLocalidades()) {
                    JSONObject l = new JSONObject();
                    l.put("nombre", localidad.getNombre());
                    l.put("precioBase", localidad.getPrecioBase());
                    l.put("numerada", localidad.isNumerada());
                    l.put("numeroAsientos", localidad.getNumeroAsientos());
                    locs.put(l);
                }
                v.put("localidades", locs);
                e.put("venue", v);
            }
            if (evento.getOferta() != null) {
                Oferta oferta = evento.getOferta();
                JSONObject o = new JSONObject();
                o.put("porcentaje", oferta.getPorcentaje());
                o.put("inicio", oferta.getInicio().toString());
                o.put("fin", oferta.getFin().toString());
                if (oferta.getLocalidad() != null && evento.getVenue() != null) {
                    String idLocalidad = evento.getVenue().getIdVenue() + "::" + oferta.getLocalidad().getNombre();
                    o.put("idLocalidad", idLocalidad);
                } else {
                    o.put("idLocalidad", JSONObject.NULL);
                }
                e.put("oferta", o);
            }
            arr.put(e);
        }
        JsonFiles.write(eventosPath, arr.toString(2));
    }

    private void guardarTiquetes(BoletaMasterState state) {
        JSONArray arr = new JSONArray();
        for (Tiquete tiquete : state.getTiquetesPorId().values()) {
            JSONObject t = new JSONObject();
            t.put("idTiquete", tiquete.getIdTiquete());
            t.put("precio", tiquete.getPrecio());
            t.put("cargoServicio", tiquete.getCargoServicio());
            t.put("cargoEmision", tiquete.getCargoEmision());
            t.put("estado", tiquete.getEstado());
            t.put("tipo", tiquete instanceof TiqueteBasico ? "BASICO" : "OTRO");
            t.put("eventoId", tiquete.getEvento() == null ? JSONObject.NULL : tiquete.getEvento().getIdEvento());
            t.put("propietarioLogin", tiquete.getCliente() == null ? JSONObject.NULL : tiquete.getCliente().getLogin());
            t.put("impreso", tiquete.isImpreso());
            t.put("fechaImpresion", tiquete.getFechaImpresion() == null ? JSONObject.NULL : tiquete.getFechaImpresion().toString());
            if (tiquete instanceof TiqueteBasico tb) {
                t.put("numeroAsiento", tb.getNumeroAsiento() == null ? JSONObject.NULL : tb.getNumeroAsiento());
                t.put("localidadNumerada", tb.isLocalidadNumerada());
            }
            if (tiquete.getEvento() != null && tiquete.getEvento().getVenue() != null && tiquete.getLocalidad() != null) {
                String idLocalidad = tiquete.getEvento().getVenue().getIdVenue() + "::" + tiquete.getLocalidad().getNombre();
                t.put("idLocalidad", idLocalidad);
            } else {
                t.put("idLocalidad", JSONObject.NULL);
            }
            arr.put(t);
        }
        JsonFiles.write(tiquetesPath, arr.toString(2));
    }

    private void guardarPaquetes(BoletaMasterState state) {
        JsonFiles.write(paquetesPath, state.getPaquetesRaw().toString(2));
    }

    private void guardarLog(BoletaMasterState state) {
        JSONArray arr = new JSONArray();
        for (EntradaLog entrada : state.getLogSistema().getEntradas()) {
            JSONObject e = new JSONObject();
            e.put("fechaHora", entrada.getFechaHora().toString());
            e.put("tipo", entrada.getTipo());
            e.put("descripcion", entrada.getDescripcion());
            arr.put(e);
        }
        JsonFiles.write(logPath, arr.toString(2));
    }

    private void guardarOfertas(BoletaMasterState state) {
        JSONArray arr = new JSONArray();
        for (OfertaMarketPlace oferta : state.getOfertasPorId().values()) {
            JSONObject o = new JSONObject();
            o.put("id", oferta.getId());
            o.put("vendedorLogin", oferta.getVendedor().getLogin());
            o.put("precioInicial", oferta.getPrecioInicial());
            o.put("estado", oferta.getEstado().name());
            o.put("fechaCreacion", oferta.getFechaCreacion().toString());
            JSONArray tiquetes = new JSONArray();
            for (Tiquete tiquete : oferta.getTiquetes()) {
                tiquetes.put(tiquete.getIdTiquete());
            }
            o.put("tiquetes", tiquetes);
            JSONArray contraArr = new JSONArray();
            for (ContraOferta contra : oferta.getContraofertas()) {
                JSONObject c = new JSONObject();
                c.put("id", contra.getId());
                c.put("compradorLogin", contra.getComprador().getLogin());
                c.put("monto", contra.getMonto());
                c.put("estado", contra.getEstado().name());
                c.put("fechaCreacion", contra.getFechaCreacion().toString());
                contraArr.put(c);
            }
            o.put("contraofertas", contraArr);
            arr.put(o);
        }
        JsonFiles.write(ofertasPath, arr.toString(2));
    }

    private Localidad buscarLocalidad(Venue venue, String identificador) {
        if (venue == null || identificador == null || identificador.isBlank()) {
            return null;
        }
        String[] partes = identificador.split("::");
        String nombreLocalidad = partes.length == 2 ? partes[1] : identificador;
        for (Localidad localidad : venue.getLocalidades()) {
            if (localidad.getNombre().equals(nombreLocalidad)) {
                return localidad;
            }
        }
        return null;
    }
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}