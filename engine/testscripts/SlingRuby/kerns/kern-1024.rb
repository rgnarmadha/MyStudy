#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1024Test < Test::Unit::TestCase
  include SlingTest

  def test_members_servlet_from_group_manager
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    member = create_user("user-member-#{m}")
    group = Group.new("g-test-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    @s.switch_user(manager)
    res = @s.execute_get(@s.url_for("/system/userManager/group/#{group.name}.members.json"))
    assert_equal("200", res.code, "Should have retrieved members as manager")
    members = JSON.parse(res.body)
    assert_equal(1, members.size, "Should not return manager in members")
    assert_equal(member.name, members[0]["userid"], "Should have found member")
    res = @s.execute_get(@s.url_for("/system/userManager/group/#{group.name}.managers.json"))
    assert_equal("200", res.code, "Should have retrieved managers as manager")
    members = JSON.parse(res.body)
    assert_equal(1, members.size, "Should not return non-manager member in managers")
    assert_equal(manager.name, members[0]["userid"], "Should have found manager member")
  end

end
