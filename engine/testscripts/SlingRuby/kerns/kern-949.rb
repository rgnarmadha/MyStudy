#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern949Test < Test::Unit::TestCase
  include SlingTest

  def test_create_managers_group
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    group = Group.new("g-test-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    @s.switch_user(manager)
    details = group.details(@s)
    managersgroupname = details["properties"]["sakai:managers-group"]
    assert_not_nil(managersgroupname, "Managers group property should be set")
    assert(details["properties"]["rep:group-managers"].include?(managersgroupname), "Group managers should include its own managers group")
    managersgroup = Group.new(managersgroupname)
    details = managersgroup.details(@s)
    assert_equal(group.name, details["properties"]["sakai:managed-group"], "Managers group should point to its managed group")
    assert_equal(managersgroupname, details["properties"]["rep:group-managers"], "Managers group should manage itself")
    members = details["members"]
    assert(members.include?(manager.name), "Should have added user to managers group")
  end

  def test_update_managers_group
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    other = create_user("user-other-#{m}")
    group = Group.new("g-test-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    details = group.details(@s)
    managersgroupname = details["properties"]["sakai:managers-group"]
    managersgroup = Group.new(managersgroupname)
    assert(managersgroup.has_member(@s, manager.name), "Should have added user to managers group")
    group.update_properties(@s, {
      ":sakai:manager@Delete" => manager.name,
      ":sakai:manager" => other.name
    })
    assert(!(managersgroup.has_member(@s, manager.name)), "Should have removed user from managers group")
    assert(managersgroup.has_member(@s, other.name), "Should have added new user to managers group")
  end

  def test_managers_group_contention
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    group = Group.new("g-test-#{m}")
    contentious = Group.new("g-test-#{m}-managers")
    other = create_user("user-other-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => contentious.name,
      ":viewer" => other.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created contentious group as admin")
    @s.switch_user(manager)
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as manager")
    @s.switch_user(manager)
    details = group.details(@s)
    managersgroupname = details["properties"]["sakai:managers-group"]
    assert_not_nil(managersgroupname, "Managers group property should be set")
    assert_not_equal(contentious.name, managersgroupname, "Managers group should not have a conflicting name")
  end

end
