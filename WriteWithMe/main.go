package main

import (
	"github.com/gorilla/websocket"
	"log"
	"net/http"
)

var broadcast = make(chan Message)
var bookClients = make(map[string]map[*websocket.Conn]bool)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type Message struct {
	Content string `json:"content"`
	BookID  string `json:"bookId"`
}

func main() {
	fs := http.FileServer(http.Dir("public"))
	http.Handle("/", fs)

	http.HandleFunc("/ws", handleConnections)

	go handleMessages()

	log.Println("Servidor iniciado en :8000")
	err := http.ListenAndServe(":8000", nil)
	if err != nil {
		log.Fatal("ListenAndServe Error: ", err)
	}
}

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
			break
		}

		if _, ok := bookClients[msg.BookID]; !ok {
			bookClients[msg.BookID] = make(map[*websocket.Conn]bool)
		}
		bookClients[msg.BookID][ws] = true

		broadcast <- msg
	}

	for _, clients := range bookClients {
		delete(clients, ws)
	}
}

func handleMessages() {
	for {
		msg := <-broadcast
		clients, ok := bookClients[msg.BookID]
		if !ok {
			continue
		}
		for client := range clients {
			err := client.WriteJSON(msg) // Envía el mensaje tal como está
			if err != nil {
				log.Printf("error: %v", err)
				client.Close()
				delete(clients, client)
			}
		}
	}
}
