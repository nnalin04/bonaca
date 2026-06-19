export const Colors = {
  headerGradientStart: '#090c2c',
  headerGradientEnd: '#555ec2',
  background: '#fafafa',
  cardBorder: '#e4e9e7',
  textPrimary: '#1f2d2b',
  textSecondary: '#4e535a',
  textOnDark: '#e1e7ef',
  accent: '#575fb4',
  white: '#ffffff',
  badge: '#f05252',
  iconMuted: '#c5cdd6',
  // Notifications additions (Figma node 286:15753)
  avatarFallbackBackground: '#e9e9e9',
  avatarIcon: '#007366',
  // Member/Metric Details additions (Figma nodes 196:4233, 197:1137)
  tabBarTrack: '#f0f3ff',
  tabBarBorder: '#edeeff',
  chartLine: '#e07a5f',
  chartAreaFill: 'rgba(224, 122, 95, 0.1)',
  chartLineSecondary: '#3a7f7c',
  chartAreaFillSecondary: 'rgba(58, 127, 124, 0.1)',
  insightCardBackground: 'rgba(213, 213, 81, 0.1)',
  // Login & Onboarding additions (Figma nodes 43:3178, 49:268, 49:364, 60:595, 60:634)
  inputBorder: '#e3e4e6',
  inputBorderSubtle: '#ebebeb',
  placeholderText: '#727779',
  error: '#d63d3d',
  avatarPlaceholderBg: '#e9e9e9',
  avatarPlaceholderFg: '#007367',
  // Select Wearable Account / Payment Gateway additions (Figma nodes 197:10387, 197:11178, 222:1723, 225:3615, 197:10384, 197:11043)
  rowBorder: '#e6e5e5',
  errorBackground: '#f6e7e7',
  errorBorder: '#d63d3d',
  errorText: '#d63d3d',
  link: '#575fb4',
  // Profile wearable-connect card additions (Figma node 39:2025)
  wearableCardBackground: '#101010',
  wearableCardIconBackground: '#1c1d2b',
} as const;

export const Radii = {
  card: 16,
  pill: 48,
  headerCorner: 24,
  cta: 8,
  cardTop: 48,
  button: 12,
  row: 12,
  toast: 8,
} as const;

export const Fonts = {
  family: 'DM Sans',
} as const;
