package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private FakeImageService fakeImageService;
    @Mock
    private StatusListener statusListener;
    @BeforeEach
    public void setup(){
        this.securityService = new SecurityService(securityRepository, fakeImageService);
        this.sensor = new Sensor("doorSensor", SensorType.DOOR);
    }
    private Set<Sensor> getSensors(boolean active, int count){
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0 ; i<count; i++){
            Sensor sensorObject = new Sensor("motionSensor", SensorType.MOTION);
            sensorObject.setActive(active);
            sensors.add(sensorObject);
        }
        return sensors;
    }
    //#1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void givenAlarmArmed_WhenSensorActivated_ThenSystemInPendingAlarm(ArmingStatus armingStatus){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.PENDING_ALARM);

    }

//#2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void givenAlarmArmedWithPendingAlarm_WhenSensorActivated_ThenSetOffAlarm(ArmingStatus armingStatus){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);

    }

    //@ParameterizedTest
    //@EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    //#3
    @Test
    void givenPendingAlarm_WhenSensorsInActivate_ThenNoAlarm(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        //when(securityService.getArmingStatus()).thenReturn(armingStatus);
        Set<Sensor> sensorSet = getSensors(false, 4);
        Sensor sensor1 = sensorSet.stream().findFirst().get();
        sensor1.setActive(true);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        securityService.changeSensorActivationStatus(sensor1, false);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);

    }
    //#4
    @Test
    void givenAlarmActive_AnyChangeInAlarmState_DoesNotAffectAlarmState() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(securityRepository, atMostOnce()).updateSensor(captor.capture());
        //assertEquals(captor.getValue(), AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        sensor.setActive(true);
        //when(securityRepository.getSensors()).thenReturn(sensorSet);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, atMost(2)).updateSensor(captor.capture());
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
//#5
    @Test
    void givenSensorActive_SystemInPendingState_changeSensorToAlarmState() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }
//#6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "ALARM", "PENDING_ALARM"})
    void givenSensorDeactivedWhileAlreadyInactive_MakeNoChangesToAlarmState(AlarmStatus alarmStatus) {
        sensor.setActive(false);
        //when(securityService.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }
    //#7
    @Test
    void givenCameraImageContainsCat_WhileSystemArmedHome_PutSystemIntoAlarmStatus(){
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }
    //#8
    @Test
    void givenCameraImageDoesNotContainsCat_WhileSensorsNotActive_PutSystemIntoNoAlarmStatus(){
        //when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Set<Sensor> sensorSet = getSensors(false, 4);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }
    //#9
    @Test
    void givenCameraImageDoesNotContainsCat_WhileSensorsActive_SystemNoAlarmStatusUpdate(){
        //when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Set<Sensor> sensorSet = getSensors(true, 4);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, never()).setAlarmStatus(captor.capture());
        //assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }
    //10
    @Test
    void givenSystemDisarmed_SetSystemStatusNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }
    //#11
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void givenSystemArmed_ResetAllSensorsToInactive(ArmingStatus armingStatus) {
        Set<Sensor> sensorSet = getSensors(true, 4);
        when(securityService.getSensors()).thenReturn(sensorSet);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(sensor1 -> {assertFalse(sensor1.getActive());});
    }
    //#12
    @Test
    void givenSystemArmedHome_WhileCameraShowsCat_PutSystemIntoAlarmStatus(){
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }
    @Test
    void addAndRemoveSensor(){
        Sensor sensor = new Sensor();
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
        assertEquals(securityService.getSensors().size(), 0);
    }
    @Test
    void addAndRemoveStatusListener(){
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, never()).setAlarmStatus(captor.capture());
    }

    @Test
    void ifSystemDisarmedAndSensorsActivatedNoChanges(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        //when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }
}
