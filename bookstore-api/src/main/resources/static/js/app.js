'use strict';
/*
 * Shelfwise front end - plain JavaScript, no framework.
 * It talks to the Spring Boot REST API with fetch() and sends the JWT in the Authorization header.
 *
 * Flow: user action -> api() call -> JSON response -> render HTML into <main id="app">.
 */

/* ---------- small helpers ---------- */
const $ = (sel, root = document) => root.querySelector(sel);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const hueOf = str => { let h = 0; for (const ch of String(str)) h = (h * 31 + ch.charCodeAt(0)) % 360; return h; };
const dateFmt = iso => new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
const app = $('#app');
const bookCache = new Map();   // id -> book, so "Add to cart" does not need another request

const state = {
  token: localStorage.getItem('bs_token'),
  user: JSON.parse(localStorage.getItem('bs_user') || 'null'),
  cart: JSON.parse(localStorage.getItem('bs_cart') || '[]'),
  genres: [],
  catalog: { page: 0, size: 12, search: '', genre: '', sort: 'id:desc' },
  admin: { tab: 'books', bookPage: 0, bookSearch: '', orderPage: 0, orderStatus: '' },
  myOrdersPage: 0
};
const isAdmin = () => state.user?.role === 'ADMIN';
const isCustomer = () => state.user?.role === 'CUSTOMER';

function persist() {
  const set = (k, v) => v ? localStorage.setItem(k, v) : localStorage.removeItem(k);
  set('bs_token', state.token);
  set('bs_user', state.user ? JSON.stringify(state.user) : null);
  localStorage.setItem('bs_cart', JSON.stringify(state.cart));
}

function toast(message, type = 'ok') {
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.textContent = message;
  $('#toasts').append(el);
  setTimeout(() => el.remove(), 3500);
}

/* ---------- API wrapper ---------- */
async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (auth && state.token) headers['Authorization'] = 'Bearer ' + state.token;
  const res = await fetch('/api' + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty body */ }
  if (!res.ok) {
    if (res.status === 401 && auth && state.token) logout(true);
    const err = new Error(data?.message || 'Something went wrong. Please try again.');
    err.status = res.status;
    err.fields = data?.fieldErrors;
    throw err;
  }
  return data;
}
const errorText = e => e.fields ? Object.values(e.fields).join('. ') : e.message;

/* ---------- covers ---------- */
function cover(b, size = '') {
  const fallback = `<div class="cover-fallback" style="--h:${hueOf(b.title)}" ${b.imageUrl ? 'hidden' : ''}><span>${esc(b.title)}</span><small>${esc(b.author)}</small></div>`;
  const img = b.imageUrl ? `<img src="${esc(b.imageUrl)}" alt="Cover of ${esc(b.title)}" loading="lazy" data-cover>` : '';
  return `<div class="cover ${size}">${img}${fallback}</div>`;
}
// If a cover image fails to load, show the generated cover instead.
document.addEventListener('error', e => {
  const img = e.target;
  if (img.tagName === 'IMG' && img.hasAttribute('data-cover')) {
    img.hidden = true;
    if (img.nextElementSibling) img.nextElementSibling.hidden = false;
  }
}, true);

/* ---------- navigation ---------- */
function renderNav() {
  const count = state.cart.reduce((n, i) => n + i.quantity, 0);
  const hash = location.hash || '#/';
  const link = (href, label) => `<a href="${href}" class="${hash === href ? 'active' : ''}">${label}</a>`;
  $('#nav').innerHTML =
    link('#/', 'Books') +
    (isCustomer() ? link('#/orders', 'My orders') : '') +
    (isAdmin() ? link('#/admin', 'Admin') : '') +
    `<button class="ghost" data-action="open-cart">Cart <span class="badge">${count}</span></button>` +
    (state.user
      ? `<span class="who">${esc(state.user.name)}</span><button class="ghost" data-action="logout">Log out</button>`
      : `<button class="primary" data-action="open-auth" data-mode="login">Log in</button>`);
}

function route() {
  const h = location.hash || '#/';
  renderNav();
  window.scrollTo(0, 0);
  if (h.startsWith('#/orders')) renderMyOrders();
  else if (h.startsWith('#/admin')) renderAdmin();
  else renderCatalog();
}

