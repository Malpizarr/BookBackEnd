//package com.example.book.Service;
//
//import com.example.book.Model.Book;
//import com.example.book.Model.Page;
//import com.example.book.Repositories.BookRepository;
//import com.example.book.Repositories.PageRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class PageService {
//
//    @Autowired
//    private PageRepository pageRepository;
//
//    @Autowired
//    private BookRepository bookRepository;
//
//    public Page addPageToBook(Long bookId, Page page) {
//        Book book = bookRepository.findById(bookId)
//                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));
//        page.setBook(book);
//        return pageRepository.save(page);
//    }
//
//    public Page getPage(Long pageId) {
//        return pageRepository.findById(pageId)
//                .orElseThrow(() -> new RuntimeException("Página no encontrada"));
//    }
//
//
//
//    // Métodos adicionales según sea necesario
//}
