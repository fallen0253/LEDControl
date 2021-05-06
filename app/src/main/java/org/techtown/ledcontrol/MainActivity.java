package org.techtown.ledcontrol;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    ImageView imgConnect;
    Switch swRoomControl1, swRoomControl2, swRoomControl3;
    BluetoothAdapter bluetoothAdapter;
    int pairedDeviceCount=0;
    Set<BluetoothDevice> devices;
    BluetoothDevice remoteDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream=null;
    InputStream inputStream=null;
    Thread workerThread=null;
    String strDelimiter="\n";
    char charDelimiter='\n';
    byte readBuffer[];
    int readBufferPosition;
    String str; // 아두이노의 전달할 값
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swRoomControl1=findViewById(R.id.swRoomControll1);
        swRoomControl2=findViewById(R.id.swRoomControll2);
        swRoomControl3=findViewById(R.id.swRoomControll3);
        imgConnect=findViewById(R.id.imgConnect);
        imgConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetooth();
            }
        });
        /*swRoomControl1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                str="1";
                sendData(str);
            }
        });
        swRoomControl2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                str="2";
                sendData(str);
            }
        });
        swRoomControl3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                str="3";
                sendData(str);
            }
        });*/
        swRoomControl1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    str="11";
                }else{
                    str="10";
                }
                sendData(str);
            }
        });
        swRoomControl2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    str="21";
                }else{
                    str="20";
                }
                sendData(str);
            }
        });
        swRoomControl3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    str="31";
                }else{
                    str="30";
                }
                sendData(str);
            }
        });
    }


    //스마트폰의 블루투스 지원 여부 검사
    void checkBluetooth(){

        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            /*showToast("블루투수를 지원하지 않는 장치입니다.");*/
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("블루투스");
            builder.setMessage("블루투스를 지원하지 않는 폰입니다.\n 확인을 누르시면 앱이 종료됩니다.");
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }else{
            //장치가 블루투수를 지원하는 경우
            if(!bluetoothAdapter.isEnabled()){
                //암시적 인텐트 안드로이드웹 내장되어있는 액티비티 활용 명시적 인텐트 내가만든 액티비티 활용
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 10);
            }else{
                selectDevice();
            }
        }
    }
    //페어링된 장치 목록 출력 및 선택
    void  selectDevice(){
        devices=bluetoothAdapter.getBondedDevices();
        pairedDeviceCount=devices.size();
        if(pairedDeviceCount==0){
            showToast("페어링된 장치가 하나도 없습니다.");
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("블루투스 장치 선택");
            List<String> listItems = new ArrayList<String>();
            for(BluetoothDevice device:devices){
                listItems.add(device.getName());
            }
            listItems.add("취소");
            // 동적배열이 안된다. 동적배열값을 toArray란 메서드로 일반 배열화 시킨다
            final CharSequence[] items=listItems.toArray(new CharSequence[listItems.size()]);
            // 목록대화상자를 만든다.(일반배열만 가능하다)
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==pairedDeviceCount){
                        showToast("취소를 선택했습니다.");
                    }else{
                        connectToSelectedDevice(items[which].toString());
                    }
                }
            });
            builder.setCancelable(false); // 뒤로 가기 버튼 사용금지
            AlertDialog dlg=builder.create();
            dlg.show();
        }
    }
    //선택한 블루투스 장치와의 연결
    void connectToSelectedDevice(String selectedDeviceName){
        remoteDevice=getDeviceFromBoundedList(selectedDeviceName);
        //기계번호
        UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try{
            bluetoothSocket=remoteDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect(); // 기기 연결 완료
            imgConnect.setImageResource(R.drawable.bluetooth);
            outputStream=bluetoothSocket.getOutputStream();
            inputStream=bluetoothSocket.getInputStream();
            beginListenForData();
        }catch (Exception e){
            showToast("소켓 연결이 되지 않습니다.");
        }
    }

    //데이터 수신 준비 및 처리
    void beginListenForData(){
        final Handler handler = new Handler();
        readBuffer = new byte[1024]; // 수신버퍼
        readBufferPosition=0; // 버퍼 내 수신 문자 저장 위치
        // 문자열 수신 쓰레드
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()){
                    try{
                        int bytesAvailable = inputStream.available(); // 수신 데이터 확인
                        if(bytesAvailable > 0){
                            byte[] packetBytes=new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for(int i=0; i<bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b==charDelimiter){
                                    byte[] encodeBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer,0,encodeBytes,0, encodeBytes.length);
                                    final String data = new String(encodeBytes,"US-ASCII");
                                    readBufferPosition=0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //data변수에 수신된 문자열에 대한 처리 작업

                                        }
                                    });
                                }else{
                                    readBuffer[readBufferPosition++]=b;
                                }
                            }
                        }
                    }catch (IOException e){
                        showToast("수신 중 오류가 발생했습니다.");
                    }
                }
            }
        });
        workerThread.start();
    }

    // 페어링된 블루투스 장치를 이름으로 찾기
    BluetoothDevice getDeviceFromBoundedList(String name){
        BluetoothDevice selectedDevice=null;
        for(BluetoothDevice device: devices){
            if (name.equals(device.getName())){
                selectedDevice=device;
                break;
            }
        }
        return selectedDevice;
    }

    //데이터 송신(아두이노로 전송)
    void sendData(String msg){
        msg+=strDelimiter;
        try {
            outputStream.write(msg.getBytes()); // 문자열 전송
        }catch (Exception e){
            showToast("문자열 전송 도중에 오류가 발생했습니다.");
        }
    }
    // 앱을 종료시킬 때 하지않으면 쓰레드 , 인풋 , 아웃풋, 블루투스 소켓을 사용할 수 없다.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            workerThread.interrupt(); // 쓰레드 중단
            inputStream.close();  // 인풋스트림 중단
            outputStream.close(); // 아웃풋스트림 중단
            bluetoothSocket.close(); // 블루투스 소켓 중단
        }catch (Exception e){
            showToast("앱 종료 중 에러 발생");
        }
    }

    void showToast(String msg){
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 10:
                if(resultCode==RESULT_OK){
                    selectDevice();
                }else if(resultCode==RESULT_CANCELED){
                    showToast("블루투수 활성화를 취소했습니다.");
                }
                break;
        }
    }
}