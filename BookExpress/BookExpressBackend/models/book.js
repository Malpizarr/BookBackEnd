// models/book.js
const mongoose = require('mongoose');
const PageSchema = require('./page').schema; // Asegúrate de que la ruta sea correcta

const BookSchema = new mongoose.Schema({
    title: String,
    description: String,
    userId: String, // ID del usuario proveniente del servicio externo
    pages: [PageSchema]
});

module.exports = mongoose.model('Book', BookSchema);
