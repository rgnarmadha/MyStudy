#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch
include SlingUsers

class TC_FormAuthTest < Test::Unit::TestCase
  include SlingTest

  def test_form_auth
    @s.trustedauth = true
    m = Time.now.to_i.to_s
    u = create_user("testuser"+m)
    details = @um.get_user_props(u.name)
    assert_equal("testuser"+m, details["rep:principalName"], "Expected username to match")
	@s.switch_user(u)
	res = @s.execute_get(@s.url_for("/system/me"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	@log.debug(res.body)
	assert_not_nil(props["user"],"system me request failed, expected to find a user object")
	assert_equal(u.name, props["user"]["userid"],"Authentication failed, didnt get expected user")
	homeFolderTestFile = "/~#{u.name}/testarea"+m
	
	res = @s.execute_post(@s.url_for(homeFolderTestFile),"testprop" => "testvalue",  "jcr:mixinTypes" => "mix:lastModified" )
	assert_equal("201",res.code, res.body)
	res = @s.execute_get(@s.url_for(homeFolderTestFile+".json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
	# check the node really was last modified by the correct user.
	assert_equal(u.name, props["jcr:lastModifiedBy"])
	@log.debug(res.body)
  end


end


