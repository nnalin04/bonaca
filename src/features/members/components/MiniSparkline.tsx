import Svg, { Path, Polygon } from 'react-native-svg';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface MiniSparklineProps {
  /** Normalized 0–1 points describing the trend line, left to right. */
  points: number[];
  width?: number;
  height?: number;
  lineColor?: string;
  areaColor?: string;
  /** Optional labels rendered evenly spaced under the chart, e.g. ['6AM', '12PM', '6PM', '12AM']. */
  xAxisLabels?: string[];
}

/** Lightweight area sparkline used on chart-style metric cards (Heart Rate, HRV, VO2 Max). */
export function MiniSparkline({
  points,
  width = 152,
  height = 54,
  lineColor = Colors.chartLine,
  areaColor = Colors.chartAreaFill,
  xAxisLabels,
}: MiniSparklineProps) {
  if (points.length < 2) return null;

  const stepX = width / (points.length - 1);
  const coords = points.map(
    (p, i) => [i * stepX, height - p * height] as const,
  );

  const linePath = coords
    .map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`)
    .join(' ');

  const areaPoints = [
    ...coords.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`),
    `${width},${height}`,
    `0,${height}`,
  ].join(' ');

  return (
    <View>
      <Svg width={width} height={height}>
        <Polygon points={areaPoints} fill={areaColor} />
        <Path
          d={linePath}
          stroke={lineColor}
          strokeWidth={2}
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </Svg>
      {xAxisLabels ? (
        <View style={[styles.axisRow, { width }]}>
          {xAxisLabels.map((label) => (
            <Text key={label} style={styles.axisLabel}>
              {label}
            </Text>
          ))}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  axisRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 4,
  },
  axisLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 10,
    color: Colors.textSecondary,
  },
});
