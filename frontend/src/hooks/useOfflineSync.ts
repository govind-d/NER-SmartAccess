import { useCallback, useEffect, useState } from 'react'
import { fieldReportsApi } from '../services/endpoints'
import { offlineDb, type QueuedReport } from '../utils/offlineDb'

/**
 * Watches connectivity and flushes the offline queue the moment it returns.
 *
 * The flow it implements:
 *   1. the officer submits a report with no network -> it is written to IndexedDB;
 *   2. the browser fires an "online" event when signal returns;
 *   3. every PENDING report is sent to /field-reports/sync in one batch;
 *   4. the server replies per item, and only the accepted ones are cleared.
 *
 * Reports are also flushed once on mount, which covers the common case of the officer
 * reopening the app after driving back into coverage.
 */
export function useOfflineSync() {
  const [online, setOnline] = useState(navigator.onLine)
  const [pending, setPending] = useState<QueuedReport[]>([])
  const [syncing, setSyncing] = useState(false)
  const [lastResult, setLastResult] = useState<string | null>(null)

  const refreshPending = useCallback(async () => {
    setPending(await offlineDb.pending())
  }, [])

  const sync = useCallback(async () => {
    const queued = await offlineDb.pending()
    if (queued.length === 0 || !navigator.onLine) {
      return
    }
    setSyncing(true)
    try {
      const payload = queued.map((report) => ({
        clientUuid: report.clientUuid,
        reportType: report.reportType,
        description: report.description,
        latitude: report.latitude,
        longitude: report.longitude,
        capturedAt: report.capturedAt,
      }))
      const result = await fieldReportsApi.sync(payload)

      // Duplicates count as success: the server already has them.
      await offlineDb.markSynced([...result.accepted, ...result.duplicates])
      await Promise.all(
        result.failed.map((failure) =>
          offlineDb.markFailed(failure.clientUuid, failure.reason),
        ),
      )
      await offlineDb.clearSynced()
      setLastResult(
        `${result.accepted.length} synced, ${result.duplicates.length} already known, ${result.failed.length} failed`,
      )
    } catch {
      setLastResult('Sync failed. It will be retried automatically.')
    } finally {
      setSyncing(false)
      await refreshPending()
    }
  }, [refreshPending])

  useEffect(() => {
    const goOnline = () => {
      setOnline(true)
      void sync()
    }
    const goOffline = () => setOnline(false)

    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)
    void refreshPending()
    void sync()

    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
    }
  }, [sync, refreshPending])

  return { online, pending, pendingCount: pending.length, syncing, lastResult, sync, refreshPending }
}
