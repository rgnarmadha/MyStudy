#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1090Test < Test::Unit::TestCase
  include SlingTest

  def test_default_group_access
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    member = create_user("user-manager-#{m}")
    manager = create_user("user-member-#{m}")
    nonmember = create_user("user-nonmember-#{m}")
    group = Group.new("g-test-#{m}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    # The current default is to make both the Group entity and its Managers
    # list publicly viewable.
    @s.switch_user(nonmember)
    res = @s.execute_get(@s.url_for(Group.url_for(group.name) + ".members.json"))
    assert_equal("200", res.code, "Group member query should be available to nonmember")
    members = JSON.parse(res.body)
    assert_equal(member.name, members[0]["userid"], "Group members should be returned to nonmember")
    details = group.details(@s)
    managersgroupname = details["properties"]["sakai:managers-group"]
    assert_not_nil(managersgroupname, "Managers group property should be available to nonmember")
    res = @s.execute_get(@s.url_for(Group.url_for(managersgroupname) + ".members.json"))
    assert_equal("200", res.code, "Managers group member query should be available to nonmember")
    members = JSON.parse(res.body)
    assert_equal(manager.name, members[0]["userid"], "Managers group members should be returned to nonmember")
  end

end
