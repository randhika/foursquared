/**
 * Copyright 2008 Joe LaPenna
 */

package com.joelapenna.foursquared.widget;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.Mayor;
import com.joelapenna.foursquare.types.User;
import com.joelapenna.foursquared.FoursquaredSettings;
import com.joelapenna.foursquared.R;
import com.joelapenna.foursquared.Sync;
import com.joelapenna.foursquared.util.RemoteResourceManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 */
public class MayorListAdapter extends BaseMayorAdapter implements ObservableAdapter {
    private static final String TAG = "MayorListAdapter";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;

    private LayoutInflater mInflater;

    private RemoteResourceManager mRrm;
    private Handler mHandler = new Handler();
    private RemoteResourceManagerObserver mResourcesObserver;
    private SyncObserver mSyncObserver;
    private Context mContext;
    private Sync mSync;

    public MayorListAdapter(Context context, RemoteResourceManager rrm, Sync sync) {
        super(context);
        mSync = sync;
        mInflater = LayoutInflater.from(context);
        mRrm = rrm;
        mResourcesObserver = new RemoteResourceManagerObserver();
        mSyncObserver = new SyncObserver();
        mSync.getObservable().addObserver(mSyncObserver);
        mRrm.addObserver(mResourcesObserver);
        mContext = context;
    }

    public void removeObserver() {
        mSync.getObservable().deleteObserver(mSyncObserver);
        mRrm.deleteObserver(mResourcesObserver);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary
        // calls to findViewById() on each row.
        final ViewHolder holder;
        Mayor mayor = (Mayor)getItem(position);
        final User user = mayor.getUser();

        // When convertView is not null, we can reuse it directly, there is no
        // need to re-inflate it. We only inflate a new View when the
        // convertView supplied by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.mayor_list_item, null);

            // Creates a ViewHolder and store references to the two children
            // views we want to bind data to.
            holder = new ViewHolder();

            holder.photo = (MaybeContactView)convertView.findViewById(R.id.photo);
            holder.firstLine = (TextView)convertView.findViewById(R.id.firstLine);
            holder.secondLine = (TextView)convertView.findViewById(R.id.mayorMessageTextView);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder)convertView.getTag();
        }

        holder.photo.setContactLookupUri(mSync.getContactLookupUri(mContext.getContentResolver(), user.getId()));

        final Uri photoUri = Uri.parse(user.getPhoto());

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(mRrm.getInputStream(photoUri));
            holder.photo.setImageBitmap(bitmap);
        } catch (IOException e) {
            if (Foursquare.MALE.equals(user.getGender())) {
                holder.photo.setImageResource(R.drawable.blank_boy);
            } else {
                holder.photo.setImageResource(R.drawable.blank_girl);
            }
        }

        holder.firstLine.setText(mayor.getUser().getFirstname());
        holder.secondLine.setText(mayor.getMessage());

        return convertView;
    }

    @Override
    public void setGroup(Group<Mayor> g) {
        super.setGroup(g);
        for (int i = 0; i < g.size(); i++) {
            Uri photoUri = Uri.parse((g.get(i)).getUser().getPhoto());
            if (!mRrm.exists(photoUri)) {
                mRrm.request(photoUri);
            }
        }
    }

    private class RemoteResourceManagerObserver implements Observer {
        @Override
        public void update(Observable observable, Object data) {
            if (DEBUG) Log.d(TAG, "Fetcher got: " + data);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    private class SyncObserver implements Observer {
        @Override
        public void update(Observable observable, Object o) {
            notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        MaybeContactView photo;
        TextView firstLine;
        TextView secondLine;
    }
}
