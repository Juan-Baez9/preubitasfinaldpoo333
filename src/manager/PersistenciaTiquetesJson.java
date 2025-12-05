package manager;

import Cliente.Cliente;
import Cliente.Usuario;
import eventos.Evento;
import eventos.Localidad;
import org.json.JSONArray;
import org.json.JSONObject;
import tiquetes.PaqueteDeluxe;
import tiquetes.PaqueteTiquetes;
import tiquetes.Tiquete;
import tiquetes.TiqueteBasico;
import tiquetes.TiqueteMultiple;
import tiquetes.TiqueteTemporada;

import java.nio.file.Paths;
import java.util.*;
/**
 * Utilidades de persistencia JSON para tiquetes y paquetes de tiquetes.
 * <p>
 * Provee carga/guardado de tiquetes simples y paquetes (múltiple, temporada, deluxe),
 * reconstruyendo referencias a usuarios, eventos y localidades cuando es posible.
 */
public class PersistenciaTiquetesJson implements IPersistenciaTiquetes {

        @Override
        public List<Tiquete> cargarTiquetes(String archivo, List<Usuario> usuarios, List<Evento> eventos) {
            return cargarTiquetesSimples(archivo, usuarios, eventos);
        }

        @Override
        public void salvarTiquetes(String archivo, List<Tiquete> tiquetes) {
            salvarTiquetesSimples(archivo, tiquetes);
        }

        private static Map<String, Usuario> indexUsuarios(List<Usuario> usuarios) {
	    Map<String, Usuario> m = new HashMap<>();
	    if (usuarios != null) {
	        for (Usuario u : usuarios) {
	            if (u != null && u.getLogin() != null)
	                m.put(u.getLogin(), u);
	        }
	    }
	    return m;
	}

