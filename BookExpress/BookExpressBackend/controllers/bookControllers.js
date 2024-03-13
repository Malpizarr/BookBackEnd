


const Book = require('../models/book');
const Page = require('../models/page');
const bookService = require('../services/bookService');

exports.createBook = async (req, res) => {
    try {
        const bookData = req.body; // Asegúrate de que esto contiene los datos del libro
        const userId = req.userId; // Usa req.userId en lugar de req.sub


        if (!userId || !bookData) {
            throw new Error("User ID and book data are required");
        }

        const book = await bookService.createBook(userId, bookData); // Pasa userId y bookData
        res.status(200).send(book);
    } catch (error) {
        console.error(error);
        res.status(500).send({ message: error.message });
    }
};

exports.getBookById = async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const book = await bookService.getBookById(bookId);
        const userId = req.userId;

        console.log("User:", userId, "Book:", book);

        if (!book) {
            return res.status(404).send({message: 'Book not found'});
        }

        const isOwner = book.userId === userId;

        if (book.status === 'Private' && !isOwner) {
            return res.status(403).send({message: 'Access denied. This book is private and you are not the owner.'});
        }

        if (!isOwner) {
            const viewBook = await bookService.viewBookById(bookId);
            if (!viewBook) {
                return res.status(404).send({message: 'Book not found'});
            }
            return res.status(200).send(viewBook);
        }

        return res.status(200).send(book);
    } catch (error) {
        console.error(error);
        return res.status(500).send({message: error.message});
    }
};



exports.getBookByUserId = async (req, res) => {
    try {
        const userId = req.params.Id;
        const books = await bookService.getBookByUserId(userId);
        if (!books || books.length === 0) {
            return res.status(404).send({ message: 'No books found for this user.' });
        }
        res.status(200).send(books);
    } catch (error) {
        res.status(500).send({ message: error.message });
    }
};

exports.getUserBooks = async (req, res) => {
    try {
        const books = await bookService.getUserBooks(req.userId);
        if (!books || books.length === 0) {
            return res.status(200).send(books);
        }
        res.status(200).send(books);
    } catch (error) {
        res.status(500).send({ message: error.message });
    }
};




exports.getPagesByBook = async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const pages = await bookService.getPagesByBook(bookId);
        res.status(200).json(pages);
    } catch (error) {
        res.status(404).json({ message: error.message });
    }
};


exports.moveToNextPage = async (req, res) => {
    try {
        const page = await bookService.moveToNextPage(req.params.bookId);
        res.status(200).send(page);
    } catch (error) {
        res.status(404).send({ message: error.message });
    }
};

exports.createPage = async (req, res) => {
    try {
        const pageData = req.body;
        const page = await bookService.createPage(req.params.bookId, pageData);
        res.status(200).send(page);
    } catch (error) {
        res.status(500).send({ message: error.message });
    }
};

exports.deleteBook = async (req, res) => {
    try {
        await bookService.deleteBook(req.params.bookId, req.userId);
        res.status(200).send({ message: 'Book deleted successfully' });
    } catch (error) {
        res.status(404).send({ message: error.message });
    }
};

exports.deletePage = async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const pageNumber = req.params.pageNumber;
        const updatedPages = await bookService.deletePage(bookId, pageNumber);

        res.status(200).send(updatedPages);
    } catch (error) {
        console.error(`Error en controlador deletePage: ${error.message}`);
        res.status(404).send({ message: error.message });
    }
};


exports.updateBook = async (req, res) => {
    try {
        const authorizationHeader = req.headers.authorization;
        const updatedBook = await bookService.updateBook(req.params.bookId, req.body, req.userId, authorizationHeader);
        res.status(200).send(updatedBook);
    } catch (error) {
        console.error("Error in updateBook:", error);
        res.status(404).send({ message: error.message });
    }
};


exports.updatePage = async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const pageNumber = req.params.pageNumber;
        const updatedPage = await bookService.updatePage(bookId, pageNumber, req.body);

        res.status(200).send(updatedPage);
    } catch (error) {
        console.error(`Error en controlador updatePage: ${error.message}`);
        res.status(404).send({ message: error.message });
    }
};


exports.getNextPage = async (req, res) => {
    try {
        const nextPage = await bookService.getNextPage(req.params.bookId, req.params.username);
        res.status(200).send(nextPage);
    } catch (error) {
        res.status(404).send({ message: error.message });
    }
};

exports.getPreviousPage = async (req, res) => {
    try {
        const previousPage = await bookService.getPreviousPage(req.params.bookId, req.params.username);
        res.status(200).send(previousPage);
    } catch (error) {
        res.status(404).send({ message: error.message });
    }
};

exports.getPageByNumber = async (req, res) => {
    try {
        const page = await bookService.getPageByNumber(req.params.bookId, req.params.pageNumber);
        res.status(200).send(page);
    } catch (error) {
        res.status(404).send({ message: error.message });
    }
};

exports.getAllFriendsBooks = async (req, res) => {
    try {
        const userId = req.userId; // Obtenido del JWT
        const authorizationHeader = req.headers.authorization;

        const friendIds = await bookService.getFriendIds(userId, authorizationHeader);

        const allBooks = await bookService.getFriendsBooks(friendIds, authorizationHeader);

        res.status(200).send(allBooks);
    } catch (error) {
        res.status(500).send({message: error.message});
    }
};

exports.friendBooks = async (req, res) => {
    const userId = req.userId;
    const authorizationHeader = req.headers.authorization;

    try {
        const friendIds = await bookService.getFriendIds(userId, authorizationHeader);
        const allBooks = await bookService.getFriendsBooks(friendIds, authorizationHeader);
        res.status(200).send(allBooks);
    } catch (error) {
        res.status(500).send({ message: error.message });
    }
};

exports.addUsers = async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const userId = req.body.userId;
        const book = await bookService.addUsers(bookId, userId);
        res.status(200).send(book);
    } catch (error) {
        res.status(500).send({ message: error.message });
    }
}

exports.editBookByToken = async (req, res) => {
    try {
        const { editToken } = req.params;
        const userId = req.userId;
        let book = await Book.findOne({ editToken });

        if (!book) {
            return res.status(404).send('Libro no encontrado.');
        }

        book = await bookService.addUsers(book._id, userId);

        res.status(200).json(book);
    } catch (err) {
        console.error(err);
        res.status(500).send('Error del servidor.');
    }
};


exports.viewBookById = async (req, res) => {
    try {
        const bookId = req.params.bookId;
        const book = await bookService.getBookById(bookId);

        const userId = req.userId;

        console.log("User:", userId + "Book:", book);

        if (!book) {
            return res.status(404).send({message: 'Book not found'});
        }

        const isOwner = book.userId === userId;


        if (book.status === 'Private' && !isOwner) {
            return res.status(403).send({message: 'Access denied. This book is private and you are not the owner.'});
        }

        res.status(200).send(book);
    } catch (error) {
        res.status(500).send({message: error.message});
    }
};

