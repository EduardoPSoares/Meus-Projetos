const ipcRenderer = window.accountAPI;

const urlParams = new URLSearchParams(window.location.search);
const mode = urlParams.get('mode') || 'register';
const isLogin = mode === 'login';

const form = document.getElementById('create-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const confirmField = document.getElementById('confirm-field');
const confirmInput = document.getElementById('confirm-password');
const createBtn = document.getElementById('btn-create');
const msg = document.getElementById('form-msg');
const successPanel = document.getElementById('success-panel');
const successUser = document.getElementById('success-user');
const successId = document.getElementById('success-id');
const windowTitle = document.getElementById('window-title');
const windowSub = document.getElementById('window-sub');
const headKicker = document.getElementById('head-kicker');

if (isLogin) {
  document.body.classList.add('login-mode');
  windowTitle.textContent = 'ENTRAR';
  windowSub.textContent = '// LOGIN';
  if (headKicker) headKicker.textContent = 'ACESSAR CONTA';
  createBtn.textContent = 'ENTRAR';
  setMessage('Entre com a conta ja criada para liberar o jogo.', '#5a9a5a');
  if (confirmField) {
    confirmField.style.display = 'none';
    document.querySelector('.field-grid').style.gridTemplateColumns = '1fr';
  }
  const rulesArea = document.getElementById('rules-area');
  if (rulesArea) rulesArea.classList.add('hidden');
} else {
  createBtn.textContent = 'CRIAR CONTA';
  setMessage('Cadastro conectado ao servidor oficial.', '#5a9a5a');
}

function setMessage(text, color) {
  msg.textContent = text;
  msg.style.color = color || '#5a9a5a';
}

function updateRule(id, ok, touched) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.toggle('ok', !!ok);
  el.classList.toggle('bad', !!touched && !ok);
}

function getValidation() {
  const username = usernameInput.value.trim().toLowerCase();
  const password = passwordInput.value;

  if (isLogin) {
    const valid = username.length >= 3 && password.length >= 3;
    return { username, password, valid };
  }

  const confirmPassword = confirmInput.value;
  const validUser = /^[a-z][a-z0-9_-]{2,19}$/.test(username);
  const validPassword = password.length >= 3 && password.length <= 32 && /^[A-Za-z0-9_.@-]+$/.test(password) && password.toLowerCase() !== username;
  const matches = password.length > 0 && password === confirmPassword;

  return { username, password, validUser, validPassword, matches, valid: validUser && validPassword && matches };
}

function refreshValidation() {
  const v = getValidation();
  if (!isLogin) {
    updateRule('rule-user', v.validUser, usernameInput.value.length > 0);
    updateRule('rule-pass', v.validPassword, passwordInput.value.length > 0);
    updateRule('rule-match', v.matches, confirmInput.value.length > 0);
  }
  createBtn.disabled = !v.valid;
}

[usernameInput, passwordInput].forEach(input => {
  input.addEventListener('input', refreshValidation);
});
if (confirmInput) {
  confirmInput.addEventListener('input', refreshValidation);
}

document.getElementById('btn-minimize').addEventListener('click', () => {
  ipcRenderer.send('account-window-minimize');
});

document.getElementById('btn-close').addEventListener('click', () => {
  ipcRenderer.send('account-window-close');
});

document.getElementById('btn-done').addEventListener('click', () => {
  ipcRenderer.send('account-window-close');
});

form.addEventListener('submit', async event => {
  event.preventDefault();
  const v = getValidation();
  if (!v.valid) {
    setMessage(isLogin ? 'Preencha usuario e senha.' : 'Confira usuario, senha e confirmacao.', '#c8a01a');
    refreshValidation();
    return;
  }

  createBtn.disabled = true;

  try {
    if (isLogin) {
      createBtn.textContent = 'ENTRANDO...';
      setMessage('Autenticando...', '#5a9a5a');
      const result = await ipcRenderer.invoke('login-account', {
        username: v.username,
        password: v.password
      });

      if (!result || !result.success) {
        throw new Error(result && result.error ? result.error : 'Falha ao autenticar');
      }

      const account = {
        username: result.username || v.username,
        password: v.password,
        accountId: result.accountId,
        activated: true
      };

      passwordInput.value = '';
      if (confirmInput) confirmInput.value = '';
      form.classList.add('hidden');
      successUser.textContent = account.username;
      successId.textContent = '';
      document.querySelector('.success-tag').textContent = 'LOGIN EFETUADO';
      successPanel.style.display = 'block';
      ipcRenderer.send('account-logged-in', account);
    } else {
      createBtn.textContent = 'CRIANDO...';
      setMessage('Enviando cadastro para o servidor oficial...', '#5a9a5a');
      const result = await ipcRenderer.invoke('register-account', {
        username: v.username,
        password: v.password
      });

      if (!result || !result.success) {
        throw new Error(result && result.error ? result.error : 'Falha ao criar conta');
      }

      const account = {
        username: result.username || v.username,
        password: v.password,
        accountId: result.accountId,
        activated: result.activated !== false
      };

      passwordInput.value = '';
      confirmInput.value = '';
      form.classList.add('hidden');
      successUser.textContent = account.username;
      successId.textContent = `ID ${account.accountId || '--'}`;
      successPanel.style.display = 'block';
      ipcRenderer.send('account-created', account);
    }
  } catch (error) {
    createBtn.disabled = false;
    createBtn.textContent = isLogin ? 'ENTRAR' : 'CRIAR CONTA';
    setMessage(error.message || 'Erro.', '#c8371a');
  }
});

refreshValidation();
