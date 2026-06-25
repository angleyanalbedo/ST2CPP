FUNCTION UDINT_TO_TIME2:TIME

	VAR_INPUT
		IN :	UDINT;
	END_VAR


	UDINT_TO_TIME2 := DINT_TO_TIME(UDINT_TO_DINT(IN));

END_FUNCTION

FUNCTION_BLOCK T_PLC_MS_block
	VAR_OUTPUT
		T_PLC_MS :	UDINT;
	END_VAR

	VAR_EXTERNAL
		PLC_TICKS_PER_SEC :	INT;
		PLC_SYS_TICK_CNT :	DINT;
	END_VAR

	VAR
		debug :	BOOL;
		N :	INT;
		Offset :	UDINT;
		temp :	DWORD := DWORD#1;
		mode :	BOOL;
		faktor :	UDINT;
		init :	BOOL;
		v_plc_ticks_per_sec :	UDINT;
		base :	UDINT := UDINT#1000;
	END_VAR


	IF init = FALSE THEN
		v_plc_ticks_per_sec := INT_TO_UDINT(PLC_TICKS_PER_SEC);
		IF v_plc_ticks_per_sec = UDINT#1024 THEN
			faktor := UDINT#1;
			mode := FALSE;
		ELSIF v_plc_ticks_per_sec > UDINT#0 THEN
			IF v_plc_ticks_per_sec <= base THEN
				faktor := base / v_plc_ticks_per_sec;
				mode := FALSE;
			ELSE
				faktor := v_plc_ticks_per_sec / base;
				mode := TRUE;
			END_IF;
		ELSE
			faktor := UDINT#1;
		END_IF;
		init := TRUE;
	END_IF;

	IF mode THEN
		T_PLC_MS := DINT_TO_UDINT(PLC_SYS_TICK_CNT) / faktor;
	ELSE
		T_PLC_MS := DINT_TO_UDINT(PLC_SYS_TICK_CNT) * faktor;
	END_IF;

	IF debug THEN
		T_PLC_MS := (DWORD_TO_UDINT(SHL(UDINT_TO_DWORD(T_PLC_MS),N) OR SHL(temp,N)) - UDINT#1) + Offset;
	END_IF;

END_FUNCTION_BLOCK

FUNCTION_BLOCK SEQUENCE_4_Block

	VAR_INPUT
		IN0 :	BOOL := TRUE;
		IN1 :	BOOL := TRUE;
		IN2 :	BOOL := TRUE;
		IN3 :	BOOL := TRUE;
		START :	BOOL;
		RST :	BOOL;
		WAIT0 :	TIME;
		DELAY0 :	TIME;
		WAIT1 :	TIME;
		DELAY1 :	TIME;
		WAIT2 :	TIME;
		DELAY2 :	TIME;
		WAIT3 :	TIME;
		DELAY3 :	TIME;
		STOP_ON_ERROR :	BOOL;
	END_VAR

	VAR_OUTPUT
		Q0 :	BOOL;
		Q1 :	BOOL;
		Q2 :	BOOL;
		Q3 :	BOOL;
		QX :	BOOL;
		RUN :	BOOL;
		_STEP :	INT := -1;
		STATUS :	BYTE;
	END_VAR

	VAR
		last :	TIME;
		edge :	BOOL;
		tx :	TIME;
		init :	BOOL;
		T_PLC_MS :	T_PLC_MS_Block;
	END_VAR

	T_PLC_MS();
	tx:= UDINT_TO_TIME(T_PLC_MS.T_PLC_MS);

	IF NOT init THEN
		last := tx;
		init := TRUE;
		status := BYTE#110;
	END_IF;

	IF rst THEN
		_step := -1;
		Q0 := FALSE;
		Q1 := FALSE;
		Q2 := FALSE;
		Q3 := FALSE;
		status := BYTE#110;
		run := FALSE;

	ELSIF start AND NOT edge THEN
		_step := 0;
		last := tx;
		status := BYTE#111;
		Q0 := FALSE;
		Q1 := FALSE;
		Q2 := FALSE;
		Q3 := FALSE;
		run := TRUE;
	END_IF;
	edge := start;

	IF status > BYTE#0 AND status < BYTE#100 AND stop_on_error THEN RETURN; END_IF;

	IF run AND _step = 0 THEN
		IF NOT q0 AND in0 AND tx - last <= wait0 THEN
			Q0 := TRUE;
			last := tx;
		ELSIF NOT q0 AND tx - last > wait0 THEN
			status := BYTE#1;
			run := FALSE;
		ELSIF q0 AND tx - last >= delay0 THEN
			_step := 1;
			last := tx;
		END_IF;
	END_IF;
	IF run AND _step = 1 THEN
		IF NOT q1 AND in1 AND tx - last <= wait1 THEN
			Q0 := FALSE;
			Q1 := TRUE;
			last := tx;
		ELSIF NOT q1 AND Tx - last > wait1 THEN
			status := BYTE#2;
			q0 := FALSE;
			run := FALSE;
		ELSIF q1 AND tx - last >= delay1 THEN
			_step := 2;
			last := tx;
		END_IF;
	END_IF;
	IF run AND _step = 2 THEN
		IF NOT q2 AND in2 AND tx - last <= wait2 THEN
			Q1 := FALSE;
			Q2 := TRUE;
			last := tx;
		ELSIF NOT q2 AND Tx - last > wait2 THEN
			status := BYTE#3;
			q1 := FALSE;
			run := FALSE;
		ELSIF q2 AND tx - last >= delay2 THEN
			_step := 3;
			last := tx;
		END_IF;
	END_IF;
	IF run AND _step = 3 THEN
		IF NOT q3 AND in3 AND tx - last <= wait3 THEN
			Q2 := FALSE;
			Q3 := TRUE;
			last := tx;
		ELSIF NOT q3 AND Tx - last > wait3 THEN
			status := BYTE#4;
			q2 := FALSE;
			run := FALSE;
		ELSIF q3 AND tx - last >= delay3 THEN
			_step := -1;
			q3 := FALSE;
			run := FALSE;
			status := BYTE#110;
		END_IF;
	END_IF;
	QX := q0 OR q1 OR q2 OR q3;

END_FUNCTION_BLOCK