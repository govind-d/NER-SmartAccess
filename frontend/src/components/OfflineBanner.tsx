import { useOfflineSync } from '../hooks/useOfflineSync'
import { IconWifiOff } from './icons'

/**
 * The strip that tells a field officer exactly where they stand: offline, or online with
 * reports still waiting to go up. Without it, "did my report actually send?" is
 * unanswerable, which is precisely the anxiety the offline module exists to remove.
 *
 * It renders nothing at all when online with an empty queue - a permanent status bar
 * saying "everything is fine" is noise that people learn to stop reading.
 */
export function OfflineBanner() {
  const { online, pendingCount, syncing, sync } = useOfflineSync()

  if (online && pendingCount === 0) {
    return null
  }

  return (
    <div
      className={`px-5 py-2.5 text-sm flex items-center justify-between gap-3 ${
        online
          ? 'bg-amber-50 text-amber-900 border-b border-amber-200'
          : 'bg-slate-900 text-slate-100'
      }`}
    >
      <span className="flex items-center gap-2.5">
        <span
          className={`w-6 h-6 rounded-lg grid place-items-center ${
            online ? 'bg-amber-100 text-amber-700' : 'bg-white/10 text-slate-300'
          }`}
        >
          <IconWifiOff width={14} height={14} />
        </span>
        {online
          ? `${pendingCount} report${pendingCount === 1 ? '' : 's'} saved on this device, waiting to sync`
          : 'You are offline. Reports are saved here and sent automatically.'}
      </span>

      {online && pendingCount > 0 && (
        <button
          className="btn-ghost py-1 px-2.5 text-xs"
          onClick={() => void sync()}
          disabled={syncing}
        >
          {syncing ? 'Syncing...' : 'Sync now'}
        </button>
      )}
    </div>
  )
}
