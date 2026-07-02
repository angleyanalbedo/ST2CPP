PROGRAM MOTOR_CTRL
  VAR_INPUT
    StartBtn AT %IX0.0 : BOOL;
    StopBtn AT %IX0.1 : BOOL;
    SpeedSensor AT %IW0 : INT;
  END_VAR
  VAR_OUTPUT
    MotorRun AT %QX0.0 : BOOL;
    SpeedOut AT %QW0 : INT;
  END_VAR
  VAR
    TargetSpeed : INT := 1000;
    CurrentSpeed : INT;
    RunLatch : BOOL;
  END_VAR
  VAR RETAIN
    TotalRunTime : DINT;
    ErrorCount : INT;
  END_VAR

  IF StartBtn AND NOT StopBtn THEN
    RunLatch := TRUE;
  END_IF;
  IF StopBtn THEN
    RunLatch := FALSE;
  END_IF;

  MotorRun := RunLatch;

  IF MotorRun THEN
    CurrentSpeed := SpeedSensor;
    SpeedOut := TargetSpeed;
    TotalRunTime := TotalRunTime + 1;
  ELSE
    SpeedOut := 0;
  END_IF;

  IF CurrentSpeed > TargetSpeed * 2 THEN
    ErrorCount := ErrorCount + 1;
  END_IF;
END_PROGRAM
