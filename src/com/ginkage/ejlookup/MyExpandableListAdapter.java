package com.ginkage.ejlookup;

import java.util.ArrayList;
import java.util.HashMap;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

class MyExpandableListAdapter extends BaseExpandableListAdapter {
	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	private Context context = null;
	private ArrayList<String> groups = new ArrayList<String>();
	private final HashMap<String, Integer> groupIdx = new HashMap<String, Integer>();
	private ArrayList<ArrayList<ResultLine>> children = new ArrayList<ArrayList<ResultLine>>();

	public MyExpandableListAdapter(Context context, ArrayList<String> groups, ArrayList<ArrayList<ResultLine>> children) {
		this.context = context;
		this.groups = groups;
		this.children = children;

		int idx = 0;
        for (String it : groups)
			groupIdx.put(it, idx++);
	}

	void addItem(ResultLine result) {
		String gname = result.getGroup();
		Integer idx = groupIdx.get(gname);
		int index = (idx == null ? groups.size() : idx);

		if (idx == null) {
			groups.add(gname);
			groupIdx.put(gname, index);
		}

		if (children.size() < index + 1)
			children.add(new ArrayList<ResultLine>());

		children.get(index).add(result);
	}

	public Object getChild(int groupPosition, int childPosition) {
		return children.get(groupPosition).get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		ResultLine result = (ResultLine) getChild(groupPosition, childPosition);
		TextView textView = new TextView(context);
		textView.setText(result.getData());
		textView.setTextSize(15.5f);
		textView.setPadding(7, 7, 7, 7);
		textView.setLineSpacing(0, 1.2f);
		if (ResultLine.theme_color == 1) {
			textView.setBackgroundColor(Color.rgb(255, 255, 255));
			textView.setTextColor(Color.rgb(0, 0, 0));
		}
		return textView;
	}

	public int getChildrenCount(int groupPosition) {
		return children.get(groupPosition).size();
	}

	public Object getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	public int getGroupCount() {
		return groups.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		String group = (String)getGroup(groupPosition);
		TextView textView = new TextView(context);
		textView.setGravity(Gravity.CENTER);
		textView.setMinLines(2);
		textView.setText(group);
		if (ResultLine.theme_color == 1) {
			textView.setBackgroundColor(Color.rgb(255, 255, 255));
			textView.setTextColor(Color.rgb(0, 0, 0));
		}
		return textView;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}

	public ArrayList<ArrayList<ResultLine>> getData()
	{
		return children;
	}

	public void setData(ArrayList<ResultLine> data)
	{
        for (ResultLine cit : data)
			addItem(cit);
	}
}