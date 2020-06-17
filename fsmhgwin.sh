echo "window mode FSMHGWIN"
java -jar target/fsmhg.jar -d test -s 0.05 -o out/win-fsmghwin -w 9000 -ss 200 -e 1 #> out/fsmhgwin.txt
echo

echo "window mode FSMHG"
java -jar target/fsmhg.jar -d test -s 0.05 -o out/win-fsmgh -w 9000 -ss 200 -e 2 #> out/fsmhg.txt
echo

java -jar target/verify.jar out/win-fsmgh out/win-fsmghwin
echo "done"
