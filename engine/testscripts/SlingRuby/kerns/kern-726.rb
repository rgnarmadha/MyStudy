#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'

class TC_Kern726Test < Test::Unit::TestCase
  include SlingTest
  def test_site_create_not_destroy
    m = Time.now.to_f.to_s.gsub('.', '_')
    siteid = "testsite_#{m}"
    siteparent = "testsite_parent_#{m}"
    sitecreator = create_user("testsiteuser#{m}")
    otheruser = create_user("testotheruser2-#{m}")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteparent}/#{siteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => siteid,
      "description" => siteid,
      "id" => siteid,
      "status" => "offline")
    siteresource = "#{siteparent}/#{siteid}/testresource"
    res = @s.execute_post(@s.url_for("/sites/#{siteresource}"),
    	"myprop" => "myvalue")
    assert_equal("201", res.code, "Site creator should be able to add resource")
    @s.switch_user(otheruser)
    # Since the site is offline, non-members should not see it.
    res = @s.execute_get(@s.url_for("/sites/#{siteparent}/#{siteid}.json"))
    assert_not_equal("200", res.code, "Non-members should not have read access")
    newsiteid = "testothersite_#{m}"
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteparent}/#{siteid}/#{newsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => newsiteid,
      "description" => newsiteid,
      "id" => newsiteid)
    assert_not_equal("200", res.code, "Non-members should not be able to create site under site")
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteresource}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => newsiteid,
      "description" => newsiteid,
      "id" => newsiteid)
    assert_not_equal("200", res.code, "Non-members should not be able to overwrite site resource")
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteparent}/#{newsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => newsiteid,
      "description" => newsiteid,
      "id" => newsiteid)
    assert_equal("200", res.code, "Non-members should be able to create site under existing site's parent")
  end
end

