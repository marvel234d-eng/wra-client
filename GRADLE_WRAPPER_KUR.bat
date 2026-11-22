@echo off
setlocal
cd /d "%~dp0"
echo Forge 1.20.1 resmi MDK paketinden Gradle wrapper dosyalari aliniyor...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.0/forge-1.20.1-47.4.0-mdk.zip'; $zip=Join-Path $env:TEMP 'forge-1.20.1-mdk.zip'; $dir=Join-Path $env:TEMP 'forge-1.20.1-mdk'; Invoke-WebRequest -Uri $url -OutFile $zip; if(Test-Path $dir){Remove-Item -Recurse -Force $dir}; Expand-Archive -Path $zip -DestinationPath $dir; Copy-Item (Join-Path $dir 'gradle') -Destination '.' -Recurse -Force; Copy-Item (Join-Path $dir 'gradlew*') -Destination '.' -Force; Write-Host 'Gradle wrapper kuruldu.'"
if errorlevel 1 (
 echo Indirme basarisiz. Forge MDK dosyasini resmi Forge sitesinden indirip gradle klasoru ve gradlew.bat dosyasini proje klasorune kopyalayabilirsiniz.
 pause
 exit /b 1
)
echo Tamam. IntelliJ IDEA ile build.gradle dosyasini acin.
pause
