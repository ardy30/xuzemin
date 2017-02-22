package com.android.jdrd.headcontrol.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.jdrd.headcontrol.R;
import com.android.jdrd.headcontrol.activity.WelcomeActivity;
import com.android.jdrd.headcontrol.common.BaseFragment;
import com.android.jdrd.headcontrol.dialog.MyDialog;
import com.android.jdrd.headcontrol.service.ServerSocketUtil;
import com.android.jdrd.headcontrol.util.Contact;
import com.android.jdrd.headcontrol.view.MyView;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Administrator on 2017/2/7.
 */

public class MapFragment extends BaseFragment implements View.OnClickListener {

    private MyReceiver receiver = new MyReceiver();
    private IntentFilter filter =new IntentFilter();
    private MyView surfaceview = null;
    private int bitmap_width = 0,bitmap_height= 0;
    //路线选择、找人时间、找人范围、转弯角度、互动时间
    private Spinner planchooce,serchtime,scope,angle,gametime;
    //路线选择、找人时间、找人范围、转弯角度、互动时间；（对应number）
    private float plannumber,serchtimenumber,scopenumber,anglenumber,gametimenumber;
    private Context context;
    private double nLenStart = 0;
    private EditText point_x,point_y;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> map_Plan;
    private float eventx,eventy;
    public static boolean Istouch = false,Isplan = false,IsFind = false;
    //路线模式布局、路线模式详细设置；
    private LinearLayout linearlayout_map,linear_plan,linear_plan_info,linear_point,linear_roam;
    private HashMap<String,Vector<Float>> arrayhash;
    private Vector<Float> xs,ys,arrayserchtime,arrayscope,arrayangle,arraygametime;
    private Vector<Float> xs_tmp = new Vector<>();
    private Vector<Float> ys_tmp  = new Vector<>();
    private Vector<Float> arrayserchtime_tmp = new Vector<>();
    private Vector<Float> arrayscope_tmp = new Vector<>();
    private Vector<Float> arrayangle_tmp = new Vector<>();
    private Vector<Float> arraygametime_tmp = new Vector<>();
    private Timer timer;
    private TimerTask task;
    private HashMap<String,HashMap<String,Vector<Float>>> arrayPlanLists = new HashMap<>();
    private Vector<String> strings = new Vector<>();
    public  MapFragment(){
        super();
    }

