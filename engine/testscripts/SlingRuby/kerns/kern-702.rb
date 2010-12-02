#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'set'
require 'sling/test'
include SlingSearch

class TC_Kern702Test < Test::Unit::TestCase
  include SlingTest

  def test_create_site_without_template
    m = Time.now.to_f.to_s.gsub('.', '_')
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteid}",
      "name" => sitename,
      "description" => sitename,
      "id" => siteid)
    assert_equal("200", res.code, "Expected to create site: #{res.body}")
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    @log.debug res.body
    props = JSON.parse(res.body)
    # assert_equal("/var/templates/sitetest/systemtemplate", props["sakai:site-template"])
    assert_equal(sitename, props["name"])
    assert_equal(sitename, props["description"])
    newname = "New Name for Test Site #{m}"
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "name" => newname)
    assert_equal("200", res.code, "Should be able to change site " + res.body)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    @log.debug res.body
    props = JSON.parse(res.body)
    assert_equal(newname, props["name"])
  end

end

