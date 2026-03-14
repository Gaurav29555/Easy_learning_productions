const express = require('express');
const User = require('../models/User');
const Review = require('../models/Review');

const router = express.Router();

// Get all workers
router.get('/', async (req, res) => {
  try {
    const { skill, location } = req.query;
    let query = { role: 'worker' };
    if (skill) query.skill = skill;
    if (location) query.location = { $regex: location, $options: 'i' };
    const workers = await User.find(query).populate('reviews');
    res.json(workers);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get worker by ID
router.get('/:id', async (req, res) => {
  try {
    const worker = await User.findById(req.params.id).populate('reviews');
    if (!worker) return res.status(404).json({ error: 'Worker not found' });
    res.json(worker);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Update worker availability
router.put('/:id/availability', async (req, res) => {
  try {
    const { availability } = req.body;
    const worker = await User.findByIdAndUpdate(req.params.id, { availability }, { new: true });
    res.json(worker);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;