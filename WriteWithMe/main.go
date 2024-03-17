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
	Action     string                 `json:"action"`
	Content    string                 `json:"content"`
	Position   int                    `json:"position"`
	Length     int                    `json:"length"`
	Attributes map[string]interface{} `json:"attributes"`
}

type Message struct {
	BookID string `json:"bookId"`
	Type   string `json:"type"` // 'operation', 'connect', etc.
	Ops    []Op   `json:"ops"`  // Lista de operaciones.
}

type Operation struct {
	Action     string
	Content    string
	Position   int
	Length     int
	Attributes map[string]string
}

type BookState struct {
	BookID     string
	Clients    map[*websocket.Conn]bool
	Operations []Operation
	Content    string
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
	bookState, ok := books[bookID]
	if !ok {
		bookState = &BookState{
			BookID:  bookID,
			Clients: make(map[*websocket.Conn]bool),
		}
		books[bookID] = bookState
	}

	bookState.Lock.Lock()
	defer bookState.Lock.Unlock()

	bookState.Clients[ws] = true

	for _, op := range msg.Ops {
		adjustedOp := adjustOperation(op, bookState.Operations)
		applyOperation(bookState, ws, adjustedOp)
	}
}

func adjustOperation(op Op, prevOperations []Operation) Op {
	adjustedPosition := op.Position
	for _, prevOp := range prevOperations {
		switch prevOp.Action {
		case "insert":
			if op.Position > prevOp.Position {
				adjustedPosition += len(prevOp.Content)
			}
		case "delete":
			if op.Position >= prevOp.Position {
				if op.Position < prevOp.Position+prevOp.Length {
					adjustedPosition = prevOp.Position // Si la posición cae dentro de una operación de borrado previa, ajustar a inicio del borrado
				} else {
					adjustedPosition -= min(op.Position-prevOp.Position, prevOp.Length)
				}
			}
		}
	}
	return Op{
		Action:     op.Action,
		Content:    op.Content,
		Position:   adjustedPosition,
		Length:     op.Length,
		Attributes: op.Attributes,
	}
}

// Todo: Implementar la mejora de eliminacion en la posicion correcta, ademas arregla el formato de la edicion
func applyOperation(bookState *BookState, ws *websocket.Conn, op Op) {
	bookContent := bookState.Content

	switch op.Action {
	case "insert":
		insertPosition := max(0, op.Position)
		if insertPosition > len(bookContent) {
			insertPosition = len(bookContent)
		}
		bookContent = bookContent[:insertPosition] + op.Content + bookContent[insertPosition:]
	case "delete":
		if op.Position < 0 || op.Position >= len(bookContent) {
			return
		}
		endPos := min(op.Position+op.Length, len(bookContent))
		if op.Position < endPos {
			bookContent = bookContent[:op.Position] + bookContent[endPos:]
		}
	case "format":

	}

	bookState.Content = bookContent

	msg := Message{
		BookID: bookState.BookID,
		Type:   "operation",
		Ops:    []Op{op},
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

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}

func deleteClient(ws *websocket.Conn, bookID string) {
	if bookState, ok := books[bookID]; ok {
		bookState.Lock.Lock()
		defer bookState.Lock.Unlock()
		delete(bookState.Clients, ws)
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func main() {
	http.HandleFunc("/ws", handleConnections)
	log.Println("Server started")
	err := http.ListenAndServe(":8000", nil)
	if err != nil {
		log.Fatal("ListenAndServe error:", err)
	}
}
