require('dotenv').config({path: require('path').resolve(__dirname, '.env')});
const WebSocket = require('ws');
const axios = require('axios');
const jwt = require('jsonwebtoken');
const mysql = require('mysql2');
const fs = require('fs');


const API_FRIENDSHIP_URL = 'http://localhost:8082/api/friendships/areFriends';

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

        // Maneja la solicitud de lista de amigos en línea
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
            const response = await axios.get(`https://bookgateway.mangotree-fab2eccd.eastus.azurecontainerapps.io/api/friendships/friends`, {
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

function esMensajeDeHandshake(message) {
    try {
        const msg = JSON.parse(message);
        return msg.type && msg.type === 'handshake';
    } catch (error) {
        return false;
    }
}

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
    console.log("Decoded JWT:", decoded);
    return decoded.sub;
}


async function areUsersFriends(userId1, userId2, token) {
    try {
        const response = await axios.get(`${API_FRIENDSHIP_URL}/${userId1}/${userId2}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        return response.data;
    } catch (error) {
        console.error('Error verifying friendship:', error);
        console.log('Error response:', error.response);
        return false;
    }
}

// Función para obtener mensajes de chat
function getChatMessages(senderId, receiverId, callback) {
    const query = `
        SELECT *
        FROM messages
        WHERE (senderId = ? AND receiverId = ?)
           OR (senderId = ? AND receiverId = ?)
        ORDER BY timestamp ASC`;

    connection.query(query, [senderId, receiverId, receiverId, senderId], (error, results) => {
        if (error) {
            return callback(error, null);
        }
        // Formatear los mensajes para el cliente
        const formattedMessages = results.map(msg => {
            return {
                senderId: msg.senderId,
                content: msg.message
            };
        });
        callback(null, formattedMessages);
    });
}

function sendMessage(senderId, receiverId, messageContent) {
    const insertQuery = 'INSERT INTO messages (senderId, receiverId, message) VALUES (?, ?, ?)';
    connection.query(insertQuery, [senderId, receiverId, messageContent], (insertError, insertResults) => {
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
                    id: insertResults.insertId
                });
                receiverWs.send(messageToSend);
            }
        }
    });
}




