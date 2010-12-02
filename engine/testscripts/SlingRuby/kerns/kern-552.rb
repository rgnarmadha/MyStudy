#!/usr/bin/env ruby
# encoding: UTF-8

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_KernMeTest < Test::Unit::TestCase
  include SlingTest
  
  def set_first_name(name, user)
    public = user.public_path_for(@s)
    path = "#{public}/authprofile"
    props = {"firstName" => name, "_charset_" => "UTF-8"}
    @s.execute_post(@s.url_for(path), props)
  end
  
  def get_system_me
    res = @s.execute_get(@s.url_for("/system/me"))
    return JSON.parse(res.body)
  end
  
  
  def test_me_service
    m = Time.now.to_i.to_s
    userid = "testuser-#{m}"
    user = create_user(userid)
    
    @s.switch_user(user)
    
    # Safe characters
    characters = "foobar"
    set_first_name(characters, user)
    json = get_system_me()
    
    # Check if name is correct.
    assert_equal(json["profile"]["firstName"], characters, "Safe characters didn't match")
    
    # Non-safe
    characters = "ççççç"
    set_first_name(characters, user)
    json = get_system_me()
    
    # Check if name is correct.
    assert_equal(json["profile"]["firstName"], characters, "Unsafe characts didn't match")
  end
  
end