const gate = message => `<section class="page narrow"><h1>Log in to continue</h1><p class="muted">${esc(message)}</p><button class="primary" data-action="open-auth" data-mode="login">Log in</button></section>`;

function pager(sel, res, action) {
  $(sel).innerHTML = res.totalPages <= 1 ? '' :
    `<button class="ghost" data-action="${action}" data-page="${res.page - 1}" ${res.page === 0 ? 'disabled' : ''}>Previous</button>
     <span>Page ${res.page + 1} of ${res.totalPages}</span>
     <button class="ghost" data-action="${action}" data-page="${res.page + 1}" ${res.last ? 'disabled' : ''}>Next</button>`;
}

/* ---------- catalog ---------- */
async function renderCatalog() {
  app.innerHTML = `<div class="catalog">
      <aside class="filters" id="filters"></aside>
      <section><div class="results-head" id="resultsHead"></div><div class="grid" id="grid"><p class="muted">Loading books…</p></div><div class="pager" id="pager"></div></section>
    </div>`;
  if (!state.genres.length) {
    try { state.genres = await api('/books/genres', { auth: false }); } catch (e) { /* filters just stay empty */ }
  }
  renderFilters();
  await loadBooks();
}

function renderFilters() {
  const c = state.catalog;
  const sorts = [['id:desc', 'Newest first'], ['title:asc', 'Title, A to Z'], ['price:asc', 'Price, low to high'], ['price:desc', 'Price, high to low']];
  $('#filters').innerHTML = `
    <h2>Genre</h2>
    <button class="chip ${c.genre === '' ? 'on' : ''}" data-action="genre" data-genre="">All genres</button>
    ${state.genres.map(g => `<button class="chip ${c.genre === g ? 'on' : ''}" data-action="genre" data-genre="${esc(g)}">${esc(g)}</button>`).join('')}
    <h2>Sort by</h2>
    <select id="sortSelect" aria-label="Sort books">${sorts.map(([v, l]) => `<option value="${v}" ${c.sort === v ? 'selected' : ''}>${l}</option>`).join('')}</select>`;
}

async function loadBooks() {
  const c = state.catalog;
  const [sortBy, direction] = c.sort.split(':');
  const q = new URLSearchParams({ page: c.page, size: c.size, sortBy, direction });
  if (c.search) q.set('search', c.search);
  if (c.genre) q.set('genre', c.genre);
  try {
    const res = await api('/books?' + q, { auth: false });
    const what = [c.search && `“${esc(c.search)}”`, c.genre && esc(c.genre)].filter(Boolean).join(' in ');
    $('#resultsHead').innerHTML = `<h1>${what || 'All books'}</h1><span class="muted">${res.totalElements} ${res.totalElements === 1 ? 'book' : 'books'}</span>`;
    $('#grid').innerHTML = res.content.length ? res.content.map(bookCard).join('')
      : `<div class="empty">No books match your search. Try a different title, author or genre.</div>`;
    pager('#pager', res, 'catalog-page');
  } catch (e) {
    $('#grid').innerHTML = `<p class="error">${esc(e.message)}</p>`;
  }
}

const ratingHtml = b => b.reviewCount ? `<p class="rating" aria-label="Rated ${b.averageRating} out of 5">★ ${b.averageRating} <span>(${b.reviewCount})</span></p>` : '';
const stockHtml = b => b.stockQuantity === 0 ? '<span class="stock out">Out of stock</span>' : b.stockQuantity <= 5 ? `<span class="stock low">Only ${b.stockQuantity} left</span>` : '';

function bookCard(b) {
  bookCache.set(b.id, b);
  return `<article class="card">
    <button class="cover-btn" data-action="open-book" data-id="${b.id}" aria-label="View ${esc(b.title)}">${cover(b)}</button>
    <div class="card-body">
      <h3><button class="link" data-action="open-book" data-id="${b.id}">${esc(b.title)}</button></h3>
      <p class="author">${esc(b.author)}</p>
      ${ratingHtml(b)}
      <div class="card-foot"><strong class="price">${money(b.price)}</strong>${stockHtml(b)}</div>
      <button class="primary block" data-action="add-cart" data-id="${b.id}" ${b.stockQuantity === 0 ? 'disabled' : ''}>Add to cart</button>
    </div></article>`;
}

