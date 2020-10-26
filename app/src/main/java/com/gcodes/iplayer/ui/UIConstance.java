package com.gcodes.iplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gcodes.iplayer.R;

public class UIConstance {

    public static class AppItemDecorator extends RecyclerView.ItemDecoration{

        public int getSpan() {
            return span;
        }

        public static final int DEFAULT_TOP = 20;
        public static final int DEFAULT_BOTTOM = 400;

        private final int span;
        private final int offset;
        private final int top;
        private final int bottom;

        public static AppItemDecorator AppItemDecoratorToolBarOffset(Context context)
        {
            return AppItemDecoratorToolBarOffset( context, DEFAULT_TOP, DEFAULT_BOTTOM );
        }

        public static AppItemDecorator AppItemDecoratorToolBarOffset(Context context, int topOffset, int bottomOffset )
        {
            int mActionBarSize = getToolBarOffset( context );
            return new AppItemDecorator( 1, 0, mActionBarSize + topOffset, bottomOffset );
        }

        public AppItemDecorator(int span)
        {
            this( span, 0 );
        }

        public AppItemDecorator(int span, int offset )
        {
            this( span, offset, DEFAULT_TOP, DEFAULT_BOTTOM );
        }

        public AppItemDecorator(int span, int offset, int top, int bottom )
        {
            this.span = span;
            this.offset = offset;
            this.top = top;
            this.bottom = bottom;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.top = offset;
            outRect.bottom = offset;

            if (parent.getChildAdapterPosition(view) < getSpan() )
                outRect.top = top;
            if (parent.getChildAdapterPosition(view) >= parent.getAdapter().getItemCount() - ( getSpan() - parent.getAdapter().getItemCount() % getSpan() ) )
                outRect.bottom = bottom;
        }
    }

    public static class HorizontalSpanDecorator extends RecyclerView.ItemDecoration
    {
        private final int span;
        private final int offset;

        public HorizontalSpanDecorator()
        {
            this.span = 20;
            this.offset = 30;
        }

        public HorizontalSpanDecorator(int span, int offset )
        {
            this.span = span;
            this.offset = offset;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.left = 0;
            outRect.right = span;
            outRect.top = span;

            if (parent.getChildAdapterPosition(view) == 0 )
                outRect.left = offset;
            if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() -1)
                outRect.right = offset;
        }
    }

    public static class AlternateItemDecorator extends RecyclerView.ItemDecoration{
        private int index = 0;
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if ( index++ % 2 == 0 )
                view.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent13));
            else
                view.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.colorAccent));
        }
    }

    public static float getToolBarOffsetPixel(Context context)
    {
        return convertDpToPixel( getToolBarOffset(context), context );
    }

    public static int getToolBarOffset(Context context)
    {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        return (int) styledAttributes.getDimension(0, 0);
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

}
