const express = require('express');
const jwt = require('jsonwebtoken');

const router = express.Router();

// Middleware
const verifyToken = (req, res, next) => {
  const token = req.header('Authorization')?.replace('Bearer ', '');
  if (!token) return res.status(401).json({ error: 'Access denied' });
  try {
    const verified = jwt.verify(token, process.env.JWT_SECRET || 'secret');
    req.user = verified;
    next();
  } catch (error) {
    res.status(400).json({ error: 'Invalid token' });
  }
};

// Process voice command
router.post('/commands', verifyToken, async (req, res) => {
  try {
    const { command } = req.body;
    // Simple parsing - in real app, use NLP
    let action = 'unknown';
    let params = {};

    if (command.toLowerCase().includes('book') && command.toLowerCase().includes('electrician')) {
      action = 'book';
      params.skill = 'electrician';
    } else if (command.toLowerCase().includes('book') && command.toLowerCase().includes('plumber')) {
      action = 'book';
      params.skill = 'plumber';
    } // Add more

    res.json({ action, params });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;