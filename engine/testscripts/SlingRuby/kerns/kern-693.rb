#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'

class TC_Kern693Test < Test::Unit::TestCase
  include SlingTest
  def test_site_too_many_methods
    m = Time.now.to_f.to_s.gsub('.', '_')
    originalsiteid = "testsite_orig_#{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{originalsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => originalsiteid,
      "description" => originalsiteid,
      "id" => originalsiteid)
    newsite = "testsite_mv_#{m}"
    # Try to simultaneously create a site from a template,
    # copy a site, and move a site.
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{newsite}",
      ":moveFrom" => "/sites/#{originalsiteid}",
      ":copyFrom" => "/sites/#{originalsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate")
    assert_equal("400", res.code, "Contradictory instructions were accepted")
  end

  def test_site_bad_source_site
    m = Time.now.to_f.to_s.gsub('.', '_')
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    newsite = "testsite_#{m}"
    nosuchsite = "/sites/nothingtosee_#{m}"
    # Try to copy a non-existent site.
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/{#newsite}",
      ":copyFrom" => nosuchsite)
    assert_equal("400", res.code, "Copying an unreachable site should fail")
    # Try to move a non-existent site.
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/{#newsite}",
      ":moveFrom" => nosuchsite)
    assert_equal("400", res.code, "Moving an unreachable site should fail")
  end

  def test_site_move_under_sites
    m = Time.now.to_f.to_s.gsub('.', '_')
    originalsiteid = "testsite_orig_#{m}"
    originalsitename = "Original Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{originalsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => originalsitename,
      "description" => originalsitename,
      "id" => originalsiteid)
    newsite = "testsite_mv_#{m}"
    # Move site, change name, but keep ID and description as they are.
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{newsite}",
      ":moveFrom" => "/sites/#{originalsiteid}",
      "name" => newsite)
    assert_equal("200", res.code, "Site creator should be able to move site to another path under /sites")
    res = @s.execute_get(@s.url_for("/sites/#{originalsiteid}.json"))
    assert_not_equal("200", res.code, "Old site should no longer exist")
    res = @s.execute_get(@s.url_for("/sites/#{newsite}.json"))
    props = JSON.parse(res.body)
    assert_equal(originalsiteid, props["id"]);
    assert_equal(newsite, props["name"]);
    assert_equal(originalsitename, props["description"]);
  end

  def test_site_copy_under_sites
    m = Time.now.to_f.to_s.gsub('.', '_')
    originalsiteid = "testsite_orig_#{m}"
    originalsitename = "Original Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{originalsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => originalsitename,
      "description" => originalsitename,
      "id" => originalsiteid)
    newsite = "testsite_mv_#{m}"
    # Move site, change name, but keep ID and description as they are.
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{newsite}",
      ":copyFrom" => "/sites/#{originalsiteid}",
      "name" => newsite)
    assert_equal("200", res.code, "Site creator should be able to copy site to another path under /sites")
    res = @s.execute_get(@s.url_for("/sites/#{newsite}.json"))
    props = JSON.parse(res.body)
    assert_equal(originalsiteid, props["id"]);
    assert_equal(newsite, props["name"]);
    assert_equal(originalsitename, props["description"]);
    res = @s.execute_get(@s.url_for("/sites/#{originalsiteid}.json"))
    assert_equal("200", res.code, "Original site should still be there")
    props = JSON.parse(res.body)
    assert_equal(originalsitename, props["name"], "Original site should not have been changed");
  end

  def test_site_move_outside_sites
    m = Time.now.to_f.to_s.gsub('.', '_')
    originalsiteid = "testsite_orig_#{m}"
    sitecreator = create_user("testuser#{m}")
    # As admin, set up an accessable space for the site creator.
    userownedpath = "/testcontentfor_#{sitecreator.name}"
    userownedurl = @s.url_for(userownedpath)
    res = @s.execute_post(userownedurl)
    res = @s.execute_post("#{userownedurl}.modifyAce.html",
      "principalId" => sitecreator.name,
      "privilege@jcr:all" => "granted")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{originalsiteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => originalsiteid,
      "description" => originalsiteid,
      "id" => originalsiteid)
    originalsitepath = "/sites/#{originalsiteid}"
    newsitepath = "#{userownedpath}/testsite_mv_#{m}"
    # Move site.
    res = @s.execute_post(@s.url_for(originalsitepath),
      ":operation" => "move",
      ":dest" => newsitepath)
    assert_equal("201", res.code, "Site creator should be able to move site to another writable URL")
    res = @s.execute_get(@s.url_for("#{originalsitepath}.json"))
    assert_not_equal("200", res.code, "Old site should no longer exist")
    res = @s.execute_get(@s.url_for("#{newsitepath}.json"))
    assert_equal("200", res.code, "Moved site should exist at new location")
    props = JSON.parse(res.body)
    assert_equal(originalsiteid, props["id"]);
    assert_equal(originalsiteid, props["name"]);
    assert_equal(originalsiteid, props["description"]);
  end
end

