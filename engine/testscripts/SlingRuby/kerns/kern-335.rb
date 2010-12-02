#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/message'
require 'test/unit.rb'
include SlingSearch
include SlingInterface
include SlingUsers
include SlingSites
include SlingMessage

class TC_Kern335Test < Test::Unit::TestCase
  include SlingTest
  
  def setup
    super
    @mm = MessageManager.new(@s)
  end


  def test_chatupdate
    m = Time.now.to_i.to_s
    user1 = create_user("chatuser1-#{m}")
    user2 = create_user("chatuser2-#{m}")
  
    @s.switch_user(user1)
    @mm.create("chat:chatuser2-#{m}", "chat", "outbox")
    @s.switch_user(user2)
    
    home1 = user1.home_path_for(@s)
    home2 = user2.home_path_for(@s)
    
    # First GET should respond with true.
    res = @s.execute_get(@s.url_for("#{home2}/message.chatupdate.json"))
    json = JSON.parse(res.body)
    assert_equal(true, json["update"], "The first GET request should respond with a true.")
    time = json["time"]
    
    
    res = @s.execute_get(@s.url_for("#{home2}/message.chatupdate.json?t=#{time}"))
    json = JSON.parse(res.body)
    assert_equal(false, json["update"], "The second GET request should respond with a false.")
    time = json["time"]
    
    @s.switch_user(user1)
    @mm.create("chat:chatuser2-#{m}", "chat", "outbox")
    
    sleep(1)
    
    res = @s.execute_get(@s.url_for("#{home1}/message.chatupdate.json?t=#{time}"))
    json = JSON.parse(res.body)
    assert_equal(true, json["update"], "After receiving another msg we should get a true update.")
    time = json["time"]
    res = @s.execute_get(@s.url_for("#{home1}/message.chatupdate.json?t=#{time}"))
    json = JSON.parse(res.body)
    assert_equal(false, json["update"], "The second GET request after the msg should respond with a false.")
    time = json["time"]
    
    
  end

end


