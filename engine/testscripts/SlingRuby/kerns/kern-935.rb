#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
include SlingSearch
include SlingUsers
include SlingContacts

class TC_Kern935Test < Test::Unit::TestCase
  include SlingTest

  def test_private_group_anon
    m = Time.now.to_i.to_s
    member = create_user("user-member-#{m}")
    viewer = create_user("user-viewer-#{m}")
    @s.switch_user(User.admin_user())
    privategroup = create_group("g-test-group-#{m}")
    privategroup.add_member(@s, member.name, "user")
    privategroup.add_viewer(@s, viewer.name)
    @s.switch_user(member)
    res = @s.execute_get(@s.url_for(Group.url_for(privategroup.name) + ".json"))
    assert_equal("404",res.code, res.body)
    @s.switch_user(viewer)
    res = @s.execute_get(@s.url_for(Group.url_for(privategroup.name) + ".json"))
    assert_equal("200",res.code, res.body)
    @s.switch_user(User.anonymous)
    res = @s.execute_get(@s.url_for(Group.url_for(privategroup.name) + ".json"))
    assert_equal("404",res.code, res.body)
  end

end
