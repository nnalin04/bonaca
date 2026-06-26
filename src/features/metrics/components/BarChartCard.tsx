import Svg, { Line, Rect } from 'react-native-svg';
import { Dimensions, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface BarChartCardProps {
  /** Normalized 0–1 bar heights, left to right. */
  values: number[];
  maxLabel: string;
  minLabel: string;
  xAxisLabels: string[];
}

const CHART_WIDTH = Dimensions.get('window').width - 64;
const CHART_HEIGHT = 280;
const BAR_WIDTH = 6;
const BAR_GAP = 6;
const BAR_START_X = 5;

export function BarChartCard({
  values,
  maxLabel,
  minLabel,
  xAxisLabels,
}: BarChartCardProps) {
  const gridLineYs = [0, 0.25, 0.5, 0.75, 1].map((t) => t * CHART_HEIGHT);

  const verticalGridXs = [CHART_WIDTH / 3, (CHART_WIDTH * 2) / 3];

  return (
    <View style={styles.card}>
      <View style={styles.chartFrame}>
        <Svg width={CHART_WIDTH} height={CHART_HEIGHT} style={styles.chart}>
          <Rect
            x={0}
            y={0}
            width={CHART_WIDTH}
            height={CHART_HEIGHT}
            rx={12}
            fill={Colors.chartAreaFill}
          />
          {gridLineYs.map((y) => (
            <Line
              key={y}
              x1={0}
              y1={y}
              x2={CHART_WIDTH}
              y2={y}
              stroke={Colors.chartGrid}
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
              stroke={Colors.chartGrid}
              strokeWidth={1}
              strokeDasharray="4,4"
            />
          ))}
          {values.map((value, index) => {
            const barHeight = Math.max(4, value * CHART_HEIGHT);
            const x = BAR_START_X + index * (BAR_WIDTH + BAR_GAP);
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

        <Text style={styles.maxLabel}>{maxLabel}</Text>
        <Text style={styles.minLabel}>{minLabel}</Text>
      </View>

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
    height: 334,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 16,
  },
  chartFrame: {
    width: CHART_WIDTH,
    height: CHART_HEIGHT,
    alignSelf: 'center',
    borderRadius: 12,
    overflow: 'hidden',
  },
  chart: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
  },
  maxLabel: {
    position: 'absolute',
    top: 6,
    left: 6,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textPrimary,
  },
  minLabel: {
    position: 'absolute',
    left: 6,
    bottom: 58,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textPrimary,
  },
  xAxisRow: {
    width: CHART_WIDTH,
    alignSelf: 'center',
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
