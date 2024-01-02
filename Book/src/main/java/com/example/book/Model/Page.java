//package com.example.book.Model;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import jakarta.persistence.*;
//import java.util.Date;
//
//@Entity
//@Table(name = "Page")
//public class Page {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long pageId;
//
//    @ManyToOne
//    @JoinColumn(name = "book_id", nullable = false)
//    @JsonBackReference
//    private Book book;
//
//    @Column(columnDefinition = "TEXT", nullable = false)
//    private String content;
//
//    @Column(nullable = false)
//    private Integer pageNumber;
//
//    @Column(nullable = false)
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date createdAt;
//
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date updatedAt;
//
//
//    public void setBook(Book book) {
//        this.book = book;
//    }
//
//    public Long getPageId() {
//        return pageId;
//    }
//
//    public void setPageId(Long pageId) {
//        this.pageId = pageId;
//    }
//
//    public String getContent() {
//        return content;
//    }
//
//    public void setContent(String content) {
//        this.content = content;
//    }
//
//    public Integer getPageNumber() {
//        return pageNumber;
//    }
//
//    public void setPageNumber(Integer pageNumber) {
//        this.pageNumber = pageNumber;
//    }
//
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public Date getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setUpdatedAt(Date updatedAt) {
//        this.updatedAt = updatedAt;
//    }
//
//    public Object getBook() {
//        return book;
//    }
//
//    public Object getId() {
//        return pageId;
//    }
//}
