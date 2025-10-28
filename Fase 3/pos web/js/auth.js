// js/auth.js
// Manejo simple de sesión (demo) y guardas por rol

// js/auth.js
// Login por SDK si existe; fallback a localStorage con credenciales demo.

const DEMO_VALID = {
  admin:  { pass: 'admin123',  rol: 'ADMIN'  },
  cajero: { pass: 'cajero123', rol: 'CAJERO' },
};

// ─── Helpers sesión ───────────────────────────────────────────────
export function getUser(){
  try{ return JSON.parse(localStorage.getItem('pos_user')||'null'); }
  catch(_){ return null; }
}
export function logout(){
  localStorage.removeItem('pos_user');
  location.replace('index.html');
}
export function requireAuth(roles = []){
  const u = getUser();
  if(!u){ location.replace('index.html'); return null; }
  if(roles.length && !roles.includes(u.rol)){
    if(u.rol === 'ADMIN') location.replace('admin.html');
    else location.replace('ventas.html');
    return null;
  }
  return u;
}

// ─── Login ────────────────────────────────────────────────────────
export async function doLogin(e){
  e?.preventDefault?.();
  const user = document.getElementById('user').value.trim();
  const pass = document.getElementById('pass').value.trim();
  const errorEl = document.getElementById('loginError');

  // Si hay SDK, intenta login real
  if (window.SDK?.Auth?.login) {
    try{
      const res = await window.SDK.Auth.login({ username:user, password:pass });
      // Se espera { username, role, token }
      if(!res || !res.username){ throw new Error('Respuesta inválida'); }
      const payload = { u: res.username, rol: res.role || 'ADMIN', token: res.token || null, ts: Date.now() };
      localStorage.setItem('pos_user', JSON.stringify(payload));
      location.href = payload.rol === 'ADMIN' ? 'admin.html' : 'ventas.html';
      return false;
    }catch(err){
      // Si falla SDK, cae a demo
      console.warn('SDK.Auth.login falló, usando fallback demo:', err?.message);
    }
  }

  // Fallback demo local
  const rec = DEMO_VALID[user];
  const ok = !!rec && pass === rec.pass;
  if(!ok){
    if(errorEl){
      errorEl.classList.remove('d-none');
      errorEl.textContent = 'Usuario o contraseña incorrectos.';
    }
    return false;
  }
  const payload = { u: user, rol: rec.rol, token: null, ts: Date.now() };
  localStorage.setItem('pos_user', JSON.stringify(payload));
  location.href = payload.rol === 'ADMIN' ? 'admin.html' : 'ventas.html';
  return false;
}
