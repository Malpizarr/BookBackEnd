const jwt = require("jsonwebtoken");
require('dotenv').config({path: require('path').resolve(__dirname, '.env')});
const mysql = require('mysql2');
const express = require('express');
const app = express();
const {json} = require("express");

app.use(json());


const connection = mysql.createConnection({
    host: process.env.DATABASE_URI,
    user: process.env.DATABASE_USER,
    password: process.env.DATABASE_PASSWORD,
    database: process.env.DATABASE_NAME,
    ssl: {
        rejectUnauthorized: false // Permite certificados no autorizados
    }
});

connection.connect((err) => {
    if (err) throw err;
    console.log('Connected to MySQL Server!');
});

const ValidateTokenJWT = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
        return res.status(401).json({message: 'No token provided'});
    }

    const token = authHeader.split(' ')[1]; // Asume que el token está después del 'Bearer'
    if (!token) {
        return res.status(401).json({message: 'Token not found'});
    }

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET, {algorithms: ['HS256']});
        req.userId = decoded.sub || decoded.userId;
        next();
    } catch (error) {
        res.status(401).json({message: 'Invalid token', error: error.message});
    }
};

app.get('/chat/unread-messages', ValidateTokenJWT, async (req, res) => {
    const userId = req.userId; // Asume que esto se obtiene de algún modo, por ejemplo, de un token JWT

    const query = `
        SELECT senderId, COUNT(*) as unreadCount 
        FROM messages 
        WHERE receiverId = ? AND is_read = FALSE 
        GROUP BY senderId`;

    try {
        const [rows] = await connection.promise().query(query, [userId]);
        res.json(rows);
    } catch (error) {
        console.error('Error al obtener mensajes no leídos:', error);
        res.status(500).send('Error al obtener mensajes no leídos');
    }
});

app.post('/chat/reset-unread-messages', ValidateTokenJWT, async (req, res) => {
    const senderId = req.body.senderId;
    const receiverId = req.userId;

    console.log("Resetting unread messages", {senderId, receiverId});

    const updateQuery = `
        UPDATE messages 
        SET is_read = TRUE 
        WHERE senderId = ? AND receiverId = ?`;

    try {
        const [result] = await connection.promise().query(updateQuery, [senderId, receiverId]);
        console.log("Updated rows:", result.affectedRows);
        res.send('Mensajes actualizados como leídos');
    } catch (error) {
        console.error('Error al resetear mensajes no leídos:', error);
        res.status(500).send('Error al resetear mensajes no leídos');
    }
});


const PORT = process.env.PORT || 8084;
app.listen(PORT, () => {
    console.log(`Servidor corriendo en el puerto ${PORT}`);
});