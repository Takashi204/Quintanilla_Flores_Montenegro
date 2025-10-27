// js/auth.js
// Manejo simple de sesión (demo) y guardas por rol

const VALID = {
  admin:  { pass: 'admin123',  rol: 'ADMIN'  },
  cajero: { pass: 'cajero123', rol: 'CAJERO' },
};

export function doLogin(e){
  e?.preventDefault?.();
  const u = document.getElementById('user').value.trim();
  const p = document.getElementById('pass').value.trim();
  const rec = VALID[u];
  const ok = !!rec && p === rec.pass;
  const errorEl = document.getElementById('loginError');
  if(!ok){
    errorEl?.classList?.remove('d-none');
    errorEl.textContent = 'Usuario o contraseña incorrectos.';
    return false;
  }
  const payload = { u, rol: rec.rol, ts: Date.now() };
  localStorage.setItem('pos_user', JSON.stringify(payload));
  // Redirigir por rol
  if(rec.rol === 'ADMIN') location.href = 'admin.html';
  else location.href = 'ventas.html';
  return false;
}

export function getUser(){
  try{ return JSON.parse(localStorage.getItem('pos_user')||'null'); }
  catch(_){ return null; }
}

export function requireAuth(roles = []){
  const u = getUser();
  if(!u){ location.replace('index.html'); return null; }
  if(roles.length && !roles.includes(u.rol)){
    // Redirigir a casa según rol
    if(u.rol === 'ADMIN') location.replace('admin.html');
    else location.replace('ventas.html');
    return null;
  }
  return u;
}

export function logout(){
  localStorage.removeItem('pos_user');
  location.replace('index.html');
}
