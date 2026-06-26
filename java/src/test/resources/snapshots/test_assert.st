PROGRAM P
    VAR
        A : INT := 42;
        B : INT := 0;
        C : INT := 0;
    END_VAR

    PRINT("=== ASSERT Test ===");

    ASSERT(A = 42);
    ASSERT(A <> 0);
    ASSERT(B = 0);
    ASSERT(A + B = A);
    ASSERT(A = 42);
    ASSERT(A > B);
    ASSERT(10 > 5);
    ASSERT(B = 0);

    C := A + 10;
    ASSERT(C = 52);

    PRINT("Done");
END_PROGRAM