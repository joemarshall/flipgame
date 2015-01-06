package com.joemarshall.flipgame;import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import processing.core.PApplet;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorReader implements SensorEventListener {

         private SensorManager m_SensorManager = null;

        /** The parent. */
        private PApplet m_Parent;
        public Object m_Callbackdelegate;

      private Method m_EventMethod;
      
      public SensorReader(PApplet parent)
      {
        m_Parent=parent;
        m_SensorManager=(SensorManager)parent.getSystemService(Context.SENSOR_SERVICE);
        findEventMethod();
        
         Sensor s = m_SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
         if (s != null)
         {
            m_SensorManager.registerListener(this, s,SensorManager.SENSOR_DELAY_FASTEST);
         }
         s = m_SensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
         if (s != null)
         {
            m_SensorManager.registerListener(this, s,SensorManager.SENSOR_DELAY_FASTEST);
         }
         s = m_SensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
         if (s != null)
         {
            m_SensorManager.registerListener(this, s,SensorManager.SENSOR_DELAY_FASTEST);
         }        
         s = m_SensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
         if (s != null)
         {
            m_SensorManager.registerListener(this, s,SensorManager.SENSOR_DELAY_FASTEST);
         }        
         s = m_SensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
         if (s != null)
         {
            m_SensorManager.registerListener(this, s,SensorManager.SENSOR_DELAY_FASTEST);
         }
      }
      
      private void findEventMethod()
      {
        try 
        {
          m_EventMethod = m_Parent.getClass().getMethod("onSensorEvent",
                                        new Class[] { int.class,float.class, float.class, float.class,
                                                        long.class, int.class });
        } catch (NoSuchMethodException e) 
        {
          PApplet.println("No onAccelerometerEvent method found");
        }                                        
      }
      
      float []rotMatrix=new float[16];
      float []orientationVec=new float[3];
      public void onSensorChanged(SensorEvent arg0) 
      {
        try
        {
          if(arg0.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR)
          {
            SensorManager.getRotationMatrixFromVector(rotMatrix,arg0.values);
            SensorManager.getOrientation(rotMatrix,orientationVec);
            m_EventMethod.invoke(m_Parent,new Object[] { arg0.sensor.getType(),orientationVec[0], orientationVec[1],orientationVec[2], arg0.timestamp,arg0.accuracy});
          }else
          {
            m_EventMethod.invoke(m_Parent,new Object[] { arg0.sensor.getType(),arg0.values[0], arg0.values[1],arg0.values[2], arg0.timestamp,arg0.accuracy});
          }
        }catch(NullPointerException e)
        {
          PApplet.println(e.toString());
        }catch(IllegalAccessException e)
        {
          PApplet.println(e.toString());
        }catch(IllegalArgumentException e)
        {
          PApplet.println(e.toString());
        }catch(InvocationTargetException e)
        {
          PApplet.println(e.toString());
        }
      }
 
   public void onAccuracyChanged(Sensor sensor, int accuracy) 
   {
   }
   
   public void stop()
   {
     m_SensorManager.unregisterListener(this);
   }

      
}
