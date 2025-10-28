// js/proveedores.js
import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['ADMIN']);
if (u) {
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('proveedores');

    // ─── Data layer ───────────────────────────────────────────────
    const KEY = 'pos_suppliers';
    const seed = [{ id:'PR-0001', nombre:'Proveedor Demo SPA', contacto:'Ana Soto', mail:'contacto@demo.cl', tel:'+56 9 1234 5678', addr:'Santiago' }];

    const Local = {
      _list: [],
      load(){ try{ this._list = JSON.parse(localStorage.getItem(KEY)||'null') || seed; this.save(); }
              catch{ this._list = seed; this.save(); } },
      save(){ localStorage.setItem(KEY, JSON.stringify(this._list)); },
      all(){ return [...this._list]; },
      upsert(s){ const i=this._list.findIndex(x=>x.id===s.id); i>=0? this._list[i]=s : this._list.push(s); this.save(); },
      remove(id){ this._list = this._list.filter(x=>x.id!==id); this.save(); }
    };
    Local.load();

    const Suppliers = {
      async list(){
        if (window.SDK?.Suppliers?.list) {
          try{ return await window.SDK.Suppliers.list(); }catch(e){ console.warn(e); }
        }
        return Local.all();
      },
      async create(s){
        if (window.SDK?.Suppliers?.create) {
          try{ return await window.SDK.Suppliers.create(s); }catch(e){ console.warn(e); }
        }
        Local.upsert(s); return s;
      },
      async update(id, s){
        if (window.SDK?.Suppliers?.update) {
          try{ return await window.SDK.Suppliers.update(id, s); }catch(e){ console.warn(e); }
        }
        Local.upsert({ ...s, id }); return s;
      },
      async remove(id){
        if (window.SDK?.Suppliers?.remove) {
          try{ return await window.SDK.Suppliers.remove(id); }catch(e){ console.warn(e); }
        }
        Local.remove(id); return true;
      }
    };

    // ─── DOM ──────────────────────────────────────────────────────
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

    let mode = 'new', editing = null, cache=[];

    async function load(){
      cache = await Suppliers.list();
      renderTable();
    }

    function renderTable(){
      const q = (search.value||'').toLowerCase();
      const rows = (cache||[])
        .map(s => ({
          id:s.id, nombre:s.nombre||s.name||'',
          contacto:s.contacto||s.contact||'',
          mail:s.mail||s.email||'',
          tel:s.tel||s.phone||'',
          addr:s.addr||s.address||''
        }))
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
          <td>${s.nombre}</td>
          <td>${s.contacto}</td>
          <td>${s.mail}</td>
          <td>${s.tel}</td>
          <td>${s.addr}</td>
          <td class="actions">
            <button data-id="${s.id}" class="btn btn-sm btn-outline-dark btn-edit">Editar</button>
            <button data-id="${s.id}" class="btn btn-sm btn-danger btn-del">Eliminar</button>
          </td>`;
        body.appendChild(tr);
      });
      attachHandlers();
    }

    function attachHandlers(){
      body.querySelectorAll('.btn-edit').forEach(b => b.addEventListener('click', ()=>{
        const s = cache.find(x=>x.id===b.dataset.id);
        openModal('edit', s);
      }));
      body.querySelectorAll('.btn-del').forEach(b => b.addEventListener('click', async ()=>{
        const id = b.dataset.id;
        if(confirm(`¿Eliminar proveedor ${id}?`)){
          await Suppliers.remove(id);
          await load();
        }
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
        f_nombre.value=s.nombre||s.name||''; f_contacto.value=s.contacto||s.contact||'';
        f_mail.value=s.mail||s.email||''; f_tel.value=s.tel||s.phone||''; f_addr.value=s.addr||s.address||'';
      }
      modalBackdrop.style.display='flex'; f_nombre.focus();
    }
    function closeModal(){ modalBackdrop.style.display='none'; }

    function validate(){
      const id = f_id.value.trim();
      const nombre = f_nombre.value.trim();
      if(!id || !nombre) return 'RUT/ID y Nombre son obligatorios.';
      if(mode==='new' && (cache||[]).some(x=>x.id===id)) return 'Ya existe un proveedor con ese RUT/ID.';
      if(f_mail.value && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f_mail.value)) return 'Correo no válido.';
      return '';
    }

    btnNew.addEventListener('click', ()=> openModal('new'));
    modalClose.addEventListener('click', closeModal);
    modalBackdrop.addEventListener('click', (e)=>{ if(e.target===modalBackdrop) closeModal(); });

    btnSave.addEventListener('click', async ()=>{
      const err = validate();
      if(err){ formError.textContent = err; formError.classList.remove('d-none'); return; }
      const dto = {
        id: f_id.value.trim(),
        nombre: f_nombre.value.trim(),
        contacto: f_contacto.value.trim(),
        mail: f_mail.value.trim(),
        tel: f_tel.value.trim(),
        addr: f_addr.value.trim()
      };
      if(mode==='new') await Suppliers.create(dto);
      else await Suppliers.update(editing.id, dto);
      closeModal(); await load();
    });

    btnExport.addEventListener('click', ()=>{
      const rows = cache || [];
      const csv = [
        ['id','nombre','contacto','mail','tel','addr'].join(','),
        ...rows.map(s => [
          s.id,
          JSON.stringify(s.nombre||s.name||''),
          JSON.stringify(s.contacto||s.contact||''),
          JSON.stringify(s.mail||s.email||''),
          JSON.stringify(s.tel||s.phone||''),
          JSON.stringify(s.addr||s.address||'')
        ].join(','))
      ].join('\n');
      const blob = new Blob([csv], {type:'text/csv;charset=utf-8;'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = 'proveedores.csv';
      document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url);
    });

    search.addEventListener('input', renderTable);
    load();
  });
}
