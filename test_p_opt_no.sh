echo "Test support 0.05 without min code optimization" > /home/liliang/fsmhg_test/sta_sup5_p_sim90_opt_no.log
echo "No optimization" >> /home/liliang/fsmhg_test/sta_sup5_p_sim90_opt_no.log
java -jar target/fsmhg.jar -d /home/liliang/data/as-733-snapshots -s 0.05 -o /home/liliang/fsmhg_test/result_sup5_p_sim90_opt_no.txt -p -sim 0.9 -opt NO >> /home/liliang/fsmhg_test/sta_sup5_p_sim90_opt_no.log
