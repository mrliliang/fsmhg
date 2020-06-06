echo "Test support 0.8 for min code optimization"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots-connected -s 0.5 -o out/as_test_p -m 10 -p -sim 0.9

# echo

# echo "No optimization"
# java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.5 -o out/as_test_p_opt_no -p -sim 0.9 -opt NO

echo "Done!"
