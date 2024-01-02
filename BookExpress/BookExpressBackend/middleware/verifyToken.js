const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
        return res.status(401).json({ message: 'No token provided' });
    }

    const token = authHeader.split(' ')[1]; // Asume que el token está después del 'Bearer'
    if (!token) {
        return res.status(401).json({ message: 'Token not found' });
    }

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET, { algorithms: ['HS256'] });

        // Asegúrate de que el token contenga el campo necesario (por ejemplo, 'sub' o 'userId')
        if (!decoded.sub && !decoded.userId) {
            return res.status(401).json({ message: 'Invalid token structure' });
        }

        // Asigna el userId basado en la estructura de tu token JWT
        req.userId = decoded.sub || decoded.userId;

        next();
    } catch (error) {
        res.status(401).json({ message: 'Invalid token', error: error.message });
    }
};

module.exports = verifyToken;
