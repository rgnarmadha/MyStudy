#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1154Test < Test::Unit::TestCase
  include SlingTest

  # Cause of SAKIII-1099.
  def test_profile_does_not_override_access
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
    home = user.home_path_for(@s)
    public = user.public_path_for(@s)
    @s.switch_user(user)
    res = @s.execute_post(@s.url_for("#{home}.modifyAce.html"), {
      "principalId" => "everyone",
      "privilege@jcr:read" => "granted"
    })
    res = @s.execute_post(@s.url_for("#{home}.modifyAce.html"), {
      "principalId" => "anonymous",
      "privilege@jcr:read" => "granted"
    })
    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for(home))
    assert_equal("200", res.code, "The default User / Group home page should be public")
    res = @s.execute_get(@s.url_for("#{public}/authprofile.json"))
    assert_equal("200", res.code, "The User / Group profile should be public")
  end

  # Cause of KERN-1159.
  def test_home_access_not_overwritten
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
    home = user.home_path_for(@s)
    public = user.public_path_for(@s)
    @s.switch_user(user)
    res = @s.execute_post(@s.url_for("#{home}.modifyAce.html"), {
      "principalId" => "everyone",
      "privilege@jcr:read" => "denied"
    })
    res = @s.execute_post(@s.url_for("#{home}.modifyAce.html"), {
      "principalId" => "anonymous",
      "privilege@jcr:read" => "denied"
    })
    # Update a user property.
    res = @s.execute_post(@s.url_for(User.url_for(user.name) + ".update.html"), {
      "testproperty" => "testvalue"
    })

    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_get(@s.url_for(home))
    assert_not_equal("200", res.code, "The User / Group home page should not be public")
  end

end
