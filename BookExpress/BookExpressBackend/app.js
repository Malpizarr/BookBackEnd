require('dotenv').config({ path: require('path').resolve(__dirname, '.env') });
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const validateJWT = require('./middleware/verifyToken');
const bookRoutes = require('./routes/bookRoutes');
const { MongoClient, ServerApiVersion } = require('mongodb');
const mongoose = require('mongoose');

const app = express();


app.use(bodyParser.json());


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

        // Iniciar el servidor Express una vez que la conexión a la base de datos esté establecida
        const PORT = process.env.PORT || 3001;
        app.listen(PORT, () => {
            console.log(`Servidor corriendo en el puerto ${PORT}.`);
        });
    })
    .catch(err => {
        console.error("Failed to connect to MongoDB!", err);
        process.exit(1);
    });
module.exports = client;
