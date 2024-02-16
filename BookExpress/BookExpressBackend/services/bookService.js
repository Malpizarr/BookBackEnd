

const mongoose = require('mongoose');
const Book = require('../models/book');
const Page = require('../models/page');
const {redisClient} = require('../middleware/redisClient');


// Servicio
exports.getBookById = async (bookId) => {
    if (!bookId) {
        throw new Error('Book ID is required');
    }

    try {
        // Intenta obtener el libro desde la caché de Redis
        const cachedBook = await redisClient.get(`book:${bookId}`); // Usamos 'get' para recuperar el valor
        if (cachedBook) {
            console.log('Retrieving from cache');
            return JSON.parse(cachedBook); // Devuelve el libro desde la caché
        }

        // Si el libro no está en la caché, lo busca en la base de datos
        const book = await Book.findById(bookId);
        if (!book) {
            throw new Error('Book not found');
        }

        // Almacena el libro en la caché de Redis con un tiempo de expiración
        await redisClient.setEx(`book:${bookId}`, 3600, JSON.stringify(book)); // Corrige los argumentos para 'setEx'
        console.log('Saving to cache');

        return book; // Devuelve el libro desde la base de datos
    } catch (error) {
        console.error(error);
        throw new Error('An error occurred while getting the book');
    }
};





exports.createBook = async (userId, bookData) => {
    if (!userId) {
        throw new Error('User ID required');
    }

    try {
        // Crea el nuevo libro
        const newBook = new Book({
            ...bookData,
            userId: userId,
            pages: [],
            status: "Public"
        });

        // Crea la primera página
        const firstPage = new Page({
            content: "Contenido inicial de la página",
            pageNumber: 1,
            createdAt: new Date(),
            bookId: newBook._id
        });

        // Agrega la primera página al libro
        newBook.pages.push(firstPage);

        // Guarda la primera página y el libro en la base de datos
        await firstPage.save();
        await newBook.save();

        await redisClient.del(`userBooks:${userId}`);
        await redisClient.setEx(`book:${newBook._id}`, 3600, JSON.stringify(newBook));
        await redisClient.del(`friendBooks:${userId}`);


        return newBook;
    } catch (error) {
        console.error(error);
        throw new Error('An error occurred while creating the book');
    }
};


exports.updatePage = async (bookId, pageNumber, pageDetails) => {
    try {
        console.log(`Actualizando página. Libro: ${bookId}, Página: ${pageNumber}`);

        // Asegurarse de que el pageNumber es un número
        const pageNum = parseInt(pageNumber, 10);
        if (isNaN(pageNum)) {
            throw new Error('Número de página no válido');
        }

        const book = await Book.findById(bookId);
        if (!book) {
            throw new Error('Libro no encontrado');
        }

        const pageIndex = book.pages.findIndex(p => p.pageNumber === pageNum);
        if (pageIndex === -1) {
            throw new Error('Página no encontrada');
        }

        // Actualizar el contenido de la página específica
        book.pages[pageIndex].content = pageDetails.content;

        // Guardar los cambios en el libro
        await book.save();

        // Actualizar la caché de Redis para este libro
        await redisClient.del(`book:${bookId}:pages`);

        const pageNumUpdate = parseInt(pageNum, 10);
        const cacheKey = `book:${bookId}:page:${pageNumUpdate}`;


        await redisClient.setEx(cacheKey, 3600, JSON.stringify(book.pages[pageIndex].toObject()));
        await redisClient.del(`book:${bookId}`);

        console.log(`Page ${pageNum} updated in book: ${bookId} and cache refreshed`);

        return book.pages[pageIndex];
    } catch (error) {
        console.error('Error al actualizar la página:', error);
        throw error;
    }
};


exports.getBookByUserId = async (userId) => {
    try {
        const books = await Book.find({ userId: userId, status: "Public" });
        return books;
    } catch (error) {
        console.error('Error en bookService.getBookByUserId:', error);
        throw error; // Lanza el error para manejarlo en el controlador
    }
};


exports.getFriendIds = async (userId, authorizationHeader) => {
    try {
        const response = await fetch(`https://bookfriendship-de865bd88c8f.herokuapp.com/api/friendships/friends`, {
            headers: { 'Authorization': authorizationHeader }
        });
        const friendsData = await response.json();

        return friendsData.map(friend => friend.friendId);
    } catch (error) {
        console.error('Error al obtener los IDs de los amigos:', error);
        throw error;
    }
};

