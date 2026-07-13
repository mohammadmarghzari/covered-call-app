import jwt from 'jsonwebtoken';
import { store } from './store.js';

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';
const TOKEN_TTL = '90d';

export function issueToken(user) {
  return jwt.sign({ sub: user.id, phone: user.phone }, JWT_SECRET, { expiresIn: TOKEN_TTL });
}

/** میدل‌ور: توکن Bearer را بررسی و کاربر را روی req.user می‌گذارد. */
export function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'unauthorized' });
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    const user = store.getUser(payload.phone);
    if (!user) return res.status(401).json({ error: 'user_not_found' });
    req.user = user;
    next();
  } catch {
    return res.status(401).json({ error: 'invalid_token' });
  }
}

/** میدل‌ور ادمین: هدر x-admin-token را با ADMIN_TOKEN مقایسه می‌کند. */
export function requireAdmin(req, res, next) {
  const adminToken = process.env.ADMIN_TOKEN;
  if (!adminToken) return res.status(503).json({ error: 'admin_disabled' });
  if (req.headers['x-admin-token'] !== adminToken) {
    return res.status(401).json({ error: 'unauthorized' });
  }
  next();
}