/* ---------- book details + reviews ---------- */
async function openBook(id) {
  try {
    const [b, reviews] = await Promise.all([api(`/books/${id}`, { auth: false }), api(`/books/${id}/reviews`, { auth: false })]);
    bookCache.set(b.id, b);
    openModal(`<div class="book-detail">
      <div class="bd-cover">${cover(b)}</div>
      <div class="bd-info">
        <h2>${esc(b.title)}</h2>
        <p class="author">by ${esc(b.author)}</p>
        ${ratingHtml(b)}
        <p class="muted">${esc(b.genre)} · ISBN ${esc(b.isbn)}</p>
        <p>${esc(b.description || 'No description yet.')}</p>
        <p><strong class="price">${money(b.price)}</strong> ${stockHtml(b)}</p>
        <div class="actions">
          <button class="primary" data-action="add-cart" data-id="${b.id}" ${b.stockQuantity === 0 ? 'disabled' : ''}>Add to cart</button>
          <button class="ghost" data-action="close-modal">Close</button>
        </div>
      </div>
      <div class="reviews">
        <h3>Reviews</h3>
        ${reviews.length ? reviews.map(r => `<div class="review"><strong>${esc(r.userName)}</strong> <span class="rating">★ ${r.rating}</span><p>${esc(r.comment || '')}</p></div>`).join('')
          : '<p class="muted">No reviews yet. Be the first to share what you think.</p>'}
        ${state.user ? `<form id="reviewForm" class="review-form" data-id="${b.id}">
            <label>Your rating <select name="rating">${[5, 4, 3, 2, 1].map(n => `<option value="${n}">${n} star${n > 1 ? 's' : ''}</option>`).join('')}</select></label>
            <label>Your review <textarea name="comment" rows="3" maxlength="1000"></textarea></label>
            <p class="form-error" id="formError"></p>
            <button class="primary">Post review</button></form>`
          : '<p class="muted">Log in to write a review.</p>'}
      </div></div>`);
  } catch (e) { toast(e.message, 'err'); }
}

/* ---------- cart ---------- */
function addToCart(id, qty = 1) {
  const b = bookCache.get(id);
  if (!b) return;
  const line = state.cart.find(i => i.bookId === id);
  if ((line ? line.quantity : 0) + qty > b.stockQuantity) { toast(`Only ${b.stockQuantity} in stock`, 'err'); return; }
  if (line) { line.quantity += qty; line.stock = b.stockQuantity; line.price = b.price; }
  else state.cart.push({ bookId: id, title: b.title, author: b.author, price: b.price, imageUrl: b.imageUrl, quantity: qty, stock: b.stockQuantity });
  persist(); renderNav(); renderCart();
  toast(`Added “${b.title}” to your cart`);
}

function renderCart() {
  const total = state.cart.reduce((s, i) => s + i.price * i.quantity, 0);
  $('#cartBody').innerHTML = state.cart.length ? state.cart.map(i => `
    <div class="cart-line">
      ${cover({ title: i.title, author: i.author, imageUrl: i.imageUrl }, 'sm')}
      <div><strong>${esc(i.title)}</strong><p class="muted">${money(i.price)} each</p>
        <div class="qty"><button data-action="cart-dec" data-id="${i.bookId}" aria-label="Decrease quantity">−</button><span>${i.quantity}</span><button data-action="cart-inc" data-id="${i.bookId}" aria-label="Increase quantity">+</button></div></div>
      <div class="line-right"><strong>${money(i.price * i.quantity)}</strong><button class="link danger" data-action="cart-remove" data-id="${i.bookId}">Remove</button></div>
    </div>`).join('') : '<p class="muted pad">Your cart is empty. Pick a book to get started.</p>';
  $('#cartFoot').innerHTML = state.cart.length ? `
    <label>Payment method
      <select id="payMethod"><option value="CARD">Card (mock payment)</option><option value="UPI">UPI (mock payment)</option><option value="COD">Cash on delivery</option></select></label>
    <div class="total"><span>Total</span><strong>${money(total)}</strong></div>
    <button class="primary block" data-action="checkout">Place order</button>` : '';
}

