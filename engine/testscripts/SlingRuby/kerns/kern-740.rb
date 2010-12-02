#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingSearch
include SlingUsers

class TC_Kern740Test < Test::Unit::TestCase
  include SlingTest
  
  #
  # Test changing the admin password using the form auth mechanism.
  #
  
  def teardown 
    # Reset the password
    @s.trustedauth = false
    admin2 = User.new("admin","2admin2")
	@s.switch_user(admin2)
	@log.info("401 is Ok")
	@s.execute_get(@s.url_for("/var/cluster/user.json?performing_teardown"))
	@log.info("401 is Ok")
	admin2.change_password(@s,"admin")
	super
  end
  
  def test_change_password_basicAuth
    m = Time.now.to_i.to_s
    @s.trustedauth = false
	@s.execute_get(@s.url_for("/var/cluster/user.json?Starting_Basic_AuthTest"))
	@log.info("Changing Admin Password with Basic Auth")
	runChangePassword("c")
	@log.info("Done Changing Admin Password with Basic Auth")
	@s.execute_get(@s.url_for("/var/cluster/user.json?Done_Basic_AuthTest"))
  end

  def test_change_password_basicAuthNewUser
    m = Time.now.to_i.to_s
    @s.trustedauth = false
	
	u = "user"+m
	testUser = create_user(u)
	
	@s.switch_user(testUser)
	
	# Can this new user update content in their own space.
	res = @s.execute_get(@s.url_for("/system/me"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	@log.debug(res.body)
	assert_not_nil(props["user"],"system me request failed, expected to find a user object")
	assert_equal(testUser.name, props["user"]["userid"],"Authentication failed, didnt get expected user")
	homeFolderTestFile = "/~#{testUser.name}/testarea"+m
	
	res = @s.execute_post(@s.url_for(homeFolderTestFile),"testprop" => "testvalue",  "jcr:mixinTypes" => "mix:lastModified" )
	assert_equal("201",res.code, res.body)
	res = @s.execute_get(@s.url_for(homeFolderTestFile+".json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	# check the node really was last modified by the correct user.
	assert_equal(testUser.name, props["jcr:lastModifiedBy"])

	
	
	res = testUser.change_password(@s,"testpass2")
	assert_equal("200",res.code,res.body)

	testUser2 = User.new(testUser.name,"testpass2")
	
	@s.switch_user(testUser2)
	res = testUser2.change_password(@s,"testpass")
	assert_equal("200",res.code,res.body)
	
	@s.execute_get(@s.url_for("/var/cluster/user.json?Done_Trusted_AuthTest"))
  end


  def test_change_password_basicAuthNewUser
    m = Time.now.to_i.to_s
    @s.trustedauth = false
	
	u = "userbasic"+m
	
	runChangePasswordForUser(u,"a")
  end

  def test_change_password_trustedAuthNewUser
    m = Time.now.to_i.to_s
    @s.trustedauth = true
	
	u = "usertrusted"+m
	
	runChangePasswordForUser(u,"b")
  end

  def test_change_password_trustedAuth
    m = Time.now.to_i.to_s
    @s.trustedauth = true
	@s.execute_get(@s.url_for("/var/cluster/user.json?Starting_Trusted_AuthTest"))
	

	
	@log.info("Changing Admin Password with Trusted Auth")
	runChangePassword("d")
	@log.info("Done Changing Admin Password with Trusted Auth")
	@s.execute_get(@s.url_for("/var/cluster/user.json?Done_Trusted_AuthTest"))
  end


  def runChangePasswordForUser(u,v)
  
	testUser = create_user(u)
	
	@s.switch_user(testUser)
	
	
	checkPersonalSpace(u,v)

	
	## This failes here for a trusted user other than admin
	res = testUser.change_password(@s,"testpass2")
	assert_equal("200",res.code,res.body)

	testUser2 = User.new(testUser.name,"testpass2")
	
	@s.switch_user(testUser2)
	res = testUser2.change_password(@s,"testpass")
	assert_equal("200",res.code,res.body)
	
	@s.execute_get(@s.url_for("/var/cluster/user.json?Done_Trusted_AuthTest"))
  end


  def runChangePassword(v)
    admin = User.new("admin","admin")
	@s.switch_user(admin)
	
	
	
	
	# check basic node update functionallity and make certain that the user we think is performing the operation.
	# this should check that the session is correctly authenticated inside JCR since if not the lastModifiedBy wont be 
	# correct.
        m = Time.now.to_i.to_s
	homeFolderTestFile = "/testarea/"+m+v
	res = @s.execute_post(@s.url_for(homeFolderTestFile),"testprop" => "testvalue",  "jcr:mixinTypes" => "mix:lastModified" )
	assert_equal("201",res.code, res.body+"\n Failed to create a node logged in as admin, must be a problem with the login, ie not really admin ?")
	res = @s.execute_get(@s.url_for(homeFolderTestFile+".json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	# check the node really was last modified by the correct user.
	assert_equal(admin.name, props["jcr:lastModifiedBy"])



    res = admin.update_properties(@s,"testproperty" => "newvalue")
	assert_equal("200",res.code,res.body)

	res = admin.change_password(@s,"2admin2")
	
	assert_equal("200",res.code,res.body)

    admin2 = User.new("admin","2admin2")
	@s.switch_user(admin2)
    res = admin2.update_properties(@s,"testproperty" => "newvalue1")
	assert_equal("200",res.code,res.body)
	res = admin2.change_password(@s,"admin")
	assert_equal("200",res.code,res.body)

	@s.switch_user(admin)
    res = admin.update_properties(@s,"testproperty" => "newvalue2")
	assert_equal("200",res.code,res.body)
  end
  
  def checkPersonalSpace(u,v)
  	# Can this new user update content in their own space.
	res = @s.execute_get(@s.url_for("/system/me"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	@log.debug(res.body)
	assert_not_nil(props["user"],"system me request failed, expected to find a user object")
	assert_equal(u, props["user"]["userid"],"Authentication failed, didnt get expected user")
	homeFolderTestFile = "/~#{u}/testarea"+u+v
	
	res = @s.execute_post(@s.url_for(homeFolderTestFile),"testprop" => "testvalue",  "jcr:mixinTypes" => "mix:lastModified" )
	assert_equal("201",res.code, res.body)
	res = @s.execute_get(@s.url_for(homeFolderTestFile+".json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	# check the node really was last modified by the correct user.
	assert_equal(u, props["jcr:lastModifiedBy"])

  end
  

  
end

