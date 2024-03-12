// models/book.js
const mongoose = require('mongoose');
const PageSchema = require('./page').schema;

const BookSchema = new mongoose.Schema({
    title: String,
    description: String,
    userId: String,
    pages: [PageSchema],
    status: String,
    allowedUsers: [String]

});

module.exports = mongoose.model('Book', BookSchema);
