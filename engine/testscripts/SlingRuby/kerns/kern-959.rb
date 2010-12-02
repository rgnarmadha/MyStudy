#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern959Test < Test::Unit::TestCase
  include SlingTest

  def test_managers_group_deleted_with_own_group
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    group = Group.new("g-test-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      "_charset_" => "UTF-8"
    })
    # Get the name of the generated managers group.
    assert_equal("200", res.code, "Should have created group as admin")
    details = group.details(@s)
    managersgroupname = details["properties"]["sakai:managers-group"]
    managersgroup = Group.new(managersgroupname)
    details = managersgroup.details(@s)
    assert_equal(group.name, details["properties"]["sakai:managed-group"], "Managers group should point to its managed group")
    # Delete the main group.
    @um = UserManager.new(@s)
    assert(@um.delete_group(group.name), "Should have deleted main group")
    res = @s.execute_get(@s.url_for("#{Group.url_for(managersgroupname)}.json"))
    assert_not_equal("200", res.code, "Should also have deleted managers group")
  end

end
