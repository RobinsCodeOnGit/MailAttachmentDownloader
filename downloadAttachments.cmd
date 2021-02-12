@echo off
java -jar mailAttachmentSaver.jar -c %~dp0config.properties -pw "Password" -u "example@mail.de" >> mailAttachmentSaver.log