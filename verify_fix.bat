@echo off
echo === PlayerRenderer Fix Verification ===

REM Check if PlayerRenderer.java contains our fix
findstr /C:"Suppress vanilla player model when a Figura avatar is active" "src\net\minecraft\client\renderer\entity\PlayerRenderer.java"
if %errorlevel% equ 0 (
    echo [OK] PlayerRenderer contains Figura avatar suppression logic
) else (
    echo [FAIL] PlayerRenderer missing Figura avatar suppression logic
)

findstr /C:"Class.forName(\"xd.harm.utils.figura.FiguraWear\")" "src\net\minecraft\client\renderer\entity\PlayerRenderer.java"
if %errorlevel% equ 0 (
    echo [OK] PlayerRenderer contains FiguraWear class reference
) else (
    echo [FAIL] PlayerRenderer missing FiguraWear class reference
)

findstr /C:"if (cur != null)" "src\net\minecraft\client\renderer\entity\PlayerRenderer.java"
if %errorlevel% equ 0 (
    echo [OK] PlayerRenderer contains avatar null check
) else (
    echo [FAIL] PlayerRenderer missing avatar null check
)

findstr /C:"return;" "src\net\minecraft\client\renderer\entity\PlayerRenderer.java"
if %errorlevel% equ 0 (
    echo [OK] PlayerRenderer contains early return to suppress vanilla render
) else (
    echo [FAIL] PlayerRenderer missing early return
)

echo.
echo === FiguraWear Verification ===

findstr /C:"ModelPart" "src\xd\harm\utils\figura\FiguraWear.java"
if %errorlevel% equ 0 (
    echo [OK] FiguraWear contains ModelPart layer disabling logic
) else (
    echo [FAIL] FiguraWear missing ModelPart layer disabling logic
)

findstr /C:"ReentrantLock" "src\xd\harm\utils\figura\FiguraWear.java"
if %errorlevel% equ 0 (
    echo [OK] FiguraWear contains ReentrantLock synchronization
) else (
    echo [FAIL] FiguraWear missing ReentrantLock synchronization
)

echo.
echo === FiguraAvatarInstaller Verification ===

findstr /C:"figura_avatars" "src\xd\harm\utils\figura\FiguraAvatarInstaller.java"
if %errorlevel% equ 0 (
    echo [OK] FiguraAvatarInstaller contains local folder fallback
) else (
    echo [FAIL] FiguraAvatarInstaller missing local folder fallback
)

echo.
echo === Test Complete ===
pause