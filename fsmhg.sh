echo "Test support 0.5 for min code optimization"
echo "Use optimization"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.5 -o out/as_test_p_opt_yes -p -sim 0.9 -opt YES

echo

echo "No optimization"
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.5 -o out/as_test_p_opt_no -p -sim 0.9 -opt NO

echo "Done!"
