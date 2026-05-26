const { startModernServer } = require('../app/server');

function startAdminPanel() {
  return startModernServer();
}

module.exports = {
  startAdminPanel
};
