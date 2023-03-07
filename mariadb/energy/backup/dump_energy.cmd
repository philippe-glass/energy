SET dbname=energy1
SET dump_file=dump_%dbname%_%date:~6,4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql

echo dump_file=%dump_file%

mysqldump energy1 --user=learning_agent --password=sql2537 >> %dump_file%
pause


