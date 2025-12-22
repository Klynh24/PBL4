import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface ChatMessagePayload {
  senderId: string;
  recipientId: number;
  text: string;
  timestamp: number;
}

export type RoomEventType = 'JOIN' | 'LEAVE' | 'CHAT' | 'USER_LIST';

export interface RoomEventPayload {
  type: RoomEventType;
  roomId: number;
  userId?: string;
  text?: string;
  timestamp?: number;
  userIds?: string[];
}

type MessageHandler = (msg: ChatMessagePayload) => void;
type RoomEventHandler = (evt: RoomEventPayload) => void;

type ConnectionState = 'disconnected' | 'connecting' | 'connected';

class WebSocketService {
  private client: Client | null = null;
  private state: ConnectionState = 'disconnected';
  private handlers = new Set<MessageHandler>();
  private roomSubscriptions = new Map<number, { subscription: StompSubscription | null; handlers: Set<RoomEventHandler> }>();
  private pendingPublishes: Array<{ destination: string; body: string }> = [];

  getConnectionState(): ConnectionState {
    return this.state;
  }

  addMessageHandler(handler: MessageHandler) {
    this.handlers.add(handler);
    return () => this.handlers.delete(handler);
  }

  connect() {
    if (this.state === 'connected' || this.state === 'connecting') return;

    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('Missing token for WebSocket connection');
    }

    this.state = 'connecting';

    const wsUrl = '/ws';

    const shouldDebug = localStorage.getItem('wsDebug') === '1';

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl) as any,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 2000,
      debug: (str) => {
        if (shouldDebug) console.log('[STOMP]', str);
      },
    });

    client.onConnect = () => {
      this.state = 'connected';

      client.subscribe('/user/queue/messages', (frame: IMessage) => {
        try {
          const payload = JSON.parse(frame.body) as ChatMessagePayload;
          this.handlers.forEach((h) => h(payload));
        } catch (e) {
          // ignore malformed payloads
        }
      });

      // re-subscribe room topics after reconnect
      for (const [roomId, entry] of this.roomSubscriptions.entries()) {
        const destination = `/topic/room/${roomId}`;
        entry.subscription?.unsubscribe();
        entry.subscription = client.subscribe(destination, (frame: IMessage) => {
          try {
            const payload = JSON.parse(frame.body) as RoomEventPayload;
            entry.handlers.forEach((h) => h(payload));
          } catch (e) {
            // ignore malformed payloads
          }
        });
      }

      // flush queued publishes (join/chat) once connected
      const pending = this.pendingPublishes.splice(0);
      for (const p of pending) {
        try {
          client.publish({ destination: p.destination, body: p.body });
        } catch {
          // ignore
        }
      }
    };

    client.onStompError = (frame) => {
      console.error('[STOMP] error', frame.headers, frame.body);
    };

    client.onWebSocketError = (evt) => {
      console.error('[WS] error', evt);
    };

    client.onWebSocketClose = () => {
      this.state = 'disconnected';
    };

    this.client = client;
    client.activate();
  }

  disconnect() {
    if (!this.client) return;

    // clear subscriptions (server will also treat disconnect as leave)
    for (const entry of this.roomSubscriptions.values()) {
      try {
        entry.subscription?.unsubscribe();
      } catch {
        // ignore
      }
    }
    this.roomSubscriptions.clear();
    this.client.deactivate();
    this.client = null;
    this.state = 'disconnected';
  }

  subscribeRoom(roomId: number, handler: RoomEventHandler) {
    if (!Number.isFinite(roomId)) {
      throw new Error('Invalid roomId');
    }

    let entry = this.roomSubscriptions.get(roomId);
    if (!entry) {
      entry = { subscription: null, handlers: new Set<RoomEventHandler>() };
      this.roomSubscriptions.set(roomId, entry);
    }

    entry.handlers.add(handler);

    // If already connected and not yet subscribed (or after reconnect), ensure subscription exists
    if (this.client && this.state === 'connected' && !entry.subscription) {
      const destination = `/topic/room/${roomId}`;
      entry.subscription = this.client.subscribe(destination, (frame: IMessage) => {
        try {
          const payload = JSON.parse(frame.body) as RoomEventPayload;
          entry?.handlers.forEach((h) => h(payload));
        } catch (e) {
          // ignore
        }
      });
    }

    return () => {
      const current = this.roomSubscriptions.get(roomId);
      if (!current) return;
      current.handlers.delete(handler);
      if (current.handlers.size === 0) {
        try {
          current.subscription?.unsubscribe();
        } finally {
          this.roomSubscriptions.delete(roomId);
        }
      }
    };
  }

  private publishOrQueue(destination: string, body: string) {
    if (this.client && this.state === 'connected') {
      this.client.publish({ destination, body });
      return;
    }
    this.pendingPublishes.push({ destination, body });
  }

  joinRoom(roomId: number) {
    this.publishOrQueue('/app/room.join', JSON.stringify({ roomId }));
  }

  leaveRoom(roomId: number) {
    if (!this.client || this.state !== 'connected') return;
    this.client.publish({ destination: '/app/room.leave', body: JSON.stringify({ roomId }) });
  }

  sendRoomMessage(roomId: number, text: string) {
    const trimmed = (text || '').trim();
    if (!trimmed) return;

    this.publishOrQueue('/app/room.chat', JSON.stringify({ roomId, text: trimmed }));
  }

  sendPrivateMessage(recipientId: number, text: string) {
    const trimmed = (text || '').trim();
    if (!trimmed) return;

    this.publishOrQueue('/app/chat.private', JSON.stringify({ recipientId, text: trimmed }));
  }
}

export const websocketService = new WebSocketService();
