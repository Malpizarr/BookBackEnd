const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
    let token;

    if (req.headers.authorization) {
        const authHeader = req.headers.authorization;
        token = authHeader.split(' ')[1]; // Asume que el token está después del 'Bearer'
    } else if (req.cookies.refreshToken) {
        token = req.cookies.refreshToken; // Intenta obtener el token de una cookie llamada 'token'
    }

    if (!token) {
        return res.status(401).json({message: 'No token provided'});
    }

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET, { algorithms: ['HS256'] });
        req.userId = decoded.sub || decoded.userId;
        next();
    } catch (error) {
        res.status(401).json({ message: 'Invalid token', error: error.message });
    }
};

module.exports = verifyToken;
