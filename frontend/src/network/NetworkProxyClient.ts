export type NetworkProxyClientHandlers = {
  onOpen?: () => void;
  onClose?: (event: CloseEvent) => void;
  onError?: (event: Event) => void;
  onText?: (json: any, raw: string) => void;
  onBinary?: (data: ArrayBuffer) => void;
};

export class NetworkProxyClient {
  private ws: WebSocket | null = null;

  connect(url: string, handlers: NetworkProxyClientHandlers = {}): WebSocket {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return this.ws;
    }

    const ws = new WebSocket(url);
    ws.binaryType = 'arraybuffer';

    ws.onopen = () => {
      handlers.onOpen?.();
    };

    ws.onclose = (event) => {
      handlers.onClose?.(event);
    };

    ws.onerror = (event) => {
      handlers.onError?.(event);
    };

    ws.onmessage = (event) => {
      if (typeof event.data === 'string') {
        try {
          const parsed = JSON.parse(event.data);
          handlers.onText?.(parsed, event.data);
        } catch {
          handlers.onText?.(null, event.data);
        }
        return;
      }

      if (event.data instanceof ArrayBuffer) {
        handlers.onBinary?.(event.data);
        return;
      }

      // Blob fallback
      if (event.data instanceof Blob) {
        event.data.arrayBuffer().then((buf) => handlers.onBinary?.(buf)).catch(() => {});
      }
    };

    this.ws = ws;
    return ws;
  }

  sendJson(payload: Record<string, any>) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify(payload));
  }

  sendText(text: string) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(text);
  }

  close() {
    if (!this.ws) return;
    try {
      this.ws.close();
    } catch {
      // ignore
    } finally {
      this.ws = null;
    }
  }

  getRawSocket(): WebSocket | null {
    return this.ws;
  }
}
