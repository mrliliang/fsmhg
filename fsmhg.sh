echo "Test support 0.6 for different partition similarity"
echo "sim = 0.9"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.9

echo

echo "sim = 0.8"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.8

echo

echo "sim = 0.7"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.7

echo

echo "sim = 0.6"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.6

echo

echo "sim = 0.5"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.5

echo

echo "sim = 0.4"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.4

echo

echo "sim = 0.3"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.3

echo
echo "sim = 0.2"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.2

echo

echo "sim = 0.1"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_p -m 10 -p -sim 0.1

echo

echo "no partition"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.6 -o out/as_test_result_80_np -m 10

# echo "Test support 3000"
# java -jar target/fsmhg.jar -d test -s 0.3 -o out/test_result_3000

echo "Done!"
