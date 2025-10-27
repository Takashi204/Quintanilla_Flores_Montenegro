// js/proveedores.js
import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['ADMIN']);
if (u) {
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('proveedores');

    const KEY = 'pos_suppliers';
    const seed = [
      { id:'PR-0001', nombre:'Proveedor Demo SPA', contacto:'Ana Soto', mail:'contacto@demo.cl', tel:'+56 9 1234 5678', addr:'Santiago' }
    ];

    const db = {
      _list: [],
      load(){
        try {
          const raw = localStorage.getItem(KEY);
          this._list = raw ? JSON.parse(raw) : seed;
          this.save();
        } catch {
          this._list = seed; this.save();
        }
      },
      save(){ localStorage.setItem(KEY, JSON.stringify(this._list)); },
      all(){ return [...this._list]; },
      upsert(s){
        const i = this._list.findIndex(x => x.id === s.id);
        (i >= 0) ? (this._list[i] = s) : this._list.push(s);
        this.save();
      },
      remove(id){ this._list = this._list.filter(x => x.id !== id); this.save(); }
    };
    db.load();

    // ---- DOM
    const body = document.getElementById('supBody');
    const search = document.getElementById('search');
    const btnNew = document.getElementById('btnNew');
    const btnExport = document.getElementById('btnExport');

    const modalBackdrop = document.getElementById('modalBackdrop');
    const modalTitle = document.getElementById('modalTitle');
    const modalClose = document.getElementById('modalClose');
    const btnSave = document.getElementById('btnSave');

    const f_id = document.getElementById('f_id');
    const f_nombre = document.getElementById('f_nombre');
    const f_contacto = document.getElementById('f_contacto');
    const f_mail = document.getElementById('f_mail');
    const f_tel = document.getElementById('f_tel');
    const f_addr = document.getElementById('f_addr');
    const formError = document.getElementById('formError');

    let mode = 'new', editing = null;

    function renderTable(){
      const q = (search.value||'').toLowerCase();
      const rows = db.all()
        .sort((a,b)=> (a.nombre||'').localeCompare(b.nombre||'', 'es'))
        .filter(s =>
          (s.id||'').toLowerCase().includes(q) ||
          (s.nombre||'').toLowerCase().includes(q) ||
          (s.mail||'').toLowerCase().includes(q) ||
          (s.contacto||'').toLowerCase().includes(q)
        );

      body.innerHTML = '';
      rows.forEach(s => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${s.id}</td>
          <td>${s.nombre||''}</td>
          <td>${s.contacto||''}</td>
          <td>${s.mail||''}</td>
          <td>${s.tel||''}</td>
          <td>${s.addr||''}</td>
          <td class="actions">
            <button data-id="${s.id}" class="btn-secondary btn-edit">Editar</button>
            <button data-id="${s.id}" class="btn-danger btn-del">Eliminar</button>
          </td>
        `;
        body.appendChild(tr);
      });
      attachHandlers();
    }

    function attachHandlers(){
      body.querySelectorAll('.btn-edit').forEach(b => b.addEventListener('click', ()=>{
        const s = db.all().find(x=>x.id===b.dataset.id);
        openModal('edit', s);
      }));
      body.querySelectorAll('.btn-del').forEach(b => b.addEventListener('click', ()=>{
        const id = b.dataset.id;
        if(confirm(`¿Eliminar proveedor ${id}?`)){ db.remove(id); renderTable(); }
      }));
    }

    function openModal(m, s=null){
      mode = m; editing = s; formError.classList.add('d-none');
      modalTitle.textContent = (m==='new')? 'Nuevo proveedor' : `Editar proveedor: ${s.id}`;
      if(m==='new'){
        f_id.disabled = false; f_id.value='';
        f_nombre.value=''; f_contacto.value=''; f_mail.value=''; f_tel.value=''; f_addr.value='';
      }else{
        f_id.disabled = true; f_id.value=s.id;
        f_nombre.value=s.nombre||''; f_contacto.value=s.contacto||'';
        f_mail.value=s.mail||''; f_tel.value=s.tel||''; f_addr.value=s.addr||'';
      }
      modalBackdrop.style.display='flex';
      f_nombre.focus();
    }
    function closeModal(){ modalBackdrop.style.display='none'; }

    function validate(){
      const id = f_id.value.trim();
      const nombre = f_nombre.value.trim();
      if(!id || !nombre) return 'RUT/ID y Nombre son obligatorios.';
      if(mode==='new' && db.all().some(x=>x.id===id)) return 'Ya existe un proveedor con ese RUT/ID.';
      if(f_mail.value && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f_mail.value)) return 'Correo no válido.';
      return '';
    }

    btnNew.addEventListener('click', ()=> openModal('new'));
    modalClose.addEventListener('click', closeModal);
    modalBackdrop.addEventListener('click', (e)=>{ if(e.target===modalBackdrop) closeModal(); });

    btnSave.addEventListener('click', ()=>{
      const err = validate();
      if(err){ formError.textContent = err; formError.classList.remove('d-none'); return; }
      const s = {
        id: f_id.value.trim(),
        nombre: f_nombre.value.trim(),
        contacto: f_contacto.value.trim(),
        mail: f_mail.value.trim(),
        tel: f_tel.value.trim(),
        addr: f_addr.value.trim()
      };
      db.upsert(s);
      closeModal();
      renderTable();
    });

    btnExport.addEventListener('click', ()=>{
      const rows = db.all();
      const csv = [
        ['id','nombre','contacto','mail','tel','addr'].join(','),
        ...rows.map(s => [s.id, JSON.stringify(s.nombre||''), JSON.stringify(s.contacto||''), JSON.stringify(s.mail||''), JSON.stringify(s.tel||''), JSON.stringify(s.addr||'')].join(','))
      ].join('\n');
      const blob = new Blob([csv], {type:'text/csv;charset=utf-8;'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = 'proveedores.csv';
      document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url);
    });

    search.addEventListener('input', renderTable);
    renderTable();
  });
}
