// js/reportes.js
import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['ADMIN']);
if(u){
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('reportes');
    const fmt = v => `$${Number(v||0).toLocaleString('es-CL')}`;

    const from = document.getElementById('from');
    const to = document.getElementById('to');
    const tbody = document.getElementById('salesBody');

    function getSales(){
      try{ return JSON.parse(localStorage.getItem('pos_sales')||'[]'); }
      catch{ return []; }
    }

    function filterRange(arr){
      const f = from.value ? new Date(from.value) : null;
      const t = to.value ? new Date(to.value) : null;
      return arr.filter(s => {
        const d = new Date(s.ts);
        if(f && d < new Date(f.getFullYear(), f.getMonth(), f.getDate())) return false;
        if(t && d > new Date(t.getFullYear(), t.getMonth(), t.getDate()+1)) return false;
        return true;
      });
    }

    function render(){
      const sales = filterRange(getSales()).sort((a,b)=> b.ts-a.ts);
      tbody.innerHTML='';
      sales.forEach(s=>{
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${s.id}</td><td>${new Date(s.ts).toLocaleString('es-CL')}</td><td>${s.user}</td><td>${s.method||'-'}</td><td>${fmt(s.total)}</td>`;
        tbody.appendChild(tr);
      });
      const total = sales.reduce((a,s)=> a + Number(s.total||0), 0);
      const count = sales.length;
      document.getElementById('rTotal').textContent = fmt(total);
      document.getElementById('rCount').textContent = String(count);
      document.getElementById('rAvg').textContent = fmt(count? total/count : 0);
      document.getElementById('rLast').textContent = sales[0] ? new Date(sales[0].ts).toLocaleString('es-CL') : '-';
    }

    document.getElementById('btnApply').addEventListener('click', render);
    document.getElementById('btnExport').addEventListener('click', ()=>{
      const rows = filterRange(getSales());
      const csv = [
        ['id','fecha','usuario','metodo','total'].join(','),
        ...rows.map(s => [s.id, new Date(s.ts).toISOString(), s.user, s.method||'', s.total].join(','))
      ].join('\n');
      const blob = new Blob([csv], {type:'text/csv;charset=utf-8;'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href=url; a.download='ventas.csv';
      document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url);
    });

    // Rango por defecto: mes actual
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    from.value = start.toISOString().slice(0,10);
    to.value = now.toISOString().slice(0,10);
    render();
  });
}
