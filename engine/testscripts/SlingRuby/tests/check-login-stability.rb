#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch
include SlingUsers

class TC_LoginStability < Test::Unit::TestCase
  include SlingTest

  def check_user_valid(u,iterations)
    @log.error("Thread Starting for #{u.name} ")
    m = Time.now.to_i.to_s
    s = SlingInterface::Sling.new()
    s.trustedauth = true
    s.switch_user(u)
    code="201"
    i=0
    res = s.execute_get(s.url_for("/system/me"))
    assert_equal("200",res.code)
    props = JSON.parse(res.body)
    assert_not_nil(props["user"],"system me request failed, expected to find a user object")
    assert_equal(u.name, props["user"]["userid"],"Authentication failed, didnt get expected user")

    while i < iterations

      homeFolderTestFile = "/~#{u.name}/testarea"+m
      res = s.execute_post(s.url_for(homeFolderTestFile),"testprop" => "testvalue",  "jcr:mixinTypes" => "mix:lastModified" )
      assert_equal(code,res.code, res.body)
      code="200"
      res = s.execute_get(s.url_for(homeFolderTestFile+".json"))
      assert_equal("200",res.code)
      props = JSON.parse(res.body)
      # check the node really was last modified by the correct user.
      assert_equal(u.name, props["jcr:lastModifiedBy"])
      i=i+1
      sleep(1)
    end 
 end
  
 def testCookieLeak() 




    m = Time.now.to_i.to_s
    threads = Array.new 

    i=0
    while i < 5
      u = create_user("testuser"+m+"_"+i.to_s)
      details = @um.get_user_props(u.name)
      assert_equal("testuser"+m+"_"+i.to_s, details["rep:principalName"], "Expected username to match")
      threads[i] = Thread.new() {
         check_user_valid(u,10000)
      }
      i=i+1
    end
    i=0
    while i < 5
      threads[i].join
      @log.error("Thread #{i} done ")
      i=i+1
    end

  end


end


