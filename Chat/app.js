require('dotenv').config({path: require('path').resolve(__dirname, '.env')});
const WebSocket = require('ws');
const axios = require('axios');
const jwt = require('jsonwebtoken');
const mysql = require('mysql2');
const fs = require('fs');
const express = require('express');
const {json} = require("express");

const app = express();

app.use(json());

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


const wss = new WebSocket.Server({port: 8083});
const connectedUsers = new Map();

// Configuración de la conexión a la base de datos, esta utilizando .env
const connection = mysql.createConnection({
    host: process.env.DATABASE_URI,
    user: process.env.DATABASE_USER,
    password: process.env.DATABASE_PASSWORD,
    database: process.env.DATABASE_NAME,
    ssl: {
        rejectUnauthorized: false // Permite certificados no autorizados
    }
});


// Conectar a la base de datos
connection.connect(error => {
    if (error) throw error;
    console.log('Conectado exitosamente a la base de datos.');
});


wss.on('connection', function connection(ws, req) {
    const token = req.headers['sec-websocket-protocol'];
    if (!token || !validateJwt(token)) {
        ws.close(1008, "Invalid Token");
        return;
    }

    const userId = getUserIdFromJwt(token);
    connectedUsers.set(userId, ws);
    console.log('Usuario conectado:', userId);

    ws.on('message', async function incoming(message) {
        const msg = JSON.parse(message);

        if (msg.type !== 'friendsListRequest') {
            console.log('Mensaje recibido:', msg.type);
        }
        if (msg.type === 'friendsListRequest') {

            const friendsList = await getFriendsList(userId, token);
            if (friendsList) {
                const onlineFriends = friendsList.filter(friendId => connectedUsers.has(friendId));
                ws.send(JSON.stringify({type: 'friendsList', friends: onlineFriends}));
            }
        } else {
            try {
                const msg = JSON.parse(message);
                if (msg.type === 'sendMessage') {
                    sendMessage(userId, msg.receiverId, msg.content);
                }
            } catch (error) {
                console.error('Error handling message:', error);
            }


            // Nueva condición para manejar solicitudes de chat
            if (msg.type === 'chatRequest') {
                getChatMessages(userId, msg.friendId, (error, chatMessages) => {
                    if (error) {
                        console.error('Error al obtener mensajes de chat:', error);
                        return;
                    }
                    ws.send(JSON.stringify({type: 'chatMessages', messages: chatMessages}));
                });
            }

            if (msg.type === 'forcedUpdatelist') {
                const friendWs = connectedUsers.get(msg.friendId);
                if (friendWs && friendWs.readyState === WebSocket.OPEN) {
                    const friendsList = await getFriendsListForced(msg.friendId, token);
                    const onlineFriends = friendsList.filter(friendId => connectedUsers.has(friendId));
                    friendWs.send(JSON.stringify({type: 'friendsList', friends: onlineFriends}));
                }
            }

            if (msg.type === 'acceptedFriendRequest') {

                // Mensaje para informar que la solicitud de amistad ha sido aceptada
                const acceptanceMessage = JSON.stringify({
                    type: 'friendshipAccepted',
                    userId: userId, // ID del usuario que acepta la solicitud
                    friendId: msg.friendId, // ID del amigo que envió la solicitud
                });

                // Envía el mensaje de aceptación al usuario que aceptó la solicitud
                if (ws.readyState === WebSocket.OPEN) {
                    ws.send(acceptanceMessage);
                }

                // Envía el mensaje de aceptación al usuario que envió la solicitud
                const friendWs = connectedUsers.get(msg.friendId);
                if (friendWs && friendWs.readyState === WebSocket.OPEN) {
                    friendWs.send(acceptanceMessage);
                }
            }


            if (msg.type === 'friendRequest') {
                // Verifica si el WebSocket del usuario actual está abierto antes de enviar

                console.log(ws.readyState === WebSocket.OPEN);
                if (ws.readyState === WebSocket.OPEN) {
                    ws.send(JSON.stringify({type: 'friendshipRequested', userId, friendId: msg.friendId}));
                }

                const friendWs = connectedUsers.get(msg.friendId);

                if (friendWs && friendWs.readyState === WebSocket.OPEN) {
                    friendWs.send(JSON.stringify({type: 'friendshipRequested', userId, friendId: msg.friendId}));
                } else {
                    console.log(`WebSocket para el amigo con ID ${msg.friendId} no está disponible o abierto.`);
                }

            }

            if (msg.type === 'deletedFriend') {
                const Message = JSON.stringify({
                    type: 'friendshipDeleted',
                    userId: userId, // ID del usuario que acepta la solicitud
                    friendId: msg.friendId, // ID del amigo que envió la solicitud
                });

                // Envía el mensaje de aceptación al usuario que aceptó la solicitud
                if (ws.readyState === WebSocket.OPEN) {
                    ws.send(Message);
                }

                // Envía el mensaje de aceptación al usuario que envió la solicitud
                const friendWs = connectedUsers.get(msg.friendId);
                if (friendWs && friendWs.readyState === WebSocket.OPEN) {
                    friendWs.send(Message);
                }
            }


            // Nueva funcionalidad para manejar la solicitud de lista de amigos en línea
            if (esSolicitudListaAmigos(message)) {
                const friendsList = await getFriendsList(userId, token);

                // Verificar si friendsList es undefined antes de proceder
                if (!friendsList) {
                    console.error('friendsList es undefined');
                    return;
                }

                const onlineFriends = friendsList.filter(friendId => connectedUsers.has(friendId));
                ws.send(JSON.stringify({type: 'friendsList', friends: onlineFriends}));
            }
        }
    });

    let friendsListCache = {};

    async function getFriendsList(userId, token) {
        // Verifica si la lista de amigos ya está en caché
        if (friendsListCache[userId]) {
            return friendsListCache[userId];
        }

        try {
            // Realiza la consulta HTTP
            const response = await axios.get(`http://localhost:8081/api/friendships/friends`, {
                headers: {'Authorization': `Bearer ${token}`}
            });

            // Guarda en caché y devuelve los resultados
            friendsListCache[userId] = response.data.map(friendship => friendship.friendId);
            return friendsListCache[userId];
        } catch (error) {
            console.error('Error retrieving friends list:', error);
            return [];
        }
    }

    async function getFriendsListForced(userId, token) {
        try {
            // Realiza la consulta HTTP
            const response = await axios.get(`http://localhost:8081/api/friendships/friends`, {
                headers: {'Authorization': `Bearer ${token}`}
            });
            friendsListCache[userId] = response.data.map(friendship => friendship.friendId);
            return friendsListCache[userId];
        } catch (error) {
            console.error('Error retrieving friends list:', error);
            return [];
        }
    }

    setInterval(() => {
        friendsListCache = {};
    }, 1000 * 60 * 60); // Limpia el caché cada hora, ajustar según necesidad


    function esSolicitudListaAmigos(message) {
        try {
            const msg = JSON.parse(message);
            return msg.type && msg.type === 'friendsListRequest';
        } catch (error) {
            return false;
        }
    }

    ws.on('close', function close() {
        connectedUsers.delete(userId);
        console.log('Usuario desconectado:', userId);
    });
});


