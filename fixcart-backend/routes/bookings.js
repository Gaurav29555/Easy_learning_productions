const express = require('express');
const jwt = require('jsonwebtoken');
const Booking = require('../models/Booking');
const User = require('../models/User');

const router = express.Router();

// Middleware to verify JWT (simplified)
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

// Create booking
router.post('/', verifyToken, async (req, res) => {
  try {
    const { workerId, time, description } = req.body;
    const booking = new Booking({ user: req.user.id, worker: workerId, time, description });
    await booking.save();
    res.status(201).json(booking);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get user bookings
router.get('/user', verifyToken, async (req, res) => {
  try {
    const bookings = await Booking.find({ user: req.user.id }).populate('worker').populate('payment');
    res.json(bookings);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get worker bookings
router.get('/worker', verifyToken, async (req, res) => {
  try {
    const bookings = await Booking.find({ worker: req.user.id }).populate('user').populate('payment');
    res.json(bookings);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Update booking status
router.put('/:id/status', verifyToken, async (req, res) => {
  try {
    const { status } = req.body;
    const booking = await Booking.findByIdAndUpdate(req.params.id, { status }, { new: true });
    res.json(booking);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;