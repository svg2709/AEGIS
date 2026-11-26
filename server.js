const express = require('express');
const path = require('path');

const app = express();
const PORT = Number(process.env.PORT) || 3000;
const HOST = '0.0.0.0';

const BLOCKED_PATHS = new Set([
  '/server.js',
  '/package.json',
  '/package-lock.json',
  '/metadata.json',
]);

app.use((req, res, next) => {
  const normalized = req.path.toLowerCase();
  if (
    BLOCKED_PATHS.has(normalized) ||
    normalized.startsWith('/node_modules') ||
    normalized.includes('/.')
  ) {
    return res.status(404).send('Not found');
  }
  next();
});

app.use(
  express.static(__dirname, {
    dotfiles: 'deny',
    index: 'index.html',
  })
);

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, HOST, () => {
  console.log(`AEGIS server running at http://${HOST}:${PORT}`);
});
