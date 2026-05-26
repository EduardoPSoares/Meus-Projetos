const { listBackendDomains } = require('./domain-loader');

function toRegisterName(domain) {
  return `register${domain.charAt(0).toUpperCase()}${domain.slice(1)}Routes`;
}

function registerBackendDomains(context = {}) {
  const loaded = [];

  for (const domain of listBackendDomains()) {
    try {
      const mod = require(`../domains/${domain}/register`);
      const fn = mod && mod[toRegisterName(domain)];
      if (typeof fn === 'function') {
        fn(context);
        loaded.push(domain);
      }
    } catch (error) {
      if (context && typeof context.log === 'function') {
        context.log('BOOT', `domain register skipped (${domain}): ${error.message}`);
      }
    }
  }

  return loaded;
}

module.exports = {
  registerBackendDomains
};
