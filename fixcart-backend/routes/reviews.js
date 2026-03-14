const express = require('express');
const jwt = require('jsonwebtoken');
const Review = require('../models/Review');
const User = require('../models/User');

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

// Create review
router.post('/', verifyToken, async (req, res) => {
  try {
    const { workerId, bookingId, rating, comment } = req.body;
    const review = new Review({ worker: workerId, user: req.user.id, booking: bookingId, rating, comment });
    await review.save();
    // Update worker rating
    const reviews = await Review.find({ worker: workerId });
    const avgRating = reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length;
    await User.findByIdAndUpdate(workerId, { rating: avgRating, $push: { reviews: review._id } });
    res.status(201).json(review);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get reviews for worker
router.get('/worker/:id', async (req, res) => {
  try {
    const reviews = await Review.find({ worker: req.params.id }).populate('user');
    res.json(reviews);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;