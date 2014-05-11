package com.team5.erapp;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

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
		View view = convertView != null ? convertView : mInflater.inflate(R.layout.row_expense, parent, false);

		CloudEntity ce = getItem(position);
		@SuppressWarnings("unchecked")
		List<Object> a = (List<Object>) ce.get("ex");
		if (ce != null) {
			TextView price = (TextView) view.findViewById(R.id.row_price);
			TextView description = (TextView) view.findViewById(R.id.row_description);
			TextView date = (TextView) view.findViewById(R.id.row_date);
			double amount = Double.parseDouble(a.get(0).toString());
			amount = Math.round(amount);
			if (amount >= 0) {
				price.setText("$" + NumberFormat.getNumberInstance(Locale.US).format(amount));
			} else {
				price.setText("empty");
			}
			if (!a.get(2).toString().equals("")) {
				description.setText(a.get(2).toString());
			} else {
				description.setText("No description");
			}
			date.setText(ce.getCreatedAt().toString().substring(4, 10));
		}
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return this.getView(position, convertView, parent);
	}
}
