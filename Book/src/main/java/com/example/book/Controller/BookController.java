//package com.example.book.Controller;
//
//import com.example.book.Model.Book;
//import com.example.book.Model.Page;
//import com.example.book.Service.BookService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Set;
//
//@RestController
//@RequestMapping("/books")
//public class BookController {
//
//    @Autowired
//    private BookService bookService;
//
//    // Crear un nuevo libro
//    @PostMapping("/create/{username}")
//    public ResponseEntity<?> createBook(@PathVariable String username, @RequestBody Book book) {
//        try {
//            bookService.createBook(username, book);
//            return ResponseEntity.ok(null);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//
//        }
//    }
//
//    @GetMapping("/{username}/all")
//    public ResponseEntity<?> getUserBooks(@PathVariable String username) {
//        try {
//            return ResponseEntity.ok(bookService.getUserBooks(username));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @GetMapping("/{username}/{bookId}")
//    public ResponseEntity<Book> getBookById(@PathVariable Long bookId, @PathVariable String username) {
//        try {
//            Book book = bookService.getBookByIdAndUsername(bookId, username);
//            return ResponseEntity.ok(book);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//
//
//    @GetMapping("/{bookId}/pages")
//    public ResponseEntity<Set<Page>> getPagesByBook(@PathVariable Long bookId) {
//        try {
//            Set<Page> pages = bookService.getPagesByBook(bookId);
//            return ResponseEntity.ok(pages);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @PostMapping("/{bookId}/nextPage")
//    public ResponseEntity<Page> nextPage(@PathVariable Long bookId) {
//        try {
//            Page Pages = bookService.moveToNextPage(bookId);
//            return ResponseEntity.ok(Pages);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @PostMapping("/{bookId}/createPage")
//    public ResponseEntity<Page> createPage(@PathVariable Long bookId, @RequestBody Page page) {
//        try {
//            Page newPage = bookService.createPage(bookId, page);
//            return ResponseEntity.ok(newPage);
//        } catch (Exception e) {
//            e.printStackTrace(); // Agrega un log para depuración
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    // Eliminar un libro
//    @DeleteMapping("/delete/{bookId}")
//    public ResponseEntity<?> deleteBook(@PathVariable Long bookId) {
//        try {
//            bookService.deleteBook(bookId);
//            return ResponseEntity.ok(null);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//
//    // Eliminar una página
//    @DeleteMapping("/{bookId}/deletePage/{pageNumber}")
//    public ResponseEntity<?> deletePage(@PathVariable Long bookId, @PathVariable int pageNumber) {
//        try {
//            Set<Page> updatedPages = bookService.deletePage(bookId, pageNumber);
//            return ResponseEntity.ok(updatedPages);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//        }
//    }
//
//    @PutMapping("/update/{bookId}")
//    public ResponseEntity<Book> updateBook(@PathVariable Long bookId, @RequestBody Book bookDetails) {
//        try {
//            Book updatedBook = bookService.updateBook(bookId, bookDetails);
//            return ResponseEntity.ok(updatedBook);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @PutMapping("/{bookId}/updatePage/{pageNumber}")
//    public ResponseEntity<Page> updatePage(@PathVariable Long bookId, @PathVariable int pageNumber, @RequestBody Page pageDetails) {
//        try {
//            Page updatedPage = bookService.updatePage(bookId, pageNumber, pageDetails);
//            return ResponseEntity.ok(updatedPage);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @GetMapping("/{bookId}/nextPage/{username}")
//    public ResponseEntity<Page> getNextPage(@PathVariable Long bookId, @PathVariable String username) {
//        try {
//            Page nextPage = bookService.getNextPage(bookId, username);
//            return ResponseEntity.ok(nextPage);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    // Obtener la página anterior
//    @GetMapping("/{bookId}/previousPage/{username}")
//    public ResponseEntity<Page> getPreviousPage(@PathVariable Long bookId, @PathVariable String username) {
//        try {
//            Page previousPage = bookService.getPreviousPage(bookId, username);
//            return ResponseEntity.ok(previousPage);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    // Obtener una página específica por número
//    @GetMapping("/{bookId}/page/{pageNumber}/{username}")
//    public ResponseEntity<Page> getPageByNumber(@PathVariable Long bookId,
//                                                @PathVariable int pageNumber,
//                                                @PathVariable String username) {
//        try {
//            Page page = bookService.getPageByNumber(bookId, pageNumber, username);
//            return ResponseEntity.ok(page);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//}
//
