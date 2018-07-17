package com.example.lading.applicationdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.Subscriber;

public class RxBackpressureActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "RxJava";

    private TextView mText;
    private Button mBtn;
    private Button mBtnBackPressure;
    private Button mBtnFilterOperatorSample;
    private Button mBtnFilterOperatorBuffer;
    private Button mBtnOperatorBackpressure;
    private Button mBtnCancal;
    private TextView mEdit;
    private Subscription mSubscription=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout4);
        initView();
    }

    private void initView() {
        mText= (TextView) findViewById(R.id.text1);
        mEdit= (TextView) findViewById(R.id.edit1);
        mBtn= (Button) findViewById(R.id.button);
        mBtnBackPressure= (Button) findViewById(R.id.button_back_pressure);
        mBtnFilterOperatorSample = (Button) findViewById(R.id.button_filter_operator_sample);
        mBtnFilterOperatorBuffer = (Button) findViewById(R.id.button_filter_operator_buffer);
        mBtnOperatorBackpressure = (Button) findViewById(R.id.button_operator_backpressure);
        mBtnCancal= (Button) findViewById(R.id.button_cancal);
        mEdit.setText("定时器，每一秒发送打印一个数字   \n\ninterval(1, TimeUnit.SECONDS)  创建一个每隔一秒发送一次事件的对象");
        mBtn.setOnClickListener(this);
        mBtnBackPressure.setOnClickListener(this);
        mBtnFilterOperatorSample.setOnClickListener(this);
        mBtnFilterOperatorBuffer.setOnClickListener(this);
        mBtnOperatorBackpressure.setOnClickListener(this);
        mText.setOnClickListener(this);
        mEdit.setOnClickListener(this);
        mBtnCancal.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.text1:
                break;
            case R.id.edit1:
                break;
            case R.id.button:
                start();
                break;
            case R.id.button_back_pressure:
                startBackPressure();
                break;
            case R.id.button_filter_operator_sample:
                startFilterOperatorSample();
                break;
            case R.id.button_filter_operator_buffer:
                startFilterOperatorBuffer();
                break;
            case R.id.button_operator_backpressure:
                startOperatorBackpressureDrop();
                break;
            case R.id.button_cancal:
                    //取消订阅
                     if (mSubscription!=null && !mSubscription.isUnsubscribed()){
                         mSubscription.unsubscribe();
                     }
                    break;
        }
    }

    private void start() {
        //interval（）是运行在computation Scheduler线程中的，因此需要转到主线程
        // 被观察者在主线程中，每1ms发送一个事件
        mSubscription = Observable.interval(1, TimeUnit.MILLISECONDS)
                //.subscribeOn(Schedulers.newThread())
                //将观察者的工作放在新线程环境中
                .observeOn(Schedulers.newThread())
                //观察者处理每1000ms才处理一个事件
                .subscribe(new Action1<Long>() {
                    @Override public void call(Long aLong) {
                        //mText.setText(aLong + "");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.w(TAG,"---->"+aLong);
                    }
                });
    }

    Observable observable = Observable.range(1, 100000);
    class MySubscriber<T> extends Subscriber<T> {

        @Override public void onStart() {
            //一定要在onStart中通知被观察者先发送一个事件
            request(1);
        }

        @Override public void onCompleted() {

        }

        @Override public void onError(Throwable e) {

        }

        @Override public void onNext(T t) {
            //处理完毕之后，在通知被观察者发送下一个事件
            // 注意在onNext()方法中，最好最后再调用request()方法.
            Log.w(TAG,"---->"+t);
            request(1);
        }
    }

    private void startBackPressure() {
        observable.observeOn(Schedulers.newThread()).subscribe(new MySubscriber());
    }


    private void startFilterOperatorSample() {
        Observable.interval(1, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                //这个操作符简单理解就是每隔200ms发送离时间点最近那个事件，其他的事件浪费掉
                .sample(200, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.w(TAG,"---->"+aLong);
                    }
                });

    }

    private void startFilterOperatorBuffer() {
        Observable.interval(1, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                //这个操作符简单理解就是把100毫秒内的事件打包成list发送
                .buffer(100, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<List<Long>>() {
                    @Override
                    public void call(List<Long> aLong) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.w(TAG,"---->"+aLong.size());
                    }
                });
    }

    private void startOperatorBackpressureDrop() {
        Observable.interval(1, TimeUnit.MILLISECONDS)
                //将observable发送的事件抛弃掉，直到subscriber再次调用request(n)方法的时候，就发送给它这之后的n个事件
                .onBackpressureDrop()
                .observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onStart() {
                        Log.w(TAG,"start");
                        request(10);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.toString());
                    }

                    @Override
                    public void onNext(Long aLong) {
                        Log.w(TAG,"---->"+aLong);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

}
