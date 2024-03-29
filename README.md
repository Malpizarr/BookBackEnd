# BookBackEnd

This is a Back-End application developed using Java, Spring Boot, Maven, JavaScript, Express.js, MongoDB, and Redis. The
application provides a platform for users to manage and share their books.

## Built With

- [Java](https://www.java.com/) - The backend language
- [Spring Boot](https://spring.io/projects/spring-boot) - The backend framework
- [Maven](https://maven.apache.org/) - Dependency Management
- [JavaScript](https://www.javascript.com/) - The backend language
- [Express.js](https://expressjs.com/) - The backend framework
- [MongoDB](https://www.mongodb.com/) - The database
- [MySQL](https://www.mysql.com/) - The database
- [Azure Blob Storage](https://azure.microsoft.com/en-us/services/storage/blobs/) - Used for storing images
- [Redis](https://redis.io/) - Used for caching

# Features

The application provides a variety of features to facilitate user interaction and data management. Here are the key
features:

## User Registration and Authentication

The application provides a secure registration and authentication system. Users can create a new account and log in to
access the application's features. The authentication process ensures that user data is securely stored and encrypted.

## User Profile Management

Users have the ability to manage their profiles. This includes changing their password for security reasons and
uploading a profile photo to personalize their account.

## Book Management

Users can manage their own digital library within the application. They can add new books, update the details of
existing books, and remove books from their library.

## Page Management within Books

In addition to managing books, users can also manage the individual pages within those books. This includes adding new
pages, editing the content of existing pages, and deleting pages.

## Viewing All Books of a User

Users can view a list of all the books they have added to their library. This allows them to easily access and manage
their collection.

## Viewing All Books of a User's Friends

Users can also view the books that their friends have added to their libraries. This feature encourages sharing and
discovery of new books among friends.

## Searching for Users by Username

If users want to find their friends on the platform, they can use the search function. By entering a friend's username,
they can quickly and easily find their friend's profile.

## API Endpoints

The application exposes the following API endpoints:

- POST /users/create: Register a new user
- GET /users/{userId}: Get user details by user ID
- PUT /users/updatePassword/{userId}: Update a user's password
- PUT /users/update/{userId}: Update a user's details
- POST /users/uploadPhoto/{userId}: Upload a user's photo
- GET /users/search: Search for users by username
- POST /books/create: Create a new book
- GET /books/all: Get all books of the authenticated user
- GET /books/{Id}/books: Get a book by its ID
- GET /books/{friendId}/all: Get all books of a friend
- GET /books/all-friends-books: Get all books of all friends
- GET /books/{bookId}/pages: Get all pages of a book
- POST /books/{bookId}/createPage: Create a new page in a book
- DELETE /books/delete/{bookId}: Delete a book
- DELETE /books/{bookId}/deletePage/{pageNumber}: Delete a page in a book
- PUT /books/update/{bookId}: Update a book's details
- PUT /books/{bookId}/updatePage/{pageNumber}: Update a page's details
- GET /books/{bookId}/page/{pageNumber}: Get a page by its number
- GET /books/{bookId}: Get a book by its ID
- POST /auth/register
- POST /auth/login
- GET /auth/userinfo
- POST /auth/refresh-token
- GET /auth/set-cookie
- POST /auth/logout
- POST /users/create
- GET /users/{userId}
- PUT /users/updatePassword/{userId}
- PUT /users/update/{userId}
- POST /users/uploadPhoto/{userId}
- GET /users/search
- GET /api/friendships/{userId}
- PUT /api/friendships/accept
- GET /api/friendships/user/{userId}
- POST /api/friendships/createfriendship
- DELETE /api/friendships/deletefriendship
- GET /api/friendships/areFriends/{userId1}/{userId2}
- GET /api/friendships/pending
- GET /api/friendships/friends
- GET /chat/unread-messages
- POST /chat/reset-unread-messages

## Authors

- [Malpizarrr](https://github.com/Malpizarrr)
