import { useEffect, useRef, useState } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStore } from '../services/api'

const WS_URL = import.meta.env.VITE_WS_URL ?? '/ws'

/**
 * One STOMP connection, shared by every component that subscribes.
 *
 * Opening a socket per component would mean five connections on the dashboard. Instead a
 * single client is created lazily, kept alive, and handed out to subscribers.
 *
 * The JWT travels in the CONNECT frame because WebSocket frames carry no HTTP headers -
 * see StompAuthChannelInterceptor on the backend.
 */

let sharedClient: Client | null = null
const connectionListeners = new Set<(connected: boolean) => void>()

function getClient(): Client {
  if (sharedClient) {
    return sharedClient
  }
  sharedClient = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: { Authorization: `Bearer ${tokenStore.access() ?? ''}` },
    reconnectDelay: 5000,       // survive a tunnel or a dropped signal
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => connectionListeners.forEach((listener) => listener(true)),
    onWebSocketClose: () => connectionListeners.forEach((listener) => listener(false)),
    debug: () => { /* silence the very chatty STOMP logger */ },
  })
  sharedClient.activate()
  return sharedClient
}

export function disconnectWebSocket() {
  sharedClient?.deactivate()
  sharedClient = null
}

/**
 * Subscribe to a topic for as long as the component is mounted.
 *
 * @param destination e.g. "/topic/vehicles"
 * @param onMessage   called with the parsed JSON payload
 */
export function useWebSocket<T>(destination: string, onMessage: (payload: T) => void) {
  const [connected, setConnected] = useState(false)
  // Kept in a ref so that a new inline callback on every render does not re-subscribe.
  const handlerRef = useRef(onMessage)
  handlerRef.current = onMessage

  useEffect(() => {
    connectionListeners.add(setConnected)
    const client = getClient()

    let subscription: { unsubscribe: () => void } | null = null
    const subscribe = () => {
      subscription = client.subscribe(destination, (message: IMessage) => {
        try {
          handlerRef.current(JSON.parse(message.body) as T)
        } catch {
          // A malformed frame must not take the page down.
        }
      })
    }

    if (client.connected) {
      subscribe()
    } else {
      const previousOnConnect = client.onConnect
      client.onConnect = (frame) => {
        previousOnConnect?.(frame)
        subscribe()
      }
    }

    return () => {
      subscription?.unsubscribe()
      connectionListeners.delete(setConnected)
    }
  }, [destination])

  return { connected }
}
