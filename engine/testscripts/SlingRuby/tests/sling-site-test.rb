#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingSites

class TC_MySiteTest < Test::Unit::TestCase
  include SlingTest

  def do_site_create
    m = Time.now.to_i.to_s
	# the location of the site container
    sitecontainerid = "sites"
	# the name of the site (under the container)
	sitename = "/sitetests/"+m
	# the final id of the site
	siteid = sitecontainerid+sitename
	# the final url of the site
	@siteurl = @s.url_for(siteid)
	@log.info("Site id is #{siteid} ")
    res = create_site(sitecontainerid,"Site "+m,sitename)
    assert_not_nil(res, "Expected site to be created ")
	@log.info("Site path #{res} ")
	
    res = @s.execute_get(@siteurl+".json");
	assert_equal("200",res.code,"Expectect to get site json at #{@siteurl}.json, result was #{res.body} ")
	@log.debug(res.body)
	props = JSON.parse(res.body)
    assert_equal("sakai/site", props["sling:resourceType"], "Expected resource type to be set")
  end

  def test_create_site
    do_site_create()
  end

  def test_read_default_site
    do_site_create()
    res = @s.execute_get_with_follow(@siteurl+".html")
    assert_equal(200, res.code.to_i, "Expected site to be able to see site "+res.body)
  end

  def test_add_group_to_site
    @m = Time.now.to_i.to_s
   site_group = create_group("g-mysitegroup"+@m)
   test_site = create_site("sites","Site Title"+@m,"/somesite"+@m)
   test_site.add_group(site_group.name)
   groups = SlingSites::Site.get_groups("sites/somesite"+@m, @s)
   assert_equal(1, groups.size, "Expected 1 group")
   assert_equal("g-mysitegroup"+@m, groups[0], "Expected group to be added")
  end

  def test_join_unjoinable_site
    @m = Time.now.to_i.to_s
    site_group = create_group("g-mysitegroup"+@m)
    site_user = create_user("mysiteuser"+@m)
    test_site = create_site("sites","Site Title"+@m,"/somesite"+@m)
    test_site.add_group(site_group.name)
    @s.switch_user(site_user)
    test_site.join(site_group.name)
    members = test_site.get_members
    assert_not_nil(members, "Expected to get member list")
    assert_equal(0, members["total"].to_i, "Expected no site members")
  end

  def do_join(site, group, user)
    site_group = create_group(group)
    site_group.set_joinable(@s, "yes")
    site_user = create_user(user)
    test_site = create_site("sites","Site Title"+site,"/somesite/"+site)
    test_site.add_group(site_group.name)
    test_site.set_joinable("yes")
    @s.switch_user(site_user)
    test_site.join(site_group.name)
    members = test_site.get_members
    assert_not_nil(members, "Expected to get member list")
    assert_equal(1, members["total"].to_i, "Expected site members")
    assert_equal(site_user.name, members["results"][0]["rep:userId"], "Expected user to match")
    @s.switch_user(SlingUsers::User.admin_user)
    return test_site
  end

  def test_join
    @m = Time.now.to_i.to_s
    return do_join("someothersite"+@m, "g-mysitegroup"+@m, "mysiteuser"+@m)    
  end

  def test_join_and_search
    @m = Time.now.to_i.to_s
    do_join("anothersite"+@m, "g-mysitegroup"+@m, "mysiteuser"+@m)
    res = @s.update_node_props("sites/somesite/anothersite"+@m, "fish" => "dog")
    assert_equal(200, res.code.to_i, "Expected site property to be updated")
    result = @search.search_for_site("dog")
    assert_not_nil(result, "Expected results back")
    assert(result["results"].size >= 1, "Expected at least one site")
    created_site = result["results"].select { |r| r["jcr:path"] == "/sites/somesite/anothersite"+@m }
    assert_equal(1, created_site.size, "Expected to find site with matching path")
    assert_equal(1, created_site[0]["member-count"].to_i, "Expected single member")
  end

  def test_multi_group_join
    @m = Time.now.to_i.to_s
    site = do_join("anothersite"+@m, "g-mysitegroup"+@m, "mysiteuser"+@m)
    group2 = create_group("g-sitegroup2"+@m)
    group2.add_member(@s, "mysiteuser"+@m, "user")
    site.add_group(group2.name)
    members = site.get_members
    assert_equal(1, members["total"].to_i, "Expected a single member")
  end

end


