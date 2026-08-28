import { NerMap } from '../components/NerMap'

export default function MapPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Live map</h1>
        <p className="text-sm text-slate-500">
          Road status, district accessibility, reported incidents and vehicles in real time.
        </p>
      </div>
      <NerMap />
    </div>
  )
}
