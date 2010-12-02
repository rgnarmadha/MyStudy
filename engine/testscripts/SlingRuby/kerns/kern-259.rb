#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern259Test < Test::Unit::TestCase

  include SlingTest
#  def test_site_membership
#    @log.warn("Site Join has been deprecated, tested elsewhere ")
#  end

  def test_site_membership
    m = Time.now.to_i.to_s
    @log.info("Creating Site at /sites/testsite/"+m)
    test_site = create_site("sites", "My Test Site " + m, "/testsite/" + m)
    test_site.set_joinable("yes")
    site_group = create_group("g-testgroup" + m)
    site_group.set_joinable(@s, "yes")
    res = test_site.add_group(site_group.name)
    assert_equal("200", res.code, "Expected group add to succeed")
    @log.info("added "+site_group.name+" to site ")


    test_user = create_user("someguy" + m)
    @s.switch_user(test_user)
    res = test_site.join(site_group.name)
    assert_equal("200", res.code, "Expected join to succeed")
    @log.info(test_user.name+" joind "+site_group.name)


    members = test_site.get_members
    @log.info("Got Members are #{members} ")
    assert_not_nil(members, "Expected to get member list")
    assert_equal(1, members["results"].size, "Expected site members")
    assert_equal(test_user.name, members["results"][0]["rep:userId"], "Expected user to match")


    membership = @sm.get_membership()["results"]
    @log.info("Got membership as #{membership} ")
    assert_equal(1, membership.size, "Expected one member")
    assert_value(membership, 'siteref', "/" + test_site.path, "Expected site path to match")
  end

  def assert_value(hash, key, value, message)
    found = false
    hash.each {|o|
      if (o[key] == value)
        found = true
      end
    }
    assert(found, message)
  end

end


