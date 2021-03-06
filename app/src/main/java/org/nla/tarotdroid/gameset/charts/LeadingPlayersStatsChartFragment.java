package org.nla.tarotdroid.gameset.charts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.nla.tarotdroid.R;
import org.nla.tarotdroid.biz.Player;
import org.nla.tarotdroid.constants.FragmentParameters;

import java.util.Map;

public class LeadingPlayersStatsChartFragment extends ChartFragment {
	
	public static LeadingPlayersStatsChartFragment newInstance(Context context) {
		LeadingPlayersStatsChartFragment fragment = new LeadingPlayersStatsChartFragment();
		Bundle arguments = new Bundle();
		arguments.putString(FragmentParameters.CHART_TITLE,
							context.getResources()
								   .getString(R.string.statNameLeadingPlayerFrequency));
		fragment.setArguments(arguments);
		return fragment;
	}
	
	protected CategorySeries buildCategoryDataset(final Map<Player, Integer> mapPlayerValues) {
		CategorySeries series = new CategorySeries("Called players 1");
		for (Player player : mapPlayerValues.keySet()) {
			series.add(player.getName() + " (" + mapPlayerValues.get(player) + ")", mapPlayerValues.get(player));
		}
		return series;
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
	    return ChartFactory.getPieChartView(
				getActivity(),
				buildCategoryDataset(statisticsComputer.getLeadingCount()),
				buildCategoryRenderer(statisticsComputer.getLeadingCountColors())
		);
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
    	// HACK found on http://code.google.com/p/android/issues/detail?id=19917 to prevent error "Unable to pause activity" (described on web site) 
    	outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
    	super.onSaveInstanceState(outState);
    }
}
