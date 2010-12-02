#!/usr/bin/ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb';

require 'rubygems'
require 'json'
require 'logger';
require 'net/http';
require 'sling/test';

class TC_KERN1105_Test < Test::Unit::TestCase

  # connectivity information
  SERVER = 'localhost';
  PORT = 8080;
  PATH = '/dev/';
  USER = 'admin';
  PASS = 'admin';

  # processing information
  MAX_RUNS = 5;

  def setup
    super;
    @log = Logger.new(STDOUT);
  end
  
  def teardown
    super;
  end
  
  def test_kern1105
    # make a call to the server to have the cookie set
    res = Net::HTTP.get_response(SERVER, PATH, PORT);
    assert_not_nil(res);
    assert_equal("200", res.code, "HTTP response should be 200.");
    
  	cookie_header = res['Set-Cookie'];
  	assert_not_nil(cookie_header, "Should have a SAKAI-TRACKING cookie.");
  	eq_pos = cookie_header.index '=';
  	assert(eq_pos > 0, "Equal sign position should be greater than 0.");
  	sc_pos = cookie_header.index ';';
  	assert(sc_pos > 0 && sc_pos > eq_pos, "Semicolon position should be greater than 0 and greater than eq_pos.");
  	cookie_val = cookie_header[eq_pos + 1..sc_pos - 1];
  	assert_not_nil(cookie_val, "Should have a SAKAI-TRACKING cookie value.");
  	
  	# make another call to ensure we get a different cookie value
  	res = Net::HTTP.get_response(SERVER, PATH, PORT);
    assert_not_nil(res);
    assert_equal("200", res.code, "HTTP response should be 200.");
    assert_not_equal(cookie_header, res['Set-Cookie'], "");

  	count = 0;
  	lastJson = nil;
  	while count < MAX_RUNS
  		Net::HTTP.start(SERVER, PORT) do |http|
  		  # Make a HTTP HEAD request with previous SAKAI-TRACKING cookie
  			headers = {'Cookie' => cookie_header};
  			req = Net::HTTP::Head.new(PATH, headers);
        req.basic_auth(USER, PASS);
  			res = http.request(req);
        assert_not_nil(res);
        assert_equal("200", res.code, "HTTP response should be 200.");
        sleep(1);
        # HTTP GET cluster user json
				req = Net::HTTP::Get.new('/var/cluster/user.cookie.json?c=' + cookie_val, headers);
        req.basic_auth(USER, PASS);
				res = http.request(req);
        assert_not_nil(res);
        assert_equal("200", res.code, "HTTP response should be 200.");
        assert_not_nil(res.body);
        json = JSON.parse(res.body);
        assert_not_nil(json);
        if(lastJson != nil)
          assert_equal(lastJson, json, "Last json should equal current json");
        end
        lastJson = json;
        assert_not_nil(json["server"]);
        assert_not_nil(json["user"]);
        assert_not_nil(json["user"]["lastUpdate"]);
        assert_not_nil(json["user"]["homeServer"]);
        assert_not_nil(json["user"]["id"]);
        assert_not_nil(json["user"]["principal"]);
        assert_not_nil(json["user"]["properties"]);
        assert_not_nil(json["user"]["properties"]["firstName"]);
        assert_not_nil(json["user"]["properties"]["lastName"]);
        assert_not_nil(json["user"]["properties"]["email"]);
        assert_not_nil(json["user"]["properties"]["path"]);
        assert_not_nil(json["user"]["declaredMembership"]);
        assert_not_nil(json["user"]["membership"]);
				count += 1;
  		end
  	end
  end
  

end
