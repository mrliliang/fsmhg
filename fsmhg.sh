echo "Test support 5000"
java -jar target/fsmhg.jar -d test -s 0.5 -o out/test_result_5000

echo "Test support 3000"
java -jar target/fsmhg.jar -d test -s 0.3 -o out/test_result_3000

echo "Test support 1000"
java -jar target/fsmhg.jar -d test -s 0.1 -o out/test_result_1000

echo "Test support 600"
java -jar target/fsmhg.jar -d test -s 0.06 -o out/test_result_600

echo "Test support 400"
java -jar target/fsmhg.jar -d test -s 0.04 -o out/test_result_400

echo "Done!"

