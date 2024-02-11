const redis = require('redis');


// Crea el cliente de Redis
const redisClient = redis.createClient({
    password: process.env.REDIS_PASSWORD, // Contraseña de Redis
    socket: {
        port: process.env.REDIS_PORT, // Puerto de Redis
        host: process.env.REDIS_HOST,
    }
});

// Escucha errores en la conexión de Redis
redisClient.on('error', (err) => console.log('Redis Client Error', err));


// Conecta el cliente de Redis antes de exportarlo
const connectRedis = async () => {
    await redisClient.connect();
};

module.exports = {redisClient, connectRedis};
