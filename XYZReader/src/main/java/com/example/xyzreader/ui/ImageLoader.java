package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.collection.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ImageLoader {
    private static ImageLoader sInstance;
    private final LruCache<String, Bitmap> mImageCache = new LruCache<String, Bitmap>(20);
    private com.android.volley.toolbox.ImageLoader mImageLoader;

    private ImageLoader(Context applicationContext) {
        RequestQueue queue = Volley.newRequestQueue(applicationContext);
        com.android.volley.toolbox.ImageLoader.ImageCache imageCache =
                new com.android.volley.toolbox.ImageLoader.ImageCache() {
                    @Override
                    public void putBitmap(String key, Bitmap value) {
                        mImageCache.put(key, value);
                    }

                    @Override
                    public Bitmap getBitmap(String key) {
                        return mImageCache.get(key);
                    }
                };
        mImageLoader = new com.android.volley.toolbox.ImageLoader(queue, imageCache);
    }

    public static ImageLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoader(context.getApplicationContext());
        }

        return sInstance;
    }

    public com.android.volley.toolbox.ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
