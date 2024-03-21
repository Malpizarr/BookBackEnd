package main

import (
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type Op struct {
	Action     string                 `json:"action"`
	Content    string                 `json:"content"`
	Position   int                    `json:"position"`
	Length     int                    `json:"length"`
	Attributes map[string]interface{} `json:"attributes"`
	PageNumber int                    `json:"pageNumber"`
}

type Message struct {
	BookID     string `json:"bookId"`
	Type       string `json:"type"`
	Ops        []Op   `json:"ops"`
	PageNumber int    `json:"pageNumber"`
}

type Operation struct {
	Action     string
	Content    string
	Position   int
	Length     int
	Attributes map[string]string
	PageNumber int
}

type BookState struct {
	BookID     string
	Clients    map[*websocket.Conn]bool
	Operations map[int][]Operation
	Pages      map[int]string
	Lock       sync.Mutex
}

var books = make(map[string]*BookState)

func handleConnections(w http.ResponseWriter, r *http.Request) {
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Fatal(err)
	}
	defer ws.Close()

	for {
		var msg Message
		err := ws.ReadJSON(&msg)
		if err != nil {
			log.Printf("error: %v", err)
			deleteClient(ws, msg.BookID)
			break
		}
		processMessage(ws, msg)
	}
}

func processMessage(ws *websocket.Conn, msg Message) {
	bookID := msg.BookID
	currentPage := msg.PageNumber
	bookState, ok := books[bookID]
	if !ok {
		bookState = &BookState{
			BookID:     bookID,
			Clients:    make(map[*websocket.Conn]bool),
			Operations: make(map[int][]Operation),
			Pages:      make(map[int]string),
		}
		books[bookID] = bookState
	}

	bookState.Lock.Lock()
	defer bookState.Lock.Unlock()

	bookState.Clients[ws] = true

	for _, op := range msg.Ops {
		adjustedOp := adjustOperation(op, bookState.Operations[currentPage], currentPage)
		applyOperation(bookState, ws, adjustedOp, currentPage)
	}
}

func adjustOperation(currentOp Op, prevOperations []Operation, currentPage int) Op {
	for _, prevOp := range prevOperations {
		if prevOp.PageNumber != currentPage {
			continue
		}

		switch {
		case prevOp.Action == "insert" && currentOp.Action == "insert":
			if prevOp.Position <= currentOp.Position {
				currentOp.Position += len(prevOp.Content)
			}
		case prevOp.Action == "delete" && currentOp.Action == "insert":
			if prevOp.Position < currentOp.Position {
				currentOp.Position -= min(prevOp.Length, currentOp.Position-prevOp.Position)
			}
		case prevOp.Action == "insert" && currentOp.Action == "delete":
			if prevOp.Position <= currentOp.Position {
				currentOp.Position += len(prevOp.Content)
			}
		case prevOp.Action == "delete" && currentOp.Action == "delete":
			if prevOp.Position < currentOp.Position {
				currentOp.Position -= min(prevOp.Length, currentOp.Position-prevOp.Position)
			}
		}
	}
	return currentOp
}

func applyOperation(bookState *BookState, ws *websocket.Conn, op Op, currentPage int) {
	pageContent := bookState.Pages[op.PageNumber]

	switch op.Action {
	case "insert":
		if op.Position >= len(pageContent) {
			pageContent += op.Content
		} else {
			pageContent = pageContent[:op.Position] + op.Content + pageContent[op.Position:]
		}
	case "delete":
		if op.Position < len(pageContent) {
			end := min(op.Position+op.Length, len(pageContent))
			pageContent = pageContent[:op.Position] + pageContent[end:]
		}
	}

	bookState.Pages[op.PageNumber] = pageContent
	broadcastOperation(bookState, ws, op, currentPage)
}

func broadcastOperation(bookState *BookState, ws *websocket.Conn, op Op, currentPage int) {
	if op.PageNumber != currentPage {
		fmt.Println("Ignoring operation on different page")
		return
	}

	msg := Message{
		BookID:     bookState.BookID,
		PageNumber: currentPage,
		Type:       "operation",
		Ops:        []Op{op},
	}
	for client := range bookState.Clients {
		if client != ws {
			if err := client.WriteJSON(msg); err != nil {
				log.Printf("error: %v", err)
				client.Close()
				delete(bookState.Clients, client)
			}
		}
	}
}

func deleteClient(ws *websocket.Conn, bookID string) {
	if bookState, ok := books[bookID]; ok {
		bookState.Lock.Lock()
		defer bookState.Lock.Unlock()
		delete(bookState.Clients, ws)
	}
}

func main() {
	http.HandleFunc("/ws", handleConnections)
	log.Println("Server started")
	err := http.ListenAndServe(":8000", nil)
	if err != nil {
		log.Fatal("ListenAndServe error:", err)
	}
}
