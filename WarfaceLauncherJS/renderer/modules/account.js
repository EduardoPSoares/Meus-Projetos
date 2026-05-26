import { LauncherState } from './state-ui.js';

export function createAccountModule(ctx) {
  const { ipcRenderer, state, getLauncherState, persistLauncherState, updatePlayButton, updateStatusMessage } = ctx;

  function showAccountMessage(message, color) {
    const msg = document.getElementById('account-status-msg');
    if (!msg) return;
    msg.textContent = message;
    msg.style.color = color || '#5a7a5a';
  }

  function updateAccountPanel() {
    const panel = document.getElementById('account-panel');
    const openBtn = document.getElementById('btn-open-account');
    if (!panel || !openBtn) return;

    const launcherState = getLauncherState();
    if (launcherState === LauncherState.BLOCKED) {
      openBtn.textContent = 'BLOQUEADO';
      openBtn.disabled = true;
      showAccountMessage('Atualizacao do launcher em andamento.', '#c8a01a');
      return;
    }

    openBtn.disabled = false;
    if (launcherState === LauncherState.NO_GAME) {
      openBtn.textContent = 'ENTRAR NO DISCORD';
      showAccountMessage('Entre no Discord para suporte e avisos oficiais.', '#8ba98b');
    } else if (launcherState === LauncherState.READY) {
      openBtn.textContent = 'SAIR';
      showAccountMessage('Conta ativa. Use este usuario no login do jogo.', '#4aaa4a');
    } else if (launcherState === LauncherState.NEEDS_LOGIN) {
      openBtn.textContent = 'LOGIN';
      showAccountMessage('Faca login para jogar.', '#c8a01a');
    } else {
      openBtn.textContent = 'CRIAR CONTA';
      showAccountMessage('Crie uma conta nova ou use LOGIN.', '#5a7a5a');
    }
  }

  function saveAccount(account) {
    state.launcherAccount = {
      username: String((account && account.username) || '').trim(),
      accountId: String((account && account.accountId) || '').trim(),
      activated: account && account.activated !== false,
      password: String((account && account.password) || '')
    };
    state.hasCreatedAccount = true;

    localStorage.setItem('wf_launcher_account', JSON.stringify({
      username: state.launcherAccount.username,
      accountId: state.launcherAccount.accountId,
      activated: state.launcherAccount.activated
    }));
    localStorage.setItem('wf_has_created_account', 'true');

    persistLauncherState({
      account: {
        username: state.launcherAccount.username,
        accountId: state.launcherAccount.accountId,
        activated: state.launcherAccount.activated
      },
      hasCreatedAccount: true
    });

    updateAccountPanel();
    updatePlayButton();
    updateStatusMessage();
  }

  function clearSavedAccount() {
    state.launcherAccount = null;
    localStorage.removeItem('wf_launcher_account');
    persistLauncherState({ account: null, hasCreatedAccount: state.hasCreatedAccount === true });
    updateAccountPanel();
    updatePlayButton();
    updateStatusMessage();
  }

  function loadSavedAccount() {
    try {
      const saved = localStorage.getItem('wf_launcher_account');
      state.launcherAccount = saved ? JSON.parse(saved) : null;
    } catch {
      state.launcherAccount = null;
    }
    state.hasCreatedAccount = localStorage.getItem('wf_has_created_account') === 'true';
    if (state.launcherAccount && !state.hasCreatedAccount) {
      state.hasCreatedAccount = true;
      localStorage.setItem('wf_has_created_account', 'true');
    }
    updateAccountPanel();
    updatePlayButton();
    updateStatusMessage();
  }

  function bindAccountIpc() {
    ipcRenderer.on('account-created', (event, account) => { if (account && account.username) saveAccount(account); });
    ipcRenderer.on('account-logged-in', (event, account) => { if (account && account.username) saveAccount(account); });
  }

  function bindAccountButton() {
    const btnOpenAccount = document.getElementById('btn-open-account');
    if (!btnOpenAccount) return;

    btnOpenAccount.addEventListener('click', async () => {
      const launcherState = getLauncherState();
      if (launcherState === LauncherState.READY) return clearSavedAccount();
      if (launcherState === LauncherState.NO_GAME) return ipcRenderer.invoke('open-discord-invite');
      const mode = launcherState === LauncherState.NEEDS_LOGIN ? 'login' : 'register';
      await ipcRenderer.invoke('open-account-window', { mode });
    });
  }

  return {
    updateAccountPanel,
    loadSavedAccount,
    bindAccountIpc,
    bindAccountButton
  };
}
