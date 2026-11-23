/* ReservasLab · UdeC — cliente web de los 3 microservicios (sin dependencias) */
(() => {
  "use strict";

  // ------------------------------------------------------------------
  // Configuración
  // ------------------------------------------------------------------
  const ENTORNOS = {
    render: {
      usuarios: "https://reservaslab-usuarios-qw7i.onrender.com",
      reservas: "https://reservaslab-reservas-qw7i.onrender.com",
      notificaciones: "https://reservaslab-notificaciones-qw7i.onrender.com",
    },
    local: {
      usuarios: "http://localhost:8080",
      reservas: "http://localhost:8083",
      notificaciones: "http://localhost:8084",
    },
  };
  const APERTURA = 7, CIERRE = 21, MAX_HORAS = 4;
  const ZONA = "America/Bogota";

  // ------------------------------------------------------------------
  // Utilidades
  // ------------------------------------------------------------------
  const $ = (sel, raiz = document) => raiz.querySelector(sel);
  const $$ = (sel, raiz = document) => [...raiz.querySelectorAll(sel)];
  const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const hh = (t) => String(t ?? "").slice(0, 5);                       // "08:00:00" → "08:00"
  const pad = (n) => String(n).padStart(2, "0");
  const hora = (n) => `${pad(n)}:00`;

  const hoyBogota = () => new Intl.DateTimeFormat("en-CA", { timeZone: ZONA }).format(new Date());
  const horaBogota = () => Number(new Intl.DateTimeFormat("en-GB", { timeZone: ZONA, hour: "2-digit", hour12: false }).format(new Date()));
  const diaSemana = (iso) => new Date(`${iso}T12:00:00`).getDay();     // 0 = domingo
  const fechaLarga = (iso) => new Date(`${iso}T12:00:00`).toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long" });
  const partesFecha = (iso) => {
    const d = new Date(`${iso}T12:00:00`);
    return { dia: pad(d.getDate()), mes: d.toLocaleDateString("es-CO", { month: "short" }).replace(".", ""), anio: d.getFullYear() };
  };
  const siguienteHabil = () => {
    const d = new Date(`${hoyBogota()}T12:00:00`);
    if (horaBogota() >= CIERRE - 1) d.setDate(d.getDate() + 1);
    if (d.getDay() === 0) d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
  };

  const almacen = {
    leer(k) { try { return JSON.parse(localStorage.getItem(k)); } catch { return null; } },
    guardar(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* sin almacenamiento */ } },
    borrar(k) { try { localStorage.removeItem(k); } catch { /* sin almacenamiento */ } },
  };

  // ------------------------------------------------------------------
  // Estado
  // ------------------------------------------------------------------
  const estado = {
    entorno: almacen.leer("rl.entorno") || "render",
    usuario: almacen.leer("rl.usuario"),
    laboratorios: [],
    agenda: null,          // Disponibilidad actual
    sel: null,             // { a, b } índices de franjas
    filtroReservas: "",
    reservas: [],
  };

  // ------------------------------------------------------------------
  // API
  // ------------------------------------------------------------------
  let pendientes = 0, avisoLento;
  function cargando(si) {
    pendientes += si ? 1 : -1;
    const caja = $("#cargando");
    if (pendientes > 0) {
      caja.hidden = false;
      $("#cargando-texto").textContent = "Cargando…";
      clearTimeout(avisoLento);
      avisoLento = setTimeout(() => {
        $("#cargando-texto").textContent = estado.entorno === "render"
          ? "El servidor se está despertando (plan gratis de Render). Puede tardar hasta un minuto…"
          : "Esperando respuesta del servidor local…";
      }, 3500);
    } else {
      pendientes = 0;
      caja.hidden = true;
      clearTimeout(avisoLento);
    }
  }

  async function api(servicio, ruta, { metodo = "GET", cuerpo } = {}) {
    const url = ENTORNOS[estado.entorno][servicio] + ruta;
    cargando(true);
    try {
      let res;
      try {
        res = await fetch(url, {
          method: metodo,
          headers: cuerpo ? { "Content-Type": "application/json" } : {},
          body: cuerpo ? JSON.stringify(cuerpo) : undefined,
        });
      } catch {
        throw new Error(`No hay conexión con el servicio ${servicio}. ¿Está encendido y desplegado con CORS?`);
      }
      const texto = await res.text();
      let datos = null;
      try { datos = texto ? JSON.parse(texto) : null; } catch { datos = texto; }
      if (!res.ok) {
        throw new Error((datos && datos.mensaje) || `Error ${res.status} en ${servicio}`);
      }
      return datos;
    } finally {
      cargando(false);
    }
  }

  // ------------------------------------------------------------------
  // Toasts
  // ------------------------------------------------------------------
  function toast(mensaje, tipo = "ok") {
    const t = document.createElement("div");
    t.className = `toast toast--${tipo}`;
    t.innerHTML = `<span class="mono">${tipo === "error" ? "Error" : "Listo"}</span>${esc(mensaje)}`;
    $("#toasts").append(t);
    setTimeout(() => t.remove(), tipo === "error" ? 6500 : 4000);
  }
  const fallo = (e) => toast(e.message, "error");

  // ------------------------------------------------------------------
  // Entorno (Render / Local)
  // ------------------------------------------------------------------
  function pintarEntorno() {
    $$("[data-entorno]").forEach((b) => b.classList.toggle("activo", b.dataset.entorno === estado.entorno));
  }
  $$("[data-entorno]").forEach((b) => b.addEventListener("click", () => {
    estado.entorno = b.dataset.entorno;
    almacen.guardar("rl.entorno", estado.entorno);
    pintarEntorno();
    toast(`Usando servidores ${estado.entorno === "render" ? "de Render" : "locales"}`);
    estado.agenda = null;
    if (estado.usuario) arrancarApp();
  }));

  // ------------------------------------------------------------------
  // Acceso
  // ------------------------------------------------------------------
  $$(".pestana").forEach((p) => p.addEventListener("click", () => {
    $$(".pestana").forEach((x) => x.classList.toggle("activa", x === p));
    $$("[data-panel]").forEach((f) => { f.hidden = f.dataset.panel !== p.dataset.pestana; });
  }));

  function iniciarSesion(u) {
    estado.usuario = { idUsuario: u.idUsuario, nombre: u.nombre, correo: u.correo, rol: u.rol };
    almacen.guardar("rl.usuario", estado.usuario);
    mostrarApp();
  }

  $("#form-login").addEventListener("submit", async (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    try {
      const r = await api("usuarios", "/api/usuarios/login", { metodo: "POST", cuerpo: { correo: f.get("correo"), clave: f.get("clave") } });
      toast(r.mensaje || "Bienvenido");
      iniciarSesion(r.usuario);
    } catch (err) { fallo(err); }
  });

  $("#form-registro").addEventListener("submit", async (e) => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.target));
    try {
      const u = await api("usuarios", "/api/usuarios/registrar", { metodo: "POST", cuerpo: f });
      toast(`Cuenta creada. Tu número de usuario es ${u.idUsuario}`);
      iniciarSesion(u);
    } catch (err) { fallo(err); }
  });

  $("#salir").addEventListener("click", () => {
    estado.usuario = null;
    almacen.borrar("rl.usuario");
    mostrarAcceso();
  });

  function mostrarAcceso() {
    $("#app").hidden = true;
    $("#acceso").hidden = false;
  }

  function mostrarApp() {
    $("#acceso").hidden = true;
    $("#app").hidden = false;
    pintarUsuario();
    arrancarApp();
  }

  function pintarUsuario() {
    const u = estado.usuario;
    $("#usuario-nombre").textContent = u.nombre;
    $("#usuario-correo").textContent = u.correo || "";
    $("#usuario-rol").textContent = `${u.rol} · #${u.idUsuario}`;
    $$("[data-solo-admin]").forEach((n) => { n.hidden = u.rol !== "ADMIN"; });
  }

  // ------------------------------------------------------------------
  // Navegación
  // ------------------------------------------------------------------
  const CARGAS = {
    agenda: () => {},
    libres: () => {},
    "mis-reservas": cargarReservas,
    notificaciones: cargarAvisos,
    cuenta: cargarCuenta,
    admin: cargarAdmin,
  };

  function irA(vista) {
    if (!CARGAS[vista] || (vista === "admin" && estado.usuario?.rol !== "ADMIN")) vista = "agenda";
    $$(".vista").forEach((v) => { v.hidden = v.dataset.vista !== vista; });
    $$(".riel__nav a").forEach((a) => a.classList.toggle("activo", a.dataset.vista === vista));
    window.scrollTo({ top: 0 });
    CARGAS[vista]();
  }
  window.addEventListener("hashchange", () => estado.usuario && irA(location.hash.slice(1)));

  async function arrancarApp() {
    const fecha = siguienteHabil();
    $("#agenda-fecha").value = $("#agenda-fecha").value || fecha;
    $("#form-libres [name=fecha]").value = $("#form-libres [name=fecha]").value || fecha;
    $$("input[type=date]").forEach((i) => { i.min = hoyBogota(); });
    irA(location.hash.slice(1));
    await cargarLaboratorios();
    contarAvisos();
  }

  // ------------------------------------------------------------------
  // 01 · Agenda
  // ------------------------------------------------------------------
  async function cargarLaboratorios() {
    try {
      estado.laboratorios = await api("reservas", "/api/laboratorios/lista");
    } catch (err) { fallo(err); estado.laboratorios = []; }
    const sel = $("#sel-laboratorio");
    const previo = sel.value;
    sel.innerHTML = estado.laboratorios.length
      ? estado.laboratorios.map((l) => `<option value="${l.idLaboratorio}">${esc(l.nombre)} — ${esc(l.ubicacion)}</option>`).join("")
      : `<option value="">No hay laboratorios</option>`;
    if (previo && estado.laboratorios.some((l) => String(l.idLaboratorio) === previo)) sel.value = previo;
    if (estado.laboratorios.length && !estado.agenda) cargarAgenda();
    else if (!estado.laboratorios.length) pintarAgenda();
  }

  async function cargarAgenda(preseleccion) {
    const id = $("#sel-laboratorio").value;
    const fecha = $("#agenda-fecha").value;
    estado.sel = null;
    if (!id || !fecha) { estado.agenda = null; pintarAgenda(); return; }
    try {
      estado.agenda = await api("reservas", `/api/laboratorios/${id}/disponibilidad?fecha=${fecha}`);
      if (preseleccion) {
        const a = estado.agenda.franjas.findIndex((f) => hh(f.inicio) === preseleccion.inicio);
        const b = estado.agenda.franjas.findIndex((f) => hh(f.fin) === preseleccion.fin);
        if (a >= 0 && b >= a) estado.sel = { a, b };
      }
    } catch (err) { fallo(err); estado.agenda = null; }
    pintarAgenda();
  }

  const esPasada = (fecha, franja) => fecha < hoyBogota() || (fecha === hoyBogota() && Number(hh(franja.inicio).slice(0, 2)) <= horaBogota());
  const libre = (f) => f.disponible && !esPasada(estado.agenda.fecha, f);

  function pintarAgenda() {
    const lista = $("#agenda-franjas"), meta = $("#agenda-meta");
    const d = estado.agenda;
    if (!d) {
      lista.className = "franjas franjas--vacio";
      lista.innerHTML = `<li><em>${estado.laboratorios.length ? "Elige un laboratorio y una fecha." : "Aún no hay laboratorios registrados."}</em></li>`;
      meta.innerHTML = "";
      pintarSolicitud();
      return;
    }
    lista.className = "franjas";
    const lab = estado.laboratorios.find((l) => l.idLaboratorio === d.idLaboratorio);
    const etiquetas = [`<span class="etiqueta">${esc(fechaLarga(d.fecha))}</span>`];
    if (lab) etiquetas.push(`<span class="etiqueta">Capacidad ${lab.capacidad}</span>`);
    if (diaSemana(d.fecha) === 0) etiquetas.push(`<span class="etiqueta etiqueta--mal">Domingo · cerrado</span>`);
    else if (d.festivo) etiquetas.push(`<span class="etiqueta etiqueta--alerta">Festivo · cerrado</span>`);
    else if (!d.laboratorioHabilitado) etiquetas.push(`<span class="etiqueta etiqueta--alerta">En mantenimiento</span>`);
    else etiquetas.push(`<span class="etiqueta etiqueta--ok">${d.franjas.filter(libre).length} horas libres</span>`);
    meta.innerHTML = etiquetas.join("");

    lista.innerHTML = d.franjas.map((f, i) => {
      const enSel = estado.sel && i >= estado.sel.a && i <= estado.sel.b;
      const clase = enSel ? "franja--sel" : libre(f) ? "franja--libre" : "franja--ocupada";
      const texto = enSel ? "Seleccionada" : libre(f) ? "Libre" : esPasada(d.fecha, f) && f.disponible ? "Ya pasó" : "No disponible";
      return `<li class="franja ${clase}"><button type="button" data-i="${i}" ${libre(f) ? "" : "disabled"}
        aria-label="${hh(f.inicio)} a ${hh(f.fin)}: ${texto}"><b>${hh(f.inicio)}</b><small>${texto}</small></button></li>`;
    }).join("");
    pintarSolicitud();
  }

  $("#agenda-franjas").addEventListener("click", (e) => {
    const b = e.target.closest("button[data-i]");
    if (!b) return;
    const i = Number(b.dataset.i);
    const s = estado.sel;
    if (s && s.a === s.b && i === s.a) {
      estado.sel = null;
    } else if (s && s.a === s.b && i > s.a) {
      const rango = estado.agenda.franjas.slice(s.a, i + 1);
      if (rango.length > MAX_HORAS) { toast(`Una reserva no puede durar más de ${MAX_HORAS} horas`, "error"); estado.sel = { a: i, b: i }; }
      else if (!rango.every(libre)) { toast("Hay horas ocupadas en medio de ese rango", "error"); estado.sel = { a: i, b: i }; }
      else estado.sel = { a: s.a, b: i };
    } else {
      estado.sel = { a: i, b: i };
    }
    pintarAgenda();
  });

  function pintarSolicitud() {
    const r = $("#solicitud-resumen"), boton = $("#btn-reservar");
    const d = estado.agenda, s = estado.sel;
    if (!d || !s) {
      r.innerHTML = "Selecciona un horario en la agenda.<em>Toca la hora de inicio y luego la última hora.</em>";
      boton.disabled = true;
      return;
    }
    const lab = estado.laboratorios.find((l) => l.idLaboratorio === d.idLaboratorio);
    const inicio = hh(d.franjas[s.a].inicio), fin = hh(d.franjas[s.b].fin);
    r.innerHTML = `${esc(lab?.nombre || "Laboratorio")}<br>${inicio} → ${fin}<em>${esc(fechaLarga(d.fecha))} · ${s.b - s.a + 1} h</em>`;
    const personas = $("#form-reserva [name=cantidadPersonas]");
    if (lab) personas.max = lab.capacidad;
    boton.disabled = false;
  }

  $("#form-agenda").addEventListener("submit", (e) => { e.preventDefault(); cargarAgenda(); });
  $("#sel-laboratorio").addEventListener("change", () => cargarAgenda());
  $("#agenda-fecha").addEventListener("change", () => cargarAgenda());

  $("#form-reserva").addEventListener("submit", async (e) => {
    e.preventDefault();
    const d = estado.agenda, s = estado.sel;
    if (!d || !s) return;
    const f = new FormData(e.target);
    try {
      const r = await api("reservas", "/api/reservas/crear", {
        metodo: "POST",
        cuerpo: {
          idUsuario: estado.usuario.idUsuario,
          idLaboratorio: d.idLaboratorio,
          fecha: d.fecha,
          horaInicio: hh(d.franjas[s.a].inicio),
          horaFin: hh(d.franjas[s.b].fin),
          cantidadPersonas: Number(f.get("cantidadPersonas")),
          motivo: f.get("motivo"),
        },
      });
      toast(`Reserva #${r.idReserva} registrada. Te llegará un correo de confirmación.`);
      e.target.reset();
      cargarAgenda();
      setTimeout(contarAvisos, 4000);
    } catch (err) { fallo(err); }
  });

  // ------------------------------------------------------------------
  // 02 · Buscar libres
  // ------------------------------------------------------------------
  $$("[data-horas]").forEach((s) => {
    const desde = s.dataset.horas === "inicio" ? APERTURA : APERTURA + 1;
    const hasta = s.dataset.horas === "inicio" ? CIERRE - 1 : CIERRE;
    for (let h = desde; h <= hasta; h++) s.add(new Option(hora(h), hora(h)));
  });
  $("#form-libres [name=horaInicio]").value = hora(8);
  $("#form-libres [name=horaFin]").value = hora(10);

  $("#form-libres").addEventListener("submit", async (e) => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.target));
    const lista = $("#lista-libres");
    try {
      const labs = await api("reservas", `/api/laboratorios/disponibles?fecha=${f.fecha}&horaInicio=${f.horaInicio}&horaFin=${f.horaFin}`);
      lista.innerHTML = labs.length
        ? labs.map((l) => `<li>
            <div><h4>${esc(l.nombre)}</h4><p class="mono">${esc(l.ubicacion)}</p></div>
            <div class="cap">${l.capacidad}<small>personas</small></div>
            <button class="boton boton--mini" data-reservar="${l.idLaboratorio}">Reservar aquí →</button>
          </li>`).join("")
        : `<li class="vacio">Ningún laboratorio está libre de ${f.horaInicio} a ${f.horaFin} ese día.</li>`;
      lista.dataset.busqueda = JSON.stringify(f);
    } catch (err) { fallo(err); lista.innerHTML = ""; }
  });

  $("#lista-libres").addEventListener("click", (e) => {
    const b = e.target.closest("[data-reservar]");
    if (!b) return;
    const f = JSON.parse($("#lista-libres").dataset.busqueda);
    $("#sel-laboratorio").value = b.dataset.reservar;
    $("#agenda-fecha").value = f.fecha;
    location.hash = "agenda";
    cargarAgenda({ inicio: f.horaInicio, fin: f.horaFin });
  });

  // ------------------------------------------------------------------
  // 03 · Mis reservas
  // ------------------------------------------------------------------
  async function cargarReservas() {
    try {
      estado.reservas = await api("reservas", `/api/reservas/usuario/${estado.usuario.idUsuario}`);
    } catch (err) { fallo(err); estado.reservas = []; }
    pintarReservas();
  }

  function filaReserva(r, { admin = false } = {}) {
    const p = partesFecha(r.fecha);
    const pasada = r.fecha < hoyBogota();
    const acciones = [];
    if (admin && r.estado === "PENDIENTE") acciones.push(`<button class="boton boton--mini" data-accion="confirmar" data-id="${r.idReserva}">Confirmar</button>`);
    if (admin && r.estado === "CONFIRMADA") acciones.push(`<button class="boton boton--mini" data-accion="finalizar" data-id="${r.idReserva}">Finalizar</button>`);
    if (["PENDIENTE", "CONFIRMADA"].includes(r.estado)) acciones.push(`<button class="boton boton--mini boton--peligro" data-accion="cancelar" data-id="${r.idReserva}">Cancelar</button>`);
    return `<li class="${pasada ? "pasada" : ""}">
      <div class="fecha">${p.dia}<small>${esc(p.mes)} ${p.anio}</small></div>
      <div><h4>${esc(r.nombreLaboratorio)}</h4><p>${esc(r.motivo)}</p></div>
      <div><span class="mono">#${r.idReserva} · ${hh(r.horaInicio)}–${hh(r.horaFin)} · ${r.cantidadPersonas} pers.</span></div>
      <div class="acciones"><span class="estado estado--${r.estado}">${r.estado}</span>${acciones.join("")}</div>
    </li>`;
  }

  function pintarReservas() {
    const lista = $("#lista-reservas");
    const visibles = estado.reservas.filter((r) => !estado.filtroReservas || r.estado === estado.filtroReservas);
    lista.className = "registro";
    lista.innerHTML = visibles.length
      ? visibles.map((r) => filaReserva(r)).join("")
      : `<li class="vacio">${estado.reservas.length ? "No hay reservas con ese estado." : "Todavía no has hecho reservas. Empieza en la Agenda."}</li>`;
  }

  $("#filtro-estados").addEventListener("click", (e) => {
    const b = e.target.closest("[data-estado]");
    if (!b) return;
    estado.filtroReservas = b.dataset.estado;
    $$("#filtro-estados .chip-btn").forEach((x) => x.classList.toggle("activo", x === b));
    pintarReservas();
  });

  async function cambiarReserva(id, accion) {
    if (accion === "cancelar" && !confirm(`¿Cancelar la reserva #${id}?`)) return null;
    try {
      const r = await api("reservas", `/api/reservas/${id}/${accion}`, { metodo: "PATCH" });
      toast(`Reserva #${id}: ${r.estado.toLowerCase()}`);
      setTimeout(contarAvisos, 4000);
      return r;
    } catch (err) { fallo(err); return null; }
  }

  $("#lista-reservas").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-accion]");
    if (!b) return;
    if (await cambiarReserva(b.dataset.id, b.dataset.accion)) cargarReservas();
  });

  // ------------------------------------------------------------------
  // 04 · Avisos
  // ------------------------------------------------------------------
  async function contarAvisos() {
    if (!estado.usuario) return;
    try {
      const n = (await api("notificaciones", `/api/notificaciones/usuario/${estado.usuario.idUsuario}/no-leidas`)).length;
      const c = $("#contador-avisos");
      c.hidden = n === 0;
      c.textContent = n;
    } catch { /* el contador es secundario */ }
  }

  async function cargarAvisos() {
    const lista = $("#lista-avisos");
    try {
      const avisos = await api("notificaciones", `/api/notificaciones/usuario/${estado.usuario.idUsuario}`);
      avisos.sort((a, b) => String(b.fechaCreacion).localeCompare(String(a.fechaCreacion)));
      lista.innerHTML = avisos.length
        ? avisos.map((n) => {
            const cuando = n.fechaCreacion ? new Date(n.fechaCreacion).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" }) : "";
            return `<li class="aviso ${n.leida ? "" : "aviso--nueva"}">
              <i class="aviso__marca" aria-hidden="true"></i>
              <div><h4>${esc(n.asunto)}</h4><pre>${esc(n.mensaje)}</pre></div>
              <div class="aviso__lado">
                <span class="mono">${esc(cuando)}</span>
                <span class="estado estado--${n.estado}">${n.estado === "ENVIADA" ? "Correo enviado" : "Correo falló"}</span>
                ${n.leida ? `<span class="mono">Leída</span>` : `<button class="boton boton--mini" data-leer="${n.idNotificacion}">Marcar leída</button>`}
              </div>
            </li>`;
          }).join("")
        : `<li class="aviso vacio" style="grid-column: span 12">No tienes avisos todavía.</li>`;
    } catch (err) { fallo(err); lista.innerHTML = ""; }
    contarAvisos();
  }

  $("#lista-avisos").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-leer]");
    if (!b) return;
    try {
      await api("notificaciones", `/api/notificaciones/${b.dataset.leer}/leida`, { metodo: "PATCH" });
      cargarAvisos();
    } catch (err) { fallo(err); }
  });

  // ------------------------------------------------------------------
  // 05 · Administración
  // ------------------------------------------------------------------
  // ------------------------------------------------------------------
  // 05 · Mi cuenta
  // ------------------------------------------------------------------
  function llenarPerfil() {
    const u = estado.usuario, f = $("#form-perfil");
    f.nombre.value = u.nombre || "";
    f.correo.value = u.correo || "";
    $$("input[name=rol]", f).forEach((r) => { r.checked = r.value === u.rol; });
    const esAdmin = u.rol === "ADMIN";
    $("#perfil-roles").hidden = !esAdmin;
    $("#perfil-rol-fijo").hidden = esAdmin;
    $("#perfil-rol-fijo").textContent = `Tu rol es ${u.rol}. Solo un administrador puede cambiarlo.`;
  }

  async function cargarCuenta() {
    llenarPerfil();
    try {
      const u = await api("usuarios", `/api/usuarios/buscar/${estado.usuario.idUsuario}`);
      estado.usuario = { idUsuario: u.idUsuario, nombre: u.nombre, correo: u.correo, rol: u.rol };
      almacen.guardar("rl.usuario", estado.usuario);
      pintarUsuario();
      llenarPerfil();
    } catch (err) { fallo(err); }
  }

  $("#form-perfil").addEventListener("submit", async (e) => {
    e.preventDefault();
    const f = e.target;
    const rol = estado.usuario.rol === "ADMIN" ? ($("input[name=rol]:checked", f)?.value || "ADMIN") : estado.usuario.rol;
    if (estado.usuario.rol === "ADMIN" && rol !== "ADMIN"
        && !confirm("Si dejas de ser ADMIN perderás el acceso al panel de administración. ¿Continuar?")) return;
    try {
      const u = await api("usuarios", `/api/usuarios/actualizar/${estado.usuario.idUsuario}`, {
        metodo: "PUT",
        cuerpo: { nombre: f.nombre.value, correo: f.correo.value, rol },
      });
      estado.usuario = { idUsuario: u.idUsuario, nombre: u.nombre, correo: u.correo, rol: u.rol };
      almacen.guardar("rl.usuario", estado.usuario);
      pintarUsuario();
      llenarPerfil();
      toast("Datos actualizados");
    } catch (err) { fallo(err); }
  });

  $("#form-clave").addEventListener("submit", async (e) => {
    e.preventDefault();
    const f = e.target;
    if (f.claveNueva.value !== f.confirmacion.value) {
      toast("La clave nueva y su confirmación no coinciden", "error");
      f.confirmacion.focus();
      return;
    }
    try {
      const r = await api("usuarios", `/api/usuarios/${estado.usuario.idUsuario}/clave`, {
        metodo: "PATCH",
        cuerpo: { claveActual: f.claveActual.value, claveNueva: f.claveNueva.value },
      });
      toast(r.mensaje || "Clave actualizada");
      f.reset();
    } catch (err) { fallo(err); }
  });

  // ------------------------------------------------------------------
  // 06 · Administración
  // ------------------------------------------------------------------
  async function cargarAdmin() {
    cargarUsuariosAdmin();
    await cargarLaboratorios();
    pintarLabsAdmin();
    cargarEstadisticas();
  }

  // ---- usuarios ----
  estado.usuariosAdmin = [];
  estado.filtroUsuarios = "";

  async function cargarUsuariosAdmin() {
    try {
      estado.usuariosAdmin = await api("usuarios", "/api/usuarios/lista");
    } catch (err) { fallo(err); estado.usuariosAdmin = []; }
    pintarUsuariosAdmin();
  }

  function pintarUsuariosAdmin() {
    const texto = $("#buscar-usuario").value.trim().toLowerCase();
    const visibles = estado.usuariosAdmin
      .filter((u) => !texto || `${u.nombre} ${u.correo}`.toLowerCase().includes(texto))
      .filter((u) => !estado.filtroUsuarios || (estado.filtroUsuarios === "activos") === Boolean(u.activo))
      .sort((a, b) => a.idUsuario - b.idUsuario);
    $("#admin-usuarios").innerHTML = visibles.length
      ? visibles.map((u) => {
          const yo = u.idUsuario === estado.usuario.idUsuario;
          const est = u.activo ? "ACTIVO" : "INACTIVO";
          const acciones = yo
            ? `<span class="mono">Tú</span>`
            : `<button class="boton boton--mini" data-usuario="${u.idUsuario}" data-accion-usuario="${u.activo ? "desactivar" : "activar"}">${u.activo ? "Desactivar" : "Activar"}</button>
               <button class="boton boton--mini boton--peligro" data-usuario="${u.idUsuario}" data-accion-usuario="eliminar" aria-label="Eliminar ${esc(u.nombre)}">✕</button>`;
          return `<li class="${yo ? "es-yo" : ""}">
            <div><h4>${esc(u.nombre)}</h4><p class="mono" style="text-transform:none">${esc(u.correo)} · #${u.idUsuario}</p></div>
            <span class="rol">${esc(u.rol)}</span>
            <span class="estado estado--${est}">${est}</span>
            <div class="acciones" style="display:flex;gap:8px">${acciones}</div>
          </li>`;
        }).join("")
      : `<li class="vacio">${estado.usuariosAdmin.length ? "Ningún usuario coincide con la búsqueda." : "No hay usuarios registrados."}</li>`;
  }

  $("#buscar-usuario").addEventListener("input", pintarUsuariosAdmin);

  $("#filtro-usuarios").addEventListener("click", (e) => {
    const b = e.target.closest("[data-filtro]");
    if (!b) return;
    estado.filtroUsuarios = b.dataset.filtro;
    $$("#filtro-usuarios .chip-btn").forEach((x) => x.classList.toggle("activo", x === b));
    pintarUsuariosAdmin();
  });

  $("#admin-usuarios").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-accion-usuario]");
    if (!b) return;
    const id = b.dataset.usuario, accion = b.dataset.accionUsuario;
    const u = estado.usuariosAdmin.find((x) => String(x.idUsuario) === id);
    try {
      if (accion === "eliminar") {
        if (!confirm(`¿Eliminar a ${u?.nombre || "este usuario"}? No se puede deshacer.`)) return;
        await api("usuarios", `/api/usuarios/eliminar/${id}`, { metodo: "DELETE" });
        toast("Usuario eliminado");
      } else {
        if (accion === "desactivar" && !confirm(`¿Desactivar a ${u?.nombre || "este usuario"}? No podrá iniciar sesión ni reservar.`)) return;
        await api("usuarios", `/api/usuarios/${id}/${accion}`, { metodo: "PATCH" });
        toast(accion === "activar" ? "Cuenta activada" : "Cuenta desactivada");
      }
      cargarUsuariosAdmin();
      cargarEstadisticas();
    } catch (err) { fallo(err); }
  });

  function pintarLabsAdmin() {
    const lista = $("#admin-labs");
    lista.innerHTML = estado.laboratorios.length
      ? estado.laboratorios.map((l) => `<li>
          <div><h4>${esc(l.nombre)}</h4><p class="mono">${esc(l.ubicacion)} · #${l.idLaboratorio}</p></div>
          <div class="cap">${l.capacidad}<small>cupo</small></div>
          <span class="estado estado--${l.estado}">${l.estado}</span>
          <div class="acciones" style="display:flex;gap:8px">
            <button class="boton boton--mini" data-estado-lab="${l.idLaboratorio}" data-nuevo="${l.estado === "DISPONIBLE" ? "MANTENIMIENTO" : "DISPONIBLE"}">
              ${l.estado === "DISPONIBLE" ? "A mantenimiento" : "Habilitar"}</button>
            <button class="boton boton--mini boton--peligro" data-eliminar-lab="${l.idLaboratorio}" aria-label="Eliminar ${esc(l.nombre)}">✕</button>
          </div>
        </li>`).join("")
      : `<li class="vacio">Sin laboratorios. Agrega el primero abajo.</li>`;
  }

  $("#admin-labs").addEventListener("click", async (e) => {
    const cambio = e.target.closest("[data-estado-lab]");
    const borrar = e.target.closest("[data-eliminar-lab]");
    try {
      if (cambio) {
        await api("reservas", `/api/laboratorios/${cambio.dataset.estadoLab}/estado`, { metodo: "PATCH", cuerpo: { estado: cambio.dataset.nuevo } });
        toast("Estado del laboratorio actualizado");
      } else if (borrar) {
        if (!confirm("¿Eliminar este laboratorio? No se puede deshacer.")) return;
        await api("reservas", `/api/laboratorios/eliminar/${borrar.dataset.eliminarLab}`, { metodo: "DELETE" });
        toast("Laboratorio eliminado");
      } else return;
      estado.agenda = null;
      cargarAdmin();
    } catch (err) { fallo(err); }
  });

  $("#form-lab").addEventListener("submit", async (e) => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.target));
    try {
      const l = await api("reservas", "/api/laboratorios/crear", { metodo: "POST", cuerpo: { ...f, capacidad: Number(f.capacidad) } });
      toast(`Laboratorio "${l.nombre}" creado`);
      e.target.reset();
      cargarAdmin();
    } catch (err) { fallo(err); }
  });

  $("#form-gestion").addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = new FormData(e.target).get("idReserva");
    const caja = $("#gestion-resultado");
    try {
      const r = await api("reservas", `/api/reservas/buscar/${id}`);
      caja.innerHTML = `<ol class="registro">${filaReserva(r, { admin: true })}</ol>`;
    } catch (err) { fallo(err); caja.innerHTML = ""; }
  });

  $("#gestion-resultado").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-accion]");
    if (!b) return;
    const r = await cambiarReserva(b.dataset.id, b.dataset.accion);
    if (r) $("#gestion-resultado").innerHTML = `<ol class="registro">${filaReserva(r, { admin: true })}</ol>`;
  });

  async function cargarEstadisticas() {
    const [usuarios, avisos, fallidas] = await Promise.allSettled([
      api("usuarios", "/api/usuarios/estadisticas"),
      api("notificaciones", "/api/notificaciones/estadisticas"),
      api("notificaciones", "/api/notificaciones/fallidas"),
    ]);

    const desglose = (obj) => Object.entries(obj || {}).map(([k, v]) => `<li><span>${esc(k)}</span><b>${v}</b></li>`).join("");

    $("#stats-usuarios").innerHTML = usuarios.status === "fulfilled"
      ? `<span class="mono">Usuarios registrados</span><div class="cifra">${usuarios.value.total}</div>
         <ul class="desglose"><li><span>Activos</span><b>${usuarios.value.activos}</b></li><li><span>Inactivos</span><b>${usuarios.value.inactivos}</b></li>${desglose(usuarios.value.porRol)}</ul>`
      : `<span class="mono">Usuarios</span><p>${esc(usuarios.reason.message)}</p>`;

    $("#stats-avisos").innerHTML = avisos.status === "fulfilled"
      ? `<span class="mono">Correos enviados</span><div class="cifra">${avisos.value.enviadas}<small style="font-size:24px">/${avisos.value.total}</small></div>
         <ul class="desglose"><li><span>Fallidos</span><b>${avisos.value.fallidas}</b></li><li><span>Sin leer</span><b>${avisos.value.noLeidas}</b></li>${desglose(avisos.value.porTipo)}</ul>`
      : `<span class="mono">Correos</span><p>${esc(avisos.reason.message)}</p>`;

    const lista = fallidas.status === "fulfilled" ? fallidas.value : [];
    $("#avisos-fallidos").innerHTML = `<span class="mono">Correos fallidos</span>` + (lista.length
      ? lista.map((n) => `<div class="fallida"><span>#${n.idNotificacion} · ${esc(n.destinatario)}<br><span class="mono">${n.intentos}/3 intentos</span></span>
          ${n.intentos < 3 ? `<button class="boton boton--mini" data-reintentar="${n.idNotificacion}">Reintentar</button>` : `<span class="mono">Agotado</span>`}</div>`).join("")
      : `<p style="margin:10px 0 0"><em>Ninguno. Todo salió bien.</em></p>`);
  }

  $("#avisos-fallidos").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-reintentar]");
    if (!b) return;
    try {
      const n = await api("notificaciones", `/api/notificaciones/${b.dataset.reintentar}/reintentar`, { metodo: "POST" });
      toast(n.estado === "ENVIADA" ? "Correo reenviado" : "Volvió a fallar", n.estado === "ENVIADA" ? "ok" : "error");
      cargarEstadisticas();
    } catch (err) { fallo(err); }
  });

  // ------------------------------------------------------------------
  // Inicio
  // ------------------------------------------------------------------
  pintarEntorno();
  if (estado.usuario) mostrarApp(); else mostrarAcceso();
})();
