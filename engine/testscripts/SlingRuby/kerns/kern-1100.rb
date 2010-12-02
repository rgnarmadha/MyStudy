#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
include SlingUsers
include SlingFile

class TC_Kern1100Test < Test::Unit::TestCase
  include SlingTest

  def test_manager_group_sees_file_members
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    member = create_user("user-manager-#{m}")
    group = Group.new("g-test-#{m}")
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":member" => member.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "Test #{m}", "Test #{m}", "This is some random content: #{m}.", "text/plain")
    assert_equal("201", res.code, "Expected to be able to create pooled content")
	uploadresult = JSON.parse(res.body)
	contentid = uploadresult["Test #{m}"]
	assert_not_nil(contentid, "Should have uploaded ID")
	contentpath = @s.url_for("/p/#{contentid}")

	# Check the initial assumption (group member unable to reach pooled content
	# or members).
    @s.switch_user(member)
    res = @s.execute_get("#{contentpath}.json")
    assert_not_equal("200", res.code, "Non-viewer should not reach pooled content")
    res = @s.execute_get("#{contentpath}.members.json")
    assert_not_equal("200", res.code, "Non-viewer should not reach pooled content members")

    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("/p/#{contentid}.members.html"), {
      ":manager" => group.name
    })
    @s.switch_user(member)
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Member of manager group should reach pooled content")
    res = @s.execute_get("#{contentpath}.members.json")
    assert_equal("200", res.code, "Member of manager group should reach pooled content members")
    members = JSON.parse(res.body)
    assert_not_nil(members["managers"].find{|e| e["groupid"] == group.name}, "Expected group not in content managers")
  end

  def test_nonmember_cannot_see_file_members
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    nonmember = create_user("user-nonmember-#{m}")
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "Test #{m}", "Test #{m}", "This is some random content: #{m}.", "text/plain")
    assert_equal("201", res.code, "Expected to be able to create pooled content")
	uploadresult = JSON.parse(res.body)
	contentid = uploadresult["Test #{m}"]
	assert_not_nil(contentid, "Should have uploaded ID")
	contentpath = @s.url_for("/p/#{contentid}")

    @s.switch_user(nonmember)
    res = @s.execute_get("#{contentpath}.json")
    assert_not_equal("200", res.code, "Non-viewer non-manager should not reach private pooled content")
    res = @s.execute_get("#{contentpath}.members.json")
    assert_not_equal("200", res.code, "Non-viewer non-manager should not reach pooled content members")

    # Make the content itself publicly viewable.
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("/p/#{contentid}.modifyAce.html"), {
      "principalId" => "everyone",
      "privilege@jcr:read" => "granted"
    })
    @s.switch_user(nonmember)
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Non-viewer non-manager should reach public pooled content")
    res = @s.execute_get("#{contentpath}.members.json")
    assert_not_equal("200", res.code, "Non-viewer non-manager should still not reach pooled content members")
    res = @s.execute_post(@s.url_for("/p/#{contentid}.members.html"), {
      ":viewer" => nonmember.name
    })
    assert_not_equal("200", res.code, "Non-viewer non-manager should not be able to add pooled content viewer")
    @s.switch_user(User.admin_user())
    res = @s.execute_get("#{contentpath}.members.json")
    members = JSON.parse(res.body)
    assert_nil(members["viewers"].find{|e| e["userid"] == nonmember.name}, "Non-viewer non-manager should not have added self")
  end

  def test_do_not_accidently_remove_manager_read_access
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    @s.switch_user(User.admin_user())
    manager = create_user("user-manager-#{m}")
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "Test #{m}", "Test #{m}", "This is some random content: #{m}.", "text/plain")
    assert_equal("201", res.code, "Expected to be able to create pooled content")
	uploadresult = JSON.parse(res.body)
	contentid = uploadresult["Test #{m}"]
	assert_not_nil(contentid, "Should have uploaded ID")
	contentpath = @s.url_for("/p/#{contentid}")

    # First, add the user as a manager.
    res = @s.execute_post(@s.url_for("/p/#{contentid}.members.html"), {
      ":manager" => manager.name
    })

    # Then add the user as a viewer.
    res = @s.execute_post(@s.url_for("/p/#{contentid}.members.html"), {
      ":viewer" => manager.name
    })

    # Then try to remove the user from the viewers list.
    res = @s.execute_post(@s.url_for("/p/#{contentid}.members.html"), {
      ":viewer@Delete" => manager.name
    })

    # Can the user still see the content?
    @s.switch_user(manager)
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Member of manager group should reach pooled content")
    res = @s.execute_get("#{contentpath}.members.json")
    assert_equal("200", res.code, "Member of manager group should reach pooled content members")
    members = JSON.parse(res.body)
    assert_not_nil(members["managers"].find{|e| e["userid"] == manager.name}, "Expected user not in content managers")
  end

end
