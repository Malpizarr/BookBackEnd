//package com.example.book.Controller;
//
//import com.example.book.Model.Page;
//import com.example.book.Service.PageService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/pages")
//public class PageController {
//
//    @Autowired
//    private PageService pageService;
//
//    @PostMapping("/add/{bookId}")
//    public Page addPageToBook(@RequestBody Page page, @PathVariable Long bookId) {
//        return pageService.addPageToBook(bookId, page);
//    }
//
//    @GetMapping("/get/{pageId}")
//    public Page getPage(@PathVariable Long pageId) {
//        return pageService.getPage(pageId);
//    }
//}
