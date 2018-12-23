package balti.migrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

import static balti.migrate.Exclusions.EXCLUDE_DATA;
import static balti.migrate.Exclusions.NOT_EXCLUDED;

/**
 * Created by sayantan on 8/10/17.
 */

public class BackupActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, OnCheck {

    ListView listView;
    CheckBox appAllSelect, dataAllSelect, permissionsAllSelect;
    Spinner appType;
    TextView nextButton;
    ImageButton selectAll, clearAll, backButton, helpButton;

    PackageManager pm;
    Vector<BackupDataPacket> appList;
    AppListAdapter adapter;
    boolean isAnyAppSelected = false;

    BroadcastReceiver progressReceiver, extraBackupsStartReceiver;

    AppUpdate appUpdate;

    Exclusions exclusions;

    class AppUpdate extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            nextButton.setEnabled(false);
            listView.setAdapter(null);

            appAllSelect.setEnabled(false);
            dataAllSelect.setEnabled(false);
            permissionsAllSelect.setEnabled(false);

            selectAll.setEnabled(false);
            clearAll.setEnabled(false);

            (findViewById(R.id.appLoadingView)).setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] params) {

            updateAppsList((int)params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            appAllSelect.setEnabled(true);
            dataAllSelect.setEnabled(true);
            permissionsAllSelect.setEnabled(true);

            selectAll.setEnabled(true);
            clearAll.setEnabled(true);

            nextButton.setEnabled(true);

            (findViewById(R.id.appLoadingView)).setVisibility(View.GONE);
            listView.setAdapter(adapter);

            dataAllSelect.setChecked(true);
            permissionsAllSelect.setChecked(true);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_layout);

        pm = getPackageManager();

        listView = findViewById(R.id.appBackupList);

        appAllSelect = findViewById(R.id.appAllSelect);
        dataAllSelect = findViewById(R.id.dataAllSelect);
        permissionsAllSelect = findViewById(R.id.permissionsAllSelect);

        nextButton = findViewById(R.id.backupActivityNext);
        selectAll = findViewById(R.id.selectAll);
        clearAll = findViewById(R.id.clearAll);
        backButton = findViewById(R.id.backupLayoutBackButton);
        helpButton = findViewById(R.id.backup_activity_help);

        final SharedPreferences main = getSharedPreferences("main", MODE_PRIVATE);
        final SharedPreferences.Editor editor = main.edit();

        appType = findViewById(R.id.appType);
        appType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                appUpdate = new AppUpdate();

                if (i > 0 && main.getBoolean("system_apps_warning", true)){
                    new AlertDialog.Builder(BackupActivity.this)
                            .setTitle(R.string.bootloop_warning)
                            .setMessage(R.string.bootloop_warning_desc)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    editor.putBoolean("system_apps_warning", false);
                                    editor.commit();
                                }
                            })
                            .show();
                }

                appUpdate.execute(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        appAllSelect.setOnCheckedChangeListener(this);
        dataAllSelect.setOnCheckedChangeListener(this);
        permissionsAllSelect.setOnCheckedChangeListener(this);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startExtraBackupsStartingActivity();
            }
        });

        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataAllSelect.setChecked(true);
                permissionsAllSelect.setChecked(true);
            }
        });

        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataAllSelect.setChecked(true);
                dataAllSelect.setChecked(false);
                appAllSelect.setChecked(false);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(BackupActivity.this)
                        .setView(View.inflate(BackupActivity.this, R.layout.backup_activity_help, null))
                        .setPositiveButton(R.string.close, null)
                        .show();
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent progressActivityStartIntent = new Intent(BackupActivity.this, BackupProgressLayout.class);
                progressActivityStartIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                progressActivityStartIntent.setAction("Migrate progress broadcast");
                startActivity(progressActivityStartIntent);

                try {
                    LocalBroadcastManager.getInstance(BackupActivity.this).unregisterReceiver(progressReceiver);
                }catch (Exception e){e.printStackTrace();}
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("Migrate progress broadcast"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("get data"));

        exclusions = new Exclusions(this);

        extraBackupsStartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ExtraBackups.setAppList(appList, dataAllSelect.isChecked(), isAnyAppSelected);
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(extraBackupsStartReceiver, new IntentFilter("extraBackupsStarted"));

        /*if (!main.getBoolean("sem_apology", false)){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.sem_apology)
                    .setMessage(R.string.sem_apology_desc)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            editor.putBoolean("sem_apology", true);
            editor.commit();
        }*/

        /*final AdView adView = findViewById(R.id.backup_activity_adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adView.setAdListener(new AdListener(){
            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                adView.setVisibility(View.GONE);
            }
        });*/
    }

    void startExtraBackupsStartingActivity(){

        Intent intent = new Intent(BackupActivity.this, ExtraBackups.class);
        startActivity(intent);
    }


    void updateAppsList(int i) {
        appAllSelect.setOnCheckedChangeListener(null);
        appAllSelect.setChecked(false);
        appAllSelect.setOnCheckedChangeListener(this);
        dataAllSelect.setOnCheckedChangeListener(null);
        dataAllSelect.setChecked(false);
        dataAllSelect.setOnCheckedChangeListener(this);
        permissionsAllSelect.setOnCheckedChangeListener(null);
        permissionsAllSelect.setChecked(false);
        permissionsAllSelect.setOnCheckedChangeListener(this);

        if (i == 2) {
            List<PackageInfo> tempAppList = pm.getInstalledPackages(0);
            appList = new Vector<>(0);
            for (int k = 0; k < tempAppList.size(); k++) {
                BackupDataPacket packet = new BackupDataPacket();
                packet.PACKAGE_INFO = tempAppList.get(k);
                packet.APP = false;
                packet.DATA = false;
                packet.PERMISSIONS = false;
                packet.IS_PERMISSIBLE = false;
                appList.add(packet);
            }
            adapter = new AppListAdapter(BackupActivity.this, appList);
        } else if (i == 1) {
            List<PackageInfo> tempAppList = pm.getInstalledPackages(0);
            appList = new Vector<>(0);
            for (int k = 0; k < tempAppList.size(); k++) {
                if ((tempAppList.get(k).applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) > 0) {
                    BackupDataPacket packet = new BackupDataPacket();
                    packet.PACKAGE_INFO = tempAppList.get(k);
                    packet.APP = false;
                    packet.DATA = false;
                    packet.PERMISSIONS = false;
                    packet.IS_PERMISSIBLE = false;
                    appList.add(packet);
                }
            }
            adapter = new AppListAdapter(BackupActivity.this, appList);
        } else if (i == 0) {
            List<PackageInfo> tempAppList = pm.getInstalledPackages(0);
            appList = new Vector<>(0);
            for (int k = 0; k < tempAppList.size(); k++) {
                if ((tempAppList.get(k).applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                    BackupDataPacket packet = new BackupDataPacket();
                    packet.PACKAGE_INFO = tempAppList.get(k);
                    packet.APP = false;
                    packet.DATA = false;
                    packet.PERMISSIONS = false;
                    packet.IS_PERMISSIBLE = false;
                    appList.add(packet);
                }
            }
            adapter = new AppListAdapter(BackupActivity.this, appList);
        }
    }


    @Override
    public void onCheck(Vector<BackupDataPacket> backupDataPackets) {
        appList = backupDataPackets;
        boolean app, data, permissions, isPermissible;

        isAnyAppSelected = false;

        if (appList.size() > 0) {
            app = data = permissions = true;
            isPermissible = appList.get(0).IS_PERMISSIBLE;
        }
        else app = data = permissions = isPermissible = false;

        for (int i = 0; i < backupDataPackets.size(); i++) {
            BackupDataPacket packet = appList.elementAt(i);
            int exclusionState = exclusions.returnExclusionState(packet.PACKAGE_INFO.packageName);
            if (exclusionState == NOT_EXCLUDED) {
                app = app && packet.APP;
                data = data && packet.DATA;
            }
            else if (exclusionState == EXCLUDE_DATA) {
                app = app && packet.APP;
            }

            isPermissible = isPermissible || packet.IS_PERMISSIBLE;
            if (packet.IS_PERMISSIBLE) permissions = permissions && packet.PERMISSIONS;

            if (packet.DATA || packet.APP)
                isAnyAppSelected = true;

        }

        permissions = permissions && isPermissible;

        dataAllSelect.setOnCheckedChangeListener(null);
        dataAllSelect.setChecked(data);
        dataAllSelect.setOnCheckedChangeListener(this);

        appAllSelect.setOnCheckedChangeListener(null);
        appAllSelect.setChecked(app);
        if (dataAllSelect.isChecked())
            appAllSelect.setEnabled(false);
        else appAllSelect.setEnabled(true);
        appAllSelect.setOnCheckedChangeListener(this);

        permissionsAllSelect.setEnabled(isPermissible);

        permissionsAllSelect.setOnCheckedChangeListener(null);
        permissionsAllSelect.setChecked(permissions);
        permissionsAllSelect.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (compoundButton == permissionsAllSelect){
            adapter.checkAllPermissions(b);
            adapter.notifyDataSetChanged();
        }
        else if (compoundButton == appAllSelect) {
            adapter.checkAllApp(b);
            adapter.notifyDataSetChanged();
        }
        else if (compoundButton == dataAllSelect) {
            if (b){
                appAllSelect.setChecked(true);
                appAllSelect.setEnabled(false);
            }
            else {
                appAllSelect.setEnabled(true);
            }
            adapter.checkAllData(b);
            adapter.notifyDataSetChanged();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }
        catch (Exception ignored){}
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(extraBackupsStartReceiver);
        }
        catch (Exception ignored){}
    }
}
