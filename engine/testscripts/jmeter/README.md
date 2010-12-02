This is the JMeter load testing script directory for Sakai 3

Before running test plans, from Sakai root:
 rm -rf sling			# to get a clean deployment
 tools/run_production.sh 	# consistent start version
 testscripts/jmeter/users.pl	# creates 2000 test accounts 

To run a selection of test plans:
 run.sh
This runs two of the KERN issue test plans and outputs to the results directory. 
The Test Plans target localhost:8080.  Edit the HTTPSampler.domain and HTTPSampler.port values in the .jmx file to your test server values if necessary.

The 'run' scripts pull down JMeter v 2.4 if it is not presently in this directory.

Test results are written to the 'results' directory. If a results file for the current test exists, new results are appended. 
