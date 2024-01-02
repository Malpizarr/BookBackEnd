//package com.example.book.Service;
//
//import com.example.book.Model.Book;
//import com.example.book.Model.Page;
//import com.example.book.Model.User;
//import com.example.book.Repositories.BookRepository;
//import com.example.book.Repositories.UserRepository;
//import com.example.book.Repositories.PageRepository;
//import jakarta.transaction.Transactional;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.Date;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
//@Service
//public class BookService {
//
//    @Autowired
//    private BookRepository bookRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PageRepository pageRepository;
//
//    public Book createBook(String username, Book book) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
//        book.setUser(user);
//
//        // Guarda el libro
//        Book savedBook = bookRepository.save(book);
//
//        // Crea una página inicial para el libro
//        Page firstPage = new Page();
//        firstPage.setContent("Contenido inicial de la página");
//        firstPage.setBook(savedBook);
//        firstPage.setCreatedAt(new Date());
//        firstPage.setPageNumber(1);
//        pageRepository.save(firstPage);
//        return savedBook;
//    }
//
//    public List<Book> getUserBooks(String username) {
//        return bookRepository.findAllByUser_Username(username);
//    }
//
//    public Set<Page> getPagesByBook(Long bookId) {
//        Optional<Book> optionalBook = bookRepository.findById(bookId);
//        if (optionalBook.isPresent()) {
//            Book book = optionalBook.get();
//            return book.getPages();
//        } else {
//            throw new RuntimeException("Libro no encontrado");
//        }
//    }
//
//    public Page moveToNextPage(Long bookId) {
//        Optional<Book> optionalBook = bookRepository.findById(bookId);
//        if (!optionalBook.isPresent()) {
//            throw new RuntimeException("Libro no encontrado");
//        }
//
//        Book book = optionalBook.get();
//        Set<Page> pages = book.getPages();
//        int currentPageNumber = book.getCurrentPage();
//
//        Optional<Page> currentPage = pages.stream()
//                .filter(page -> page.getPageNumber().equals(currentPageNumber))
//                .findFirst();
//
//        if (currentPage.isPresent()) {
//            // If current page is not the last page, return the next page
//            int nextPageNumber = currentPageNumber + 1;
//            return pages.stream()
//                    .filter(page -> page.getPageNumber().equals(nextPageNumber))
//                    .findFirst()
//                    .orElseGet(() -> createAndSaveNewPage(book, nextPageNumber));
//        } else {
//            // If current page does not exist, start from the first page
//            return createAndSaveNewPage(book, 1);
//        }
//    }
//
//    private Page createAndSaveNewPage(Book book, int pageNumber) {
//        Page newPage = new Page();
//        newPage.setBook(book);
//        newPage.setPageNumber(pageNumber);
//        newPage.setContent(""); // Set default content or use a parameter
//        newPage.setCreatedAt(new Date()); // Set creation date
//        // Optionally set updated date if required
//
//        book.getPages().add(newPage);
//        book.setCurrentPage(pageNumber);
//        bookRepository.save(book);
//        return newPage;
//    }
//
//
//    @Transactional
//    public Page createPage(Long bookId, Page newPage) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));
//
//        // Renumerar las páginas existentes primero
//        List<Page> subsequentPages = pageRepository.findAllByBookAndPageNumberGreaterThan(book, newPage.getPageNumber() - 1);
//        subsequentPages.forEach(page -> page.setPageNumber(page.getPageNumber() + 1));
//        pageRepository.saveAll(subsequentPages);
//
//        // Luego insertar la nueva página
//        newPage.setBook(book);
//        newPage.setCreatedAt(new Date());
//        return pageRepository.save(newPage);
//    }
//
//
//    public Set<Page> deletePage(Long bookId, int pageNumber) {
//        Optional<Book> optionalBook = bookRepository.findById(bookId);
//        if (optionalBook.isPresent()) {
//            Book book = optionalBook.get();
//
//            // Usando pageNumber en lugar de pageId para la comparación
//            Page pageToDelete = book.getPages().stream()
//                    .filter(page -> page.getPageNumber() == pageNumber)
//                    .findFirst()
//                    .orElse(null);
//
//            if (pageToDelete != null) {
//                int deletedPageNumber = pageToDelete.getPageNumber();
//                book.getPages().remove(pageToDelete);
//                pageRepository.delete(pageToDelete);
//
//                // Renumerar y guardar las páginas restantes
//                book.getPages().stream()
//                        .filter(page -> page.getPageNumber() > deletedPageNumber)
//                        .forEach(page -> {
//                            page.setPageNumber(page.getPageNumber() - 1);
//                            pageRepository.save(page); // Guardar cada página actualizada
//                        });
//
//                return book.getPages();
//            } else {
//                throw new RuntimeException("Página no encontrada en el libro");
//            }
//        } else {
//            throw new RuntimeException("Libro no encontrado");
//        }
//    }
//
//
//
//    public Book updateBook(Long bookId, Book bookDetails) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));
//
//        // Update book details
//        book.setTitle(bookDetails.getTitle());
//        // more fields to update
//
//        return bookRepository.save(book);
//    }
//
//    public Page updatePage(Long bookId, int pageNum, Page pageDetails) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));
//
//        Page pageToUpdate = book.getPages().stream()
//                .filter(p -> p.getPageNumber().equals(pageNum))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Página no encontrada"));
//
//        pageToUpdate.setContent(pageDetails.getContent());
//        // Otros campos a actualizar si es necesario
//
//        return pageRepository.save(pageToUpdate);
//    }
//
//
//
//    public void deleteBook(Long bookId) {
//        Optional<Book> optionalBook = bookRepository.findById(bookId);
//        if (optionalBook.isPresent()) {
//            Book book = optionalBook.get();
//            bookRepository.delete(book);
//        } else {
//            throw new RuntimeException("Libro no encontrado");
//        }
//    }
//
//
//    public Book getBookByIdAndUsername(Long bookId, String username) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));
//        if (!book.getUser().getUsername().equals(username)) {
//            throw new RuntimeException("No tienes permiso para ver este libro");
//        }
//        return book;
//    }
//
//    public Page getNextPage(Long bookId, String username) {
//        Book book = getBookByIdAndUsername(bookId, username);
//        int currentPage = book.getCurrentPage();
//        Optional<Page> nextPage = book.getPages().stream()
//                .filter(page -> page.getPageNumber() == currentPage + 1)
//                .findFirst();
//
//        nextPage.ifPresent(page -> updateCurrentPage(bookId, page.getPageNumber(), username));
//        return nextPage.orElse(null);
//    }
//
//    public Page getPreviousPage(Long bookId, String username) {
//        Book book = getBookByIdAndUsername(bookId, username);
//        int currentPage = book.getCurrentPage();
//        Optional<Page> previousPage = book.getPages().stream()
//                .filter(page -> page.getPageNumber() == currentPage - 1)
//                .findFirst();
//
//        previousPage.ifPresent(page -> updateCurrentPage(bookId, page.getPageNumber(), username));
//        return previousPage.orElse(null);
//    }
//
//    public Page getPageByNumber(Long bookId, int pageNumber, String username) {
//        Book book = getBookByIdAndUsername(bookId, username);
//        Page page = book.getPages().stream()
//                .filter(p -> p.getPageNumber() == pageNumber)
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Página no encontrada"));
//
//        updateCurrentPage(bookId, pageNumber, username);
//        return page;
//    }
//
//    public void updateCurrentPage(Long bookId, int pageNumber, String username) {
//        Book book = getBookByIdAndUsername(bookId, username);
//        book.setCurrentPage(pageNumber);
//        bookRepository.save(book);
//    }
//}
//