function openCart() { renderCart(); $('#cartDrawer').classList.add('open'); $('#cartDrawer').setAttribute('aria-hidden', 'false'); $('#scrim').hidden = false; }
function closeCart() { $('#cartDrawer').classList.remove('open'); $('#cartDrawer').setAttribute('aria-hidden', 'true'); $('#scrim').hidden = true; }

function changeQty(id, delta) {
  const line = state.cart.find(i => i.bookId === id);
  if (!line) return;
  if (line.quantity + delta > line.stock) { toast(`Only ${line.stock} in stock`, 'err'); return; }
  line.quantity += delta;
  if (line.quantity <= 0) state.cart = state.cart.filter(i => i.bookId !== id);
  persist(); renderNav(); renderCart();
}

async function checkout() {
  if (!state.user) { closeCart(); openAuth('login'); return; }
  if (!isCustomer()) { toast('Admin accounts can’t place orders. Log in with a customer account.', 'err'); return; }
  const btn = $('[data-action="checkout"]');
  btn.disabled = true;
  try {
    const order = await api('/orders', { method: 'POST', body: {
      paymentMethod: $('#payMethod').value,
      items: state.cart.map(i => ({ bookId: i.bookId, quantity: i.quantity }))
    } });
    state.cart = []; persist(); renderNav(); renderCart(); closeCart();
    toast(`Order #${order.id} placed`);
    if (location.hash === '#/orders') route(); else location.hash = '#/orders';
  } catch (e) { toast(e.message, 'err'); btn.disabled = false; }
}

/* ---------- modal ---------- */
function openModal(html, small = false) {
  $('#modalRoot').innerHTML = `<div class="modal-backdrop"><div class="modal ${small ? 'small' : ''}" role="dialog" aria-modal="true">${html}</div></div>`;
  const first = $('#modalRoot input, #modalRoot select, #modalRoot textarea');
  if (first && small) first.focus();
}
function closeModal() { $('#modalRoot').innerHTML = ''; }

/* ---------- auth ---------- */
function openAuth(mode = 'login') {
  closeCart();
  const login = `<form id="loginForm" class="form">
      <label>Email <input name="email" type="email" required autocomplete="email"></label>
      <label>Password <input name="password" type="password" required autocomplete="current-password"></label>
      <p class="form-error" id="formError"></p>
      <button class="primary block">Log in</button></form>`;
  const register = `<form id="registerForm" class="form">
      <label>Full name <input name="name" required maxlength="100" autocomplete="name"></label>
      <label>Email <input name="email" type="email" required autocomplete="email"></label>
      <label>Password <input name="password" type="password" required minlength="6" autocomplete="new-password"></label>
      <label class="check"><input type="checkbox" id="adminToggle"> I’m a store admin</label>
      <label id="adminCodeRow" hidden>Admin code <input name="adminCode" autocomplete="off"></label>
      <p class="form-error" id="formError"></p>
      <button class="primary block">Create account</button></form>`;
  openModal(`<div class="tabs">
      <button class="${mode === 'login' ? 'on' : ''}" data-action="open-auth" data-mode="login">Log in</button>
      <button class="${mode === 'register' ? 'on' : ''}" data-action="open-auth" data-mode="register">Create account</button></div>
      <div style="padding-top:1rem">${mode === 'login' ? login : register}</div>`, true);
}

async function doLogin(email, password) {
  const r = await api('/login', { method: 'POST', auth: false, body: { email, password } });
  state.token = r.token; state.user = r.user; persist();
  closeModal(); toast(`Welcome, ${r.user.name}`);
  if (isAdmin()) location.hash = '#/admin'; else route();
}

function logout(expired = false) {
  state.token = null; state.user = null; persist();
  toast(expired ? 'Your session expired. Please log in again.' : 'You’re logged out');
  if (location.hash !== '#/') location.hash = '#/'; else route();
}

