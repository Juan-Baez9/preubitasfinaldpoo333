const state = {
  dataLoaded: false,
  usuarios: null,
  tiquetes: [],
  eventos: [],
  cliente: null,
};

const loginForm = document.querySelector('#login-form');
const loginError = document.querySelector('#login-error');
const ticketSection = document.querySelector('#ticket-section');
const ticketTitle = document.querySelector('#ticket-title');
const ticketGrid = document.querySelector('#ticket-grid');
const ticketStats = document.querySelector('#ticket-stats');
const ticketTemplate = document.querySelector('#ticket-card-template');

async function loadData() {
  if (state.dataLoaded) return;
  const [usuarios, tiquetes, eventos] = await Promise.all([
    fetch('../data/usuarios.json').then((r) => r.json()),
    fetch('../data/tiquetes.json').then((r) => r.json()),
    fetch('../data/eventos.json').then((r) => r.json()),
  ]);
  state.usuarios = usuarios;
  state.tiquetes = tiquetes;
  state.eventos = eventos;
  state.dataLoaded = true;
}

function formatCurrency(value) {
  return value.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
}

function getEventImage(evento) {
  const stored = localStorage.getItem(`event-image-${evento.idEvento}`);
  return stored || evento.imagenUrl || 'https://images.unsplash.com/photo-1521336575822-6da63fb45455?auto=format&fit=crop&w=1200&q=80';
}

function buildDetailsList(wrapper, items) {
  wrapper.innerHTML = '';
  items.forEach(({ label, value }) => {
    const dt = document.createElement('dt');
    dt.textContent = label;
    const dd = document.createElement('dd');
    dd.textContent = value;
    wrapper.append(dt, dd);
  });
}

function renderStats(tiquetesCliente) {
  ticketStats.innerHTML = '';
  const total = tiquetesCliente.length;
  const totalPrecio = tiquetesCliente.reduce((sum, t) => sum + t.precio + t.cargoServicio + t.cargoEmision, 0);
  const chipTotal = document.createElement('span');
  chipTotal.className = 'chip';
  chipTotal.textContent = `${total} tiquete${total === 1 ? '' : 's'}`;

  const chipValor = document.createElement('span');
  chipValor.className = 'chip';
  chipValor.textContent = `Valor total ${formatCurrency(totalPrecio)}`;

  ticketStats.append(chipTotal, chipValor);
}

function renderTicket(ticket, evento) {
  const clone = ticketTemplate.content.cloneNode(true);
  const eyebrow = clone.querySelector('.ticket-card__eyebrow');
  const title = clone.querySelector('.ticket-card__title');
  const meta = clone.querySelector('.ticket-card__meta');
  const price = clone.querySelector('.ticket-card__price');
  const tag = clone.querySelector('.ticket-card__tag');
  const img = clone.querySelector('.ticket-card__image');
  const details = clone.querySelector('.ticket-card__details');
  const qrWrapper = clone.querySelector('.qr');
  const imageForm = clone.querySelector('.image-form');
  const imageInput = imageForm.querySelector('input[name="image"]');

  eyebrow.textContent = `${evento.tipoEvento} • ${evento.idEvento}`;
  title.textContent = evento.nombre;
  meta.textContent = `${evento.fecha} · ${evento.venue.nombre} (${evento.venue.ubicacion})`;
  price.textContent = `${formatCurrency(ticket.precio)} + cargos`;
  tag.textContent = `Tiquete #${ticket.idTiquete}`;
  img.src = getEventImage(evento);
  img.alt = `Imagen del evento ${evento.nombre}`;

  buildDetailsList(details, [
    { label: 'Localidad', value: ticket.idLocalidad.split('::')[1] || ticket.idLocalidad },
    { label: 'Estado', value: ticket.estado },
    { label: 'Asiento', value: ticket.numeroAsiento ?? 'No numerado' },
    { label: 'Cargo servicio', value: formatCurrency(ticket.cargoServicio) },
    { label: 'Cargo emisión', value: formatCurrency(ticket.cargoEmision) },
    { label: 'Propietario', value: state.cliente?.nombre || ticket.propietarioLogin },
  ]);

  new QRCode(qrWrapper, {
    text: JSON.stringify({
      id: ticket.idTiquete,
      evento: evento.nombre,
      fecha: evento.fecha,
      localidad: ticket.idLocalidad,
      cliente: state.cliente?.login,
    }),
    width: 120,
    height: 120,
    colorDark: '#0f2542',
    colorLight: '#ffffff',
  });

  imageInput.value = localStorage.getItem(`event-image-${evento.idEvento}`) || '';
  imageForm.addEventListener('submit', (event) => {
    event.preventDefault();
    const url = imageInput.value.trim();
    if (!url) return;
    localStorage.setItem(`event-image-${evento.idEvento}`, url);
    img.src = url;
  });

  ticketGrid.appendChild(clone);
}

async function handleLogin(event) {
  event.preventDefault();
  loginError.textContent = '';
  await loadData();

  const login = (document.querySelector('#login').value || '').trim();
  const password = (document.querySelector('#password').value || '').trim();

  const cliente = state.usuarios.clientes.find((c) => c.login === login && c.password === password);
  if (!cliente) {
    loginError.textContent = 'Login o contraseña incorrectos.';
    ticketSection.classList.add('hidden');
    return;
  }

  state.cliente = cliente;
  ticketTitle.textContent = `Hola, ${cliente.nombre}`;

  const tiquetesCliente = state.tiquetes.filter((t) => cliente.tiquetes.includes(t.idTiquete));
  renderStats(tiquetesCliente);

  ticketGrid.innerHTML = '';
  tiquetesCliente.forEach((t) => {
    const evento = state.eventos.find((e) => e.idEvento === t.eventoId);
    if (evento) renderTicket(t, evento);
  });

  ticketSection.classList.remove('hidden');
  ticketSection.scrollIntoView({ behavior: 'smooth' });
}

loginForm.addEventListener('submit', handleLogin);
