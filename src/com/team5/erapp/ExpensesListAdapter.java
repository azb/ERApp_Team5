package com.team5.erapp;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.team5.erapp.R;
import com.google.cloud.backend.core.CloudEntity;

/**
 * This ArrayAdapter uses CloudEntities as items and displays them as a post in
 * the guestbook. Layout uses row.xml.
 *
 */
public class ExpensesListAdapter extends ArrayAdapter<CloudEntity> {

    private LayoutInflater mInflater;

    /**
     * Creates a new instance of this adapter.
     *
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public ExpensesListAdapter(Context context, int textViewResourceId, List<CloudEntity> objects) {
        super(context, textViewResourceId, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ?
                convertView : mInflater.inflate(R.layout.row_post, parent, false);

        CloudEntity ce = getItem(position);
        if (ce != null) {
            TextView message = (TextView) view.findViewById(R.id.messageContent);
            TextView description = (TextView) view.findViewById(R.id.textView_description);
            TextView date = (TextView) view.findViewById(R.id.textView_date);
            TextView signature = (TextView) view.findViewById(R.id.signature);
            if (message != null) {
                message.setText(ce.get("price").toString());
            }
            if(description != null && ce.get("description") != null) {
            	description.setText(ce.get("description").toString());
            }
            if(date != null) {
            	date.setText(ce.getUpdatedAt().toString().substring(0,10));
            }
            if (signature != null) {
                signature.setText(getAuthor(ce));
            }
        }

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return this.getView(position, convertView, parent);
    }

    /**
     * Gets the author field of the CloudEntity.
     *
     * @param post the CloudEntity
     * @return author string
     */
    private String getAuthor(CloudEntity post) {
        if (post.getCreatedBy() != null) {
            return post.getCreatedBy().replaceFirst("@.*", "");
        } else {
            return "<anonymous>";
        }
    }
}