/* ---------- my orders (customer) ---------- */
function orderCard(o, withCustomer = false) {
  return `<article class="order">
    <div class="order-head"><strong>Order #${o.id}</strong><span class="muted">${dateFmt(o.createdAt)}</span></div>
    ${withCustomer ? `<p class="muted">${esc(o.customerName)} (${esc(o.customerEmail)})</p>` : ''}
    <ul>${o.items.map(i => `<li>${esc(i.title)} × ${i.quantity} <span class="muted">${money(i.lineTotal)}</span></li>`).join('')}</ul>
    <div class="order-foot"><span><span class="pill ${o.status}">${o.status}</span> <span class="pill ${o.paymentStatus}">Payment ${o.paymentStatus.toLowerCase()}</span> <span class="muted">${o.paymentMethod}</span></span><strong>${money(o.totalAmount)}</strong></div>
  </article>`;
}

async function renderMyOrders() {
  if (!isCustomer()) { app.innerHTML = gate('Log in with a customer account to see your orders.'); return; }
  app.innerHTML = `<section class="page"><h1>My orders</h1><div id="ordersList"><p class="muted">Loading…</p></div><div class="pager" id="pager"></div></section>`;
  try {
    const res = await api(`/orders/my?page=${state.myOrdersPage}&size=5`);
    $('#ordersList').innerHTML = res.content.length ? res.content.map(o => orderCard(o)).join('')
      : '<div class="empty">You haven’t ordered anything yet. Browse the books and add something to your cart.</div>';
    pager('#pager', res, 'my-orders-page');
  } catch (e) { $('#ordersList').innerHTML = `<p class="error">${esc(e.message)}</p>`; }
}

/* ---------- admin ---------- */
async function renderAdmin() {
  if (!isAdmin()) { app.innerHTML = gate('This area is for store admins. Log in with an admin account.'); return; }
  const t = state.admin.tab;
  app.innerHTML = `<section class="page"><div class="page-head"><h1>Admin</h1>
    <div class="tabs"><button class="${t === 'books' ? 'on' : ''}" data-action="admin-tab" data-tab="books">Books</button>
    <button class="${t === 'orders' ? 'on' : ''}" data-action="admin-tab" data-tab="orders">Orders</button></div></div>
    <div id="adminBody"></div></section>`;
  if (t === 'books') await adminBooks(); else await adminOrders();
}

async function adminBooks() {
  const a = state.admin;
  $('#adminBody').innerHTML = `<div class="toolbar">
      <form id="adminSearchForm"><input id="adminSearch" type="search" placeholder="Search books" aria-label="Search books" value="${esc(a.bookSearch)}"><button class="ghost">Search</button></form>
      <button class="primary" data-action="book-form" data-id="">Add book</button></div>
    <div class="table-wrap" id="tableWrap"><p class="pad muted">Loading…</p></div><div class="pager" id="pager"></div>`;
  const q = new URLSearchParams({ page: a.bookPage, size: 10, sortBy: 'id', direction: 'desc' });
  if (a.bookSearch) q.set('search', a.bookSearch);
  try {
    const res = await api('/books?' + q, { auth: false });
    res.content.forEach(b => bookCache.set(b.id, b));
    $('#tableWrap').innerHTML = res.content.length ? `<table><thead><tr><th>Title</th><th>Author</th><th>Genre</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead><tbody>
      ${res.content.map(b => `<tr><td><strong>${esc(b.title)}</strong><br><span class="muted">${esc(b.isbn)}</span></td><td>${esc(b.author)}</td><td>${esc(b.genre)}</td><td>${money(b.price)}</td>
        <td>${b.stockQuantity === 0 ? '<span class="stock out">0</span>' : b.stockQuantity}</td>
        <td><div class="actions"><button class="ghost" data-action="book-form" data-id="${b.id}">Edit</button><button class="ghost danger" data-action="book-delete" data-id="${b.id}">Delete</button></div></td></tr>`).join('')}
      </tbody></table>` : '<p class="pad muted">No books found.</p>';
    pager('#pager', res, 'admin-book-page');
  } catch (e) { $('#tableWrap').innerHTML = `<p class="pad error">${esc(e.message)}</p>`; }
}

const NEXT_STATUS = { PENDING: ['SHIPPED', 'CANCELLED'], SHIPPED: ['DELIVERED', 'CANCELLED'] };

