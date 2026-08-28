import type { AccessibilityLevel, DeliveryStatus, RiskLevel, RoadStatus, Severity } from '../types'

/** Formatting and colour helpers shared by every screen, so one status looks the same
 *  on the map, in a table and on a badge. */

export function formatDateTime(value?: string): string {
  if (!value) return '-'
  return new Date(value).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
  })
}

export function formatRelative(value?: string): string {
  if (!value) return '-'
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  return `${Math.round(hours / 24)} d ago`
}

export function humanise(value?: string): string {
  if (!value) return '-'
  return value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, ' ')
}

export const roadStatusColor: Record<RoadStatus, string> = {
  OPEN: '#16a34a',
  PARTIALLY_ACCESSIBLE: '#eab308',
  HIGH_RISK: '#f97316',
  BLOCKED: '#dc2626',
}

export const riskColor: Record<RiskLevel, string> = {
  LOW_RISK: '#16a34a',
  MEDIUM_RISK: '#eab308',
  HIGH_RISK: '#dc2626',
}

export const accessibilityColor: Record<AccessibilityLevel, string> = {
  FULLY_ACCESSIBLE: '#16a34a',
  PARTIALLY_ACCESSIBLE: '#eab308',
  RESTRICTED: '#f97316',
  CUT_OFF: '#dc2626',
}

export function severityBadge(severity: Severity): string {
  switch (severity) {
    case 'CRITICAL': return 'bg-red-100 text-red-800 border-red-300'
    case 'HIGH': return 'bg-orange-100 text-orange-800 border-orange-300'
    case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 border-yellow-300'
    default: return 'bg-slate-100 text-slate-700 border-slate-300'
  }
}

export function deliveryBadge(status: DeliveryStatus): string {
  switch (status) {
    case 'DELIVERED': return 'bg-green-100 text-green-800 border-green-300'
    case 'IN_TRANSIT': return 'bg-blue-100 text-blue-800 border-blue-300'
    case 'DELAYED': return 'bg-red-100 text-red-800 border-red-300'
    default: return 'bg-slate-100 text-slate-700 border-slate-300'
  }
}
