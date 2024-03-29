package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.VolleyError;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utilities.AnimationUtility;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private Cursor mCursor;
    private boolean mIsRefreshing = false;
    private BroadcastReceiver mRefreshingReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                        mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                        updateRefreshingUI();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        ButterKnife.bind(this);

        mAdapter = new Adapter(mCursor);

        mAdapter.setHasStableIds(true);

        mRecyclerView.setAdapter(mAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(
                mRefreshingReceiver, new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        DynamicHeightImageView thumbnailView;

        @BindView(R.id.article_title)
        TextView titleView;

        @BindView(R.id.article_subtitle)
        TextView subtitleView;

        @BindView(R.id.meta_bar)
        LinearLayout metaBar;

        ImageLoader imageLoader;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            this.imageLoader = ImageLoader.getInstance(AppMain.sContext);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        public void swapCursor(Cursor cursor) {
            if (cursor != null) {
                mCursor = cursor;
                notifyDataSetChanged();
            }
        }

        @Override
        public long getItemId(int position) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getLong(ArticleLoader.Query._ID);
            }

            return 0;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(
                    view1 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                            Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setData(ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));

                            startActivity(
                                    intent,
                                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                                            ArticleListActivity.this,
                                            vh.thumbnailView,
                                            vh.thumbnailView.getTransitionName())
                                            .toBundle());

                        } else {
                            startActivity(
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
                    });
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(),
                            DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL)
                            .toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));

            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            holder
                    .imageLoader
                    .getImageLoader()
                    .get(
                            mCursor.getString(ArticleLoader.Query.THUMB_URL),
                            new com.android.volley.toolbox.ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(com.android.volley.toolbox.ImageLoader.ImageContainer imageContainer, boolean b) {
                                    Bitmap bitmap = imageContainer.getBitmap();
                                    if (bitmap != null) {
                                        Palette.Builder builder = new Palette.Builder(bitmap);
                                        Palette p = builder.generate();

                                        TransitionDrawable td =
                                                AnimationUtility.createTransitionDrawble(ArticleListActivity.this, bitmap);

                                        holder.thumbnailView.setImageDrawable(td);
                                        td.startTransition(300);
                                        if (!getResources().getBoolean(R.bool.isTablet)) {
                                            holder.metaBar.setBackgroundColor(p.getDarkMutedColor(0xFF333333));
                                        }
                                    }
                                }

                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                }
                            });
        }

        @Override
        public int getItemCount() {
            return mCursor != null ? mCursor.getCount() : 0;
        }
    }
}
