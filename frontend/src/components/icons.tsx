import type { SVGProps } from 'react'

/**
 * Inline SVG icons.
 *
 * Deliberately hand-rolled rather than pulling in an icon package: the interface needs
 * about a dozen glyphs, and a dependency would add hundreds of kilobytes to a bundle
 * that a field officer may be downloading over a 2G connection in a valley.
 *
 * All icons share a 24x24 viewBox and inherit `currentColor`, so they take the colour of
 * whatever text they sit next to.
 */

type IconProps = SVGProps<SVGSVGElement>

function Icon({ children, ...props }: IconProps & { children: React.ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      width={18}
      height={18}
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  )
}

export const IconDashboard = (p: IconProps) => (
  <Icon {...p}><rect x="3" y="3" width="7" height="9" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" /><rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="16" width="7" height="5" rx="1.5" /></Icon>
)
export const IconMap = (p: IconProps) => (
  <Icon {...p}><path d="M9 4 3 6.5v13L9 17l6 2.5 6-2.5v-13L15 6.5 9 4Z" /><path d="M9 4v13M15 6.5v13" /></Icon>
)
export const IconTruck = (p: IconProps) => (
  <Icon {...p}><path d="M3 7h11v9H3zM14 10h4l3 3v3h-7z" /><circle cx="7" cy="18" r="1.8" /><circle cx="17" cy="18" r="1.8" /></Icon>
)
export const IconPackage = (p: IconProps) => (
  <Icon {...p}><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z" /><path d="m4 7.5 8 4.5 8-4.5M12 12v9" /></Icon>
)
export const IconAlertTriangle = (p: IconProps) => (
  <Icon {...p}><path d="M10.3 4.3 2.6 17.5A2 2 0 0 0 4.3 20.5h15.4a2 2 0 0 0 1.7-3L13.7 4.3a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4.5M12 17h.01" /></Icon>
)
export const IconPin = (p: IconProps) => (
  <Icon {...p}><path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11Z" /><circle cx="12" cy="10" r="2.6" /></Icon>
)
export const IconRoute = (p: IconProps) => (
  <Icon {...p}><circle cx="6" cy="18" r="2.5" /><circle cx="18" cy="6" r="2.5" /><path d="M8.5 18H14a3.5 3.5 0 0 0 0-7h-4a3.5 3.5 0 0 1 0-7h5.5" /></Icon>
)
export const IconBrain = (p: IconProps) => (
  <Icon {...p}><path d="M9 4.5A2.5 2.5 0 0 0 6.5 7 2.5 2.5 0 0 0 5 11.5c0 1 .4 1.9 1 2.5a2.5 2.5 0 0 0 2 4c.6.6 1.4 1 2.3 1h.2V4.8A2.4 2.4 0 0 0 9 4.5Z" /><path d="M15 4.5A2.5 2.5 0 0 1 17.5 7 2.5 2.5 0 0 1 19 11.5c0 1-.4 1.9-1 2.5a2.5 2.5 0 0 1-2 4c-.6.6-1.4 1-2.3 1h-.2V4.8a2.4 2.4 0 0 1 1.5-.3Z" /></Icon>
)
export const IconBell = (p: IconProps) => (
  <Icon {...p}><path d="M18 8.5a6 6 0 1 0-12 0c0 6-2 7-2 7h16s-2-1-2-7Z" /><path d="M13.7 20a2 2 0 0 1-3.4 0" /></Icon>
)
export const IconUsers = (p: IconProps) => (
  <Icon {...p}><circle cx="9" cy="8" r="3.2" /><path d="M2.5 20a6.5 6.5 0 0 1 13 0" /><path d="M16.5 5.2a3.2 3.2 0 0 1 0 5.6M18 20a6.5 6.5 0 0 0-2.2-4.9" /></Icon>
)
export const IconLogout = (p: IconProps) => (
  <Icon {...p}><path d="M15 17l5-5-5-5M20 12H9M11 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h5" /></Icon>
)
export const IconWifiOff = (p: IconProps) => (
  <Icon {...p}><path d="m2 2 20 20M8.8 15.8a4.5 4.5 0 0 1 6.4 0M5.5 12.5a9 9 0 0 1 3.3-2.1M18.5 12.5a9 9 0 0 0-2.4-1.7M2.5 9.2A14 14 0 0 1 7 6.5M21.5 9.2A14 14 0 0 0 13 5.2" /><path d="M12 19h.01" /></Icon>
)
