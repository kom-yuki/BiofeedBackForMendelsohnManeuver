package com.example.yuki.biofeedbackformendelsohnmaneuver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 定数（Bluetooth LE Gatt UUID）
    // Private Service
    private static final UUID UUID_SERVICE_PRIVATE = UUID.fromString("4880c12c-fdcb-4077-8920-a450d7f9b907");
    private static final UUID UUID_CHARACTERISTIC_PRIVATE1 = UUID.fromString("fec26ec4-6d71-4442-9f81-55bc21d658d6");
    private static final UUID UUID_CHARACTERISTIC_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // 定数
    private static final int REQUEST_ENABLEBLUETOOTH = 1; // Bluetooth機能の有効化要求時の識別コード
    private static final int REQUEST_CONNECTDEVICE = 2; // デバイス接続要求時の識別コード

    // メンバー変数
    private BluetoothAdapter mBluetoothAdapter;    // BluetoothAdapter : Bluetooth処理で必要
    private String mDeviceAddress = "";    // デバイスアドレス
    private String strDeviceName = "";
    private BluetoothGatt mBluetoothGatt = null;    // Gattサービスの検索、キャラスタリスティックの読み書き
    private BluetoothGattService mservice = null;
    private BluetoothGattCharacteristic mDataCharacteristic = null;

    //データ配列
    private ArrayList<Double> data1 = new ArrayList<>();
    private ArrayList<Double> data2 = new ArrayList<>();
    private ArrayList<Double> normalizedData1 = new ArrayList<>();
    private ArrayList<Double> normalizedData2 = new ArrayList<>();
    private ArrayList<Integer> log = new ArrayList<>();
    private ArrayList<Integer> swallow = new ArrayList<>();
    private ArrayList<ArrayList<Double>> Templist = new ArrayList<>();
    private ArrayList<ArrayList<Double>> DTWDistance = new ArrayList<>();
    private double maxData1, maxData2, minData1, minData2;
    private int flagST, flagDetection;
    private int setSensorFlag;
    private int sampling, ansei;
    private int timeCount=0;
    private int maxTimeCount=0;
    private int onset,offset,detectedPoint,usedTemplate;

    private CheckSwallow checker1, checker2;

    // GUIアイテム
    private Button mButton_start;    // 計測開始ボタン
    private Button mButton_end;    // 計測終了ボタン
    private Button mButton_log; //ログボタン
    private Button mButton_set_on; //センサ位置調整用ボタン
    private Button mButton_set_off; //センサ位置調整用ボタン


    // Spinnerオブジェクトを取得
    private Spinner spinner_channel;
    private Spinner spinner_hantei;
    private Spinner spinner_countDown;
    private Spinner spinner_trial; //試行事例
    private LineChart mChart; //グラフ描画用


    private EditText subject; //被験者ID
    private TextView filename; //ファイル名用
    private TextView timeText; //挙上時間カウント用

    private String TAG;

    // メイン(UI)スレッドでHandlerのインスタンスを生成する
    final Handler handler = new Handler();

    // BluetoothGattコールバック
    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        // 接続状態変更（connectGatt()の結果として呼ばれる。）
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }

            if (BluetoothProfile.STATE_CONNECTED == newState) {    // 接続完了
                mBluetoothGatt.discoverServices();    // サービス検索
                runOnUiThread(new Runnable() {
                    public void run() {
                        // GUIアイテムの有効無効の設定
                        // 切断ボタンを有効にする
                        //mButton_Disconnect.setEnabled(true);
                    }
                });
                return;
            }
            if (BluetoothProfile.STATE_DISCONNECTED == newState) {    // 切断完了（接続可能範囲から外れて切断された）
                // 接続可能範囲に入ったら自動接続するために、mBluetoothGatt.connect()を呼び出す。
                mBluetoothGatt.connect();
                return;
            }
        }

        // サービス検索が完了したときの処理（mBluetoothGatt.discoverServices()の結果として呼ばれる。）
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }

            // 発見されたサービスのループ
            for (final BluetoothGattService service : gatt.getServices()) {
                // サービスごとに個別の処理
                if ((null == service) || (null == service.getUuid())) {
                    continue;
                }
                if (UUID_SERVICE_PRIVATE.equals(service.getUuid())) {    // プライベートサービス
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // GUIアイテムの有効無効の設定
                            mButton_start.setEnabled(true);
                            mButton_set_on.setEnabled(true);
                            ((TextView) findViewById(R.id.textview_BLEconnect)).setText("機器接続中");
                            ((TextView) findViewById(R.id.textview_BLEconnect)).setTextColor(Color.GREEN);
                            //変数へ代入
                            mservice = mBluetoothGatt.getService(UUID_SERVICE_PRIVATE);
                            mDataCharacteristic = mservice.getCharacteristic(UUID_CHARACTERISTIC_PRIVATE1);
                        }
                    });
                    continue;
                }
            }
        }

        // キャラクタリスティック変更が通知されたときの処理
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // キャラクタリスティックごとに個別の処理
            if (UUID_CHARACTERISTIC_PRIVATE1.equals(characteristic.getUuid())) {
                byte[] bytes = characteristic.getValue();

                byte[] byteChara1 = {bytes[0], bytes[1]};
                byte[] byteChara2 = {bytes[3], bytes[4]};

                ByteBuffer bb1 = ByteBuffer.wrap( byteChara1 );
                ByteBuffer bb2 = ByteBuffer.wrap( byteChara2 );

                double CH1 = bb1.getShort()*0.0016;
                double CH2 = bb2.getShort()*0.0016;

                if (setSensorFlag == 1) {
                    if (spinner_channel.getSelectedItemPosition() + 1 == 1) {
                        addEntry(CH1);
                    } else if (spinner_channel.getSelectedItemPosition() + 1 == 2) {
                        addEntry(CH2);
                    }

                    //最大値，最小値の記憶
                    if (maxData1 < CH1) {
                        maxData1 = CH1;
                    } else if (minData1 > CH1) {
                        minData1 = CH1;
                    }

                    if (maxData2 < CH2) {
                        maxData2 = CH2;
                    } else if (minData2 > CH2) {
                        minData2 = CH2;
                    }
                }

                //センサ位置調整していないとき，挙上検出行う
                if (setSensorFlag == 0) {
                    data1.add(CH1);
                    data2.add(CH2);

                    //正規化
                    normalizedData1.add((CH1-minData1)/(maxData1-minData1));
                    normalizedData2.add((CH2-minData2)/(maxData2-minData2));
                    ArrayList<Double> data = null;

                    //挙上判定するセンサデータ選択
                    if (spinner_channel.getSelectedItemPosition()+1 == 1){
                        data = normalizedData1;
                    }
                    else if(spinner_channel.getSelectedItemPosition()+1 == 2){
                        data = normalizedData2;
                    }

                    //ログの時間を表示させる。
                    if (flagST == 1) {
                        timeCount++;
                        // 別スレッドを実行
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // Handlerを使用してメイン(UI)スレッドに処理を依頼する
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        timeText.setText(String.valueOf((float) timeCount / 10) + "秒");
                                    }
                                });
                            }
                        }).start();
                    } else {
                        if (maxTimeCount < timeCount) {
                            maxTimeCount = timeCount;
                        }
                        timeCount = 0;
                    }

                    //挙上判定
                    if (flagDetection == 0){
                        if (checker1.checkOnset(data, sampling, ansei)){
                            onset = sampling;
                            swallow.add(sampling);
                            flagDetection = 1;
                            writeCommand("3");
                            DrawDetection(1,onset);
                        }
                        checker1.addData(data, sampling, 1);
                        checker2.addData(data, sampling, 2);
                    }
                    else if (flagDetection == 1 && sampling >= onset + 10){ //メンデルソン手技用の検出アルゴリズム
                        //select = 1はSPRINGで最適部分検出，select = 2は最初に閾値下回った部分を検出
                        if (checker1.checkOffset(data, sampling, 1)) { //checkerer1は長いV字型
                            usedTemplate = 1;
                            detectedPoint = sampling;
                            offset = checker1.getSPRING_DTW().getT_end() - 1;
                            swallow.add(offset);
                            flagDetection = 2;
                            writeCommand("3");
                            DrawDetection(1,offset);
                            checker2.addData(data, sampling, 2);
                        }
                        else if (checker2.checkOffset(data, sampling, 2) ){
                            usedTemplate = 2;
                            swallow.add(sampling);
                            offset = sampling - 1;
                            flagDetection = 2;
                            writeCommand("3");
                            DrawDetection(1,offset);
                        }
                    }
                    else if (flagDetection == 1 && sampling < onset + 10 && sampling > onset + 5){ //通常嚥下用の検出アルゴリズム
                        checker1.addData(data, sampling, 1);
                        checker2.addData(data, sampling, 2);
                        if (data.get(sampling) > data.get(onset-5)*0.7){
                            offset = sampling;
                            swallow.add(sampling);
                            flagDetection = 2;
                            writeCommand("3");
                            DrawDetection(1, offset);
                        }
                    }
                    else{
                        checker1.addData(data, sampling, 1);
                        checker2.addData(data, sampling, 2);
                    }

                    //取得データ及び検出結果の描画
                    if (spinner_channel.getSelectedItemPosition()+1 == 1){
                        addEntry((CH1-minData1)/(maxData1-minData1));
                    }
                    else if(spinner_channel.getSelectedItemPosition()+1 == 2){
                        addEntry((CH2-minData2)/(maxData2-minData2));
                    }
                }
                sampling++;
                return;
            }
        }

        //クライアントからペリフェラルはコマンドの書き込みをしたとき呼ばれる?
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: " + status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.e(TAG, "onCharacteristicWrite: GATT_SUCCESS");
                    break;
                case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    Log.e(TAG, "onCharacteristicWrite: GATT_WRITE_NOT_PERMITTED");
                    break;
                case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                    Log.e(TAG, "onCharacteristicWrite: GATT_REQUEST_NOT_SUPPORTED");
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    Log.e(TAG, "onCharacteristicWrite: GATT_FAILURE");
                    break;
                case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                    break;
                case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                    break;
                case BluetoothGatt.GATT_INVALID_OFFSET:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // GUIアイテム
        mButton_start = (Button) findViewById(R.id.button_start);
        mButton_start.setOnClickListener(this);
        mButton_end = (Button) findViewById(R.id.button_end);
        mButton_end.setOnClickListener(this);
        mButton_log = (Button) findViewById(R.id.button_log);
        mButton_log.setOnClickListener(this);
        mButton_set_on = (Button) findViewById(R.id.button_set_on);
        mButton_set_on.setOnClickListener(this);
        mButton_set_off = (Button) findViewById(R.id.button_set_off);
        mButton_set_off.setOnClickListener(this);

        subject = findViewById(R.id.subject_name);
        subject.setOnClickListener(this);
        filename = findViewById(R.id.file_text);
        filename.setOnClickListener(this);
        timeText = findViewById(R.id.timeCount);

        ArrayAdapter<String> adapter_channel = new ArrayAdapter<>(this, R.layout.spinner_item,getResources().getStringArray(R.array.channel));
        ArrayAdapter<String> adapter_hantei = new ArrayAdapter<>(this, R.layout.spinner_item,getResources().getStringArray(R.array.hantei));
        ArrayAdapter<String> adapter_countDown = new ArrayAdapter<>(this, R.layout.spinner_item,getResources().getStringArray(R.array.countDown));
        ArrayAdapter<String> adapter_trial = new ArrayAdapter<>(this, R.layout.spinner_item,getResources().getStringArray(R.array.trial_name));

        adapter_channel.setDropDownViewResource(R.layout.spinner_center_item);
        adapter_hantei.setDropDownViewResource(R.layout.spinner_center_item);
        adapter_countDown.setDropDownViewResource(R.layout.spinner_center_item);
        adapter_trial.setDropDownViewResource(R.layout.spinner_center_item);

        spinner_channel = findViewById(R.id.channel);
        spinner_channel.setAdapter(adapter_channel);
        spinner_hantei = findViewById(R.id.hantei);
        spinner_hantei.setAdapter(adapter_hantei);
        spinner_countDown = findViewById(R.id.countDown);
        spinner_countDown.setAdapter(adapter_countDown);
        spinner_trial = findViewById(R.id.trial_name);
        spinner_trial.setAdapter(adapter_trial);

        ((TextView) findViewById(R.id.textview_BLEconnect)).setText("機器未接続");
        ((TextView) findViewById(R.id.textview_BLEconnect)).setTextColor(Color.GRAY);


        initChartForNormalize();

        // Android端末がBLEをサポートしてるかの確認
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_is_not_supported, Toast.LENGTH_SHORT).show();
            finish();    // アプリ終了宣言
            return;
        }

        // Bluetoothアダプタの取得
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (null == mBluetoothAdapter) {    // Android端末がBluetoothをサポートしていない
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show();
            finish();    // アプリ終了宣言
            return;
        }

        //テンプレートの読み込み
        String[] filesName;
        AssetManager assetManager = getResources().getAssets();
        try {
            filesName = assetManager.list("template");
            for (int i=0; i<filesName.length; i++){
                BufferedReader br = new BufferedReader(new InputStreamReader(assetManager.open("template/" + filesName[i])));
                ArrayList<Double> data = new ArrayList<>();
                ArrayList<Double> DTW = new ArrayList<>();
                String line;
                while((line = br.readLine()) != null){
                    data.add(Double.parseDouble(line));
                }
                Templist.add(data);
                DTWDistance.add(DTW);
                br.close();
            }
            ( (TextView)findViewById( R.id.textview_message ) ).setText("テンプレートを読み込みました。");
        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error!!")
                    .setMessage("テンプレートが読み込めませんでした。")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void initChart() {

        mChart.clear();
        mChart = findViewById(R.id.chart);
        mChart.clearAnimation();

        flagST = 0;
        flagDetection = 0;
        setSensorFlag = 0;
        sampling = 0;
        timeCount = 0;
        maxTimeCount = 0;

        timeText.setText(String.valueOf((float)timeCount/10) + "秒");

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(false);

        // enable scaling and dragging
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        // set an alternative background color
        //mChart.setBackgroundColor(Color.GRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(false);
        xl.setEnabled(true);
        xl.removeAllLimitLines();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setTextColor(Color.BLACK);

        leftAxis.setAxisMaximum(1.5f);
        leftAxis.setAxisMinimum(-0.5f);
        leftAxis.setEnabled(true);

        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();

        rightAxis.setEnabled(false);
    }

    private void initChartForNormalize() {

        sampling = 0;
        mChart = findViewById(R.id.chart);
        mChart.clearAnimation();

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(false);

        // enable scaling and dragging
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        // set an alternative background color
        //mChart.setBackgroundColor(Color.GRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(false);
        xl.setEnabled(true);
        xl.removeAllLimitLines();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);

        leftAxis.setEnabled(true);

        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();

        LimitLine upper_limit = new LimitLine(1.5f, "Upper Limit");
        upper_limit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

        LimitLine lower_limit = new LimitLine(1.0f, "Lower Limit");
        lower_limit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(upper_limit);
        leftAxis.addLimitLine(lower_limit);

        leftAxis.setAxisMaximum(2.0f);
        leftAxis.setAxisMinimum(0.5f);

        rightAxis.setEnabled(false);
    }

    private void addEntry(double value) {
        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set1 = data.getDataSetByIndex(0);

            if (set1 == null) {
                if (spinner_channel.getSelectedItemPosition()+1 == 1){
                    set1 = createSet(0);
                }
                else if (spinner_channel.getSelectedItemPosition()+1 == 2){
                    set1 = createSet(1);
                }
                data.addDataSet(set1);
            }

            data.addEntry(new Entry(set1.getEntryCount(),(float) value), 0);


            //x軸について
            XAxis xl = mChart.getXAxis();
            if(sampling < 120){
                xl.setAxisMaximum(120f);
                xl.setAxisMinimum(0f);
            }
            else{
                xl.setAxisMaximum(sampling);
            }

            //更新を通知
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(120);
            mChart.setVisibleXRangeMinimum(120);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet(int num) {
        LineDataSet set1;
        set1 = new LineDataSet(null, "CH" + (num+1));


        set1.setDrawCircles(false);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        if(num == 0){
            set1.setColor(ColorTemplate.JOYFUL_COLORS[4]);
        }
        else{
            set1.setColor(ColorTemplate.JOYFUL_COLORS[0]);
        }

        set1.setLineWidth(8f);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setValueTextColor(Color.WHITE);
        set1.setValueTextSize(9f);
        set1.setDrawValues(false);
        return set1;
    }

    // 初回表示時、および、ポーズからの復帰時
    @Override
    protected void onResume() {
        super.onResume();

        // Android端末のBluetooth機能の有効化要求
        requestBluetoothFeature();

        // GUIアイテムの有効無効の設定
        mButton_start.setEnabled(false);
        mButton_end.setEnabled(false);
        mButton_log.setEnabled(false);
        mButton_set_on.setEnabled(false);
        mButton_set_off.setEnabled(false);


        // デバイスアドレスが空でなければ、接続ボタンを有効にする。
        if (!mDeviceAddress.equals("")) {
            connect();
        }
    }

    // 別のアクティビティ（か別のアプリ）に移行したことで、バックグラウンドに追いやられた時
    @Override
    protected void onPause() {
        super.onPause();
        // 切断
        disconnect();
    }

    // アクティビティの終了直前
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mBluetoothGatt) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    // Android端末のBluetooth機能の有効化要求
    private void requestBluetoothFeature() {
        if (mBluetoothAdapter.isEnabled()) {
            return;
        }
        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLEBLUETOOTH);
    }

    /// 機能の有効化ダイアログの操作結果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLEBLUETOOTH: // Bluetooth有効化要求
                if (Activity.RESULT_CANCELED == resultCode) {    // 有効にされなかった
                    Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show();
                    finish();    // アプリ終了宣言
                    return;
                }
                break;
            case REQUEST_CONNECTDEVICE: // デバイス接続要求

                if (Activity.RESULT_OK == resultCode) {
                    // デバイスリストアクティビティからの情報の取得
                    strDeviceName = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_NAME);
                    mDeviceAddress = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_ADDRESS);
                } else {
                    strDeviceName = "";
                    mDeviceAddress = "";
                }
                ((TextView) findViewById(R.id.textview_message)).setText("");
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // オプションメニュー作成時の処理
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    // オプションメニューのアイテム選択時の処理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_search:
                Intent devicelistactivityIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(devicelistactivityIntent, REQUEST_CONNECTDEVICE);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (mButton_start.getId() == v.getId())    //スタートボタン押されたとき
        {
            ( (TextView)findViewById( R.id.textview_message ) ).setText("計測中");
            writeCommand("1");
            setCharacteristicNotification( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE1 );
            initChart();
            countDownDialog();

            checker1 = new CheckSwallow(Templist.get(1)); //長いV字型
            checker2 = new CheckSwallow(Templist.get(0)); //バスタブ型
            ansei = (spinner_countDown.getSelectedItemPosition()+1)*10;
            mButton_start.setEnabled(false);
            mButton_set_on.setEnabled(false);
            mButton_set_off.setEnabled(false);
            mButton_end.setEnabled(true);
            mButton_log.setEnabled(true);
            spinner_channel.setClickable(false);
            spinner_hantei.setClickable(false);
            spinner_countDown.setClickable(false);


            Date date = new Date(System.currentTimeMillis());
            DateFormat df  = new SimpleDateFormat("yyyyMMddkkmmss");
            filename.setText(df.format(date)); //ファイル名が未記入なら日付が入る.

            return;
        }
        if (mButton_end.getId() == v.getId())   //エンドボタン押されたとき
        {
            writeCommand("2");
            ( (TextView)findViewById( R.id.textview_message ) ).setText("計測終了");

            //エンドボタン押されて，通信切り終わってから初期化（0.5sのディレイ）
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mButton_end.setEnabled(false);
                    mButton_log.setEnabled(false);
                    mButton_start.setEnabled(true);
                    mButton_set_on.setEnabled(true);
                    mButton_set_off.setEnabled(false);
                    spinner_channel.setClickable(true);
                    spinner_hantei.setClickable(true);
                    spinner_countDown.setClickable(true);

                    mChart.setTouchEnabled(true);
                    mChart.setDragEnabled(true);

                    writeData();
                    init();
                }
            }, 250);
            return;
        }
        if (mButton_log.getId() == v.getId()) //ログボタン
        {
            if (flagST == 0){
                flagST = 1;
            }
            else {
                flagST = 0;
            }
            DrawDetection(0,sampling);
            log.add(sampling);
        }
        if (mButton_set_on.getId() == v.getId() && setSensorFlag == 0) //センサ位置調整用ボタン
        {
            writeCommand("1");
            maxData1 = Double.MIN_VALUE;
            minData1 = Double.MAX_VALUE;
            maxData2 = Double.MIN_VALUE;
            minData2 = Double.MAX_VALUE;

            setSensorFlag = 1;
            ( (TextView)findViewById( R.id.textview_message ) ).setText("センサ位置調整中");
            initChartForNormalize();
            setCharacteristicNotification( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE1 );

            mButton_start.setEnabled(false);
            mButton_set_on.setEnabled(false);
            mButton_set_off.setEnabled(true);
            mButton_end.setEnabled(false);
            mButton_log.setEnabled(false);
            spinner_channel.setClickable(false);
            spinner_hantei.setClickable(false);
            spinner_countDown.setClickable(false);
        }
        if (mButton_set_off.getId() == v.getId() && setSensorFlag == 1)
        {
            writeCommand("2");

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setSensorFlag = 0;
                    ( (TextView)findViewById( R.id.textview_message ) ).setText("センサ位置調整完了");
                    initChartForNormalize();

                    init();
                    mButton_start.setEnabled(true);
                    mButton_set_on.setEnabled(true);
                    mButton_set_off.setEnabled(false);
                    mButton_end.setEnabled(false);
                    mButton_log.setEnabled(false);
                    spinner_channel.setClickable(true);
                    spinner_hantei.setClickable(true);
                    spinner_countDown.setClickable(true);

                    mChart.setTouchEnabled(true);
                    mChart.setDragEnabled(true);
                }
            }, 250);
        }

        if (subject.getId() == v.getId()) //被験者名指定選択用
        {
            subject.setFocusable(true);
            subject.setFocusableInTouchMode(true);
            subject.requestFocus();
        }
    }

    // カウントダウン
    private void countDownDialog() {

        CustomDialog dlg = new CustomDialog(this, R.style.Theme_AppCompat_Light_Dialog);
        dlg.setCancelable(false);
        dlg.setShowTime((spinner_countDown.getSelectedItemPosition()+1)*1000);
        dlg.show();
    }

    // 接続
    private void connect() {
        if (mDeviceAddress.equals("")) {    // DeviceAddressが空の場合は処理しない
            return;
        }

        if (null != mBluetoothGatt) {    // mBluetoothGattがnullでないなら接続済みか、接続中。
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        mBluetoothGatt = device.connectGatt(this, false, mGattcallback);

    }

    // 切断
    private void disconnect() {

        if (null == mBluetoothGatt) {
            return;
        }


        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mservice = null;
        mDataCharacteristic = null;
        data1.clear();
        data2.clear();
        normalizedData1.clear();
        normalizedData2.clear();
        log.clear();
        swallow.clear();
        DTWDistance.clear();

        // GUIアイテムの有効無効の設定
        // 接続ボタンのみ有効にする
        ((TextView) findViewById(R.id.textview_BLEconnect)).setText("機器未接続");
        ((TextView) findViewById(R.id.textview_BLEconnect)).setTextColor(Color.GRAY);
        ((TextView) findViewById(R.id.textview_message)).setText("");

        mButton_start.setEnabled(false);
        mButton_end.setEnabled(false);
        mButton_log.setEnabled(false);
        mButton_set_off.setEnabled(false);
        mButton_set_on.setEnabled(false);
        mChart.removeAllViews();
        filename.setText("");
    }

    //ファイル書き込み
    private void writeData(){
        Date date = new Date(System.currentTimeMillis());
        DateFormat df  = new SimpleDateFormat("yyyyMMdd");
        File dir_init = new File(Environment.getExternalStorageDirectory() + "/BiofeedbackForMendelsohnManeuver");
        if(!dir_init.exists()){
            dir_init.mkdir();
        }
        File dir = new File(Environment.getExternalStorageDirectory() + "/BiofeedbackForMendelsohnManeuver/" + df.format(date));
        if(!dir.exists()){
            dir.mkdir();
        }
        File dir_data = new File(Environment.getExternalStorageDirectory() + "/BiofeedbackForMendelsohnManeuver/" + df.format(date) + "/data/");
        if(!dir_data.exists()){
            dir_data.mkdir();
        }
        File dir_DTW = new File(Environment.getExternalStorageDirectory() + "/BiofeedbackForMendelsohnManeuver/" + df.format(date) + "/DTWDistance/");
        if(!dir_DTW.exists()){
            dir_DTW.mkdir();
        }


        String name = filename.getText().toString();
        if(name.equals("")){
            ( (TextView)findViewById( R.id.textview_message ) ).setText("ファイル名を指定してください");
        }
        else {
            String filePath = dir_data + "/" + name + ".csv";
            String filePath_DTW = dir_DTW + "/" + name + "_DTWDistance.csv";

            File file = new File(filePath);
            File file_DTW = new File(filePath_DTW);

            file.getParentFile().mkdir();
            file_DTW.getParentFile().mkdir();

            FileOutputStream fos, fos_DTW;
            try {
                fos = new FileOutputStream(file, true);
                fos_DTW = new FileOutputStream(file_DTW, true);

                OutputStreamWriter osw = new OutputStreamWriter(fos, "SJIS");
                OutputStreamWriter osw_DTW = new OutputStreamWriter(fos_DTW, "SJIS");

                BufferedWriter bw = new BufferedWriter(osw);
                BufferedWriter bw_DTW = new BufferedWriter(osw_DTW);


                bw.write("被験者名," + subject.getText().toString() + "\n");
                if (spinner_trial.getSelectedItemPosition() == 0){
                    bw.write("試行事例," + "通常嚥下" + "\n");
                }
                else {
                    bw.write("試行事例," + "メンデルソン手技" + "\n");
                }
                bw.write("隆起部上センサ," + (spinner_channel.getSelectedItemPosition()+1) + "\n");
                bw.write("安静時間," + String.valueOf((spinner_countDown.getSelectedItemPosition()+1)*10) + "\n");

                if (usedTemplate == 1){
                    bw.write(  "検出テンプレート" + ",長いV時型"+ "\n");
                }
                else if (usedTemplate == 2){
                    bw.write(  "検出テンプレート" + ",バスタブ型"+ "\n");
                }
                else {
                    bw.write("検出テンプレート" + ",未検出"+ "\n");
                }

                bw.write("CH1,CH2,,LOG,Detection,,normalized-CH1,normalized-CH2\n");
                for (int i = 0; i < data1.size(); i++) {
                    bw.write(data1.get(i) + "," + data2.get(i) + ",,");
                    if(log.contains(i)){
                        bw.write( 1 + ",");
                    }
                    else {
                        bw.write( 0 + ",");
                    }
                    if(swallow.contains(i)){
                        bw.write(1 + ",,");
                    }
                    else {
                        bw.write(0 + ",,");
                    }
                    bw.write(normalizedData1.get(i) + "," + normalizedData2.get(i) + "\n");
                }
                bw.flush();
                bw.close();

                bw_DTW.write("template1,template2,,diff2\n");
                for (int t=0; t<sampling; t++){
                    double DTWDistance = checker1.getDTW().get(t);
                    double DTWDistance2 = checker2.getDTW().get(t);
                    double smoothingDTWDiff = checker2.getSPRING_DTW().getSmoothingDTWDiff().get(t);

                    if (t<onset + 10){
                        bw_DTW.write(",,,\n");
                    }
                    else {
                        bw_DTW.write(String.valueOf(DTWDistance) + "," + String.valueOf(DTWDistance2) + ",," + String.valueOf(smoothingDTWDiff)+"\n");
                    }
                }
                bw_DTW.flush();
                bw_DTW.close();

                ((TextView) findViewById(R.id.textview_message)).setText("ファイルを書き込みました。");

                String[] paths = {Environment.getExternalStorageDirectory().toString()+ "/BiofeedbackForMendelsohnManeuver/"+ df.format(date) + "/data/" + name + ".csv"};
                String[] paths_DTW = {Environment.getExternalStorageDirectory().toString()+ "/BiofeedbackForMendelsohnManeuver/"+ df.format(date) + "/DTWDistance/" + name + "_DTWDistance.csv"};

                String[] mimeTypes = {"text/csv"};
                MediaScannerConnection.OnScanCompletedListener mScanCompletedListener = new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d("MediaScannerConnection", "Scanned " + path + ":");
                        Log.d("MediaScannerConnection", "-> uri=" + uri);
                    }
                };
                MediaScannerConnection.scanFile(getApplicationContext(), paths, mimeTypes, mScanCompletedListener);
                MediaScannerConnection.scanFile(getApplicationContext(), paths_DTW, mimeTypes, mScanCompletedListener);


            } catch (Exception e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error!!")
                        .setMessage("ファイルの書き込みに失敗しました。\n" + e)
                        .setPositiveButton("OK", null)
                        .show();
            }

        }
    }


    // キャラクタリスティック通知の設定
    private void setCharacteristicNotification( UUID uuid_service, UUID uuid_characteristic)
    {
        if( null == mBluetoothGatt )
        {
            return;
        }
        boolean enable = true;
        BluetoothGattCharacteristic blechar = mBluetoothGatt.getService( uuid_service ).getCharacteristic( uuid_characteristic );
        mBluetoothGatt.setCharacteristicNotification( blechar, enable );
        BluetoothGattDescriptor descriptor = blechar.getDescriptor( UUID_CHARACTERISTIC_DESCRIPTOR );
        descriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
        mBluetoothGatt.writeDescriptor( descriptor );
    }


    // ペリフェラルへコマンドの書き込み
    private void writeCommand(String command) {
        if (null == mBluetoothGatt) {
            return;
        }
        if (mDataCharacteristic != null) {
            //コマンドのセット
            String WrtVal = command;
            mDataCharacteristic.setValue(WrtVal);
            //書き込み
            mBluetoothGatt.writeCharacteristic(mDataCharacteristic);
        }
    }

    private void init(){
        data1.clear();
        data2.clear();
        normalizedData1.clear();
        normalizedData2.clear();
        log.clear();
        swallow.clear();
        for (ArrayList<Double> elem : DTWDistance){
            elem.clear();
        }
    }



    //Bluetoothボタンのイベントを拾う
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN){
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP){ //音量アップでログ
                if (mButton_log.isEnabled()){
                    mButton_log.callOnClick();
                }
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){ //真ん中で開始終了
                if (mButton_start.isEnabled()){
                    mButton_start.callOnClick();
                }
                else if (mButton_end.isEnabled()){
                    mButton_end.callOnClick();
                }
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS){
                if (mButton_set_on.isEnabled()){
                    mButton_set_on.callOnClick();
                }
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT){
                if (mButton_set_off.isEnabled()){
                    mButton_set_off.callOnClick();
                }
            }

        }
        return super.dispatchKeyEvent(event);
    }

    private void DrawDetection(int select, int time){
        XAxis xAxis = mChart.getXAxis();
        if (select == 0){
            LimitLine detection = new LimitLine(time);
            detection.setLineWidth(8f);
            detection.setLineColor(ColorTemplate.JOYFUL_COLORS[3]);
            xAxis.addLimitLine(detection);
        }
        else {
            LimitLine detection = new LimitLine(time);
            detection.setLineWidth(8f);
            detection.setLineColor(ColorTemplate.JOYFUL_COLORS[1]);
            xAxis.addLimitLine(detection);
        }
    }
}

