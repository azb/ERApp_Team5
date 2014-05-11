package com.team5.erapp;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.cloud.backend.core.CloudEntity;

public class EmployeesListAdapter extends ArrayAdapter<CloudEntity> {

	private LayoutInflater mInflater;

	/**
	 * Creates a new instance of this adapter.
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public EmployeesListAdapter(Context context, int textViewResourceId, List<CloudEntity> objects) {
		super(context, textViewResourceId, objects);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView != null ? convertView : mInflater.inflate(R.layout.row_employee, parent, false);

		CloudEntity ce = getItem(position);
		if (ce != null) {
			TextView name = (TextView) view.findViewById(R.id.row_name);
			TextView email = (TextView) view.findViewById(R.id.row_email);
			name.setText(ce.get("name").toString());
			email.setText(ce.get("email").toString());
		}
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return this.getView(position, convertView, parent);
	}
}
