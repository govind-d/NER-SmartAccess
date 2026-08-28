import { useOfflineSync } from '../hooks/useOfflineSync'

/**
 * The strip that tells a field officer exactly where they stand: offline, or online with
 * reports still waiting to go up. Without it, "did my report actually send?" is
 * unanswerable, which is precisely the anxiety the offline module exists to remove.
 */
export function OfflineBanner() {
  const { online, pendingCount, syncing, sync } = useOfflineSync()

  if (online && pendingCount === 0) {
    return null
  }

  return (
    <div
      className={`px-4 py-2 text-sm flex items-center justify-between ${
        online ? 'bg-amber-100 text-amber-900' : 'bg-slate-800 text-slate-100'
      }`}
    >
      <span>
        {online
          ? `${pendingCount} report(s) saved on this device and waiting to sync`
          : 'You are offline. Reports will be saved here and sent automatically.'}
      </span>
      {online && pendingCount > 0 && (
        <button className="btn-ghost py-1 text-xs" onClick={() => void sync()} disabled={syncing}>
          {syncing ? 'Syncing...' : 'Sync now'}
        </button>
      )}
    </div>
  )
}
