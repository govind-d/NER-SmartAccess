import { NerMap } from '../components/NerMap'
import { LiveBadge, PageHeader } from '../components/ui'

export default function MapPage() {
  return (
    <div className="space-y-5">
      <PageHeader
        title="Live map"
        subtitle="Road status, district accessibility, reported incidents and vehicles in real time"
        badge={<LiveBadge label="Streaming" />}
      />
      <NerMap />
    </div>
  )
}
