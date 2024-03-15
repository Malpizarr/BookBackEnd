package main

import (
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

//--TODO: Arreglar la comunicacion en tiempo real

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type CRDTOperation struct {
	Action      string `json:"action"`      // "add" o "delete"
	Char        string `json:"char"`        // Cambiado de rune a string
	Position    int    `json:"position"`    // Posición en el documento
	ClientID    string `json:"clientId"`    // ID único para cada cliente
	OperationID string `json:"operationId"` // ID único para cada operación
}

type Message struct {
	BookID  string        `json:"bookId"`
	Type    string        `json:"type"`              // "connect", "operation", "initial"
	Op      CRDTOperation `json:"op,omitempty"`      // omitempty para que no se incluya si está vacío
	Content string        `json:"content,omitempty"` // Para el estado inicial y otros mensajes que no sean operaciones
}

type BookState struct {
	Document     []CRDTOperation
	Clients      map[*Connection]bool
	Lock         *sync.RWMutex // Usa un puntero aquí
	OperationLog []CRDTOperation
}

var bookClients = make(map[string]*BookState) // Mapa de ID de libro a estado del libro

type Connection struct {
	*websocket.Conn
	send chan Message
}

func handleConnections(w http.ResponseWriter, r *http.Request) {
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("Upgrade Error: %v\n", err)
		return
	}
	defer ws.Close()

	conn := &Connection{Conn: ws, send: make(chan Message, 256)}
	defer close(conn.send)

	var clientBookID string
	for {
		var msg Message
		err := ws.ReadJSON(&msg)
		if err != nil {
			log.Printf("ReadJSON Error: %v\n", err)
			break
		}

		switch msg.Type {
		case "connect":
			clientBookID = msg.BookID
			handleClientConnect(conn, clientBookID)
		case "operation":
			handleCRDTOperation(clientBookID, msg.Op)
		}
	}
}

func handleClientConnect(conn *Connection, bookID string) {
	ensureBookState(bookID)
	bookClients[bookID].Clients[conn] = true
	broadcastInitialState(bookID, conn)
}

func ensureBookState(bookID string) {
	if _, ok := bookClients[bookID]; !ok {
		bookClients[bookID] = &BookState{
			Document:     []CRDTOperation{},
			Clients:      make(map[*Connection]bool),
			Lock:         &sync.RWMutex{},
			OperationLog: []CRDTOperation{},
		}
	}
}

func handleCRDTOperation(bookID string, op CRDTOperation) {
	state := bookClients[bookID]
	state.Lock.Lock()
	defer state.Lock.Unlock()

	switch op.Action {
	case "add":
		position := op.Position
		if position > len(state.Document) {
			position = len(state.Document)
		}
		newOp := CRDTOperation{
			Action:      "add",
			Char:        op.Char,
			Position:    position,
			ClientID:    op.ClientID,
			OperationID: op.OperationID,
		}
		state.Document = append(state.Document[:position], append([]CRDTOperation{newOp}, state.Document[position:]...)...)

	case "remove":
		if op.Position >= 0 && op.Position < len(state.Document) {
			state.Document = append(state.Document[:op.Position], state.Document[op.Position+1:]...)
		}
	}

	state.OperationLog = append(state.OperationLog, op)
	broadcastToBook(bookID, Message{
		Type: "operation",
		Op:   op,
	})
}

func broadcastInitialState(bookID string, conn *Connection) {
	state := bookClients[bookID]
	state.Lock.RLock()
	defer state.Lock.RUnlock()

	var documentContent []rune
	for _, op := range state.OperationLog {
		if op.Action == "add" {
			position := op.Position
			if position > len(documentContent) {
				position = len(documentContent)
			}
			for _, char := range op.Char {
				documentContent = append(documentContent[:position], append([]rune{char}, documentContent[position:]...)...)
				position++ // Incrementamos la posición para el siguiente carácter
			}

		}
		//--TODO: handle remove action
	}

	// Convertir el documento a string y enviarlo al cliente.
	initialContent := string(documentContent)
	select {
	case conn.send <- Message{Type: "initial", Content: initialContent}:

	default:
		close(conn.send)
		delete(state.Clients, conn)
	}
}

// broadcastToBook difunde una operación CRDT a todos los clientes conectados al libro.
func broadcastToBook(bookID string, msg Message) {
	state := bookClients[bookID]
	state.Lock.RLock()
	defer state.Lock.RUnlock()

	for client := range state.Clients {
		select {
		case client.send <- msg:
		default:
			close(client.send)
			delete(state.Clients, client)
		}
	}
}

func main() {
	fs := http.FileServer(http.Dir("public")) // Sirve archivos estáticos desde el directorio "public".
	http.Handle("/", fs)

	http.HandleFunc("/ws", handleConnections) // Maneja las conexiones WebSocket en la ruta "/ws".

	log.Println("Server started on :8000")
	err := http.ListenAndServe(":8000", nil) // Inicia el servidor en el puerto 8000.
	if err != nil {
		log.Fatal("ListenAndServe Error: ", err)
	}
}
