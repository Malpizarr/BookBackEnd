const WebSocket = require('ws');
const readline = require('readline');

const token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2ZGQ3NmQ1My1iM2JjLTRkNGUtYTg2My05ODAyNjhmNTIxZTAiLCJpYXQiOjE3MDUyNTc4NTcsImV4cCI6MTcwNTI2MTQ1N30.Fv0DsvdL35UdcgzlDnn0r2j7YTpAQLMK5rZZcXawUec'; // Reemplaza con tu token JWT
const ws = new WebSocket('ws://localhost:8080', token);

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

let otherUserId;

ws.on('open', function open() {
    console.log('Conectado al servidor');

    // Solicitar al usuario el ID del otro usuario para el chat
    rl.question('Ingresa el ID del otro usuario: ', (answer) => {
        otherUserId = answer;

        // Enviar mensaje de handshake al servidor con el otherUserId
        ws.send(JSON.stringify({type: 'handshake', otherUserId: otherUserId}));

        // Espera y envía mensajes desde la línea de comandos
        rl.on('line', (line) => {
            ws.send(JSON.stringify({receiverId: otherUserId, message: line}));
        });
    });
});

ws.on('message', function incoming(data) {
    const message = JSON.parse(data.toString());
    if (message.senderId) {
        console.log(`Mensaje de ${message.senderId}: ${message.message}`);
    } else {
        console.log('Mensaje recibido:', message.message);
    }
});

ws.on('close', function close() {
    console.log('Desconectado del servidor');
    rl.close();
});

ws.on('error', function error(err) {
    console.error('Error en la conexión:', err);
});

