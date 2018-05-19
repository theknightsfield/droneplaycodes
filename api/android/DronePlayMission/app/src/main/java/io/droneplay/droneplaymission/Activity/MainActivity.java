package io.droneplay.droneplaymission.Activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;
import io.droneplay.droneplaymission.HelperUtils;
import io.droneplay.droneplaymission.MainListAdapter;
import io.droneplay.droneplaymission.MainListItem;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.ToastUtils;
import io.droneplay.droneplaymission.WaypointManager;

public class MainActivity extends AppCompatActivity implements MainListAdapter.ListBtnClickListener, HelperUtils.titleInputClickListener {

    ListView listview ;
    MainListAdapter adapter;
    Button newMissionBtn;

    Context context;

    private List<String> missingPermission = new ArrayList<>();

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        adapter = new MainListAdapter(this);
        listview = (ListView) findViewById(R.id.listviewMain);
        listview.setAdapter(adapter);
        newMissionBtn = findViewById(R.id.btnNewMission);

        checkAndRequestPermissions();
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
            Init();
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }



    //region Registration n' Permissions Helpers

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
            Init();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
        else {
            Init();
        }

    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_doing_message));
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_success_message));
                            } else {
                                ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_message));
                            }
                            //Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                            //Log.d(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
                            notifyStatusChange();
                        }
                    });
                }
            });
        }
    }

    private void notifyStatusChange() {
        //DronePlayMissionApplication.getEventBus().post(new MainActivity.ConnectivityChangeEvent());
    }


    private void addButton(String title) {
        String newId = HelperUtils.generateButtonID(adapter.getList());
        adapter.addItem(title, newId);
        HelperUtils.saveButtons(adapter.getList());
        adapter.notifyDataSetChanged();
    }

    private void deleteButton(String buttonid) {
        HelperUtils.deleteMissionsFromFile(buttonid);
        adapter.removeItem(buttonid);
        HelperUtils.saveButtons(adapter.getList());
        adapter.notifyDataSetChanged();
    }

    private void Init() {
        newMissionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelperUtils.showTitleInputDialog(MainActivity.this,MainActivity.this);
            }
        });

        if (!missingPermission.isEmpty()) {
            return;
        }

        ArrayList<MainListItem> list = HelperUtils.loadButtonsFromFile();
        if (list == null) return;

        adapter.setItems(list);
    }

    private void prepareMission(String buttonID) {
        if (HelperUtils.isExistMission(buttonID) == false) {
            ToastUtils.setResultToToast("Mission is not ready !");
            return;
        }

        WaypointManager.getInstance().setMission(buttonID);
        WaypointMission mission = WaypointManager.getInstance().getWaypointMission();

        if (mission == null) {
            ToastUtils.setResultToToast("Mission is not ready !");
            return;
        }

//        DJIError djiError = MissionControl.getInstance().getWaypointMissionOperator().loadMission(mission);
//        if (djiError != null)
//            showResultToast(djiError);
//        else {
            Intent intent = new Intent(MainActivity.this, MissionRunActivity.class);
            intent.putExtra(MissionRunActivity.PARAM_BUTTON_ID, buttonID);
            startActivity(intent);
//        }
    }


    public void showYesNoDialog(final MainListAdapter.CLICK_TYPE kind, final String buttonId) {

        String strMsg = "Are you sure ?";
        switch(kind) {
            case RUN:
                strMsg = "Run this mission ?";
                break;

            case DELETE:
                strMsg = "Delete this mission ?";
                break;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int choice) {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        switch(kind) {
                            case RUN:
                                prepareMission(buttonId);
                                break;

                            case DELETE:
                                deleteButton(buttonId);
                                break;
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(strMsg)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    public void onListBtnClick(MainListAdapter.CLICK_TYPE kind, String buttonId) {

        switch(kind) {
            case RUN:
                showYesNoDialog( kind, buttonId);
                break;

            case DELETE:
                showYesNoDialog( kind, buttonId);
                break;

            case EDIT:
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra(MissionRunActivity.PARAM_BUTTON_ID, buttonId);
                startActivity(intent);
                break;
        }

    }

    @Override
    public void onTitileInputClick(String buttonTitle) {
        addButton(buttonTitle);
    }

    private void showResultToast(DJIError djiError) {
        ToastUtils.setResultToToast(djiError == null ? "Action started!" : djiError.getDescription());
    }
}