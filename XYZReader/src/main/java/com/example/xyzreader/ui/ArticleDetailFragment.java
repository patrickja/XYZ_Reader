package com.example.xyzreader.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;

import com.android.volley.VolleyError;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.utilities.AnimationUtility;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment representing a single Article detail screen. This fragment is either contained in a
 * {@link ArticleListActivity} in two-pane mode (on tablets) or a {@link ArticleDetailActivity} on
 * handsets.
 */
public class ArticleDetailFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";
    View mRootView;
    Window mWindow;
    @BindView(R.id.photo)
    ImageView mPhotoView;
    @BindView(R.id.detail_toolbar)
    Toolbar mToolbar;
    @Nullable
    @BindView(R.id.card_toolbar)
    Toolbar mCardToolbar;
    @BindView(R.id.share_fab)
    FloatingActionButton mFab;
    @BindView(R.id.article_title)
    TextView mTitleView;
    @BindView(R.id.article_byline)
    TextView mBylineView;
    @BindView(R.id.article_body)
    TextView mBodyView;
    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.meta_bar)
    LinearLayout mTitleByLineLinearLayout;
    private boolean flagIsCard;
    private Cursor mCursor;
    private long mItemId;
    private int mMutedColor = 0xFF333333;
    private AppCompatActivity mActivity;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html) {
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        flagIsCard = getResources().getBoolean(R.bool.detail_is_card);

        setHasOptionsMenu(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide(Gravity.BOTTOM);
            getActivity().getWindow().setReenterTransition(slide);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initToolbar();
        getLoaderManager().initLoader(0, null, this);

        if (Build.VERSION.SDK_INT >= 21) {
            mWindow = getActivity().getWindow();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof AppCompatActivity) {
            mActivity = (AppCompatActivity) context;
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        ButterKnife.bind(this, mRootView);

        mFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(
                                Intent.createChooser(
                                        ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                                                .getIntent(),
                                        getString(R.string.action_share)));
                    }
                });
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        final Toolbar toolbarTitle = flagIsCard ? mCardToolbar : mToolbar;

        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView.setTypeface(
                Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            Spanned byLineContent =
                    fromHtml(
                            DateUtils.getRelativeTimeSpanString(
                                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                                    System.currentTimeMillis(),
                                    DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL)
                                    .toString()
                                    + " by <font color='#ffffff'>"
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                    + "</font>");

            Spanned bodyContent = fromHtml(mCursor.getString(ArticleLoader.Query.BODY));

            collapsingToolbarLayout.setExpandedTitleColor(
                    getResources().getColor(android.R.color.transparent));
            mBylineView.setText(byLineContent);
            mBodyView.setText(bodyContent);
            ImageLoader.getInstance(getActivity())
                    .getImageLoader()
                    .get(
                            mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                            new com.android.volley.toolbox.ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(com.android.volley.toolbox.ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {

                                Palette.Builder builder = new Palette.Builder(bitmap);
                                Palette p = builder.generate();
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                TransitionDrawable td =
                                        AnimationUtility.createTransitionDrawble(AppMain.sContext, bitmap);
                                mPhotoView.setImageDrawable(td);
                                td.startTransition(300);

                                mTitleByLineLinearLayout.setBackgroundColor(mMutedColor);
                                toolbarTitle.setBackgroundColor(mMutedColor);
                                collapsingToolbarLayout.setContentScrimColor(mMutedColor);

                                if (Build.VERSION.SDK_INT >= 21 && mWindow != null) {
                                    mWindow.setStatusBarColor(mMutedColor);
                                }
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }
                            });
        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A");
            mBodyView.setText("N/A");
        }
    }

    private void initToolbar() {

        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            if (flagIsCard) {
                actionBar.setDisplayShowTitleEnabled(false);
            } else {
                actionBar.setDisplayShowTitleEnabled(true);
            }

            mToolbar.setNavigationOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getActivity().onBackPressed();
                        }
                    });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        } else {
            bindViews();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
  }
}
