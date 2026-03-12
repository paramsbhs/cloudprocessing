@REM Maven Wrapper for Windows
@REM Downloads Maven if not already cached, then delegates all arguments to it.

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET PN=%__MVNW_ARG0_NAME__%
@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="wrapperUrl" SET DOWNLOAD_URL=%%B
)

@SET JAVA_HOME_JAVA21=C:\Program Files\Java\jdk-21
@IF EXIST "%JAVA_HOME_JAVA21%\bin\java.exe" SET JAVA_HOME=%JAVA_HOME_JAVA21%

@"%JAVA_HOME%\bin\java" -cp %WRAPPER_JAR% %WRAPPER_LAUNCHER% %MAVEN_PROJECTBASEDIR% %*
