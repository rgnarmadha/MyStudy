#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1020Test < Test::Unit::TestCase
  include SlingTest

  def test_find_all_groups
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    manager = create_user("user-manager-#{m}")
    member = create_user("user-member-#{m}")
    managedgroup = Group.new("test-managership-#{m}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => managedgroup.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "sakai:group-id" => managedgroup.name,
      "sakai:group-title" => "#{m} Manager's group",
      "sakai:group-description" => "Test group for manager",
      "_charset_" => "UTF-8"
    })
    membergroup = Group.new("test-membership-#{m}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => membergroup.name,
      ":sakai:manager" => member.name,
      ":member" => manager.name,
      "sakai:group-id" => membergroup.name,
      "sakai:group-title" => "#{m} Member's group",
      "sakai:group-description" => "Test group for member",
      "_charset_" => "UTF-8"
    })
    @s.switch_user(manager)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    groups = me["groups"]
    assert_equal(2, groups.size, "Should have two groups in summary")
    assert_not_nil(groups.find{|e| e["groupid"] == managedgroup.name}, "Manager should be a member of the group")
    assert_not_nil(groups.find{|e| e["groupid"] == membergroup.name}, "Expected group not returned")
    res = @s.execute_get(@s.url_for("/system/me/managedgroups.json"))
    assert_equal("200", res.code, "My Managed Groups servlet should return successfully")
    managedgroups = JSON.parse(res.body)
    assert_equal(3, managedgroups.size, "Should have one managed group")
    assert_equal(managedgroup.name, managedgroups["results"][0]["sakai:group-id"], "Did not retrieve the managed group")
    res = @s.execute_get(@s.url_for("/system/me/groups.tidy.json"))
    assert_equal("200", res.code, "My Groups servlet should return successfully")
    groups = JSON.parse(res.body)
    assert_equal(3, groups.size, "Should have two groups")
    assert_not_nil(groups["results"].find{|e| e["sakai:group-id"] == managedgroup.name}, "Expected group not returned")
    assert_not_nil(groups["results"].find{|e| e["sakai:group-id"] == membergroup.name}, "Expected group not returned")
  end

  def test_find_matching_groups
    m = Time.now.to_f.to_s.gsub('.', '')
    other = Time.now.to_f.to_s.gsub('.', 'XX')
    excluded = Time.now.to_f.to_s.gsub('.', 'YY')
    @s.switch_user(User.admin_user())
    manager = create_user("user-manager-#{m}")
    member = create_user("user-member-#{m}")
    managedgroupMatching = Group.new("test-managership-#{m}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => managedgroupMatching.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "sakai:group-id" => managedgroupMatching.name,
      "sakai:group-title" => "#{m} Manager's group",
      "sakai:group-description" => "Embedde#{other}ded ",
      "_charset_" => "UTF-8"
    })
    managedgroupEmbeddedMatching = Group.new("test-managership-#{other}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => managedgroupEmbeddedMatching.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "sakai:group-id" => managedgroupEmbeddedMatching.name,
      "sakai:group-title" => "#{other} Manager's group",
      "sakai:group-description" => "Test group for manager",
      "_charset_" => "UTF-8"
    })
    managedgroupNotMatching = Group.new("test-managership-#{excluded}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => managedgroupNotMatching.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "sakai:group-id" => managedgroupNotMatching.name,
      "sakai:group-title" => "#{excluded} Manager's group",
      "sakai:group-description" => "Test group for manager",
      "_charset_" => "UTF-8"
    })
    @s.switch_user(manager)
    res = @s.execute_get(@s.url_for("/system/me/managedgroups.tidy.json"))
    assert_equal("200", res.code, "My Managed Groups servlet should return successfully")
    managedgroups = JSON.parse(res.body)
    assert_equal(3, managedgroups["total"], "Should have three managed groups")
    assert_not_nil(managedgroups["results"].find{|e| e["sakai:group-id"] == managedgroupMatching.name}, "Expected group not returned")
    assert_not_nil(managedgroups["results"].find{|e| e["sakai:group-id"] == managedgroupEmbeddedMatching.name}, "Expected group not returned")
    assert_not_nil(managedgroups["results"].find{|e| e["sakai:group-id"] == managedgroupNotMatching.name}, "Expected group not returned")
    res = @s.execute_get(@s.url_for("/system/me/managedgroups.json?q=#{m}"))
    assert_equal("200", res.code, "My Managed Groups servlet should return successfully")
    managedgroups = JSON.parse(res.body)
    assert_equal(1, managedgroups["total"], "Should have one filtered managed group")
    assert_equal(managedgroupMatching.name, managedgroups["results"][0]["sakai:group-id"], "Did not retrieve the managed group")
    res = @s.execute_get(@s.url_for("/system/me/managedgroups.json?q=*#{other}*"))
    assert_equal("200", res.code, "My Managed Groups servlet should return successfully")
    managedgroups = JSON.parse(res.body)
    assert_equal(2, managedgroups["total"], "Should have two filtered managed groups from wildcarded search")
    assert_not_nil(managedgroups["results"].find{|e| e["sakai:group-id"] == managedgroupEmbeddedMatching.name}, "Expected group not returned")
    assert_not_nil(managedgroups["results"].find{|e| e["sakai:group-id"] == managedgroupMatching.name}, "Expected group not returned")
    assert_equal(managedgroupMatching.name, managedgroups["results"][0]["sakai:group-id"], "Did not retrieve the managed group")
  end

end
