// js/pos.js
// POS compartido para ventas.html y cajero.html (usa window.SDK)
// Versión con flujo de pago en pago.html

// js/pos.js
// POS compartido para ventas.html y cajero.html (usa window.SDK)
// Flujo: carrito -> pago.html -> ticket.html

export function initPOS({ selectors, user }) {
  // Utilidades
  const $ = (sel) => document.querySelector(sel);
  const CLP = v => '$' + Number(v || 0).toLocaleString('es-CL');

  // Referencias DOM
  const qInput    = $(selectors.q);
  const listEl    = $(selectors.list);
  const cartEl    = $(selectors.cart);
  const subEl     = $(selectors.sub);
  const ivaEl     = $(selectors.iva);
  const totalEl   = $(selectors.total);
  const chargeBtn = $(selectors.chargeBtn);

  if (!qInput || !listEl || !cartEl || !subEl || !ivaEl || !totalEl || !chargeBtn) {
    console.error('initPOS: faltan selectores/elementos en el HTML', selectors);
    return;
  }

  // Estado
  let catalogo = []; // [{id, nombre, precio, stock,...}]
  let carrito  = []; // [{id, nombre, precio, qty}]

  // =========================
  //   Carga catálogo
  // =========================
  async function cargarCatalogo() {
    try {
      if (window.SDK?.Products?.list) {
        catalogo = await window.SDK.Products.list();
      } else {
        // Fallback local
        const raw = localStorage.getItem('pos_products') || '[]';
        catalogo = JSON.parse(raw);
      }
      renderLista(catalogo);
    } catch (err) {
      console.error('Error al listar productos:', err);
      listEl.innerHTML = `<div class="text-danger small p-2">No se pudo cargar el catálogo.</div>`;
    }
  }

  // =========================
  //   Render lista productos
  // =========================
  function renderLista(rows) {
    listEl.innerHTML = '';
    if (!rows.length) {
      listEl.innerHTML = `<div class="text-secondary p-2">Sin resultados…</div>`;
      return;
    }

    rows.forEach(p => {
      const stockNum = Number(p.stock || 0);
      const precioNum = Number(p.precio || p.price || 0);

      const item = document.createElement('div');
      item.className = 'list-group-item d-flex justify-content-between align-items-center bg-transparent text-light border-secondary';
      item.innerHTML = `
        <div>
          <strong>${p.nombre || p.name || '(sin nombre)'}</strong>
          <div class="small text-secondary">${p.id} · Stock ${stockNum}</div>
        </div>
        <div class="text-end">
          <div class="fw-bold">${CLP(precioNum)}</div>
          <button class="btn btn-sm btn-info mt-1">Agregar</button>
        </div>
      `;

      item.querySelector('button').onclick = () => addItem(p);
      listEl.appendChild(item);
    });
  }

  function filtrar() {
    const q = (qInput.value || '').toLowerCase();
    const res = catalogo.filter(p =>
      (p.nombre || p.name || '').toLowerCase().includes(q) ||
      String(p.id || '').toLowerCase().includes(q)
    );
    renderLista(res);
  }

  // =========================
  //   Carrito
  // =========================
  function addItem(p) {
    const id = p.id;
    const precioNum = Number(p.precio || p.price || 0);

    const existe = carrito.find(i => i.id === id);
    if (existe) {
      existe.qty += 1;
    } else {
      carrito.push({
        id,
        nombre: p.nombre || p.name || '',
        precio: precioNum,
        qty: 1
      });
    }
    renderCarrito();
  }

  function inc(id) {
    const it = carrito.find(i => i.id === id);
    if (it) {
      it.qty += 1;
      renderCarrito();
    }
  }

  function dec(id) {
    const it = carrito.find(i => i.id === id);
    if (it) {
      it.qty -= 1;
      if (it.qty <= 0) {
        carrito = carrito.filter(x => x.id !== id);
      }
      renderCarrito();
    }
  }

  function delItem(id) {
    carrito = carrito.filter(i => i.id !== id);
    renderCarrito();
  }

  function calcTotales() {
    const subtotal = carrito.reduce((a, i) => a + (i.precio * i.qty), 0);
    const iva      = Math.round(subtotal * 0.19);
    const total    = subtotal + iva;
    return { subtotal, iva, total };
  }

  function renderCarrito() {
    if (carrito.length === 0) {
      cartEl.innerHTML = '<div class="text-secondary">Agrega productos…</div>';
    } else {
      cartEl.innerHTML = carrito.map(i => `
        <div class="d-flex justify-content-between align-items-start py-1 border-bottom border-secondary">
          <div class="me-2 small">
            <div class="fw-semibold">${i.nombre}</div>
            <div class="text-secondary">x${i.qty} · ${CLP(i.precio)}</div>
            <div class="d-flex gap-1 mt-1">
              <button class="btn btn-sm btn-outline-light" data-act="dec" data-id="${i.id}">-</button>
              <button class="btn btn-sm btn-outline-light" data-act="inc" data-id="${i.id}">+</button>
              <button class="btn btn-sm btn-outline-danger" data-act="del" data-id="${i.id}">x</button>
            </div>
          </div>
          <div class="text-end small fw-bold">${CLP(i.precio * i.qty)}</div>
        </div>
      `).join('');

      // Eventos +/-/x
      cartEl.querySelectorAll('button').forEach(b => {
        const id  = b.getAttribute('data-id');
        const act = b.getAttribute('data-act');
        b.onclick = () => {
          if (act === 'inc') inc(id);
          else if (act === 'dec') dec(id);
          else if (act === 'del') delItem(id);
        };
      });
    }

    const { subtotal, iva, total } = calcTotales();
    subEl.textContent   = CLP(subtotal);
    ivaEl.textContent   = CLP(iva);
    totalEl.textContent = CLP(total);
  }

  // =========================
  //   Cobrar -> pago.html
  // =========================
  function cobrar() {
    if (carrito.length === 0) {
      alert('Carrito vacío');
      return;
    }

    const { subtotal, iva, total } = calcTotales();

    // Info que necesita la pantalla de pago
    const checkoutPayload = {
      items: carrito.map(i => ({
        id: i.id,
        nombre: i.nombre,
        precio: i.precio,
        qty: i.qty
      })),
      subtotal,
      iva,
      total,
      ts: Date.now(),
      user: user?.u || 'desconocido'
    };

    // Guardar en sesión
    sessionStorage.setItem('pos_checkout', JSON.stringify(checkoutPayload));

    // Ir a página de pago
    window.location.href = 'pago.html';
  }

  // Eventos
  qInput.addEventListener('input', filtrar);
  qInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') filtrar();
  });

  chargeBtn.addEventListener('click', cobrar);

  // Init
  cargarCatalogo();
  renderCarrito();
}
