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

class TC_Kern939Test < Test::Unit::TestCase
  include SlingTest

  # This test depends on knowledge about the default user pages.
  def test_default_user_pages
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    user = create_user("testuser-#{m}")
    @s.switch_user(user)
    path = "#{user.home_path_for(@s)}/pages"
    res = @s.execute_get(@s.url_for("#{path}.2.json"))
    assert_equal("200", res.code, "Should have created pages in postprocessing")
    json = JSON.parse(res.body)
    assert_not_nil(json["index.html"], "Expected default page not found")
    assert_equal(json["index.html"]["jcr:primaryType"], "nt:file", "Default home page is not a file")
  end

  # This test depends on knowledge about the default group pages.
  def test_default_group_pages
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    group = create_group("g-testgroup-#{m}")
    path = "#{group.home_path_for(@s)}/pages"
    res = @s.execute_get(@s.url_for("#{path}.2.json"))
    assert_equal("200", res.code, "Should have created pages in postprocessing")
    json = JSON.parse(res.body)
    assert_not_nil(json["index.html"], "Expected default page not found")
    assert_equal(json["index.html"]["jcr:primaryType"], "nt:file", "Default home page is not a file")
  end

  def test_override_default_template
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    templatepath = "/test/template-#{m}"
    res = @s.execute_post(@s.url_for(templatepath), {
      ":operation" => "import",
      ":contentType" => "json",
      ":content" => "{nondefaultpage: {pageproperty: pagevalue}, nondefaultproperty: nondefaultvalue}"
    })
    # Check some assumptions before proceeding to the real test.
    assert_equal("201", res.code, "Did not successfully create test template")
    res = @s.execute_get(@s.url_for("#{templatepath}.2.json"))
    assert_equal("200", res.code, "New page content is missing")
    props = JSON.parse(res.body)
    assert_equal("nondefaultvalue", props["nondefaultproperty"], "Top-level property is missing")
    assert_equal("pagevalue", props["nondefaultpage"]["pageproperty"], "Sub-page is missing")
    # And now for the real test...
    userid = "testuser-#{m}"
    password = "testuser"
    res = @s.execute_post(@s.url_for("#{$USER_URI}"), {
      ":name" => userid,
      "pwd" => password,
      "pwdConfirm" => password,
      ":sakai:pages-template" => templatepath,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created user as admin")
    user = User.new(userid)
    @s.switch_user(user)
    res = @s.execute_get(@s.url_for("#{user.home_path_for(@s)}/pages.2.json"))
    assert_equal("200", res.code, "New page content is missing")
    props = JSON.parse(res.body)
    assert_equal("nondefaultvalue", props["nondefaultproperty"], "Top-level property is missing")
    assert_equal("pagevalue", props["nondefaultpage"]["pageproperty"], "Sub-page is missing")
  end

  def test_default_user_access
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    user = create_user("testuser-#{m}")
    @s.switch_user(user)
    path = "#{user.home_path_for(@s)}/pages"
    res = @s.execute_get(@s.url_for("#{path}.json"))
    assert_equal("200", res.code, "The user should be able to reach the user's pages")
    res = @s.execute_post(@s.url_for("#{path}/newnode"),
      "newprop" => "newval")
    assert_equal("201", res.code, "Users should be able to add to their own pages")
    res = @s.execute_get(@s.url_for("#{path}/newnode.json"))
    assert_equal("200", res.code, "New page content is missing")
    props = JSON.parse(res.body)
    assert_equal("newval", props["newprop"], "New page content is incorrect")
    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for("#{path}.json"))
    assert_equal("200", res.code, "By default, the user's pages are public")
  end

  def test_default_group_access
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    manager = create_user("manager-#{m}")
    member = create_user("member-#{m}")
    otheruser = create_user("otheruser-#{m}")
    group = Group.new("g-test-#{m}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      ":member" => member.name,
      "_charset_" => "UTF-8"
    })
    path = "#{group.home_path_for(@s)}/pages"

    @s.switch_user(member)
    res = @s.execute_get(@s.url_for("#{path}.json"))
    assert_equal("200", res.code, "Members should be able to read the group's pages")
    res = @s.execute_post(@s.url_for("#{path}/newnode"),
      "newprop" => "newval")
    assert_not_equal("201", res.code, "Members should not be able to add to the group's pages")
    res = @s.execute_get(@s.url_for("#{path}/newnode.json"))
    assert_not_equal("200", res.code, "New page content should not have been created")

    @s.switch_user(manager)
    res = @s.execute_get(@s.url_for("#{path}.json"))
    assert_equal("200", res.code, "Managers should be able to read the group's pages")
    res = @s.execute_post(@s.url_for("#{path}/newnode"),
      "newprop" => "newval")
    assert_equal("201", res.code, "Managers should be able to add to the group's pages")
    res = @s.execute_get(@s.url_for("#{path}/newnode.json"))
    assert_equal("200", res.code, "New page content is missing")
    props = JSON.parse(res.body)
    assert_equal("newval", props["newprop"], "New page content is incorrect")

    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for("#{path}.json"))
    assert_equal("200", res.code, "By default, the group's pages are public")
    @s.switch_user(otheruser)
    res = @s.execute_post(@s.url_for("#{path}/newernode"),
      "newprop" => "newval")
    assert_not_equal("201", res.code, "Non-members should not be able to add to the group's pages")
  end

end
