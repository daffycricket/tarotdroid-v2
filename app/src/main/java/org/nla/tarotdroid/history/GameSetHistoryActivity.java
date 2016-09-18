package org.nla.tarotdroid.history;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.nla.tarotdroid.BuildConfig;
import org.nla.tarotdroid.R;
import org.nla.tarotdroid.TarotDroidApp;
import org.nla.tarotdroid.biz.GameSet;
import org.nla.tarotdroid.constants.ActivityParams;
import org.nla.tarotdroid.constants.RequestCodes;
import org.nla.tarotdroid.core.BaseActivity;
import org.nla.tarotdroid.core.IAsyncCallback;
import org.nla.tarotdroid.core.ThumbnailItem;
import org.nla.tarotdroid.core.dal.DalException;
import org.nla.tarotdroid.core.helpers.AuditHelper;
import org.nla.tarotdroid.core.helpers.AuditHelper.ErrorTypes;
import org.nla.tarotdroid.core.helpers.AuditHelper.EventTypes;
import org.nla.tarotdroid.core.helpers.BluetoothHelper;
import org.nla.tarotdroid.core.helpers.UIHelper;
import org.nla.tarotdroid.gameset.TabGameSetActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnItemClick;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class GameSetHistoryActivity extends BaseActivity {

    private static final Item[] allItems = {
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblEditGameSet),
                     android.R.drawable.ic_menu_edit,
                     Item.ItemTypes.edit),
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblDeleteGameSet),
                     R.drawable.gd_action_bar_trashcan,
                     Item.ItemTypes.remove),
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblBluetoothSend),
                     R.drawable.stat_sys_data_bluetooth,
                     Item.ItemTypes.transferOverBluetooth),
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblExcelExport),
                     R.drawable.ic_excel,
                     Item.ItemTypes.exportToExcel),
    };

    private static final Comparator<GameSet> gameSetCreationDateDescendingComparator = new Comparator<GameSet>() {

        @Override
        public int compare(final GameSet arg0, final GameSet arg1) {
            return (arg1.getCreationTs().compareTo(arg0.getCreationTs()));
        }
    };
    private static final Item[] limitedItems = {
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblEditGameSet),
                     android.R.drawable.ic_menu_edit,
                     Item.ItemTypes.edit),
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblDeleteGameSet),
                     R.drawable.gd_action_bar_trashcan,
                     Item.ItemTypes.remove),
            new Item(TarotDroidApp.get().getResources().getString(R.string.lblBluetoothSend),
                     R.drawable.stat_sys_data_bluetooth,
                     Item.ItemTypes.transferOverBluetooth)
    };
    private static final String PENDING_REAUTH_KEY = "pendingReauthRequest";
    @BindView(R.id.listView) protected ListView listView;

    private final IAsyncCallback<Object> refreshCallback = new IAsyncCallback<Object>() {

        @Override
        public void execute(Object isNull, Exception e) {
            // TODO Check if exception must not be handled
            GameSetHistoryActivity.this.refresh();
        }
    };
    @Inject BluetoothHelper bluetoothHelper;
    private boolean pendingReauthRequest;
    private ProgressDialog progressDialog;
    private final DialogInterface.OnClickListener removeAllGameSetsDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    RemoveAllGameSetsTask removeAllGameSetsTask = new RemoveAllGameSetsTask(
                            GameSetHistoryActivity.this,
                            GameSetHistoryActivity.this.progressDialog);
                    removeAllGameSetsTask.setCallback(refreshCallback);
                    removeAllGameSetsTask.execute();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                default:
                    break;
            }
        }
    };

    private ReceiveGameSetTask receiveGameSetTask;
    private SendGameSetTask sendGameSetTask;
    private String tempExcelFilePath;

    private final DialogInterface.OnClickListener exportExcelByEmailDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    try {
                        Uri uri = Uri.fromFile(new File(tempExcelFilePath));

                        StringBuilder contentText = new StringBuilder();
                        contentText.append(getString(R.string.lblDbExportEmailContent));

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");
                        intent.putExtra(Intent.EXTRA_SUBJECT,
                                        getString(R.string.lblDbExportEmailTitle));
                        intent.putExtra(Intent.EXTRA_TEXT, contentText.toString());
                        intent.putExtra(Intent.EXTRA_STREAM, uri);

                        startActivity(Intent.createChooser(intent,
                                                           getString(R.string.lblDbExportAndroidIntentTitle)));
                    } catch (Exception e) {
                        auditHelper.auditError(ErrorTypes.exportExcelError,
                                               e,
                                               GameSetHistoryActivity.this);
                    }
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                default:
                    break;
            }
        }
    };
    private final IAsyncCallback<String> excelExportCallback = new IAsyncCallback<String>() {

        @Override
        public void execute(String filePath, Exception e) {

            // TODO Check exception
            onGameSetExportedToExcelFile(filePath);
        }
    };

    /**
     * Temporary GameSet. Use with care, this is pretty much a global variable
     * (it's used between differents inner classes).
     */
    private GameSet tempGameSet;

    @Override
    protected void auditEvent() {
        auditHelper.auditEvent(AuditHelper.EventTypes.displayGameSetHistoryPage);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_gameset_history;
    }

    private boolean isBluetoothActivated() {
        boolean isActivated = bluetoothHelper.isBluetoothEnabled();
        if (!isActivated) {
            Toast.makeText(GameSetHistoryActivity.this,
                           TarotDroidApp.get(this)
                                        .getResources()
                                        .getString(R.string.msgActivateBluetooth),
                           Toast.LENGTH_SHORT).show();
        }
        return isActivated;
    }

    @Override
    public void onBackPressed() {
        // cancel send task if running
        try {
            if (this.sendGameSetTask != null) {
                this.sendGameSetTask.cancel(true);
            }
        } catch (Exception e) {
        }

        // cancel receive task if running
        try {
            if (this.receiveGameSetTask != null) {
                this.receiveGameSetTask.cancel(true);
            }
        } catch (Exception e) {
        }

        super.onBackPressed();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            // initialize progress dialog
            progressDialog = new ProgressDialog(this);
            progressDialog.setCanceledOnTouchOutside(false);

            // initialize bluetooth
            bluetoothHelper.setActivity(this);

            // set excuse as background image
            listView.setCacheColorHint(0);
            listView.setBackgroundResource(R.drawable.img_excuse);

            // set action bar properties
            registerForContextMenu(listView);

            // set internal properties
            tempExcelFilePath = null;
            tempGameSet = null;

            // wait for the dal to be initiated to refresh the game sets
            if (TarotDroidApp.get(this).getLoadDalTask().getStatus() == AsyncTask.Status.RUNNING) {
                TarotDroidApp.get(this)
                             .getLoadDalTask()
                             .showDialogOnActivity(this,
                                                   this.getResources()
                                                       .getString(R.string.msgGameSetsRetrieval));
                TarotDroidApp.get(this).getLoadDalTask().setCallback(new IAsyncCallback<String>() {

                    @Override
                    public void execute(String result, Exception e) {

                        // TODO Check exception
                        if (result != null && result.toString().length() > 0) {
                            UIHelper.showSimpleRichTextDialog(GameSetHistoryActivity.this,
                                                              result,
                                                              "Erreur de chargement");
                        }

                        GameSetHistoryActivity.this.refresh();
                    }
                });
            }
            // refresh the game sets
            else {
                this.refresh();
            }

            Object currentRunningTask = getLastNonConfigurationInstance();
            if (currentRunningTask != null) {
                if (currentRunningTask instanceof UpSyncGameSetTask) {
                    UpSyncGameSetTask task = (UpSyncGameSetTask) currentRunningTask;
                    task.attach(this);
                }
            }
        } catch (Exception e) {
            auditHelper.auditError(ErrorTypes.gameSetHistoryActivityError, e, this);
        }
    }

    @Override
    protected void inject() {
        TarotDroidApp.get(this).getComponent().inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenuBlueTooth = menu.addSubMenu(this.getString(R.string.lblBluetoothItem));
        MenuItem miBluetooth = subMenuBlueTooth.getItem();
        miBluetooth.setIcon(R.drawable.stat_sys_data_bluetooth);
        miBluetooth.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem miBlueToothDiscover = subMenuBlueTooth.add(R.string.lblBluetoothDiscover)
                                                       .setIcon(R.drawable.ic_menu_allfriends);
        MenuItem miBlueToothGetDiscoverable = subMenuBlueTooth.add(R.string.lblBluetoothGetDiscoverable)
                                                              .setIcon(android.R.drawable.ic_menu_myplaces);
        MenuItem miBlueToothReceive = subMenuBlueTooth.add(R.string.lblBluetoothReceive)
                                                      .setIcon(R.drawable.ic_menu_download);
        MenuItem miBlueToothHelp = subMenuBlueTooth.add(R.string.lblBluetoothHelp)
                                                   .setIcon(android.R.drawable.ic_menu_info_details);

        miBlueToothDiscover.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (isBluetoothActivated()) {
                    bluetoothHelper.startDiscovery();
                    auditHelper.auditEvent(AuditHelper.EventTypes.actionBluetoothDiscoverDevices);
                }
                return true;
            }
        });

        miBlueToothGetDiscoverable.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (isBluetoothActivated()) {
                    bluetoothHelper.setBluetoothDeviceDiscoverable();
                    auditHelper.auditEvent(AuditHelper.EventTypes.actionBluetoothSetDiscoverable);
                }
                return true;
            }
        });

        miBlueToothReceive.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (isBluetoothActivated()) {
                    // retrieve game count
                    int gameSetCount;
                    try {
                        gameSetCount = TarotDroidApp.get(GameSetHistoryActivity.this)
                                                    .getDalService()
                                                    .getGameSetCount();
                    } catch (DalException de) {
                        gameSetCount = 0;
                    }

                    // prevent user from downloading if game set count > 5 and
                    // limited version
                    if (!BuildConfig.IS_FULL && gameSetCount >= 5) {
                        Toast.makeText(GameSetHistoryActivity.this,
                                       getResources().getString(R.string.msgLimitedVersionInformation),
                                       Toast.LENGTH_SHORT).show();
                    }

                    // ok for download
                    else {
                        try {
                            GameSetHistoryActivity.this.receiveGameSetTask = new ReceiveGameSetTask(
                                    GameSetHistoryActivity.this,
                                    GameSetHistoryActivity.this.progressDialog,
                                    bluetoothHelper.getBluetoothAdapter());
                            GameSetHistoryActivity.this.receiveGameSetTask.setCallback(
                                    refreshCallback);
                            GameSetHistoryActivity.this.receiveGameSetTask.execute();
                            auditHelper.auditEvent(AuditHelper.EventTypes.actionBluetoothReceiveGameSet);
                        } catch (IOException ioe) {
                            Log.v(BuildConfig.APP_LOG_TAG,
                                  "TarotDroid Exception in " + this.getClass().toString(),
                                  ioe);
                            Toast.makeText(GameSetHistoryActivity.this,
                                           getResources().getString(R.string.msgBluetoothError,
                                                                    ioe),
                                           Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                return true;
            }
        });

        miBlueToothHelp.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                UIHelper.showSimpleRichTextDialog(GameSetHistoryActivity.this,
                                                  getResources()
                                                          .getText(R.string.msgHelpBluetooth)
                                                          .toString(),
                                                  getResources().getString(R.string.titleHelpBluetooth));
                return true;
            }
        });

        // TODO Improve Massive excel export
        // if (!AppContext.getApplication().isAppLimited()) {
        // com.actionbarsherlock.view.MenuItem miGlobalExport =
        // menu.add(this.getString(R.string.lblExcelExport)).setIcon(R.drawable.ic_excel);
        // miGlobalExport.setShowAsAction(com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_NEVER);
        // //miGlobalExport.setIcon(R.drawable.ic_excel);
        //
        // miGlobalExport.setOnMenuItemClickListener(new
        // OnMenuItemClickListener() {
        // @Override
        // public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem
        // item) {
        // ExportToExcelTask task = new
        // ExportToExcelTask(GameSetHistoryActivity.this, progressDialog);
        // task.execute();
        // return true;
        // }
        // });
        // }

        MenuItem miBin = menu.add(this.getString(R.string.lblInitDalItem));
        miBin.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        miBin.setIcon(R.drawable.gd_action_bar_trashcan);

        miBin.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameSetHistoryActivity.this);
                builder.setTitle(GameSetHistoryActivity.this.getString(R.string.titleReinitDalYesNo));
                builder.setMessage(Html.fromHtml(GameSetHistoryActivity.this.getText(R.string.msgReinitDalYesNo)
                                                                            .toString()));
                builder.setPositiveButton(GameSetHistoryActivity.this.getString(R.string.btnOk),
                                          GameSetHistoryActivity.this.removeAllGameSetsDialogClickListener);
                builder.setNegativeButton(GameSetHistoryActivity.this.getString(R.string.btnCancel),
                                          GameSetHistoryActivity.this.removeAllGameSetsDialogClickListener)
                       .show();
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                return true;
            }
        });

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bluetoothHelper.cancelDiscovery();
        } catch (Exception e) {
        }
    }

    private void onGameSetExportedToExcelFile(String filePath) {
        // should never happen
        if (filePath == null) {
            UIHelper.showSimpleRichTextDialog(this,
                                              this.getString(R.string.msgDbExportFailed),
                                              this.getString(R.string.titleDbExportFailed));
        } else {
            tempExcelFilePath = filePath;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.titleGameSetExportedToExcelFile));
            builder.setMessage(Html.fromHtml(this.getText(R.string.msgGameSetExportedToExcelFile)
                                                 .toString()));
            builder.setPositiveButton(this.getString(R.string.btnOk),
                                      this.exportExcelByEmailDialogClickListener);
            builder.setNegativeButton(this.getString(R.string.btnCancel),
                                      this.exportExcelByEmailDialogClickListener);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.show();
        }
    }

    @OnItemClick(R.id.listView)
    protected void onListItemClick(AdapterView<?> parent, int position) {
        final GameSet gameSet = (GameSet) listView.getAdapter().getItem(position);

        final Item[] items = !BuildConfig.IS_FULL ? limitedItems : allItems;

        ListAdapter adapter = new ArrayAdapter<Item>(this,
                                                     android.R.layout.select_dialog_item,
                                                     android.R.id.text1,
                                                     items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // User super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);

                // Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

                // Add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                return v;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format(this.getString(R.string.lblGameSetHistoryActivityMenuTitle),
                                       new SimpleDateFormat("dd/MM/yy").format(gameSet.getCreationTs())));

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int itemIndex) {
                Item item = items[itemIndex];
                if (item.itemType == Item.ItemTypes.remove) {
                    RemoveGameSetDialogClickListener removeGameSetDialogClickListener = new RemoveGameSetDialogClickListener(
                            gameSet);
                    AlertDialog.Builder builder = new AlertDialog.Builder(GameSetHistoryActivity.this);
                    builder.setTitle(GameSetHistoryActivity.this.getString(R.string.titleRemoveGameSetYesNo));
                    builder.setMessage(Html.fromHtml(GameSetHistoryActivity.this.getText(R.string.msgRemoveGameSetYesNo)
                                                                                .toString()));
                    builder.setPositiveButton(GameSetHistoryActivity.this.getString(R.string.btnOk),
                                              removeGameSetDialogClickListener);
                    builder.setNegativeButton(GameSetHistoryActivity.this.getString(R.string.btnCancel),
                                              removeGameSetDialogClickListener).show();
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                } else if (item.itemType == Item.ItemTypes.transferOverBluetooth) {
                    if (!bluetoothHelper.isBluetoothEnabled()) {
                        Toast.makeText(GameSetHistoryActivity.this,
                                       GameSetHistoryActivity.this.getString(R.string.msgActivateBluetooth),
                                       Toast.LENGTH_LONG).show();
                    }

                    try {
                        // make sure at least one device was discovered
                        if (bluetoothHelper.getBluetoothDeviceCount() == 0) {
                            Toast.makeText(GameSetHistoryActivity.this,
                                           GameSetHistoryActivity.this.getString(R.string.msgRunDiscoverDevicesFirst),
                                           Toast.LENGTH_LONG).show();
                        }

                        // display devices and download
                        final String[] items = bluetoothHelper.getBluetoothDeviceNames();

                        AlertDialog.Builder builder = new AlertDialog.Builder(GameSetHistoryActivity.this);
                        builder.setTitle(GameSetHistoryActivity.this.getString(R.string.lblSelectBluetoothDevice));
                        builder.setItems(items, new BluetoothDeviceClickListener(gameSet, items));
                        AlertDialog alert = builder.create();
                        alert.show();
                    } catch (Exception e) {
                        auditHelper.auditError(ErrorTypes.gameSetHistoryActivityError,
                                               e,
                                               GameSetHistoryActivity.this);
                    }
                } else if (item.itemType == Item.ItemTypes.exportToExcel) {
                    try {
                        if (BuildConfig.IS_FULL) {

                            ExportToExcelTask task = new ExportToExcelTask(GameSetHistoryActivity.this,
                                                                           progressDialog,
                                                                           localizationHelper);
                            task.setCallback(excelExportCallback);
                            task.execute(gameSet);
                        }
                    } catch (Exception e) {
                        Toast.makeText(GameSetHistoryActivity.this,
                                       getResources().getText(R.string.msgGameSetExportError)
                                                     .toString() + e.getMessage(),
                                       Toast.LENGTH_LONG)
                             .show();
                        auditHelper.auditError(ErrorTypes.excelFileStorage, e);
                    }
                } else if (item.itemType == Item.ItemTypes.edit) {
                    Intent intent = new Intent(GameSetHistoryActivity.this,
                                               TabGameSetActivity.class);
                    intent.putExtra(ActivityParams.PARAM_GAMESET_ID, gameSet.getId());
                    GameSetHistoryActivity.this.startActivityForResult(intent,
                                                                       RequestCodes.DISPLAY_WITH_FACEBOOK);
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.pendingReauthRequest = savedInstanceState.getBoolean(PENDING_REAUTH_KEY, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (TarotDroidApp.get(this).getLoadDalTask().getStatus() == AsyncTask.Status.FINISHED) {
            this.refresh();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PENDING_REAUTH_KEY, this.pendingReauthRequest);
    }

    public void refresh() {
        List<GameSet> gameSets = TarotDroidApp.get(this).getDalService().getAllGameSets();
        Collections.sort(gameSets, gameSetCreationDateDescendingComparator);
        this.listView.setAdapter(new GameSetAdapter(this, gameSets));
        this.setTitle();
    }

    private void sendGameSetOverBluetooth(
            final GameSet gameSet,
            final BluetoothDevice bluetoothDevice
    ) {
        try {
            auditHelper.auditEvent(EventTypes.actionBluetoothSendGameSet);
            this.sendGameSetTask = new SendGameSetTask(this,
                                                       this.progressDialog,
                                                       gameSet,
                                                       bluetoothDevice,
                                                       this.bluetoothHelper.getBluetoothAdapter());
            this.sendGameSetTask.setCallback(refreshCallback);
            this.sendGameSetTask.execute();
        } catch (IOException ioe) {
            Log.v(BuildConfig.APP_LOG_TAG,
                  "TarotDroid Exception in " + this.getClass().toString(),
                  ioe);
            Toast.makeText(GameSetHistoryActivity.this,
                           this.getResources().getString(R.string.msgBluetoothError, ioe),
                           Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void setTitle() {
        if (TarotDroidApp.get(this).getDalService() == null || TarotDroidApp.get(this)
                                                                            .getDalService()
                                                                            .getAllGameSets()
                                                                            .size() == 0) {
            this.setTitle(this.getResources()
                              .getString(R.string.lblGameSetHistoryActivityTitleNone));
        } else if (TarotDroidApp.get(this).getDalService().getAllGameSets().size() == 1) {
            this.setTitle(this.getResources()
                              .getString(R.string.lblGameSetHistoryActivityTitleSingle));
        } else {
            this.setTitle(this.getResources()
                              .getString(R.string.lblGameSetHistoryActivityTitlePlural,
                                         TarotDroidApp.get(this)
                                                      .getDalService()
                                                      .getAllGameSets()
                                                      .size()));
        }

    }

    private static class Item {

        public final int icon;
        public final ItemTypes itemType;
        public final String text;

        public Item(String text, Integer icon, ItemTypes itemType) {
            this.text = text;
            this.icon = icon;
            this.itemType = itemType;
        }

        @Override
        public String toString() {
            return text;
        }

        protected enum ItemTypes {
            edit, exportToExcel, remove, transferOverBluetooth
        }
    }

    private class BluetoothDeviceClickListener implements DialogInterface.OnClickListener {

        private final String[] bluetoothDeviceNames;
        private final GameSet gameSet;

        public BluetoothDeviceClickListener(
                final GameSet gameSet,
                final String[] bluetoothDeviceNames
        ) {
            this.gameSet = gameSet;
            this.bluetoothDeviceNames = bluetoothDeviceNames;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            auditHelper.auditEvent(AuditHelper.EventTypes.actionBluetoothSendGameSet);
            GameSetHistoryActivity.this.sendGameSetOverBluetooth(this.gameSet,
                                                                 bluetoothHelper
                                                                         .getBluetoothDevice(this.bluetoothDeviceNames[which]));
        }
    }

    private class GameSetAdapter extends ArrayAdapter<GameSet> {

        public GameSetAdapter(Context context, List<GameSet> gameSets) {
            super(context, R.layout.thumbnail_item, gameSets);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GameSet gameSet = this.getItem(position);

            int drawableId;
            switch (gameSet.getGameStyleType()) {
                case Tarot3:
                    drawableId = R.drawable.icon_3players;
                    break;
                case Tarot4:
                    drawableId = R.drawable.icon_4players;
                    break;
                case Tarot5:
                    drawableId = R.drawable.icon_5players;
                    break;
                case None:
                default:
                    throw new IllegalStateException("unknown gameSet type: " + gameSet.getGameStyleType());
            }

            ThumbnailItem thumbnailItem = new ThumbnailItem(this.getContext(),
                                                            drawableId,
                                                            localizationHelper.buildGameSetHistoryTitle(
                                                                    gameSet),
                                                            localizationHelper.buildGameSetHistoryDescription(
                                                                    gameSet));

            return thumbnailItem;
        }
    }

    private class RemoveGameSetDialogClickListener implements DialogInterface.OnClickListener {

        private final GameSet gameSet;

        public RemoveGameSetDialogClickListener(final GameSet gameSet) {
            this.gameSet = gameSet;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    RemoveGameSetTask removeGameSetTask = new RemoveGameSetTask(
                            GameSetHistoryActivity.this,
                            GameSetHistoryActivity.this.progressDialog,
                            this.gameSet);
                    removeGameSetTask.setCallback(refreshCallback);
                    removeGameSetTask.execute();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                default:
                    break;
            }
        }
    }
}