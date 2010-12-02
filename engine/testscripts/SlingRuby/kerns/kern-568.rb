#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'time'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern568Test < Test::Unit::TestCase
  include SlingTest
  
  # Just check that we get a response to a bad t parameter
  def test_malformed_time
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    home = user.home_path_for(@s)
    firstres = @s.execute_get(@s.url_for("#{home}/message.chatupdate.json"))
    assert_equal(200, firstres.code.to_i)

    params = {"t" => "invalid"}
    res = @s.execute_get(@s.url_for("#{home}/message.chatupdate.json"), params)
    assert_equal(200, res.code.to_i)
  end

  # Test that the pulltime comes through right, ignoring the update flag
  def test_correct_pulltime
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    home = user.home_path_for(@s)
    firstres = @s.execute_get(@s.url_for("#{home}/message.chatupdate.json"))
    assert_equal(200, firstres.code.to_i)

    sleep(1)

    # Note that ruby and the the server JVM have to be in the same timezone for this to pass.
    # This should not pose a problem because testing is generally against localhost but is worth noting.
    now = Time.now
    expected = now.xmlschema(3)
    msec = (now.to_f * 1000).to_i

    params = {"t" => msec}
    home = user.home_path_for(@s)
    res = @s.execute_get(@s.url_for("#{home}/message.chatupdate.json"), params)
    json = JSON.parse(res.body)
    assert_equal(expected, json["pulltime"])
  end

  # We want to make sure that, no matter what time is specified,
  # If this is the first request on record for the user, we update
  # and that the pulltime is not what we specify for the first
  # request and is the specified timestamp for the second
  def test_preserve_first_check
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)

    # Time.at accepts an unsigned long, so go as far into the future as we can.
    # This ends up sometime in 2038.
    sec = 2147483647
    time = Time.at(sec)
    schematime = time.xmlschema(3)
    params = {"t" => sec * 1000 }

    home = user.home_path_for(@s)
    firstres = @s.execute_get(@s.url_for("#{home}/message.chatupdate.json"), params)
    json = JSON.parse(firstres.body)
    assert_equal(true, json["update"], "First check should always force update")
    assert_not_equal(schematime, json["pulltime"], "First check should not return an arbitrary timestamp")

    # On second request for the same time, we should see false
    res = @s.execute_get(@s.url_for("#{home}/message.chatupdate.json"), params)
    json = JSON.parse(res.body)
    assert_equal(false, json["update"], "Second check should not force update")
    assert_equal(schematime, json["pulltime"], "Second check should return specified pulltime")
  end

end

