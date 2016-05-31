package com.github.mikephil.charting.highlight;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.utils.PointD;
import com.github.mikephil.charting.utils.SelectionDetail;

/**
 * Created by Philipp Jahoda on 22/07/15.
 */
public class BarHighlighter extends ChartHighlighter<BarDataProvider> {

    public BarHighlighter(BarDataProvider chart) {
        super(chart);
    }

    @Override
    public Highlight getHighlight(float x, float y) {

        BarData barData = mChart.getBarData();

        PointD pos = getValsForTouch(x, y);

        SelectionDetail selectionDetail = getSelectionDetail((float) pos.x, x, y);
        if (selectionDetail == null)
            return null;

        IBarDataSet set = barData.getDataSetByIndex(selectionDetail.dataSetIndex);
        if (set.isStacked()) {

            return getStackedHighlight(selectionDetail,
                    set,
                    (float) pos.x,
                    (float) pos.y);
        }

        return new Highlight(
                selectionDetail.xValue,
                selectionDetail.yValue,
                selectionDetail.dataIndex,
                selectionDetail.dataSetIndex,
                -1);
    }

//    @Override
//    protected float getXForTouch(float x) {
//
//        if (!mChart.getBarData().isGrouped()) {
//            return super.getXForTouch(x);
//        } else {
//            return getBase(x);
//
//            float baseNoSpace = getBase(x);
//
//            int setCount = mChart.getBarData().getDataSetCount();
//            int xIndex = (int) baseNoSpace / setCount;
//
//            int valCount = mChart.getData().getXValCount();
//
//            if (xIndex < 0)
//                xIndex = 0;
//            else if (xIndex >= valCount)
//                xIndex = valCount - 1;
//
//            return xIndex;
//        }
//    }

    @Override
    protected SelectionDetail getSelectionDetail(float xVal, float x, float y) {

        BarData barData = mChart.getBarData();

        int closestDataSetIndex = 0;
        float closestDistance = Float.MAX_VALUE;
        Entry closestEntry = null;

        for(int i = 0; i < barData.getDataSets().size(); i++) {

            IBarDataSet dataSet = barData.getDataSetByIndex(i);

            final Entry entry = dataSet.getEntryForXPos(xVal);

            final float distance = Math.abs(xVal - entry.getX());

            if(distance < closestDistance) {
                closestDataSetIndex = i;
                closestDistance = distance;
                closestEntry = entry;
            }
        }

        return new SelectionDetail(x, y, closestEntry.getX(),
                closestEntry.getY(),
                closestDataSetIndex,
                barData.getDataSetByIndex(closestDataSetIndex));
    }

    /**
     * This method creates the Highlight object that also indicates which yValue of a stacked BarEntry has been
     * selected.
     *
     * @param selectionDetail the selection detail to work with looking for stacked values
     * @param set
     * @param xVal
     * @param yValue
     * @return
     */
    protected Highlight getStackedHighlight(
            SelectionDetail selectionDetail,
            IBarDataSet set,
            float xVal,
            double yValue) {

        BarEntry entry = set.getEntryForXPos(xVal);

        if (entry == null)
            return null;

        // not stacked
        if (entry.getYVals() == null) {
            return new Highlight(entry.getX(),
                    entry.getY(),
                    selectionDetail.dataIndex,
                    selectionDetail.dataSetIndex);
        } else {
            Range[] ranges = getRanges(entry);

            if (ranges.length > 0) {
                int stackIndex = getClosestStackIndex(ranges, (float) yValue);
                return new Highlight(
                        entry.getX(),
                        entry.getPositiveSum() - entry.getNegativeSum(),
                        selectionDetail.dataIndex,
                        selectionDetail.dataSetIndex,
                        stackIndex,
                        ranges[stackIndex]
                );
            }
        }

        return null;
    }

    /**
     * Returns the index of the closest yValue inside the values array / ranges (stacked barchart) to the yValue
     * given as
     * a parameter.
     *
     * @param ranges
     * @param value
     * @return
     */
    protected int getClosestStackIndex(Range[] ranges, float value) {

        if (ranges == null || ranges.length == 0)
            return 0;

        int stackIndex = 0;

        for (Range range : ranges) {
            if (range.contains(value))
                return stackIndex;
            else
                stackIndex++;
        }

        int length = Math.max(ranges.length - 1, 0);

        return (value > ranges[length].to) ? length : 0;
    }

    /**
     * Splits up the stack-values of the given bar-entry into Range objects.
     *
     * @param entry
     * @return
     */
    protected Range[] getRanges(BarEntry entry) {

        float[] values = entry.getYVals();

        if (values == null || values.length == 0)
            return new Range[0];

        Range[] ranges = new Range[values.length];

        float negRemain = -entry.getNegativeSum();
        float posRemain = 0f;

        for (int i = 0; i < ranges.length; i++) {

            float value = values[i];

            if (value < 0) {
                ranges[i] = new Range(negRemain, negRemain + Math.abs(value));
                negRemain += Math.abs(value);
            } else {
                ranges[i] = new Range(posRemain, posRemain + value);
                posRemain += value;
            }
        }

        return ranges;
    }
}