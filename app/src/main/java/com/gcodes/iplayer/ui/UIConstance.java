package com.gcodes.iplayer.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UIConstance {

    public static class AppItemDecorator extends RecyclerView.ItemDecoration{

        public int getSpan() {
            return span;
        }

        private final int span;

        public AppItemDecorator(int span )
        {
            this.span = span;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.top = 10;
            outRect.bottom = 10;

            if (parent.getChildAdapterPosition(view) < getSpan() )
                outRect.top = 20;
            if (parent.getChildAdapterPosition(view) >= parent.getAdapter().getItemCount() - ( getSpan() - parent.getAdapter().getItemCount() % getSpan() ) )
                outRect.bottom = 400;
        }
    }

}
