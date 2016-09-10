package org.nla.tarotdroid.ui.cloud;

import com.google.common.base.Predicate;

import org.nla.tarotdroid.biz.GameSet;

public class GameSetToUploadForModificationPredicate implements Predicate<GameSet> {

	@Override
	public boolean apply(GameSet gameSet) {
		return gameSet.getSyncTimestamp() == null && gameSet.getSyncAccount() != null;
	}
}