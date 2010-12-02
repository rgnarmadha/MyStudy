#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'set'
require 'sling/test'
include SlingSearch

class TC_Kern637Test < Test::Unit::TestCase
  include SlingTest
  def test_site_is_not_template
    m = Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    sitetemplate = "/var/templates/sitetest/systemtemplate"
    # Make sure the template is there as expected.
    res = @s.execute_get(@s.url_for(sitetemplate + ".json"))
    props = JSON.parse(res.body)
    assert(props["sakai:is-site-template"], "Expected to find template with is-site-template")
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteid}",
      "sakai:site-template" => sitetemplate,
      "name" => sitename,
      "description" => sitename,
      "id" => siteid)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    @log.debug res.body
    props = JSON.parse(res.body)
    assert(!(props["sakai:is-site-template"]), "Expected site not is-site-template")
  end
end