async function adminOrders() {
  const a = state.admin;
  const statuses = ['', 'PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
  $('#adminBody').innerHTML = `<div class="toolbar"><label>Show <select id="orderFilter">${statuses.map(s => `<option value="${s}" ${a.orderStatus === s ? 'selected' : ''}>${s ? s.charAt(0) + s.slice(1).toLowerCase() : 'All orders'}</option>`).join('')}</select></label></div>
    <div class="table-wrap" id="tableWrap"><p class="pad muted">Loading…</p></div><div class="pager" id="pager"></div>`;
  const q = new URLSearchParams({ page: a.orderPage, size: 10 });
  if (a.orderStatus) q.set('status', a.orderStatus);
  try {
    const res = await api('/orders?' + q);
    $('#tableWrap').innerHTML = res.content.length ? `<table><thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>Total</th><th>Payment</th><th>Status</th></tr></thead><tbody>
      ${res.content.map(o => {
        const next = NEXT_STATUS[o.status];
        return `<tr><td>#${o.id}<br><span class="muted">${dateFmt(o.createdAt)}</span></td>
          <td>${esc(o.customerName)}<br><span class="muted">${esc(o.customerEmail)}</span></td>
          <td>${o.items.map(i => `${esc(i.title)} × ${i.quantity}`).join('<br>')}</td>
          <td>${money(o.totalAmount)}</td>
          <td><span class="pill ${o.paymentStatus}">${o.paymentStatus}</span><br><span class="muted">${o.paymentMethod}</span></td>
          <td>${next ? `<div class="actions"><select id="st-${o.id}" aria-label="New status for order ${o.id}">${next.map(s => `<option>${s}</option>`).join('')}</select><button class="ghost" data-action="order-status" data-id="${o.id}">Update</button></div>`
            : `<span class="pill ${o.status}">${o.status}</span>`}</td></tr>`;
      }).join('')}</tbody></table>` : '<p class="pad muted">No orders found.</p>';
    pager('#pager', res, 'admin-order-page');
  } catch (e) { $('#tableWrap').innerHTML = `<p class="pad error">${esc(e.message)}</p>`; }
}

function openBookForm(id) {
  const b = id ? bookCache.get(Number(id)) : { title: '', author: '', genre: '', isbn: '', price: '', stockQuantity: 0, imageUrl: '', description: '' };
  const f = (label, name, value, extra = '') => `<label>${label}<input name="${name}" value="${esc(value ?? '')}" ${extra}></label>`;
  openModal(`<form id="bookForm" class="form" data-id="${id || ''}">
    <h2>${id ? 'Edit book' : 'Add book'}</h2>
    ${f('Title', 'title', b.title, 'required maxlength="200"')}
    <div class="row">${f('Author(s)', 'author', b.author, 'required maxlength="200"')}${f('Genre', 'genre', b.genre, 'required maxlength="80"')}</div>
    <div class="row">${f('ISBN', 'isbn', b.isbn, 'required maxlength="20"')}${f('Price (₹)', 'price', b.price, 'required type="number" min="0.01" step="0.01"')}</div>
    <div class="row">${f('Stock quantity', 'stockQuantity', b.stockQuantity, 'required type="number" min="0" step="1"')}${f('Cover image URL', 'imageUrl', b.imageUrl, 'maxlength="500"')}</div>
    <label>Description <textarea name="description" rows="4" maxlength="2000">${esc(b.description || '')}</textarea></label>
    <p class="form-error" id="formError"></p>
    <div class="actions"><button type="button" class="ghost" data-action="close-modal">Cancel</button><button class="primary">Save book</button></div></form>`);
}

/* ---------- events (one listener for all clicks) ---------- */
document.addEventListener('click', async e => {
  if (e.target.classList.contains('modal-backdrop')) return closeModal();
  const el = e.target.closest('[data-action]');
  if (!el) return;
  const { action, id, page, genre, mode, tab } = el.dataset;
  const bookId = Number(id);
  switch (action) {
    case 'open-book': return openBook(bookId);
    case 'add-cart': return addToCart(bookId);
    case 'open-cart': return openCart();
    case 'close-cart': return closeCart();
    case 'close-modal': return closeModal();
    case 'cart-inc': return changeQty(bookId, 1);
    case 'cart-dec': return changeQty(bookId, -1);
    case 'cart-remove': state.cart = state.cart.filter(i => i.bookId !== bookId); persist(); renderNav(); return renderCart();
    case 'checkout': return checkout();
    case 'open-auth': return openAuth(mode);
    case 'logout': return logout();
    case 'genre': state.catalog.genre = genre; state.catalog.page = 0; renderFilters(); return loadBooks();
    case 'catalog-page': state.catalog.page = Number(page); window.scrollTo(0, 0); return loadBooks();
    case 'my-orders-page': state.myOrdersPage = Number(page); return renderMyOrders();
    case 'admin-tab': state.admin.tab = tab; return renderAdmin();
    case 'admin-book-page': state.admin.bookPage = Number(page); return adminBooks();
    case 'admin-order-page': state.admin.orderPage = Number(page); return adminOrders();
    case 'book-form': return openBookForm(id);
    case 'book-delete': {
      const b = bookCache.get(bookId);
      if (!confirm(`Delete “${b?.title}”? This cannot be undone.`)) return;
      try { await api(`/books/${bookId}`, { method: 'DELETE' }); toast('Book deleted'); state.genres = []; return adminBooks(); }
      catch (err) { toast(err.message, 'err'); }
      return;
    }
    case 'order-status': {
      try {
        await api(`/orders/${bookId}/status`, { method: 'PUT', body: { status: $(`#st-${bookId}`).value } });
        toast(`Order #${bookId} updated`); return adminOrders();
      } catch (err) { toast(err.message, 'err'); }
    }
  }
});

document.addEventListener('change', e => {
  if (e.target.id === 'sortSelect') { state.catalog.sort = e.target.value; state.catalog.page = 0; loadBooks(); }
  if (e.target.id === 'orderFilter') { state.admin.orderStatus = e.target.value; state.admin.orderPage = 0; adminOrders(); }
  if (e.target.id === 'adminToggle') $('#adminCodeRow').hidden = !e.target.checked;
});

document.addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  const data = Object.fromEntries(new FormData(form));
  const showError = err => { const p = $('#formError'); if (p) p.textContent = errorText(err); else toast(errorText(err), 'err'); };
  try {
    switch (form.id) {
      case 'searchForm':
        state.catalog.search = $('#searchInput').value.trim(); state.catalog.page = 0;
        if ((location.hash || '#/') === '#/') route(); else location.hash = '#/';
        break;
      case 'adminSearchForm':
        state.admin.bookSearch = $('#adminSearch').value.trim(); state.admin.bookPage = 0; adminBooks();
        break;
      case 'loginForm':
        await doLogin(data.email, data.password);
        break;
      case 'registerForm':
        await api('/register', { method: 'POST', auth: false, body: {
          name: data.name, email: data.email, password: data.password,
          role: $('#adminToggle').checked ? 'ADMIN' : 'CUSTOMER', adminCode: data.adminCode || null } });
        await doLogin(data.email, data.password);
        break;
      case 'reviewForm':
        await api(`/books/${form.dataset.id}/reviews`, { method: 'POST', body: { rating: Number(data.rating), comment: data.comment } });
        toast('Thanks for your review'); openBook(Number(form.dataset.id));
        break;
      case 'bookForm': {
        const id = form.dataset.id;
        const body = { title: data.title, author: data.author, genre: data.genre, isbn: data.isbn,
          price: Number(data.price), stockQuantity: parseInt(data.stockQuantity, 10),
          imageUrl: data.imageUrl || null, description: data.description };
        await api(id ? `/books/${id}` : '/books', { method: id ? 'PUT' : 'POST', body });
        closeModal(); toast(id ? 'Book updated' : 'Book added'); state.genres = []; adminBooks();
        break;
      }
    }
  } catch (err) { showError(err); }
});

document.addEventListener('keydown', e => { if (e.key === 'Escape') { closeModal(); closeCart(); } });
window.addEventListener('hashchange', route);

renderNav();
renderCart();
route();
