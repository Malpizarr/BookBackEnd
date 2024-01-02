//package com.example.book.Repositories;
//
//import com.example.book.Model.Book;
//import com.example.book.Model.Page;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PageRepository extends JpaRepository<Page, Long> {
//
//    List<Page> findAllByBookAndPageNumberGreaterThan(Book book, Integer pageNumber);
//
//    Optional<Page> findByBookAndPageNumber(Book book, Integer pageNumber);
//}
