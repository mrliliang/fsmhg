echo "Test support 0.05 for min code optimization" > /home/liliang/fsmhg_test/sta_sup5_p_sim90_opt_yes.log
echo "Use optimization" >> /home/liliang/fsmhg_test/sta_sup5_p_sim90_opt_yes.log
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.05 -o /home/liliang/fsmhg_test/result_sup5_p_sim90_opt_yes.txt -p -sim 0.9 -opt YES >> /home/liliang/fsmhg_test/sta_sup5_p_sim90_opt_yes.log
