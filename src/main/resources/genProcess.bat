REM -------------------------------------------------------
REM Explanation
REM -------------------------------------------------------
REM Generates all files required to parse a module's xml
REM file. Uses the khiProcess.xsd model file to generate
REM Java code automatically.
REM Execute this file to update the code whenever there is
REM a Change to the .xsd
REM Note: Update the paths according to your local folders.
"C:\Program Files\Java\jdk1.8.0_45\bin\xjc.exe" -d result -p test.beetlekhi C:\Users\Marmotte\workspace\BeetleKhiCodeGen\resources\xsd\khiProcess.xsd 