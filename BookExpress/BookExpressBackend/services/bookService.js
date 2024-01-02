


const mongoose = require('mongoose');
const Book = require('../models/book');
const Page = require('../models/page');


exports.createBook = async (userId, bookData) => {
    if (!userId) {
        throw new Error('User ID required');
    }

    try {
        // Crear el nuevo libro
        const newBook = new Book({
            ...bookData,
            userId: userId,
            pages: [] // Inicializa pages como un arreglo vacío
        });

        // Crear la primera página
        const firstPage = new Page({
            content: "Contenido inicial de la página",
            pageNumber: 1,
            createdAt: new Date(),
            bookId: newBook._id // Relaciona la página con el libro
        });

        // Agregar la primera página al libro
        newBook.pages.push(firstPage);

        // Guardar la primera página y el libro
        await firstPage.save();
        await newBook.save();

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

        book.pages[pageIndex].content = pageDetails.content;
        // ...otros campos a actualizar...

        await book.save();

        return book.pages[pageIndex];
    } catch (error) {
        console.error('Error al actualizar la página:', error);
        throw error;
    }
};



// En tu servicio
exports.getUserBooks = async (userId) => {
    if (!userId) {
        throw new Error('User ID is required');
    }

    try {
        const books = await Book.find({ userId }).populate('pages');
        return books;
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

    try {
        const book = await Book.findById(bookId); // Simplemente busca el libro
        if (!book) throw new Error('Book not found');

        // Encontrar la página específica por su número directamente en las páginas del libro
        const page = book.pages.find(p => p.pageNumber === pageNum);
        if (!page) {
            console.log(`Page not found in book ${bookId} for page number ${pageNum}`);
            throw new Error('Page not found');
        }

        return page;
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
    const book = await Book.findById(bookId);
    if (!book) {
        console.log(`Libro no encontrado con ID: ${bookId}`);
        throw new Error('Book not found');
    }

    if (book.pages.length === 0) {
        console.log(`No hay páginas para el libro con ID: ${bookId}`);
        // Maneja el caso de un libro sin páginas
    }

    return book.pages;
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

        return newPage;
    } catch (error) {
        console.error('Error al crear la página:', error);
        throw error;
    }
};


exports.deleteBook = async (bookId) => {
    const result = await Book.deleteOne({ _id: bookId });
    if (result.deletedCount === 0) {
        throw new Error('Book not found');
    }
};



exports.deletePage = async (bookId, pageNumber) => {
    try {
        console.log(`Eliminando página. Libro: ${bookId}, Página: ${pageNumber}`);

        // Convertir pageNumber a un número
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

        // Eliminar la página del arreglo de páginas
        book.pages.splice(pageIndex, 1);
        await book.save();

        return book.pages;
    } catch (error) {
        console.error('Error al eliminar la página:', error);
        throw error;
    }
};


exports.updateBook = async (bookId, updates) => {
    const book = await Book.findOne({ _id: bookId });
    if (!book) throw new Error('Book not found');

    Object.assign(book, updates);
    await book.save();

    return book;
};