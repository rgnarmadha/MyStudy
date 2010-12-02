#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingSearch
include SlingUsers

class TC_Kern734Test < Test::Unit::TestCase
  include SlingTest
  
  #
  # Test changing the admin password using the form auth mechanism.
  #
  
  
  def test_modify_prefs_basicAuth
    m = Time.now.to_i.to_s
    @s.trustedauth = false
    admin = User.new("admin","admin")
	modifyPrefs(admin,"basic")
  end

  def test_modify_prefs_trustedAuth
    m = Time.now.to_i.to_s
    @s.trustedauth = true
    admin = User.new("admin","admin")
	modifyPrefs(admin,"trusted")
  end

  def test_modify_prefs_basicAuthNewUser
    m = Time.now.to_i.to_s
    @s.trustedauth = false
	
	u = "user"+m
	testUser = create_user(u)
	
	modifyPrefs(testUser,"basic")
  end
  def test_modify_prefs_trustedAuthNewUser
    m = Time.now.to_i.to_s
    @s.trustedauth = false
	
	u = "userbasic"+m
	testUser = create_user(u)
	
	modifyPrefs(testUser,"trusted")
  end
  
  def modifyPrefs(testUser,type)
    m = Time.now.to_i.to_s
	
	@s.switch_user(testUser)
	
	# Can this new user update content in their own space.
	res = @s.execute_get(@s.url_for("/system/me"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	@log.debug(res.body)
	assert_not_nil(props["user"],"system me request failed, expected to find a user object")
	assert_equal(testUser.name, props["user"]["userid"],"Authentication failed, didnt get expected user")
	homeFolderTestFile = "/~#{testUser.name}/testarea"+m+type
	
	res = @s.execute_post(@s.url_for(homeFolderTestFile),"testprop" => "testvalue",  "jcr:mixinTypes" => "mix:lastModified" )
	assert_equal("201",res.code, res.body)
	res = @s.execute_get(@s.url_for(homeFolderTestFile+".json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	# check the node really was last modified by the correct user.
	assert_equal(testUser.name, props["jcr:lastModifiedBy"])
	
  end




  

  
end

