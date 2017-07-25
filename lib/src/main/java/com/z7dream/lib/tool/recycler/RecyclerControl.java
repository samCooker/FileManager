package com.z7dream.lib.tool.recycler;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.z7dream.lib.base.mvp.BaseAppli;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


/**
 * Created by xiaoyu.zhang on 2016/6/30.
 */

public class RecyclerControl {
    private boolean isRefComplete = true, isLoadMoreComplete = true;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayoutManager linearLayoutManager;
    private GridLayoutManager gridLayoutManager;
    private OnControlGetDataListListener onControlGetDataListListener;
    private OnScrollListener onScrollListener;
    private Disposable delay1Disposable, delay2Disposable, delay3Disposable;
    private GestureDetector mGestureDetector;
    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener;
    private boolean isSwipeRefreshLayoutEnable;

    public static final int[] COLORS = {0xff2196F3};

    /**
     * @param swipeRefreshLayout
     * @param layoutManager
     * @param onControlGetDataListListener
     */
    public RecyclerControl(SwipeRefreshLayout swipeRefreshLayout,
                           LinearLayoutManager layoutManager, OnControlGetDataListListener onControlGetDataListListener) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.linearLayoutManager = layoutManager;
        this.onControlGetDataListListener = onControlGetDataListListener;

        onScrollListener = new OnScrollListener();
        swipeRefreshLayout.setColorSchemeColors(COLORS);

    }

    public RecyclerControl(SwipeRefreshLayout swipeRefreshLayout,
                           GridLayoutManager gridLayoutManager, OnControlGetDataListListener onControlGetDataListListener) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.gridLayoutManager = gridLayoutManager;
        this.onControlGetDataListListener = onControlGetDataListListener;

        onScrollListener = new OnScrollListener();
        swipeRefreshLayout.setColorSchemeColors(COLORS);
    }


    public void setSwipeRefreshLayoutEnable(boolean b) {
        isSwipeRefreshLayoutEnable = b;
        swipeRefreshLayout.setEnabled(isSwipeRefreshLayoutEnable);
    }

    public void onRefresh() {
        if (isRefComplete) {
            isRefComplete = false;
            delayGetData(true);
        }
    }

    public void getDataComplete(boolean isRef) {
        if (isRef) {
            isRefComplete = true;
        } else {
            isLoadMoreComplete = true;
        }
        delayComplete();
    }

    public boolean isRefComplete() {
        return isRefComplete;
    }

    public OnScrollListener getOnScrollListener() {
        return onScrollListener;
    }

    public void destory() {
        if (delay1Disposable != null && !delay1Disposable.isDisposed()) {
            delay1Disposable.dispose();
        }
        if (delay2Disposable != null && !delay2Disposable.isDisposed()) {
            delay2Disposable.dispose();
        }
        if (delay3Disposable != null && !delay3Disposable.isDisposed()) {
            delay3Disposable.dispose();
        }

        swipeRefreshLayout = null;
        linearLayoutManager = null;
        gridLayoutManager = null;
        onControlGetDataListListener = null;
        onScrollListener = null;
    }

    private void delayGetData(final boolean isRef) {
        delay1Disposable = Observable.just(1).delay(100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    if (isSwipeRefreshLayoutEnable) {
                        swipeRefreshLayout.setEnabled(false);
                    }
                    swipeRefreshLayout.setRefreshing(true);
                    delay2Disposable = Observable.just(1).delay(200, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(integer1 -> {
                                onControlGetDataListListener.onControlGetDataList(isRef);
                            }, error -> {
                            });
                }, error -> {
                });

    }

    private void delayComplete() {
        delay3Disposable = Observable.just(1).delay(300, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    if (swipeRefreshLayout == null) return;
                    if (isSwipeRefreshLayoutEnable) {
                        swipeRefreshLayout.setEnabled(true);
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (isLoadMoreComplete) {
                int lastViewPos = -1;
                if (linearLayoutManager != null)
                    lastViewPos = linearLayoutManager.findLastVisibleItemPosition();
                if (gridLayoutManager != null)
                    lastViewPos = gridLayoutManager.findLastVisibleItemPosition();

                //当最后一个item的pos小于总item数量-1
                if (lastViewPos >= 0 && lastViewPos >= (recyclerView.getAdapter().getItemCount() - 1)) {
                    if (isLoadMoreComplete) {
                        isLoadMoreComplete = false;
                        delayGetData(false);
                    }
                }
            }
            if (onControlGetDataListListener != null)
                onControlGetDataListListener.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (onControlGetDataListListener != null)
                onControlGetDataListListener.onScrolled(recyclerView, dx, dy);
        }
    }

    public void openGestureDetector(RecyclerView recyclerView, GestureListener listener) {
        if (simpleOnGestureListener == null) {
            simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return listener.onFling(e1, e2, velocityX, velocityY);
                }
            };
        }

        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(BaseAppli.getContext(), simpleOnGestureListener);
        }
        recyclerView.setOnTouchListener((v, event) -> {
            if (mGestureDetector != null) {
                return mGestureDetector.onTouchEvent(event);
            }
            return false;
        });
    }

    public interface GestureListener {
        boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
    }

    public interface OnControlGetDataListListener {
        /**
         * 调用网路数据方法
         *
         * @param isRef 是否是刷新
         */
        void onControlGetDataList(boolean isRef) throws InterruptedException;

        void onScrollStateChanged(RecyclerView recyclerView, int newState);

        void onScrolled(RecyclerView recyclerView, int dx, int dy);
    }
}