    private Thread thread = null ;

    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    if(surfaceview !=null&&surfaceview.point_xs.size()>=1){
                        surfaceview.point_xs.removeAllElements();
                        surfaceview.point_ys.removeAllElements();
                        arrayserchtime.removeAllElements();
                        arrayscope.removeAllElements();
                        arrayangle.removeAllElements();
                        arraygametime.removeAllElements();
                    }
                    linear_plan_info.setVisibility(View.GONE);
                    Istouch = false;
                    Contact.debugLog("btn_sure");
                    break;
                case 2:
                    Contact.debugLog("btn_cancle");
                    break;
                case 3:
                    sendBundle(Contact.Command,Contact.StopSearch,"");
                    Contact.debugLog("btn_warn_sure"+thread.isAlive());
                    if(thread.isAlive()){
                        thread = new Thread();
                    }
                    Contact.debugLog("btn_warn_sure"+thread.isAlive());
                    sendBundle(Contact.Command,Contact.StopSearch,"");
                    break;
                case 4:
                    resetTimer();
                    synchronized (thread) {
                        thread.notify();
                        try {
                            thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 5:
//                    synchronized (thread) {
//                        thread.notify();
//                    }
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @SuppressLint("ValidFragment")
    public MapFragment(Context context){
        super(context);
        this.context = context;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {//给当前的fragment绘制UI布局，可以使用线程更新UI
        mView=inflater.inflate(R.layout.fragment_map,container,false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void initView() {
        surfaceview=(MyView)findViewById(R.id.surfaceview);
        surfaceview.myview_height = 600;
        surfaceview.myview_width = 1000;
    }

    @Override
    public void initData() {
        surfaceview.bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.tuzi);
        surfaceview.rotbitmap = surfaceview.bitmap;
//                BitmapFactory.decodeResource(getResources(), R.mipmap.tuzi);

//        InputStream is = getResources().openRawResource(R.mipmap.tuzi);
//        surfaceview.gifMovie = Movie.decodeStream(is);
//        surfaceview.bitmap = Bitmap.createBitmap(surfaceview.bitmap,0,0,100,100);
//
//        bitmap_width = surfaceview.bitmap.getWidth();
//        bitmap_height= surfaceview.bitmap.getHeight();
        planchooce = (Spinner) findViewById(R.id.spinner_plan);
        serchtime = (Spinner) findViewById(R.id.serchtime);
        scope = (Spinner) findViewById(R.id.scope);
//        angle = (Spinner) findViewById(R.id.angle);
        gametime = (Spinner) findViewById(R.id.gametime);
        point_x = (EditText) findViewById(R.id.point_x);
        point_y = (EditText) findViewById(R.id.point_y);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void initEvent() {
        linearlayout_map = (LinearLayout) findViewById(R.id.linearlayout_map);
        linear_plan = (LinearLayout) findViewById(R.id.relative_plan);
        linear_plan_info = (LinearLayout) findViewById(R.id.relative_plan_info);
        linear_point = (LinearLayout)findViewById(R.id.linearlayout_point);
        linear_roam = (LinearLayout)findViewById(R.id.linearlayout_roam);
        findViewById(R.id.button_clearlast).setOnClickListener(this);
        findViewById(R.id.button_clearall).setOnClickListener(this);
        findViewById(R.id.button_plan).setOnClickListener(this);
        findViewById(R.id.button_plan_stop).setOnClickListener(this);
        findViewById(R.id.button_pointchooce).setOnClickListener(this);
        findViewById(R.id.button_pathplan).setOnClickListener(this);
        findViewById(R.id.button_cruise).setOnClickListener(this);
        findViewById(R.id.button_savenext).setOnClickListener(this);
        findViewById(R.id.button_saveall).setOnClickListener(this);
        findViewById(R.id.button_execut).setOnClickListener(this);
        findViewById(R.id.button_next).setOnClickListener(this);
        findViewById(R.id.button_move).setOnClickListener(this);
        findViewById(R.id.button_roam_start).setOnClickListener(this);
        findViewById(R.id.button_roam_stop).setOnClickListener(this);
        findViewById(R.id.button_return).setOnClickListener(this);
        findViewById(R.id.button_remove).setOnClickListener(this);
        findViewById(R.id.button_plan_back).setOnClickListener(this);
        findViewById(R.id.button_plan_info_back).setOnClickListener(this);
        findViewById(R.id.button_point_back).setOnClickListener(this);
        findViewById(R.id.button_roam_back).setOnClickListener(this);
        surfaceview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int nCnt = event.getPointerCount();
                int n = event.getAction();

                if(n==MotionEvent.ACTION_DOWN && 1 == nCnt){
                    eventx = event.getX();
                    eventy = event.getY();
                    Contact.debugLog(eventx+"    "+eventy);
                    if (Istouch) {
                        if (eventx > 20 && eventx < 1020 && eventy > 20 && eventy < 620) {
                            float a, b;
                            a = (event.getX() - 20 - surfaceview.translate_x) / surfaceview.scale;
                            b = ((event.getY() - 20 - surfaceview.translate_y) / surfaceview.scale);
                            int x = (int) a % surfaceview.Scale;
                            int x_int = (int) a / surfaceview.Scale;
                            int y = (int) b % surfaceview.Scale;
                            int y_int = (int) b / surfaceview.Scale;
                            if (x > (surfaceview.Scale / 2)) {
                                a = surfaceview.Scale * (x_int + 1) + 20;
                            } else {
                                a = surfaceview.Scale * x_int + 20;
                            }
                            if (y > (surfaceview.Scale / 2)) {
                                b = surfaceview.Scale * (y_int + 1) + 20;
                            } else {
                                b = surfaceview.Scale * y_int + 20;
                            }
                            if(Isplan){
                                Istouch = false;
                                surfaceview.point_xs.add(a);
                                surfaceview.point_ys.add(b);
                                surfaceview.Isplan = true;
                            }else{
                                sendNativePoint(a,b,0);
                                surfaceview.point_xs.removeAllElements();
                                surfaceview.point_ys.removeAllElements();
                                surfaceview.point_xs.add(a);
                                surfaceview.point_ys.add(b);
                                surfaceview.Isplan = false;
                                Istouch = false;
                            }
                        }
                    } else {

                    }
                }else if(n==MotionEvent.ACTION_MOVE&&1 == nCnt){
                    surfaceview.translate_x += (event.getX() - eventx)/5;
                    surfaceview.translate_y += (event.getY() - eventy)/5;
                    eventx = event.getX();
                    eventy = event.getY();
                }

                //放大缩小
                if( (n&MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && 2 == nCnt)//<span style="color:#ff0000;">2表示两个手指</span>
                {
                    int xlen = Math.abs((int)event.getX(0) - (int)event.getX(1));
                    int ylen = Math.abs((int)event.getY(0) - (int)event.getY(1));

                    nLenStart = Math.sqrt((double)xlen*xlen + (double)ylen * ylen);
                }else if( (n&MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP  && 2 == nCnt)
                {
                    int xlen = Math.abs((int)event.getX(0) - (int)event.getX(1));
                    int ylen = Math.abs((int)event.getY(0) - (int)event.getY(1));
                    double nLenEnd = Math.sqrt((double)xlen*xlen + (double)ylen * ylen);
                    if(nLenEnd > nLenStart)//通过两个手指开始距离和结束距离，来判断放大缩小
                    {
                        if(surfaceview.scale < 3) {
                            surfaceview.scale = (float) (surfaceview.scale + 0.2);
                        }
                    }
                    else
                    {
                        if(surfaceview.scale > 1){
                            surfaceview.scale = (float) (surfaceview.scale - 0.2);
                        }
                    }
                }
                return true;
            }
        });

        planchooce.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(Istouch){
                    Contact.showWarntext(context,handler);
                }else {
                    plannumber = position;
                    Contact.debugLog("plannumber = " + plannumber);
                    HashMap<String, Vector<Float>> array = arrayPlanLists.get(strings.get((int) plannumber));
                    surfaceview.point_xs = array.get("point_xs");
                    surfaceview.point_ys = array.get("point_ys");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        filter.addAction("com.jdrd.fragment.Map");
        if(context!=null && receiver != null && filter != null){
            context.registerReceiver(receiver,filter);
        }
        init();
        updatekey();


    }
    @Override
    public void onDestroy() {
        super.onDestroy();
//        dialog.dismiss();
        if(context!=null && receiver !=null) {
            context.unregisterReceiver(receiver);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            //清除上一步
            case R.id.button_clearlast:
                if(surfaceview !=null&&surfaceview.point_xs.size()>=1){
                    if(Istouch){
                        surfaceview.point_xs.remove(surfaceview.point_xs.size()-1);
                        surfaceview.point_ys.remove(surfaceview.point_ys.size()-1);
                        xs.remove(surfaceview.point_ys.size()-1);
                        ys.remove(surfaceview.point_ys.size()-1);
                        arrayserchtime.remove(surfaceview.point_ys.size()-1);
                        arrayscope.remove(surfaceview.point_ys.size()-1);
//                        arrayangle.remove(surfaceview.point_ys.size()-1);
                        arraygametime.remove(surfaceview.point_ys.size()-1);
                    }else{
                        surfaceview.point_xs.remove(surfaceview.point_xs.size()-1);
                        surfaceview.point_ys.remove(surfaceview.point_ys.size()-1);
                    }
                }
                Istouch = true;
                break;
            //清除所有
            case R.id.button_clearall:
                if(surfaceview!=null){
                    surfaceview.point_xs = new Vector<>();
                    surfaceview.point_ys = new Vector<>();
                    xs = new Vector<>();
                    ys = new Vector<>();
                    arrayserchtime = new Vector<>();
                    arrayscope = new Vector<>();
//                    arrayangle = new Vector<>();
                    arraygametime = new Vector<>();
                }

                Istouch = false;
                linear_plan_info.setVisibility(View.GONE);
                linear_plan.setVisibility(View.VISIBLE);
                break;
            //规划新路线
            case R.id.button_plan:
                if(Istouch){
                    Contact.showWarntext(context,handler);
                }else {
                    Contact.debugLog("规划新路线 = " + arrayPlanLists.toString());
                    arrayserchtime = new Vector<Float>();
                    arrayscope = new Vector<Float>();
//                    arrayangle = new Vector<Float>();
                    arraygametime = new Vector<Float>();
                    xs = new Vector<Float>();
                    ys = new Vector<Float>();
                    Contact.debugLog("规划新路线 = surfaceview.point_xs" + surfaceview.point_xs.toString());

                    surfaceview.point_xs = new Vector<Float>();
                    surfaceview.point_ys = new Vector<Float>();
                    Contact.debugLog("规划新路线 = surfaceview.point_xs" + surfaceview.point_xs.toString());

                    Contact.debugLog("规划新路线 = " + arrayPlanLists.toString());
                    Istouch = true;
                    linear_plan_info.setVisibility(View.VISIBLE);
                    linear_plan.setVisibility(View.GONE);
                }
                break;
            //删除当前路线
            case R.id.button_remove:
                if(Istouch){
                    Contact.showWarntext(context,handler);
                }else {
                    arrayPlanLists.remove(strings.get((int) plannumber));
                    writeXML();
                    updatekey();
                }

                break;
            //停止执行
            case R.id.button_plan_stop:
//                handler.sendEmptyMessage(3);
//                findViewById(R.id.button_execut).setClickable(true);
//                findViewById(R.id.button_plan_stop).setClickable(false);
                handler.sendEmptyMessage(4);
//                sendCamera();
//                if(Istouch){
//                    Contact.showWarntext(context,handler);
//                }else {

//                }
                break;
            //执行路线
            case R.id.button_execut:
                if(Istouch){
                    Contact.showWarntext(context,handler);
                }else{
                    startPlan();
//                    Contact.showWarn(context,handler);
                }
                break;
            //下一步
            case R.id.button_savenext:
                if(!Istouch){
                    arrayserchtime.add(serchtimenumber);
                    arrayscope.add(scopenumber);
//                    arrayangle.add(anglenumber);
                    arraygametime.add(gametimenumber);
                    xs.add(surfaceview.point_xs.elementAt(arrayserchtime.size()-1));
                    ys.add(surfaceview.point_ys.elementAt(arrayserchtime.size()-1));
                    Istouch = true;
                    Toast.makeText(context,"请选取一个新的目标点",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context,"没有选取一个新的目标点",Toast.LENGTH_SHORT).show();
                }
                break;
            //规划完毕
            case R.id.button_saveall:
                if(!Istouch){
                    arrayserchtime.add(serchtimenumber);
                    arrayscope.add(scopenumber);
//                    arrayangle.add(anglenumber);
                    arraygametime.add(gametimenumber);
                    xs.add(surfaceview.point_xs.elementAt(arrayserchtime.size()-1));
                    ys.add(surfaceview.point_ys.elementAt(arrayserchtime.size()-1));
                }
                if(surfaceview.point_xs.size()> 0 ){
                    arrayhash = new HashMap<>();
                    arrayhash.put("point_xs",xs);
                    arrayhash.put("point_ys",ys);
                    arrayhash.put("arrayserchtime",arrayserchtime);
                    arrayhash.put("arrayscope",arrayscope);
//                    arrayhash.put("arrayangle",arrayangle);
                    arrayhash.put("arraygametime",arraygametime);
                    dialog();
                }else{
                    Toast.makeText(context,"没有规划任何路线",Toast.LENGTH_SHORT).show();
                }
                break;
            //漫游模式
            case R.id.button_cruise:
                Isplan = false;
                Istouch = true;
                setVisible();
                linear_roam.setVisibility(View.VISIBLE);
                linearlayout_map.setVisibility(View.GONE);
                break;
            //路线规划模式
            case R.id.button_pathplan:
                surfaceview.Isplan = true;
                setVisible();
                linear_plan.setVisibility(View.VISIBLE);
                linearlayout_map.setVisibility(View.GONE);
                readXML();
                updatekey();
                Istouch = false;
                Isplan = true;
                break;
            //点选模式
            case R.id.button_pointchooce:
                surfaceview.Isplan = false;
                Istouch = true;
                setVisible();
                linear_point.setVisibility(View.VISIBLE);
                linearlayout_map.setVisibility(View.GONE);
                Isplan = false;
                break;
            case R.id.button_return:
                surfaceview.scale = 1;
                surfaceview.translate_x = 0;
                surfaceview.translate_y = 0;
                break;
            //点选下一步
            case R.id.button_next:
                Istouch = true;
                break;
            //点选移动
            case R.id.button_move:
                Contact.debugLog( "button_move");
                if(point_x.getText()!=null&& !point_x.getText().toString().equals("") && point_y.getText()!=null&& !point_y.getText().equals("")){
                    sendNativePoint(Float.valueOf(point_x.getText().toString().trim()),Float.valueOf(point_y.getText().toString().trim()),0);

                    Map map  = new LinkedHashMap();
                    map.put("point_x",Float.valueOf(point_x.getText().toString().trim()));
                    map.put("point_y",Float.valueOf(point_y.getText().toString().trim()));
                    map.put("angle",0);
                    sendBundle(Contact.Command,Contact.Navigation,map);

                }else{
                    Toast.makeText(context,"请输入正确的坐标",Toast.LENGTH_SHORT).show();
                }
                break;
            //开始漫游
            case R.id.button_roam_start:

                break;
            //停止漫游
            case R.id.button_roam_stop:

                break;
            case R.id.button_plan_back:
                linearlayout_map.setVisibility(View.VISIBLE);
                linear_plan.setVisibility(View.GONE);
                surfaceview.point_xs.removeAllElements();
                surfaceview.point_ys.removeAllElements();
                Isplan = false;
                break;
            case R.id.button_plan_info_back:
                linear_plan.setVisibility(View.VISIBLE);
                linear_plan_info.setVisibility(View.GONE);
                Istouch = false;
                break;
            case R.id.button_point_back:
                Istouch = false;
                linearlayout_map.setVisibility(View.VISIBLE);
                linear_point.setVisibility(View.GONE);
                break;
            case R.id.button_roam_back:
                linearlayout_map.setVisibility(View.VISIBLE);
                linear_roam.setVisibility(View.GONE);
                break;
        }
    }


    private void init(){

        map_Plan = new ArrayList<>();
        map_Plan.add("远");
        map_Plan.add("中");
        map_Plan.add("近");
        adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,map_Plan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scope.setAdapter(adapter);
        scope.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                scopenumber = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        map_Plan = new ArrayList<>();
        map_Plan.add("1分钟");
        map_Plan.add("2分钟");
        map_Plan.add("3分钟");
        adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,map_Plan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serchtime.setAdapter(adapter);
        serchtime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serchtimenumber = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
//        map_Plan = new ArrayList<>();
//        map_Plan.add("30度");
//        map_Plan.add("60度");
//        map_Plan.add("90度");
//        adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,map_Plan);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        angle.setAdapter(adapter);
//        angle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                anglenumber = position;
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });
        map_Plan = new ArrayList<>();
        map_Plan.add("3分钟");
        map_Plan.add("6分钟");
        map_Plan.add("9分钟");
        adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,map_Plan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gametime.setAdapter(adapter);
        gametime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gametimenumber = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setVisible(){
        linear_plan.setVisibility(View.GONE);
        linear_plan_info.setVisibility(View.GONE);
        linear_point.setVisibility(View.GONE);
        linear_roam.setVisibility(View.GONE);
    }

    //底层获取
    private void getUpPoint(float native_x,float native_y){
//        Toast.makeText(context,native_x+"    "+native_y,Toast.LENGTH_SHORT).show();
        if(native_x <= 0 && native_x >= -6 && native_y >= -7.6 && native_y <=2.4){
            surfaceview.bitmap_y = native_x * -100 - 60 +20;
            surfaceview.bitmap_x = native_y * -100 + 300 + 20;
        }
    }
    //设置方向
    private void getAngle(int angle){
        Toast.makeText(context,angle+"",Toast.LENGTH_SHORT).show();
        surfaceview.rote = -angle;
    }
    //发往底层
    private void sendNativePoint(float up_x,float up_y ,int angle){
//        if(up_x >= 20 && up_x <=620 && up_y >= 20 && up_y <= 1020){
//            up_x = (up_x -20) / -100;
//            up_y = (float) (((up_y-20) / 100) - 7.6);
        Map map  = new LinkedHashMap();
        float a = (float) (up_y / -100 + 0.6);
        map.put("point_x",a);
        Contact.debugLog( "x"+a);
        a = (float) (((up_x / -100) + 3.0));
        map.put("point_y",a);
        Contact.debugLog( "y"+a);
        map.put("angle",angle);
        sendBundle(Contact.Command,Contact.Navigation,map);
//        }
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String StringE = intent.getStringExtra("msg");
            if(StringE !=null && !StringE.equals("")){
                pasreJson(StringE);
            }
        }
    }
    public void pasreJson(String string){
        JSONObject object ;
        try {
            object = new JSONObject(string);
            String type = object.getString(Contact.Type);
            String funtion = object.getString(Contact.Function);
            String data = object.getString(Contact.Data);
            if(object !=null){
                if(type.equals(Contact.State)){
                    if(funtion.equals(Contact.Navigation)){
                        JSONObject jsonObject = new JSONObject(data);
                        String flag = jsonObject.getString(Contact.Result);
                        if(flag.equals("success")){
                            Toast.makeText(context,"success",Toast.LENGTH_SHORT).show();
                            handler.sendEmptyMessage(4);
                        }else if(flag.equals("fail")){
                            Toast.makeText(context,"success",Toast.LENGTH_SHORT).show();
                            handler.sendEmptyMessage(4);
                        }else if(flag.equals("navigating")){

                        }
                        getUpPoint(jsonObject.getInt("x"),jsonObject.getInt("y"));
                        getAngle(jsonObject.getInt("angle"));
                    }else if(funtion.equals(Contact.Camera)){
                        JSONObject jsonObject = new JSONObject(data);
                        String str =  jsonObject.getString("result");
                        if(str.equals("body")){
                            IsFind = true;
                            try {
                                resetTimer();
                                ServerSocketUtil.sendDateToClient(string);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else if(str.equals("nobody")){
                        }else if(str.equals("away")){
                            handler.sendEmptyMessage(4);
                        }
                    }
                }else if(type.equals(Contact.Param)){
                    if(funtion.equals(Contact.Navigation)){

                    }
                }else if(type.equals(Contact.Command)){
                    if(funtion.equals("")){

                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendBundle(String type,String function,Map data){
        try {
            Gson gson = new Gson();
            Map map = new LinkedHashMap();
            map.put("type", type);
            map.put("function", function);
            map.put("data", data);
            String s = gson.toJson(map);
            ServerSocketUtil.sendDateToClient(s);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBundle(String type,String function,String data){
        try {
            Gson gson = new Gson();
            Map map = new LinkedHashMap();
            map.put(Contact.Type, type);
            map.put(Contact.Function, function);
            map.put(Contact.Data, data);
            String s = gson.toJson(map);
            ServerSocketUtil.sendDateToClient(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendBundle(String type,String function,Float data){
        try {
            Gson gson = new Gson();
            Map map = new LinkedHashMap();
            map.put(Contact.Type, type);
            map.put(Contact.Function, function);
            map.put(Contact.Data, data);
            String s = gson.toJson(map);
            ServerSocketUtil.sendDateToClient(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //xml写入
    public synchronized void writeXML() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element planLists = document.createElement("planLists");

            Iterator<Map.Entry<String, HashMap<String,Vector<Float>>>> iter = arrayPlanLists.entrySet().iterator();
            while(iter.hasNext()){
                HashMap<String,Vector<Float>> planList = new HashMap<>();
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();

                planList = (HashMap<String, Vector<Float>>) entry.getValue();

                Element keysElement = document.createElement("key");
                keysElement.setAttribute("key",key);

                Vector<Float> point_xs = planList.get("point_xs");
                Vector<Float> point_ys = planList.get("point_ys");
                Vector<Float> arrayserchtime = planList.get("arrayserchtime");
                Vector<Float> arrayscope = planList.get("arrayscope");
//                Vector<Float> arrayangle = planList.get("arrayangle");
                Vector<Float> arraygametime = planList.get("arraygametime");
                Element point_xs_ = document.createElement("point_xs");
                Element point_ys_ = document.createElement("point_ys");
                Element arrayserchtime_ = document.createElement("arrayserchtime");
                Element arrayscope_ = document.createElement("arrayscope");
//                Element arrayangle_ = document.createElement("arrayangle");
                Element arraygametime_ = document.createElement("arraygametime");

                for(int x = 0;x < point_xs.size();x ++){
                    Element item_xs = document.createElement("item_xs");
                    item_xs.setTextContent(point_xs.elementAt(x)+"");
                    Element item_ys = document.createElement("item_ys");
                    item_ys.setTextContent(point_ys.elementAt(x)+"");
                    Element item_serchtime = document.createElement("item_serchtime");
                    item_serchtime.setTextContent(arrayserchtime.elementAt(x)+"");
                    Element item_scope = document.createElement("item_scope");
                    item_scope.setTextContent(arrayscope.elementAt(x)+"");
//                    Element item_angle = document.createElement("item_angle");
//                    item_angle.setTextContent(arrayangle.elementAt(x)+"");
                    Element item_gametime = document.createElement("item_gametime");
                    item_gametime.setTextContent(arraygametime.elementAt(x)+"");
                    point_xs_.appendChild(item_xs);
                    point_ys_.appendChild(item_ys);
                    arrayserchtime_.appendChild(item_serchtime);
                    arrayscope_.appendChild(item_scope);
//                    arrayangle_.appendChild(item_angle);
                    arraygametime_.appendChild(item_gametime);
                }
                keysElement.appendChild(point_xs_);
                keysElement.appendChild(point_ys_);
                keysElement.appendChild(arrayserchtime_);
                keysElement.appendChild(arrayscope_);
//                keysElement.appendChild(arrayangle_);
                keysElement.appendChild(arraygametime_);

                planLists.appendChild(keysElement);
            }

            document.appendChild(planLists);

            TransformerFactory transfactory = TransformerFactory.newInstance();
            Transformer transformer = transfactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");// 设置输出采用的编码方式
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");// 是否自动添加额外的空白
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");// 是否忽略XML声明
            FileOutputStream fos = new FileOutputStream(Contact.filePath);
            Source source = new DOMSource(document);
            Result result = new StreamResult(fos);
            transformer.transform(source, result);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
    //xml读取
    public synchronized  void readXML() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File file = new File(Contact.filePath);
            Document parse = null;
            if (!file.exists()) {
                parse = builder.parse(context.getAssets().open("map.xml"));
            }else{
                parse = builder.parse(file);
            }
            parse.normalize();
            Element root = parse.getDocumentElement();
            NodeList planLists = root.getElementsByTagName("key");
            arrayPlanLists  = new HashMap<>();
            for (int i = 0; i < planLists.getLength(); i++) {
                arrayhash = new HashMap<>();
                Element item = (Element) planLists.item(i);
                String key = item.getAttribute("key");
                NodeList nodes = item.getElementsByTagName("item_xs");
                getFloat(item,"item_xs","point_xs");
                getFloat(item,"item_ys","point_ys");
                getFloat(item,"item_serchtime","arrayserchtime");
                getFloat(item,"item_scope","arrayscope");
//                getFloat(item,"item_angle","arrayangle");
                getFloat(item,"item_gametime","arraygametime");
                arrayPlanLists.put(key,arrayhash);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void getFloat(Element item,String keys,String key){
        NodeList nodes = item.getElementsByTagName(keys);
        Vector<Float> floats=new Vector<Float>();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node node =  nodes.item(j);
            String string = node.getFirstChild().getNodeValue();
            floats.add(Float.valueOf(string));
        }
        arrayhash.put(key,floats);
    }

    //获取路线key名称
    public Vector<String> getKey(){
        Iterator<Map.Entry<String, HashMap<String,Vector<Float>>>> iter = arrayPlanLists.entrySet().iterator();
        Vector<String> strings = new Vector<>();
        while(iter.hasNext()) {
            HashMap<String, Vector<Float>> planList = new HashMap<>();
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            strings.add(key);
        }
        return strings;
    }
    public void updatekey(){
        Istouch = false;
        strings = getKey();
        if(strings !=null && context!=null){
            adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,strings);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            planchooce.setAdapter(adapter);
        }
    }
    private MyDialog dialog ;
    private EditText editText;
    private void dialog() {
        dialog = new MyDialog(context);
        editText = (EditText) dialog.getEditText();
        dialog.setOnPositiveListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrayPlanLists.put(editText.getText().toString().trim(),arrayhash);
                writeXML();
                surfaceview.point_xs.removeAllElements();
                surfaceview.point_ys.removeAllElements();
                arrayserchtime.removeAllElements();
                arrayscope.removeAllElements();
//                arrayangle.removeAllElements();
                arraygametime.removeAllElements();
                linear_plan_info.setVisibility(View.GONE);
                linear_plan.setVisibility(View.VISIBLE);
                readXML();
                updatekey();
                dialog.dismiss();
            }
        });
        dialog.setOnNegativeListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(context,"请继续规划路线",Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
    public void startPlan(){
        thread = null;
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String,Vector<Float>> array = arrayPlanLists.get(strings.get((int) plannumber));
                    Contact.debugLog("array = " + array.toString());
                    xs_tmp = array.get("point_xs");
                    ys_tmp  = array.get("point_ys");
                    arrayserchtime_tmp = array.get("arrayserchtime");
                    arrayscope_tmp = array.get("arrayscope");
//                    arrayangle_tmp = array.get("arrayangle");
                    arraygametime_tmp = array.get("arraygametime");
                    int i = 0;
                    while(i < xs_tmp.size()){
                        Contact.debugLog("thread = " + i);
                        sendNativePoint(xs_tmp.get(i),ys_tmp.get(i),0);
                        synchronized (thread){
                            try {
                                thread.wait();
                                //到达对应地点notify
                                //发送 找人时间以及机器人旋转以及找人范围
                                Contact.debugLog("发送 找人时间以及机器人旋转以及找人范围 = "+i);
                                IsFind = false;
                                resetTimer();
                                if(arrayserchtime_tmp.get(i) == 0){
                                    Contact.debugLog("arrayserchtime_tmp = " + 0);
                                    timer.schedule(task, 1 * 60 * 1000);
                                }else if(arrayserchtime_tmp.get(i) == 1){
                                    Contact.debugLog("arrayserchtime_tmp = " + 1);
                                    timer.schedule(task, 2 * 60 * 1000);
                                }else if(arrayserchtime_tmp.get(i) == 2){
                                    Contact.debugLog("arrayserchtime_tmp = " + 2);
                                    timer.schedule(task, 3 * 60 * 1000);
                                }
                                sendCamera(arrayscope_tmp.get(i));
                                sendBundle(Contact.Command,Contact.Peoplesearch,"");
                                Contact.debugLog("如果有找到人则到达指定位置 = "+i);

                                thread.wait();

                                //如果有找到人则到达指定位置
                                Contact.debugLog("如果有找到人则到达指定位置 = "+i);
                                if(IsFind){
                                    IsFind = false;
                                    Contact.debugLog("互动时间 = "+i);
                                    resetTimer2();
                                    if(arraygametime_tmp.get(i) == 0){
                                        Contact.debugLog("arraygametime_tmp = " + 0);
                                        timer.schedule(task, 1 * 60 * 1000);
                                    }else if(arraygametime_tmp.get(i) == 1){
                                        Contact.debugLog("arraygametime_tmp = " + 1);
                                        timer.schedule(task, 2 * 60 * 1000);
                                    }else if(arraygametime_tmp.get(i) == 2){
                                        Contact.debugLog("arraygametime_tmp = " + 2);
                                        timer.schedule(task, 3 * 60 * 1000);
                                    }
                                    thread.wait();
                                }
                                Contact.debugLog("返回路线位置 = "+xs_tmp.get(i)+ys_tmp.get(i));

                                sendNativePoint(xs_tmp.get(i),ys_tmp.get(i),0);

                                thread.wait();

                                //返回后进行下一个地点
                                i++;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Contact.debugLog("thread = "+i);
                    }
                }
            });
            thread.start();
    }

    public void resetTimer(){
        timer = new Timer();
        task = new TimerTask() {
            public void run () {
                sendBundle(Contact.Command,Contact.StopSearch,"");
                handler.sendEmptyMessage(4);
            }
        };
    }
    public void resetTimer2(){
        timer = new Timer();
        task = new TimerTask() {
            public void run () {
//                sendBundle(Contact.Command,Contact.StopSearch,"");
                handler.sendEmptyMessage(4);
            }
        };
    }
    public void sendCamera(Float scope){
        Intent intent = new Intent();
        if(scope == 0){
            intent.putExtra("msg", "远");
        }else if(scope == 1){
            intent.putExtra("msg", "中");
        }else if(scope == 2){
            intent.putExtra("msg", "近");
        }else{
            intent.putExtra("msg", "关闭");
        }
        intent.setAction("com.jdrd.CursorSDKExample.TD_CAMERA");
        context.sendBroadcast(intent);
    }
}
