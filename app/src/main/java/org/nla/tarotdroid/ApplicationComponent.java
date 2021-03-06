package org.nla.tarotdroid;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import org.nla.tarotdroid.biz.GameSetParameters;
import org.nla.tarotdroid.core.AppParams;
import org.nla.tarotdroid.core.BaseActivity;
import org.nla.tarotdroid.core.BaseFragment;
import org.nla.tarotdroid.core.ExportDatabaseHelper;
import org.nla.tarotdroid.core.GameSetWrapper;
import org.nla.tarotdroid.core.ImportDatabaseHelper;
import org.nla.tarotdroid.core.dal.IDalService;
import org.nla.tarotdroid.core.helpers.AuditHelper;
import org.nla.tarotdroid.core.helpers.BluetoothHelper;
import org.nla.tarotdroid.core.helpers.LocalizationHelper;
import org.nla.tarotdroid.dashboard.MainDashboardActivity;
import org.nla.tarotdroid.dashboard.MainPreferencesActivity;
import org.nla.tarotdroid.dashboard.MainPreferencesFragment;
import org.nla.tarotdroid.dashboard.NewGameSetDashboardActivity;
import org.nla.tarotdroid.gameset.BaseGameActivity;
import org.nla.tarotdroid.gameset.CreateGameSetActivity;
import org.nla.tarotdroid.gameset.DisplayAndRemoveGameDialogActivity;
import org.nla.tarotdroid.gameset.GameReadViewPagerActivity;
import org.nla.tarotdroid.gameset.GameSetChartViewPagerActivity;
import org.nla.tarotdroid.gameset.GameSetGamesFragment;
import org.nla.tarotdroid.gameset.GameSetSynthesisFragment;
import org.nla.tarotdroid.gameset.StandardGameActivity;
import org.nla.tarotdroid.gameset.TabGameSetActivity;
import org.nla.tarotdroid.gameset.TabGameSetPreferencesActivity;
import org.nla.tarotdroid.gameset.TabGameSetPreferencesFragment;
import org.nla.tarotdroid.gameset.charts.BetsStatsChartFragment;
import org.nla.tarotdroid.gameset.charts.KingsStatsChartFragment;
import org.nla.tarotdroid.gameset.charts.SuccessesStatsChartFragment;
import org.nla.tarotdroid.gameset.controls.BaseRow;
import org.nla.tarotdroid.gameset.controls.BelgianTarotGameRow;
import org.nla.tarotdroid.gameset.controls.PenaltyGameRow;
import org.nla.tarotdroid.gameset.controls.PlayerSelectorRow;
import org.nla.tarotdroid.gameset.controls.StandardTarot5GameRow;
import org.nla.tarotdroid.gameset.controls.StandardTarotGameRow;
import org.nla.tarotdroid.history.GameSetHistoryActivity;
import org.nla.tarotdroid.players.PlayerListActivity;
import org.nla.tarotdroid.players.PlayerStatisticsActivity;
import org.nla.tarotdroid.splash.SplashActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(
        modules = {ApplicationModule.class}
)
public interface ApplicationComponent {

    void inject(TarotDroidApp tarotDroidApp);

    void inject(MainDashboardActivity mainDashboardActivity);

    void inject(GameSetHistoryActivity gameSetHistoryActivity);

    void inject(TabGameSetActivity tabGameSetActivity);

    void inject(CreateGameSetActivity createGameSetActivity);

    void inject(DisplayAndRemoveGameDialogActivity displayAndRemoveGameDialogActivity);

    void inject(GameReadViewPagerActivity gameReadViewPagerActivity);

    void inject(GameSetChartViewPagerActivity gameSetChartViewPagerActivity);

    void inject(GameSetGamesFragment gameSetGamesFragment);

    void inject(GameSetSynthesisFragment gameSetSynthesisFragment);

    void inject(StandardTarotGameRow standardTarotGameRow);

    void inject(StandardTarot5GameRow standardTarot5GameRow);

    void inject(BelgianTarotGameRow belgianTarotGameRow);

    void inject(PlayerSelectorRow playerSelectorRow);

    void inject(PenaltyGameRow penaltyGameRow);

    void inject(StandardGameActivity standardGameActivity);

    void inject(BaseGameActivity baseGameActivity);

    void inject(MainPreferencesActivity mainPreferencesActivity);

    void inject(TabGameSetPreferencesActivity tabGameSetPreferencesActivity);

    void inject(PlayerListActivity playerListActivity);

    void inject(PlayerStatisticsActivity playerStatisticsActivity);

    void inject(SuccessesStatsChartFragment successesStatsChartFragment);

    void inject(NewGameSetDashboardActivity newGameSetDashboardActivity);

    void inject(KingsStatsChartFragment kingsStatsChartFragment);

    void inject(BetsStatsChartFragment betsStatsChartFragment);

    void inject(SplashActivity splashActivity);

    void inject(TabGameSetPreferencesFragment tabGameSetPreferencesFragment);

    void inject(MainPreferencesFragment mainPreferencesFragment);

    void inject(BaseActivity baseActivity);

    void inject(BaseFragment baseFragment);

    void inject(BaseRow baseRow);

    Application application();

    Context context();

    SharedPreferences sharedPreferences();

    GameSetParameters gameSetParameters();

    GameSetWrapper gameSetWrapper();

    AppParams appParams();

    AuditHelper auditHelper();

    LocalizationHelper localizationHelper();

    BluetoothHelper bluetoothHelper();

    IDalService dalService();

    ImportDatabaseHelper importDatabaseHelper();

    ExportDatabaseHelper exportDatabaseHelper();

    final class Initializer {

        private Initializer() {
            // static class
        }

        static ApplicationComponent init(final TarotDroidApp app) {
            return DaggerApplicationComponent
                    .builder()
                    .applicationModule(new ApplicationModule(app))
                    .build();
        }
    }
}