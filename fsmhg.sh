echo "Test support 0.8 partition"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.8 -o out/as_test_result_80_p -m 10 -p -sim 0.4
# java -Xms56320M -Xmx56320M -XX:PermSize=563200M -XX:MaxPermSize=563200M -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.5 -o out/as_test_result_80_p -m 10 -p -sim 0.4

echo

echo "Test support 0.8 no partition"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.8 -o out/as_test_result_80_np -m 10

# echo "Test support 3000"
# java -jar target/fsmhg.jar -d test -s 0.3 -o out/test_result_3000

# echo "Test support 1000"
# java -jar target/fsmhg.jar -d test -s 0.1 -o out/test_result_1000

# echo "Test support 600"
# java -jar target/fsmhg.jar -d test -s 0.06 -o out/test_result_600

# echo "Test support 400"
# java -jar target/fsmhg.jar -d test -s 0.04 -o out/test_result_400

echo "Done!"
