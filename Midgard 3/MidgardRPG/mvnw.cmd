@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://
@echo off

@REM Set the current directory to the location of this script
set "MVNW_REPOURL=https://repo.maven.apache.org/maven2"
set "MVNW_VERBOSE=false"

@REM Determine the Java command to use to start the JVM
if not "%JAVA_HOME%"=="" goto javaHomeSet
set "JAVACMD=java"
goto javaHomeDone

:javaHomeSet
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:javaHomeDone

@REM Locate the project base dir
set "MAVEN_PROJECTBASEDIR=%~dp0"

@REM Find the maven-wrapper.jar or download it
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

@REM Download maven-wrapper.jar if it doesn't exist
if exist "%WRAPPER_JAR%" goto wrapperExists

@REM Download the wrapper jar
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

echo Downloading Maven wrapper...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"

if not exist "%WRAPPER_JAR%" (
    echo ERROR: Failed to download Maven wrapper jar.
    echo Falling back to direct Maven download...
    goto directDownload
)

:wrapperExists
"%JAVACMD%" ^
  -jar "%WRAPPER_JAR%" %*
if %ERRORLEVEL% neq 0 goto directDownload
goto end

:directDownload
@REM If wrapper JAR download fails, try downloading Maven directly
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

echo Downloading Apache Maven 3.9.9...
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
set "MAVEN_ZIP=%TEMP%\apache-maven-3.9.9-bin.zip"

powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%MAVEN_URL%', '%MAVEN_ZIP%')"

if not exist "%MAVEN_ZIP%" (
    echo ERROR: Failed to download Maven.
    echo Please install Maven manually: https://maven.apache.org/download.cgi
    exit /b 1
)

echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
del "%MAVEN_ZIP%" 2>nul

:runMaven
"%MAVEN_HOME%\bin\mvn.cmd" %*

:end
