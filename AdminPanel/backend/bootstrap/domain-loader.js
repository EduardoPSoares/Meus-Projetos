const backendDomains = [
  'auth',
  'services',
  'launcher',
  'shop',
  'rewards',
  'players',
  'cdn'
];

function listBackendDomains() {
  return backendDomains.slice();
}

module.exports = {
  listBackendDomains
};
