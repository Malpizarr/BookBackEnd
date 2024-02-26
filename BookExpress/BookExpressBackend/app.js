require('dotenv').config({ path: require('path').resolve(__dirname, '.env') });
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const validateJWT = require('./middleware/verifyToken');
const bookRoutes = require('./routes/bookRoutes');
const { MongoClient, ServerApiVersion } = require('mongodb');
const mongoose = require('mongoose');
const cookieParser = require('cookie-parser');
const app = express();
const {redisClient, getAsync, setAsync, connectRedis} = require('./middleware/redisClient');


connectRedis().then(() => {
    console.log("Connected to Redis");
}).catch((err) => {
    console.error("Error connecting to Redis:", err);
});


app.use(cookieParser());


app.use(bodyParser.json({limit: '2mb'}));
app.use(bodyParser.urlencoded({limit: '2mb', extended: true}));



app.use('/books', validateJWT, bookRoutes);



mongoose.connect(process.env.MONGO_URI, { useNewUrlParser: true, useUnifiedTopology: true })
    .then(() => console.log('MongoDB connected...'))
    .catch(err => console.error('MongoDB connection error:', err));


// Conexión a MongoDB
const uri = process.env.MONGO_URI;
const client = new MongoClient(uri, {
    serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true,
    }
});

client.connect()
    .then(() => {
        console.log("Successfully connected to MongoDB!");

        const PORT = process.env.PORT || 3000;
        app.listen(PORT, () => {
            console.log(`Servidor corriendo en el puerto ${PORT}.`);
        });
    })
    .catch(err => {
        console.error("Failed to connect to MongoDB!", err);
        process.exit(1);
    });

module.exports = {redisClient, client};
