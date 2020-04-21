echo "window mode FSMHGWIN"
java -jar target/fsmhg.jar -d test -s 0.04 -o out/win-fsmghwin -w 9000 -ss 100 -e 1 #> out/fsmhgwin.txt

echo "window mode FSMHG"
java -jar target/fsmhg.jar -d test -s 0.04 -o out/win-fsmgh -w 9000 -ss 100 -e 2 #> out/fsmhg.txt

echo "done"
