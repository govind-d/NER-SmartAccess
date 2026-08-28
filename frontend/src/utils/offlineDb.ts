import { openDB, type DBSchema, type IDBPDatabase } from 'idb'

/**
 * The offline store for field reports.
 *
 * This is the heart of the offline module. When a field officer submits a report with no
 * network, it is written here first and marked PENDING. The moment connectivity returns,
 * the queue is pushed to POST /field-reports/sync.
 *
 * Two details make it safe:
 *  - every report carries a clientUuid generated on the device, so re-sending the same
 *    report is harmless (the server recognises it and reports it as a duplicate);
 *  - capturedAt records when the officer actually filled the form, not when it synced,
 *    so a report written at 09:14 in a valley still appears on the timeline at 09:14.
 */

export interface QueuedReport {
  clientUuid: string
  reportType: string
  description: string
  latitude: number
  longitude: number
  capturedAt: string
  status: 'PENDING' | 'SYNCED' | 'FAILED'
  lastError?: string
}

interface SmartLogixDb extends DBSchema {
  reports: {
    key: string
    value: QueuedReport
    indexes: { 'by-status': string }
  }
}

const DB_NAME = 'ner-smartlogix'
const DB_VERSION = 1

let dbPromise: Promise<IDBPDatabase<SmartLogixDb>> | null = null

function db() {
  if (!dbPromise) {
    dbPromise = openDB<SmartLogixDb>(DB_NAME, DB_VERSION, {
      upgrade(database) {
        const store = database.createObjectStore('reports', { keyPath: 'clientUuid' })
        store.createIndex('by-status', 'status')
      },
    })
  }
  return dbPromise
}

export const offlineDb = {
  async queue(report: Omit<QueuedReport, 'status'>) {
    const database = await db()
    await database.put('reports', { ...report, status: 'PENDING' })
  },

  async pending(): Promise<QueuedReport[]> {
    const database = await db()
    return database.getAllFromIndex('reports', 'by-status', 'PENDING')
  },

  async all(): Promise<QueuedReport[]> {
    const database = await db()
    return database.getAll('reports')
  },

  async markSynced(clientUuids: string[]) {
    const database = await db()
    const tx = database.transaction('reports', 'readwrite')
    await Promise.all(
      clientUuids.map(async (uuid) => {
        const existing = await tx.store.get(uuid)
        if (existing) {
          await tx.store.put({ ...existing, status: 'SYNCED' })
        }
      }),
    )
    await tx.done
  },

  async markFailed(clientUuid: string, reason: string) {
    const database = await db()
    const existing = await database.get('reports', clientUuid)
    if (existing) {
      await database.put('reports', { ...existing, status: 'FAILED', lastError: reason })
    }
  },

  /** Housekeeping: successfully synced rows have no further purpose on the device. */
  async clearSynced() {
    const database = await db()
    const synced = await database.getAllFromIndex('reports', 'by-status', 'SYNCED')
    const tx = database.transaction('reports', 'readwrite')
    await Promise.all(synced.map((report) => tx.store.delete(report.clientUuid)))
    await tx.done
  },
}

/** crypto.randomUUID is available in every browser that can run a service worker. */
export function newClientUuid(): string {
  return crypto.randomUUID()
}
