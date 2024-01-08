const express = require('express');
const router = express.Router();
const bookController = require('../controllers/bookControllers'); // Asegúrate de que la ruta sea correcta
const validateJWT = require('../middleware/verifyToken');


router.post('/create', validateJWT, bookController.createBook);
router.get('/all', validateJWT, bookController.getUserBooks);
router.get('/:Id/books', validateJWT, bookController.getBookByUserId);
router.get('/:friendId/all', validateJWT, bookController.friendBooks);
router.get('/:bookId/pages', validateJWT, bookController.getPagesByBook);
router.post('/:bookId/createPage', validateJWT, bookController.createPage);
router.delete('/delete/:bookId', validateJWT, bookController.deleteBook);
router.delete('/:bookId/deletePage/:pageNumber', validateJWT, bookController.deletePage);
router.put('/update/:bookId', validateJWT, bookController.updateBook);
router.put('/:bookId/updatePage/:pageNumber', validateJWT, bookController.updatePage);
router.get('/:bookId/page/:pageNumber',validateJWT, bookController.getPageByNumber);

module.exports = router;
