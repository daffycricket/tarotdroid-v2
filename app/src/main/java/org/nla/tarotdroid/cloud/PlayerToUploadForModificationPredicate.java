package org.nla.tarotdroid.cloud;

import com.google.common.base.Predicate;

import org.nla.tarotdroid.biz.Player;

/**
 * Predicate to figure which players to upload t the cloud.
 */
public class PlayerToUploadForModificationPredicate implements Predicate<Player> {
	
	/* (non-Javadoc)
	 * @see ch.lambdaj.function.matcher.Predicate#apply(java.lang.Object)
	 */
	@Override
	public boolean apply(Player player) {

		return player.getSyncTimestamp() == null && player.getSyncAccount() != null;
	}
}