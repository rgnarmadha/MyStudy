#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern254Test < Test::Unit::TestCase

  include SlingTest

  def test_modify_user_after_group_join
    m = "1b"+Time.now.to_i.to_s
	testuser = "testuser#{m}"
    u = create_user(testuser)
    g1 = create_group("g-testgroup#{m}")
    g1.add_member(@s, u.name, "user")
    g2 = create_group("g-testgroup2#{m}")
    g2.add_member(@s, u.name, "user")
    g2.add_member(@s, g1.name, "group")
    details = g2.details(@s)
    members = details["members"]
    assert_not_nil(members, "Expected a list of members")
    assert_equal(1, members.select{|m| m == testuser}.size, "Expected no dupes "+members.to_s)
  end

end

#Test::Unit::UI::Console::TestRunner.run(TC_Kern254Test)

