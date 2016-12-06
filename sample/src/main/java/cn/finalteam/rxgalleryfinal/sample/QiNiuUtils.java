package cn.finalteam.rxgalleryfinal.sample;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import org.json.JSONObject;

import rx.AsyncEmitter;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by guozhong on 16/12/6.
 */
public class QiNiuUtils {
    private volatile static QiNiuUtils singleton;
    private UploadManager uploadManager;

    private QiNiuUtils() {
        uploadManager = new UploadManager();
    }

    public static QiNiuUtils getSingleton() {
        if (singleton == null) {
            synchronized (QiNiuUtils.class) {
                if (singleton == null) {
                    singleton = new QiNiuUtils();
                }
            }
        }
        return singleton;
    }


    public void upload(String path, String key, String token) {

        uploadManager.put(path, key, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo info, JSONObject res) {
                        //res包含hash、key等信息，具体字段取决于上传策略的设置
                        Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                    }
                }, null);
    }

    public Observable<ResponseInfo> rxUpload(String path, String key, String token) {
        return Observable.fromAsync(new Action1<AsyncEmitter<ResponseInfo>>() {
            @Override
            public void call(AsyncEmitter<ResponseInfo> responseInfoAsyncEmitter) {
                UploadManager uploadManager = new UploadManager();
                uploadManager.put(path, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                                if (info.statusCode == 200) {
                                    responseInfoAsyncEmitter.onNext(info);
                                    responseInfoAsyncEmitter.onCompleted();
                                } else {
                                    responseInfoAsyncEmitter.onError(new Throwable(info.toString()));
                                }
                            }
                        }, null);
            }
        }, AsyncEmitter.BackpressureMode.BUFFER);
    }

    public Observable<SensorEvent> observeSensorChanged(final SensorManager sensorManager, final Sensor sensor, final int samplingPeriodUs) {
        return Observable.fromAsync(new Action1<AsyncEmitter<SensorEvent>>() {
            @Override
            public void call(final AsyncEmitter<SensorEvent> sensorEventAsyncEmitter) {
                final SensorEventListener sensorListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        sensorEventAsyncEmitter.onNext(sensorEvent);
                    }

                    @Override
                    public void onAccuracyChanged(Sensor originSensor, int i) {
                        // ignored for this example
                    }
                };
                // (1) - unregistering listener when unsubscribed
                sensorEventAsyncEmitter.setCancellation(new AsyncEmitter.Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        sensorManager.unregisterListener(sensorListener, sensor);
                    }
                });
                sensorManager.registerListener(sensorListener, sensor, samplingPeriodUs);

            }
            // (4) - specifying the backpressure strategy to use
        }, AsyncEmitter.BackpressureMode.BUFFER);
    }


}