//拡張したダイアログ
class CustomDialog extends Dialog {

    private Handler mHandler = new Handler();   // ハンドラー
    private int mShowTime;
    private Runnable mRunDismiss;
    private Runnable mRunCountdown;
    private int mCount;

    // ////////////////////////////////////////////////////////////・
    // コンストラクタ一式
    public CustomDialog(Context context, boolean cancelable,
                        OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.setContentView(R.layout.dialog);
    }
    public CustomDialog(Context context, int theme) {
        super(context, theme);
        this.setContentView(R.layout.dialog);
    }
    public CustomDialog(Context context) {
        super(context);
        this.setContentView(R.layout.dialog);
    }

    // ////////////////////////////////////////////////////////////
    // カウントダウンしたい期間を設定する
    public void setShowTime(int time) {
        this.mShowTime = time;
        this.mCount = (time / 1000) + 1;
    }

    // ////////////////////////////////////////////////////////////
    // ダイアログの表示開始
    @Override
    public void show() {
        final CustomDialog dlg = this;

        // ダイアログを削除するためのモノ
        this.mRunDismiss = new Runnable() {
            public void run() {
                dlg.dismiss();
            }
        };

        // 一定期間ごとに表示されるタイマーを更新するためのモノ
        this.mRunCountdown = new Runnable() {
            public void run() {
                dlg.updateCountdown();
                // 一定期間ごとにカウントダウン
                dlg.mHandler.postDelayed(dlg.mRunCountdown, 1000);
            }
        };
        // しばらくまってから実行
        this.mHandler.postDelayed(this.mRunDismiss, this.mShowTime);
        this.mHandler.postDelayed(this.mRunCountdown, 0);

        super.show();

    }

    // ////////////////////////////////////////////////////////////
    // カウントダウンを更新する
    public void updateCountdown() {
        this.mCount--;
        TextView count = (TextView)this.findViewById(R.id.text_count);
        count.setText(""+this.mCount);
    }
}
