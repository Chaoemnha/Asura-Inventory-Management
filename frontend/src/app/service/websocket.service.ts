import { Injectable } from "@angular/core";
import { Observable, Subject } from "rxjs";
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root',
})
export class WebSocketService{
    private socket!: WebSocket;
    private messageSubject = new Subject<any>();

    constructor(){}

    connect(): void{
        if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
            return;
        }
        this.socket = new WebSocket(environment.WS_URL);
        this.socket.onopen = (ws)=>{
                    console.log('-----------------', ws)
        };

        this.socket.onmessage = (event:any)=>{
            let message;
                if (event.data.startsWith('{')) {
                    message = JSON.parse(event.data);
                } else {
                    message = {
                        type: 'PLAIN_TEXT',
                        data: event.data
                    };
                }
            this.messageSubject.next(message);
        };

        this.socket.onclose = ()=>{
            console.log('Websocket ngat ket noi');
            setTimeout(() => {
                this.connect()
            }, 3000);
        };
        this.socket.onerror = ()=>{
            console.log('Websocket err: ');
        };
    }

    disconnect(): void{
        if(this.socket){
            this.socket.close();
        }
    }

    getMessages(): Observable<any>{
        const a = this.messageSubject.asObservable();
        return this.messageSubject.asObservable();
    }
}