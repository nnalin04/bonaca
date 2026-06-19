import Svg, { Line, Rect } from 'react-native-svg';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface BarChartCardProps {
  /** Normalized 0–1 bar heights, left to right. */
  values: number[];
  maxLabel: string;
  minLabel: string;
  xAxisLabels: string[];
}

const CHART_WIDTH = 326;
const CHART_HEIGHT = 280;
const BAR_WIDTH = 6;
const BAR_GAP = 6;

export function BarChartCard({
  values,
  maxLabel,
  minLabel,
  xAxisLabels,
}: BarChartCardProps) {
  const totalBarSpan = values.length * (BAR_WIDTH + BAR_GAP) - BAR_GAP;
  const startX = Math.max(0, (CHART_WIDTH - totalBarSpan) / 2);
  const gridLineYs = [0, 0.25, 0.5, 0.75, 1].map((t) => t * CHART_HEIGHT);

  const verticalGridXs = [CHART_WIDTH / 3, (CHART_WIDTH * 2) / 3];

  return (
    <View style={styles.card}>
      <Text style={styles.maxLabel}>{maxLabel}</Text>

      <Svg width={CHART_WIDTH} height={CHART_HEIGHT} style={styles.chart}>
        <Rect x={0} y={0} width={CHART_WIDTH} height={CHART_HEIGHT} fill={Colors.chartAreaFill} />
        {gridLineYs.map((y) => (
          <Line
            key={y}
            x1={0}
            y1={y}
            x2={CHART_WIDTH}
            y2={y}
            stroke={Colors.cardBorder}
            strokeWidth={1}
          />
        ))}
        {verticalGridXs.map((x) => (
          <Line
            key={x}
            x1={x}
            y1={0}
            x2={x}
            y2={CHART_HEIGHT}
            stroke={Colors.cardBorder}
            strokeWidth={1}
            strokeDasharray="4,4"
          />
        ))}
        {values.map((value, index) => {
          const barHeight = Math.max(4, value * CHART_HEIGHT);
          const x = startX + index * (BAR_WIDTH + BAR_GAP);
          const y = CHART_HEIGHT - barHeight;
          return (
            <Rect
              key={index}
              x={x}
              y={y}
              width={BAR_WIDTH}
              height={barHeight}
              rx={5}
              fill={Colors.chartLine}
            />
          );
        })}
      </Svg>

      <Text style={styles.minLabel}>{minLabel}</Text>

      <View style={styles.xAxisRow}>
        {xAxisLabels.map((label) => (
          <Text key={label} style={styles.xAxisLabel}>
            {label}
          </Text>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 16,
    gap: 4,
  },
  chart: {
    alignSelf: 'center',
  },
  maxLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textPrimary,
  },
  minLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textPrimary,
  },
  xAxisRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 4,
  },
  xAxisLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
});