exports.getFriendsBooks = async (friendIds, authorizationHeader) => {
    try {
        const uniqueFriendIds = new Set(friendIds);

        const booksPromises = Array.from(uniqueFriendIds).map(async friendId => {
            const cacheKey = `friendBooks:${friendId}`;

            const cachedBooks = await redisClient.get(cacheKey);
            if (cachedBooks) {
                console.log(`Retrieving books from cache for friend: ${friendId}`);
                return JSON.parse(cachedBooks);
            }

            const username = await getUsernameById(friendId, authorizationHeader);

            const books = await Book.find({userId: friendId, status: "Public"}).lean();
            console.log(books)

            const booksWithUsername = books.map(book => ({...book, username}));

            await redisClient.setEx(cacheKey, 3600, JSON.stringify(booksWithUsername));

            return booksWithUsername;
        });

        const booksResults = await Promise.all(booksPromises);
        return booksResults.flat(); // Aplanar el array de arrays de libros
    } catch (error) {
        console.error('Error al obtener libros de amigos:', error);
        throw error;
    }
};



const getUsernameById = async (userId, authorizationHeader) => {
    try {
        const response = await fetch(`https://bookauth-c0fd8fb7a366.herokuapp.com/users/${userId}`, {
            headers: { 'Authorization': authorizationHeader }
        });
        const userData = await response.json();
        return userData.username;
    } catch (error) {
        console.error('Error al obtener el nombre de usuario:', error);
        return '';
    }
};





// En tu servicio
exports.getUserBooks = async (userId) => {
    if (!userId) {
        throw new Error('User ID is required');
    }

    try {
        // Intenta obtener los libros del usuario desde la caché de Redis
        const cachedBooks = await redisClient.get(`userBooks:${userId}`);
        if (cachedBooks) {
            console.log('Retrieving books from cache for user:', userId);
            return JSON.parse(cachedBooks);  // Devuelve los libros desde la caché
        }

        const books = await Book.find({ userId }).populate('pages');
        if (!books || books.length === 0) {
            return [];  // Devuelve un arreglo vacío si no hay libros
        }

        await redisClient.setEx(`userBooks:${userId}`, 3600, JSON.stringify(books));
        console.log('Saving books to cache for user:', userId);

        return books;  // Devuelve los libros desde la base de datos
    } catch (error) {
        console.error(error);
        throw new Error('An error occurred while getting the user books');
    }
};


exports.getPageByNumber = async (bookId, pageNumber) => {
    if (!bookId || !pageNumber) {
        throw new Error('Book ID and Page Number are required');
    }

    const pageNum = parseInt(pageNumber, 10);
    const cacheKey = `book:${bookId}:page:${pageNum}`;

    try {
        // Intenta obtener la página desde la caché de Redis
        const cachedPage = await redisClient.get(cacheKey);
        if (cachedPage) {
            console.log(`Retrieving page ${pageNum} from cache for book: ${bookId}`);
            return JSON.parse(cachedPage);  // Devuelve la página desde la caché
        }

        const book = await Book.findById(bookId); // Busca el libro en la base de datos
        if (!book) throw new Error('Book not found');

        // Encuentra la página específica por su número directamente en las páginas del libro
        const page = book.pages.find(p => p.pageNumber === pageNum);
        if (!page) {
            console.log(`Page not found in book ${bookId} for page number ${pageNum}`);
            throw new Error('Page not found');
        }


        await redisClient.setEx(cacheKey, 3600, JSON.stringify(page));
        console.log(`Saving page ${pageNum} to cache for book: ${bookId}`);

        return page;  // Devuelve la página desde la base de datos
    } catch (error) {
        console.error(error);
        throw new Error('An error occurred while getting the page');
    }
};






exports.getBookByIdAndUserId = async (bookId, userId) => {
    const book = await Book.findOne({ _id: bookId, userId }).populate('pages');
    if (!book) throw new Error('Book not found');

    return book;
};

