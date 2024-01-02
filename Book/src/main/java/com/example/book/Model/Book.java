//package com.example.book.Model;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import jakarta.persistence.*;
//
//import java.util.Date;
//import java.util.Set;
//
//@Entity
//@Table(name = "Book")
//public class Book {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long bookId;
//
//    @ManyToOne
//    @JoinColumn(name = "user_id", nullable = false)
//    @JsonBackReference
//    private User user;
//
//    @Column(nullable = false, length = 100)
//    private String title;
//
//    @Column(columnDefinition = "TEXT")
//    private String description;
//
//    @Temporal(TemporalType.TIMESTAMP)
//    @Column(nullable = false)
//    private Date createdAt;
//
//    @PrePersist
//    protected void onCreate() {
//        if (createdAt == null) {
//            createdAt = new Date();
//        }
//    }
//
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date updatedAt;
//
//    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
//    @JsonManagedReference
//    private Set<Page> pages;
//
//    public void setUser(User user) {
//        this.user = user;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public Long getBookId() {
//        return bookId;
//    }
//
//    public void setBookId(Long bookId) {
//        this.bookId = bookId;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
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
//    public Set<Page> getPages() {
//        return pages;
//    }
//
//    public void setPages(Set<Page> pages) {
//        this.pages = pages;
//    }
//
//    @Transient
//    private int currentPage = 1; // Inicialmente en la página 1
//
//    public int getCurrentPage() {
//        return currentPage;
//    }
//
//    // Setter para actualizar la página actual
//    public void setCurrentPage(int currentPage) {
//        this.currentPage = currentPage;
//    }
//}
