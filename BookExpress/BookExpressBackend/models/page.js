// models/page.js
const mongoose = require('mongoose');

const PageSchema = new mongoose.Schema({
    content: String,
    pageNumber: Number,
    createdAt: { type: Date, default: Date.now },
    bookId: { type: mongoose.Schema.Types.ObjectId, ref: 'Book' }
});

module.exports = mongoose.model('Page', PageSchema);
