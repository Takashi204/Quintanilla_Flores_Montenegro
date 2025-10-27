// js/inventario.js
import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['ADMIN']);
if(u){
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('inventario');

    const KEY = 'pos_products';
    const seed = [
      { id:'P-0001', nombre:'Producto Demo', stock:10, precio:1000, vence:'2025-12-31' },
    ];

    const db = {
      _list: [],
      load(){ try{
        const raw = localStorage.getItem(KEY);
        this._list = raw ? JSON.parse(raw) : seed;
        this.save();
      }catch{ this._list = seed; this.save(); } },
      save(){ localStorage.setItem(KEY, JSON.stringify(this._list)); },
      all(){ return [...this._list]; },
      upsert(p){ const i=this._list.findIndex(x=>x.id===p.id); if(i>=0) this._list[i]=p; else this._list.push(p); this.save(); },
      remove(id){ this._list = this._list.filter(x=>x.id!==id); this.save(); }
    };
    db.load();

    const tbody = document.querySelector('tbody');
    const search = document.getElementById('search');
    const modalBackdrop = document.getElementById('modalBackdrop');
    const modalTitle = document.getElementById('modalTitle');
    const modalClose = document.getElementById('modalClose');
    const btnNew = document.getElementById('btnNew');
    const btnSave = document.getElementById('btnSave');
    const formError = document.getElementById('formError');
    const f_id = document.getElementById('f_id');
    const f_nombre = document.getElementById('f_nombre');
    const f_stock = document.getElementById('f_stock');
    const f_precio = document.getElementById('f_precio');
    const f_vence = document.getElementById('f_vence');

    let mode='new', editing=null;

    function renderTable(){
      const q=(search.value||'').toLowerCase();
      const rows = db.all()
        .sort((a,b)=> a.nombre.localeCompare(b.nombre, 'es'))
        .filter(p => p.id.toLowerCase().includes(q) || p.nombre.toLowerCase().includes(q));
      tbody.innerHTML='';
      rows.forEach(p=>{
        const tr=document.createElement('tr');
        tr.innerHTML = `
          <td>${p.id}</td>
          <td>${p.nombre}</td>
          <td>${Number(p.stock)||0}</td>
          <td>$${(Number(p.precio)||0).toLocaleString('es-CL')}</td>
          <td>${p.vence||''}</td>
          <td class="actions">
            <button data-id="${p.id}" class="btn-secondary btn-edit">Editar</button>
            <button data-id="${p.id}" class="btn-danger btn-del">Eliminar</button>
          </td>`;
        tbody.appendChild(tr);
      });
      attachHandlers();
    }

    function attachHandlers(){
      tbody.querySelectorAll('.btn-edit').forEach(b=> b.addEventListener('click', ()=>{
        const p = db.all().find(x=>x.id===b.dataset.id);
        openModal('edit', p);
      }));
      tbody.querySelectorAll('.btn-del').forEach(b=> b.addEventListener('click', ()=>{
        const id = b.dataset.id;
        if(confirm(`¿Eliminar producto ${id}?`)){ db.remove(id); renderTable(); }
      }));
    }

    function openModal(m, p=null){
      mode=m; editing=p; formError.classList.add('d-none');
      modalTitle.textContent = (m==='new')? 'Nuevo producto' : `Editar producto: ${p.id}`;
      if(m==='new'){
        f_id.disabled=false; f_id.value='';
        f_nombre.value=''; f_stock.value='0'; f_precio.value='0'; f_vence.value='';
      }else{
        f_id.disabled=true; f_id.value=p.id;
        f_nombre.value=p.nombre; f_stock.value=String(p.stock);
        f_precio.value=String(p.precio); f_vence.value=p.vence||'';
      }
      modalBackdrop.style.display='flex'; f_nombre.focus();
    }
    function closeModal(){ modalBackdrop.style.display='none'; }

    function validate(){
      const id=f_id.value.trim(), nombre=f_nombre.value.trim();
      const stock=Number(f_stock.value), precio=Number(f_precio.value);
      if(!id || !nombre) return 'Código y nombre son obligatorios.';
      if(stock < 0 || !Number.isFinite(stock)) return 'Stock debe ser un número ≥ 0.';
      if(precio < 0 || !Number.isFinite(precio)) return 'Precio debe ser un número ≥ 0.';
      if(mode==='new' && db.all().some(x=>x.id===id)) return 'Ya existe un producto con ese código.';
      if(f_vence.value){
        const d = new Date(f_vence.value);
        if(isNaN(d)) return 'Fecha de vencimiento no válida.';
      }
      return '';
    }

    btnNew.addEventListener('click', ()=> openModal('new'));
    modalClose.addEventListener('click', closeModal);
    modalBackdrop.addEventListener('click', (e)=>{ if(e.target===modalBackdrop) closeModal(); });
    btnSave.addEventListener('click', ()=>{
      const err = validate();
      if(err){ formError.textContent=err; formError.classList.remove('d-none'); return; }
      const p = {
        id: f_id.value.trim(),
        nombre: f_nombre.value.trim(),
        stock: Number(f_stock.value),
        precio: Number(f_precio.value),
        vence: f_vence.value || undefined
      };
      db.upsert(p);
      closeModal();
      renderTable();
    });

    document.getElementById('btnExport').addEventListener('click', ()=>{
      const rows = db.all();
      const csv = [
        ['id','nombre','stock','precio','vence'].join(','),
        ...rows.map(p => [p.id, JSON.stringify(p.nombre), p.stock, p.precio, p.vence||''].join(','))
      ].join('\n');
      const blob = new Blob([csv], {type:'text/csv;charset=utf-8;'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href=url; a.download='productos.csv';
      document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url);
    });

    search.addEventListener('input', renderTable);
    renderTable();
  });
}