exports.getPagesByBook = async (bookId) => {
    const cacheKey = `book:${bookId}:pages`;

    try {
        // Intenta obtener las páginas del libro desde la caché de Redis
        const cachedPages = await redisClient.get(cacheKey);
        if (cachedPages) {
            console.log(`Retrieving pages from cache for book: ${bookId}`);
            return JSON.parse(cachedPages);  // Devuelve las páginas desde la caché
        }

        const book = await Book.findById(bookId).populate('pages');
        if (!book) {
            console.log(`Book not found with ID: ${bookId}`);
            throw new Error('Book not found');
        }

        if (book.pages.length === 0) {
            console.log(`No pages for the book with ID: ${bookId}`);
            // Maneja el caso de un libro sin páginas
            return [];
        }

        // Almacena las páginas en la caché de Redis con un tiempo de expiración
        await redisClient.setEx(cacheKey, 3600, JSON.stringify(book.pages));
        console.log(`Saving pages to cache for book: ${bookId}`);

        return book.pages;  // Devuelve las páginas desde la base de datos
    } catch (error) {
        console.error(error);
        throw new Error('An error occurred while getting the pages');
    }
};




exports.moveToNextPage = async (bookId) => {
    const book = await Book.findOne({ _id: bookId }).populate('pages');
    if (!book) throw new Error('Book not found');

    const currentPage = book.pages.find(page => page.current);
    if (!currentPage) throw new Error('No current page found');

    const nextPageIndex = book.pages.findIndex(page => page._id.toString() === currentPage._id.toString()) + 1;
    if (nextPageIndex >= book.pages.length) throw new Error('No next page found');

    const nextPage = book.pages[nextPageIndex];
    nextPage.current = true;
    currentPage.current = false;
    await nextPage.save();
    await currentPage.save();

    return nextPage;
};
exports.createPage = async (bookId, page) => {
    try {
        const book = await Book.findById(bookId);
        if (!book) throw new Error('Libro no encontrado');

        // Determinar el número para la nueva página
        const lastPage = book.pages[book.pages.length - 1];
        const newPageNumber = lastPage ? lastPage.pageNumber + 1 : 1;

        // Crear la nueva página con el número de página asignado
        const newPage = new Page({ ...page, pageNumber: newPageNumber, bookId });
        await newPage.save();

        // Añadir la nueva página al libro
        book.pages.push(newPage);
        await book.save();

        const pageNum = parseInt(newPageNumber, 10);
        const cacheKey = `book:${bookId}:page:${pageNum}`;
        await redisClient.setEx(cacheKey, 3600, JSON.stringify(newPage));
        await redisClient.setEx(`book:${bookId}`, 3600, JSON.stringify(book));
        await redisClient.del(`book:${bookId}:pages`);

        return newPage;
    } catch (error) {
        console.error('Error al crear la página:', error);
        throw error;
    }
};


exports.deleteBook = async (bookId, userId) => {
    try {
        const result = await Book.deleteOne({_id: bookId});
        if (result.deletedCount === 0) {
            throw new Error('Book not found');
        }

        await redisClient.del(`book:${bookId}`);

        await redisClient.del(`userBooks:${userId}`); // Invalida la caché de los libros del usuario

        await redisClient.del(`friendBooks:${userId}`);

        console.log(`Book with ID ${bookId} deleted and cache invalidated`);


        return {message: 'Book deleted successfully'};
    } catch (error) {
        console.error(error);
        throw new Error('An error occurred while deleting the book');
    }
};


exports.deletePage = async (bookId, pageNumber) => {
    try {
        console.log(`Eliminando página. Libro: ${bookId}, Página: ${pageNumber}`);

        const pageNum = parseInt(pageNumber, 10);
        if (isNaN(pageNum)) {
            throw new Error('Número de página no válido');
        }

        const book = await Book.findById(bookId);
        if (!book) {
            throw new Error('Libro no encontrado');
        }

        const pageIndex = book.pages.findIndex(p => p.pageNumber === pageNum);
        if (pageIndex === -1) {
            throw new Error('Página no encontrada');
        }

        book.pages.splice(pageIndex, 1);
        await book.save();

        await redisClient.del(`book:${bookId}`);
        await redisClient.del(`book:${bookId}:pages`);

        return book.pages;
    } catch (error) {
        console.error('Error al eliminar la página:', error);
        throw error;
    }
};


exports.updateBook = async (bookId, updates, userId, auth) => {
    const book = await Book.findOne({ _id: bookId });
    if (!book) throw new Error('Book not found');

    Object.assign(book, updates);
    await book.save();

    await redisClient.del(`book:${bookId}`);

    await redisClient.del(`userBooks:${userId}`);

    await redisClient.del(`friendBooks:${userId}`);

    return book;
};
