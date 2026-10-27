// URL de l'API : locale en développement, Render en production.
const isLocal =
  typeof location !== 'undefined' &&
  (location.hostname === 'localhost' || location.hostname === '127.0.0.1');

export const API_BASE = isLocal
  ? 'http://localhost:8080'
  : 'https://stockflow-g7pe.onrender.com';