	private static Map<String, Evento> indexEventos(List<Evento> eventos) {
	    Map<String, Evento> m = new HashMap<>();
	    if (eventos != null) {
	        for (Evento e : eventos) {
	            if (e != null && e.getIdEvento() != null)
	                m.put(e.getIdEvento(), e);
	        }
	    }
	    return m;
	}
	/**
	 * Carga paquetes de tiquetes desde un JSON, reconstruyendo la información disponible.
	 *
	 * @param archivo      ruta del archivo JSON.
	 * @param usuarios     lista de usuarios existentes (para mapear propietario si corresponde).
	 * @param eventos      lista de eventos existentes (para contexto).
	 * @param simplesPorId mapa auxiliar de tiquetes simples por id (para armar paquetes deluxe).
	 * @return lista de paquetes cargados.
	 * @throws RuntimeException si ocurre un error de lectura/mapeo JSON.
	 */
        public List<PaqueteTiquetes> cargarPaquetes(
                String archivo,
                List<Usuario> usuarios,
                List<Evento> eventos,
                Map<Integer, Tiquete> simplesPorId
        ) {
	    Map<String, Usuario> uIndex = indexUsuarios(usuarios);
	    Map<String, Evento>  eIndex = indexEventos(eventos);

	    List<PaqueteTiquetes> result = new ArrayList<>();
            String raw = JsonFiles.read(Paths.get(archivo));
	    if (raw == null || raw.isBlank()) return result;

	    JSONArray arr = new JSONArray(raw);

	    for (int i = 0; i < arr.length(); i++) {
	        JSONObject jt = arr.getJSONObject(i);
	        String tipo = jt.optString("tipo", "OTRO");

	   
	        if (!"MULTIPLE".equals(tipo) && !"TEMPORADA".equals(tipo) && !"DELUXE".equals(tipo)) {
	            continue;
	        }

	        
	        Usuario propietario = jt.isNull("propietarioLogin") ? null : uIndex.get(jt.getString("propietarioLogin"));
	        Evento  evento      = jt.isNull("eventoId")         ? null : eIndex.get(jt.getString("eventoId"));

	        PaqueteTiquetes p;

	        switch (tipo) {
	            case "MULTIPLE": {
	                
	                TiqueteMultiple tm = new TiqueteMultiple(
	                        jt.getInt("cantidadEntradas"),
	                        jt.getDouble("precioTotal")
	                );
	                p = tm;
	                break;
	            }

	            case "TEMPORADA": {
	                
	                TiqueteTemporada tp = new TiqueteTemporada(
	                        jt.getInt("cantidadEventos"),
	                        jt.getDouble("precioTotal")
	                );
	                p = tp;
	                break;
	            }

	            case "DELUXE": {
	                
	                List<String> beneficios = new ArrayList<>();
	                JSONArray jBenef = jt.optJSONArray("beneficios");
	                if (jBenef != null) {
	                    for (int k = 0; k < jBenef.length(); k++) {
	                        beneficios.add(jBenef.getString(k));
	                    }
	                }

	               
	                List<Tiquete> incluidos = new ArrayList<>();
	                JSONArray jIncluidos = jt.optJSONArray("tiquetesIncluidos");
	                if (jIncluidos != null && simplesPorId != null) {
	                    for (int k = 0; k < jIncluidos.length(); k++) {
	                        
	                        int idT = jIncluidos.getInt(k);
	                        Tiquete simple = simplesPorId.get(idT);
	                        if (simple != null) incluidos.add(simple);
	                    }
	                }
	               
	                PaqueteDeluxe pd = new PaqueteDeluxe(beneficios, incluidos);
	                p = pd;
	                break;
	            }

	            default:
	                continue;
	        }

	       
                result.add(p);
            }

	    return result;
	}
	/**
	 * Serializa y guarda paquetes de tiquetes a un archivo JSON.
	 *
	 * @param archivo  ruta del archivo destino.
	 * @param paquetes paquetes a persistir (múltiple, temporada, deluxe).
	 * @throws RuntimeException si ocurre un error de escritura/serialización.
	 */
        public void salvarPaquetes(String archivo, List<PaqueteTiquetes> paquetes) {
            JSONArray arr = new JSONArray();

            for (PaqueteTiquetes p : paquetes) {
                JSONObject jt = new JSONObject();

                if (p instanceof TiqueteMultiple tm) {
                    jt.put("tipo", "MULTIPLE");
                    jt.put("cantidadEntradas", tm.getCantidadEntradas());
                    jt.put("precioTotal", tm.getPrecioTotal());
                } else if (p instanceof TiqueteTemporada tp) {
                    jt.put("tipo", "TEMPORADA");
                    jt.put("cantidadEventos", tp.getCantidadEventos());
                    jt.put("precioTotal", tp.getPrecioTotal());
                } else if (p instanceof PaqueteDeluxe pd) {
                    jt.put("tipo", "DELUXE");

                    JSONArray jBenef = new JSONArray();
                    for (String b : pd.getBeneficios()) {
                        jBenef.put(b);
                    }
                    jt.put("beneficios", jBenef);

                    JSONArray jIncluidos = new JSONArray();
                    for (Tiquete t : pd.getTiquetes()) {
                        jIncluidos.put(t.getIdTiquete());
                    }
                    jt.put("tiquetesIncluidos", jIncluidos);
                } else {
                    jt.put("tipo", "OTRO_PAQUETE");
                }

                arr.put(jt);
            }

            JsonFiles.write(Paths.get(archivo), arr.toString(2));
        }

/**
 * Carga tiquetes simples (p. ej., {@link tiquetes.TiqueteBasico}) desde un JSON.
 * <p>
 * Intenta reconstruir propietario, evento y localidad (si existen en las colecciones provistas).
 *
 * @param archivo  ruta del archivo JSON.
 * @param usuarios lista de usuarios existentes para asociar propietario.
 * @param eventos  lista de eventos existentes para asociar evento/localidad.
 * @return lista de tiquetes cargados.
 * @throws RuntimeException si ocurre un error de lectura/mapeo JSON.
 */
 public List<Tiquete> cargarTiquetesSimples(String archivo,
                                            List<Usuario> usuarios,
                                            List<Evento> eventos) {
     Map<String, Usuario> uIndex = indexUsuarios(usuarios);
     Map<String, Evento>  eIndex = indexEventos(eventos);

     List<Tiquete> result = new ArrayList<>();
     String raw = JsonFiles.read(Paths.get(archivo));
     if (raw == null || raw.isBlank()) return result;

     JSONArray arr = new JSONArray(raw);

     for (int i = 0; i < arr.length(); i++) {
         JSONObject jt = arr.getJSONObject(i);
         String tipo = jt.optString("tipo", "OTRO");

         if (!"BASICO".equals(tipo) && !"OTRO".equals(tipo)) continue;

         Usuario propietario = jt.isNull("propietarioLogin") ? null : uIndex.get(jt.getString("propietarioLogin"));
         Evento  evento      = jt.isNull("eventoId") ? null : eIndex.get(jt.getString("eventoId"));

         Localidad localidad = null;
         if (evento != null && evento.getVenue() != null) {
             for (Localidad l : evento.getVenue().getLocalidades()) {
                 if (l.getNombre().equals(jt.optString("idLocalidad", ""))) {
                     localidad = l;
                     break;
                 }
             }
         }

         Tiquete t = null;

         if ("BASICO".equals(tipo)) {
             TiqueteBasico tb = new TiqueteBasico(
                 (Cliente) propietario,
                 jt.getInt("idTiquete"),
                 jt.getDouble("precio"),
                 jt.getDouble("cargoServicio"),
                 jt.getDouble("cargoEmision"),
                 jt.getString("estado"),
                 localidad,
                 evento,
                 jt.optInt("numeroAsiento", 0),
                 jt.optBoolean("localidadNumerada", localidad != null && localidad.isNumerada())
             );
             t = tb;
         }

         if (t != null) {
             result.add(t);
             if (propietario instanceof Cliente) {
                 ((Cliente) propietario).agregarTiquete(t);
                 t.setCliente((Cliente) propietario);
             }
             if (evento != null) {
                 evento.registrarTiquete(t);
             }
             if (localidad != null) {
                 localidad.agregarTiquete(t);
             }
         }
     }

     return result;
 }
 /**
  * Serializa y guarda tiquetes simples en un archivo JSON.
  *
  * @param archivo  ruta del archivo destino.
  * @param tiquetes tiquetes a persistir.
  * @throws RuntimeException si ocurre un error de escritura/serialización.
  */
        public void salvarTiquetesSimples(String archivo, List<Tiquete> tiquetes) {
            JSONArray arr = new JSONArray();

            for (Tiquete t : tiquetes) {

                JSONObject jt = new JSONObject();
                String loginDueno = null;
                if (t.getCliente() instanceof Cliente c) loginDueno = c.getLogin();
                jt.put("propietarioLogin", loginDueno == null ? JSONObject.NULL : loginDueno);
                jt.put("idTiquete", t.getIdTiquete());
                jt.put("precio", t.getPrecio());
                jt.put("cargoServicio", t.getCargoServicio());
                jt.put("cargoEmision", t.getCargoEmision());
                jt.put("estado", t.getEstado());

                jt.put("eventoId", (t.getEvento()==null) ? JSONObject.NULL : t.getEvento().getIdEvento());

                String tipoOut = (t instanceof TiqueteBasico) ? "BASICO" : "OTRO";
                jt.put("tipo", tipoOut);

                if (t instanceof TiqueteBasico tb) {
                    jt.put("numeroAsiento", tb.getNumeroAsiento());
                    jt.put("localidadNumerada", tb.isLocalidadNumerada());
                    if (t.getEvento()!=null && t.getEvento().getVenue()!=null && t.getLocalidad()!=null) {
                        String locId = t.getEvento().getVenue().getIdVenue() + "::" + t.getLocalidad().getNombre();
                        jt.put("idLocalidad", locId);
                    } else {
                        jt.put("idLocalidad", JSONObject.NULL);
                    }
                }

                arr.put(jt);
            }

            JsonFiles.write(Paths.get(archivo), arr.toString(2));
        }
}