function validateJwt(token) {
    try {
        jwt.verify(token, process.env.JWT_SECRET);
        return true;
    } catch (error) {
        return false;
    }
}

function getUserIdFromJwt(token) {
    const decoded = jwt.decode(token);
    return decoded.sub;
}


// Función para obtener mensajes de chat
function getChatMessages(senderId, receiverId, callback) {
    const query = `
        SELECT senderId, receiverId, message, timestamp
        FROM messages
        WHERE (senderId = ?
          AND receiverId = ?)
           OR (senderId = ?
          AND receiverId = ?)`

    connection.query(query, [senderId, receiverId, receiverId, senderId], (error, results) => {
        if (error) {
            return callback(error, null);
        }

        const formattedMessages = results.map(msg => {
            const utcDate = new Date(msg.timestamp); // Considera el timestamp como UTC

            const date = utcDate.toDateString(); // Agrega la fecha al mensaje
            const hours = utcDate.getHours() > 12 ? utcDate.getHours() - 12 : utcDate.getHours();
            const amPm = utcDate.getHours() >= 12 ? 'PM' : 'AM';
            const minutes = utcDate.getMinutes() < 10 ? '0' + utcDate.getMinutes() : utcDate.getMinutes();
            const timestampFormatted = hours + ':' + minutes + ' ' + amPm; // Formato '6:45 PM'

            return {
                senderId: msg.senderId,
                receiverId: msg.receiverId,
                content: msg.message,
                timestamp: timestampFormatted,
                date: date // Agrega la fecha al mensaje
            };
        });

        callback(null, formattedMessages);
    });
}


function sendMessage(senderId, receiverId, messageContent) {
    const now = new Date();

    // Formatear para MySQL ('YYYY-MM-DD HH:MM:SS')
    const timestampMySQL = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0') + ' ' +
        String(now.getHours()).padStart(2, '0') + ':' +
        String(now.getMinutes()).padStart(2, '0') + ':' +
        String(now.getSeconds()).padStart(2, '0');

    // Formatear para visualización amigable ('HH:MM AM/PM')
    let hours = now.getHours();
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12; // La hora '0' debe ser '12'
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const timestamp = hours + ':' + minutes + ' ' + ampm;

    const timestampISO = new Date().toISOString();


    // Ajusta la hora a la zona horaria local de Costa Rica (UTC-6)
    const insertQuery = 'INSERT INTO messages (senderId, receiverId, message, timestamp, is_read) VALUES (?, ?, ?, ?, FALSE)';
    connection.query(insertQuery, [senderId, receiverId, messageContent, timestampMySQL], (insertError, insertResults) => {
        if (insertError) {
            console.error('Error al guardar el mensaje:', insertError);
            return;
        }
        console.log('Mensaje guardado:', insertResults.insertId);


        if (connectedUsers.has(receiverId)) {
            const receiverWs = connectedUsers.get(receiverId);
            if (receiverWs) {
                const messageToSend = JSON.stringify({
                    type: 'message',
                    senderId: senderId,
                    content: messageContent,
                    id: insertResults.insertId,
                    timestamp: timestampISO, // Envía el timestamp formateado
                    date: now.toDateString() // Envía la fecha
                });
                receiverWs.send(messageToSend);

                // Envía un mensaje de tipo 'newUnreadMessage' al destinatario
                const unreadMessage = JSON.stringify({
                    type: 'newUnreadMessage',
                    senderId: senderId
                });
                receiverWs.send(unreadMessage);
            }
        }
    });
}







