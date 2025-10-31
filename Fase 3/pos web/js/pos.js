// js/pos.js
// POS compartido para ventas.html y cajero.html (usa window.SDK)

export function initPOS({ selectors, user, goToTicket = 'ticket.html' }){
  // ----- Utils -----
  const $ = (sel) => document.querySelector(sel);
  const fmt = v => `$${Number(v||0).toLocaleString('es-CL')}`;

  // ----- DOM -----
  const qInput   = $(selectors.q);
  const listEl   = $(selectors.list);
  const cartEl   = $(selectors.cart);
  const subEl    = $(selectors.sub);
  const ivaEl    = $(selectors.iva);
  const totalEl  = $(selectors.total);
  const chargeBtn = document.querySelector(selectors.chargeBtn) || $(selectors.chargeBtn);

  if(!qInput || !listEl || !cartEl || !subEl || !ivaEl || !totalEl){
    console.error('initPOS: faltan selectores/elementos en el HTML', selectors);
    return;
  }

  // ----- Estado -----
  let catalogo = []; // [{id, nombre, precio, ...}]
  let carrito  = []; // [{id, nombre, precio, qty}]

  // ----- Cargar productos desde SDK -----
  async function cargarCatalogo(){
    try{
      catalogo = await window.SDK.Products.list();
      renderLista(catalogo);
    }catch(err){
      console.error('Error al listar productos:', err);
      listEl.innerHTML = `<div class="text-danger small p-2">No se pudo cargar el catálogo.</div>`;
    }
  }

  // ----- Render de lista y búsqueda -----
  function renderLista(rows){
    listEl.innerHTML = '';
    if(!rows.length){
      listEl.innerHTML = `<div class="text-secondary p-2">Sin resultados…</div>`;
      return;
    }
    rows.forEach(p=>{
      const item = document.createElement('div');
      item.className = 'list-group-item d-flex justify-content-between align-items-center';
      item.innerHTML = `
        <div>
          <strong>${p.nombre}</strong>
          <div class="small text-secondary">${p.id}</div>
        </div>
        <div class="text-end">
          <div class="fw-bold">${fmt(p.precio)}</div>
          <button class="btn btn-sm btn-info mt-1">Agregar</button>
        </div>`;
      item.querySelector('button').onclick = () => addItem(p);
      listEl.appendChild(item);
    });
  }

  function filtrar(){
    const q = (qInput.value || '').toLowerCase();
    const res = catalogo.filter(p =>
      (p.nombre||'').toLowerCase().includes(q) ||
      (p.id||'').toLowerCase().includes(q)
    );
    renderLista(res);
  }

  // ----- Carrito -----
  function addItem(p){
    const ex = carrito.find(i => i.id === p.id);
    if (ex){ ex.qty += 1; }
    else { carrito.push({ id:p.id, nombre:p.nombre, precio:Number(p.precio)||0, qty:1 }); }
    renderCarrito();
  }
  function inc(id){ const it = carrito.find(i=>i.id===id); if(it){ it.qty++; renderCarrito(); } }
  function dec(id){
    const it = carrito.find(i=>i.id===id);
    if(it){ it.qty = Math.max(1, it.qty-1); renderCarrito(); }
  }
  function delItem(id){ carrito = carrito.filter(i=>i.id!==id); renderCarrito(); }

  function renderCarrito(){
    if(carrito.length === 0){
      cartEl.innerHTML = '<div class="text-secondary">Agrega productos…</div>';
    }else{
      cartEl.innerHTML = carrito.map(i=>`
        <div class="d-flex justify-content-between align-items-center py-1">
          <div class="me-2">${i.nombre} <span class="text-secondary">x${i.qty}</span></div>
          <div>
            <button class="btn btn-sm btn-outline-light" data-act="dec" data-id="${i.id}">-</button>
            <button class="btn btn-sm btn-outline-light" data-act="inc" data-id="${i.id}">+</button>
            <button class="btn btn-sm btn-outline-danger" data-act="del" data-id="${i.id}">x</button>
          </div>
        </div>
      `).join('');
      // Delegación:
      cartEl.querySelectorAll('button').forEach(b=>{
        const id  = b.getAttribute('data-id');
        const act = b.getAttribute('data-act');
        b.onclick = () => {
          if(act==='inc') inc(id);
          else if(act==='dec') dec(id);
          else if(act==='del') delItem(id);
        };
      });
    }
    calcularTotales();
  }

  function calcularTotales(){
    const sub = carrito.reduce((a,i)=> a + i.precio*i.qty, 0);
    const iva = Math.round(sub * 0.19);
    const tot = sub + iva;
    subEl.textContent   = fmt(sub);
    ivaEl.textContent   = fmt(iva);
    totalEl.textContent = fmt(tot);
  }

  // ----- Cobrar -----
  async function cobrar(){
    if(carrito.length === 0){ alert('Carrito vacío'); return; }
    try{
      const sale = await window.SDK.Sales.create({
        user: user?.u || 'desconocido',
        items: carrito,
        method: 'Efectivo'
      });
      // guardar id para ticket
      sessionStorage.setItem('pos_last_sale', sale.id);
      // limpiar carrito y redirigir
      carrito = [];
      renderCarrito();
      location.href = goToTicket;
    }catch(err){
      console.error('Error al cobrar:', err);
      alert('No se pudo registrar la venta.');
    }
  }

  // ----- Wire events -----
  qInput.addEventListener('input', filtrar);
  qInput.addEventListener('keydown', (e)=>{ if(e.key==='Enter') filtrar(); });
  if(chargeBtn) chargeBtn.addEventListener('click', cobrar);

  // init
  cargarCatalogo();
  renderCarrito();
}
