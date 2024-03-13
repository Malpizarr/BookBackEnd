// models/book.js
const mongoose = require('mongoose');
const PageSchema = require('./page').schema;

const { v4: uuidv4 } = require('uuid');

const BookSchema = new mongoose.Schema({
    title: String,
    description: String,
    userId: String,
    pages: [PageSchema],
    status: String,
    allowedUsers: [String],
    editToken: { type: String, default: uuidv4 }
});


module.exports = mongoose.model('Book', BookSchema);
