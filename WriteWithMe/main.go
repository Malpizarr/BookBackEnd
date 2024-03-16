package main

import (
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
	Action     string            `json:"action"`
	Content    string            `json:"content"`
	Position   int               `json:"position"`
	Attributes map[string]string `json:"attributes"`
}

// Message ahora incluye un slice de Ops para permitir múltiples operaciones.
type Message struct {
	BookID string `json:"bookId"`
	Type   string `json:"type"` // 'operation', 'connect', etc.
	Ops    []Op   `json:"ops"`  // Lista de operaciones.
}

type BookState struct {
	Clients map[*websocket.Conn]bool
	Lock    sync.Mutex
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
	if _, ok := books[bookID]; !ok {
		books[bookID] = &BookState{Clients: make(map[*websocket.Conn]bool)}
	}

	bookState := books[bookID]
	bookState.Lock.Lock()
	defer bookState.Lock.Unlock()

	if _, ok := bookState.Clients[ws]; !ok {
		bookState.Clients[ws] = true
	}

	for client := range bookState.Clients {
		if client != ws {
			err := client.WriteJSON(msg)
			if err != nil {
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
