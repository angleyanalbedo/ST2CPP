(* traffic_light.st — 十字路口交通灯控制 *)

PROGRAM TRAFFIC_LIGHT
    VAR
        phase       : INT := 1;
        tick_count  : DINT := 0;
        green_ticks : DINT := 3000;
        yellow_ticks: DINT := 500;
        clear_ticks : DINT := 200;
        ns_r        : BOOL := TRUE;
        ns_y        : BOOL := FALSE;
        ns_g        : BOOL := FALSE;
        ew_r        : BOOL := TRUE;
        ew_y        : BOOL := FALSE;
        ew_g        : BOOL := FALSE;
    END_VAR

    tick_count := tick_count + 1;

    IF phase = 1 AND tick_count >= green_ticks THEN
        tick_count := 0;
        phase := 2;
    ELSIF phase = 2 AND tick_count >= yellow_ticks THEN
        tick_count := 0;
        phase := 3;
    ELSIF phase = 3 AND tick_count >= clear_ticks THEN
        tick_count := 0;
        phase := 4;
    ELSIF phase = 4 AND tick_count >= green_ticks THEN
        tick_count := 0;
        phase := 5;
    ELSIF phase = 5 AND tick_count >= yellow_ticks THEN
        tick_count := 0;
        phase := 6;
    ELSIF phase = 6 AND tick_count >= clear_ticks THEN
        tick_count := 0;
        phase := 1;
    END_IF;

    ns_r := FALSE;
    ns_y := FALSE;
    ns_g := FALSE;
    ew_r := FALSE;
    ew_y := FALSE;
    ew_g := FALSE;

    IF phase = 1 THEN
        ns_g := TRUE;
        ew_r := TRUE;
    ELSIF phase = 2 THEN
        ns_y := TRUE;
        ew_r := TRUE;
    ELSIF phase = 3 THEN
        ns_r := TRUE;
        ew_r := TRUE;
    ELSIF phase = 4 THEN
        ns_r := TRUE;
        ew_g := TRUE;
    ELSIF phase = 5 THEN
        ns_r := TRUE;
        ew_y := TRUE;
    ELSE
        ns_r := TRUE;
        ew_r := TRUE;
    END_IF;

END_PROGRAM
