#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1064Test < Test::Unit::TestCase
  include SlingTest

  def test_restrict_group_visibility_to_logged_in_users
    m = Time.now.to_f.to_s.gsub('.', '')
    nonmember = create_user("user-nonmember-#{m}")
    group = Group.new("g-test-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      "testproperty" => m,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for("/system/userManager/group/#{group.name}.json"))
    assert_equal("200", res.code, "By default, the new Group is public")

    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for("/system/userManager/group/#{group.name}.json"))
    assert_equal("200", res.code, "The Group should still be visible to anonymous users")

    @s.switch_user(User.admin_user())
    group.update_properties(@s, {
      ":viewer" => "everyone"
    })
    @s.switch_user(nonmember)
    res = @s.execute_get(@s.url_for("/system/userManager/group/#{group.name}.json"))
    assert_equal("200", res.code, "The Group should be visible to logged-in users")
    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for("/system/userManager/group/#{group.name}.json"))
    assert_not_equal("200", res.code, "The Group should no longer be visible to anonymous users")
  end

end
