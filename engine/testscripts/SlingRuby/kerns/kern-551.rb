#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'set'
require 'sling/test'
include SlingSearch

class TC_Kern551Test < Test::Unit::TestCase
  include SlingTest
  def simple_site_create(siteid, sitename)
    sitetemplate = "/var/templates/sitetest/systemtemplate"
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteid}",
      "sakai:site-template" => sitetemplate,
      "name" => sitename,
      "description" => sitename,
      "id" => siteid)
  end

  def get_rolemap(props)
    roles = props["sakai:roles"]
    rolememberships = props["sakai:rolemembers"]
    rolemap = Hash[*roles.zip(rolememberships).flatten]
  end

  def test_collaborators_add_members
    m = "1"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    siteurl = @s.url_for("/sites/#{siteid}.json")
    sitecreator = create_user("testuser#{m}")
    collaborator = create_user("testcollaborator#{m}")
    viewer = create_user("testviewer#{m}")
    nonmember = create_user("testnonmember#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    # These next checks rely on the current site authz and
    # template defaults and need to be updated if they change.
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    rolemap = get_rolemap(props)
	# These groups will have been created by the site create mechanism.
	# If this doesnt happen correctly then things like the Home folder wont be configured
	# Correctly and some of the actions that follow will fail. (eg updating managers or viewers)
    collaborators = SlingUsers::Group.new(rolemap["Collaborator"])
    viewers = SlingUsers::Group.new(rolemap["Viewer"])

	# Check if the groups exist and what is the  structure.
	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(collaborators.name) + ".tidy.json"))
	@log.info("Collaborators should have group-managers of #{sitecreator.name} and #{collaborators.name} ")
	@log.debug(res.body)
	props = JSON.parse(res.body)
	assert(props["properties"]["rep:group-managers"].include?(collaborators.name))
	assert(props["properties"]["rep:group-managers"].include?(sitecreator.name))
	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(viewers.name) + ".tidy.json"))
	@log.info("Viewers should have group-managers containing #{collaborators.name} ")
	@log.debug(res.body)
	props = JSON.parse(res.body)
	assert(props["properties"]["rep:group-managers"].include?(collaborators.name))
	assert(props["properties"]["rep:group-managers"].include?(sitecreator.name))

	# adding collaborator as a member of collaborators
    collaborators.add_member(@s, collaborator.name, "user")

	#check that the collaborator user and sitecreator user are both members of the collaborators group,
    # and so should have management of both the collaborators group and the viewers group.
	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(collaborators.name) + ".tidy.json"))
	@log.info("Collaborators Should now contain #{collaborator.name} as a member")
	@log.debug(res.body)
	props = JSON.parse(res.body)
	assert(props["members"].include?(collaborator.name))
	assert(props["members"].include?(sitecreator.name))

	collaborators.update_properties(@s,{"rep:group-managers" => [ collaborator.name ]})
	viewers.update_properties(@s,{"rep:group-managers" => [ collaborator.name ]})

	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(collaborators.name) + ".tidy.json"))
	@log.info("Collaborators should have group-managers of #{sitecreator.name} and #{collaborators.name} ")
	@log.debug(res.body)
	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(viewers.name) + ".tidy.json"))
	@log.info("Viewers should have group-managers containing #{collaborators.name} ")
	@log.debug(res.body)


	@log.info("As far as I can tell, #{collaborator.name} is a member of #{collaborators.name} which is a manager of both  #{collaborators.name} and #{viewers.name} ")
	@log.info("So the following tests where we switch to #{collaborator.name} and add #{viewer.name} as a member should be Ok")


    @s.switch_user(collaborator)

	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(viewers.name) + ".markingtherequest.json"))


    res = viewers.add_member(@s, viewer.name, "user")
    assert_equal("200", res.code, "Collaborator should be able to add members as a member of Collaborators which manage the viewers group")


    @s.switch_user(viewer)
    res = viewers.add_member(@s, nonmember.name, "user")
    assert_not_equal("200", res.code, "Viewers should not be able to add members")
  end

  def test_groupchanges
    m = "1a"+Time.now.to_i.to_s
    viewer = create_user("testviewer#{m}")
    collaborator = create_user("testcollaborator#{m}")
    nonmember = create_user("testnonmember#{m}")
    @s.switch_user(collaborator)
    collaborators = create_group("g-collaborator#{m}")
    viewers = create_group("g-viewer#{m}")

	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(collaborators.name) + ".tidy.json"))
	@log.debug(res.body)
	res = @s.execute_get(@s.url_for(SlingUsers::Group.url_for(viewers.name) + ".tidy.json"))
	@log.debug(res.body)

    res = collaborators.add_member(@s, collaborator.name, "user")
    assert_equal("200", res.code, "Collaborators should be able to add members")
    res = viewers.add_member(@s, viewer.name, "user")
    assert_equal("200", res.code, "Collaborators should be able to add members")
    @s.switch_user(viewer)
    res = viewers.add_member(@s, nonmember.name, "user")
    assert_not_equal("200", res.code, "Viewers should not be able to add members")
  end

  def test_access_schemes
    m = "2"+Time.now.to_i.to_s
    siteid = "testsite2#{m}"
    sitename = "Test Site #{m}"
    siteurl = @s.url_for("/sites/#{siteid}.json")
    sitecreator = create_user("testuser#{m}")
    collaborator = create_user("testcollaborator#{m}")
    viewer = create_user("testviewer#{m}")
    nonmember = create_user("testnonmember#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    # These next checks rely on the current site authz and
    # template defaults and need to be updated if they change.
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    rolemap = get_rolemap(props)
    collaborators = SlingUsers::Group.new(rolemap["Collaborator"])
    viewers = SlingUsers::Group.new(rolemap["Viewer"])
    collaborators.add_member(@s, collaborator.name, "user")
    viewers.add_member(@s, viewer.name, "user")

    assert_equal("online", props["status"]);
    assert_equal("everyone", props["access"]);
    res = Net::HTTP.get_response(URI.parse(siteurl))
    assert_equal("200", res.code, "Anonymous users should have read access")
    res = Net::HTTP.post_form(URI.parse(siteurl), {"testprop" => "testval"})
    assert_not_equal("200", res.code, "Anonymous users should not have write access")

    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "status" => "offline")
    res = Net::HTTP.get_response(URI.parse(siteurl))
    assert_not_equal("200", res.code, "Anonymous users should no longer have read access")
    @s.switch_user(nonmember)
    res = @s.execute_get(siteurl)
    assert_not_equal("200", res.code, "Non-members should no longer have read access")
    @s.switch_user(viewer)
    res = @s.execute_get(siteurl)
    assert_not_equal("200", res.code, "Viewers should no longer have read access")
    @s.switch_user(collaborator)
    res = @s.execute_get(siteurl)
    assert_equal("200", res.code, "Collaborators should have read access to offline sites")
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "testprop" => "testval")
    assert_equal("200", res.code, "Collaborators should have write access to offline sites")

    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "status" => "online")
    res = Net::HTTP.get_response(URI.parse(siteurl))
    assert_equal("200", res.code, "Anonymous users should have regained read access")
    res = Net::HTTP.post_form(URI.parse(siteurl), {"testprop" => "testval"})
    assert_not_equal("200", res.code, "Anonymous users should not have write access")

    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "access" => "sakaiUsers")
    res = Net::HTTP.get_response(URI.parse(siteurl))
    assert_not_equal("200", res.code, "Anonymous users should no longer have read access")
    @s.switch_user(nonmember)
    res = @s.execute_get(siteurl)
    assert_equal("200", res.code, "Non-members should have read access")
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "testprop" => "testval")
    assert_not_equal("200", res.code, "Non-members should not have write access")

    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "access" => "invite")
    @s.switch_user(nonmember)
    res = @s.execute_get(siteurl)
    assert_not_equal("200", res.code, "Non-members should no longer have read access")
    @s.switch_user(viewer)
    res = @s.execute_get(siteurl)
    assert_equal("200", res.code, "Viewers should have read access")
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "testprop" => "testval")
    assert_not_equal("200", res.code, "Viewers should not have write access")
    @s.switch_user(collaborator)
    res = @s.execute_get(siteurl)
    assert_equal("200", res.code, "Collaborators should have read access")
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "testprop" => "testval")
    assert_equal("200", res.code, "Collaborators should have write access")
  end

  def test_retain_default_groups
    m = "3"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    collaborator = create_user("testcollaborator#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    props = JSON.parse(res.body)
    rolemap = get_rolemap(props)
    collaborators = SlingUsers::Group.new(rolemap["Collaborator"])
    viewers = SlingUsers::Group.new(rolemap["Viewer"])

    newname = "New Name for Test Site #{m}"
    @s.switch_user(collaborator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"),
      "name" => newname)
    assert_not_equal("200", res.code, "Expected not to change site " + res.body)

    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/system/userManager/group/#{collaborators.name}.update.html"),
      ":member" => collaborator.name)
    assert_equal("200", res.code, "Expected to add user to default collaborator group " + res.body)
    @s.switch_user(collaborator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"),
      "name" => newname)
    assert_equal("200", res.code, "Now should be able to change site " + res.body)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    @log.debug res.body
    props = JSON.parse(res.body)
    assert_equal(newname, props["name"])
  end

  def test_creator_membership
    m = "4"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    props = JSON.parse(res.body)
    rolemap = get_rolemap(props)
    collaborators = SlingUsers::Group.new(rolemap["Collaborator"])
    assert(collaborators.has_member(@s, sitecreator.name), "Site creator should be in collaborator membership")
  end

  def test_site_properties_on_create
    m = "5"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    assert_equal("200", res.code, "Expected to create site: #{res.body}")
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    @log.debug res.body
    props = JSON.parse(res.body)
    assert_equal("/var/templates/sitetest/systemtemplate", props["sakai:site-template"])
    assert_equal(sitename, props["name"])
    assert_equal(sitename, props["description"])
  end

  def test_is_maintainer
    m = "6"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    siteurl = @s.url_for("/sites/#{siteid}.json")
    sitecreator = create_user("testuser#{m}")
    collaborator = create_user("testcollaborator#{m}")
    viewer = create_user("testviewer#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    rolemap = get_rolemap(props)
    collaborators = SlingUsers::Group.new(rolemap["Collaborator"])
    viewers = SlingUsers::Group.new(rolemap["Viewer"])
    collaborators.add_member(@s, collaborator.name, "user")
    viewers.add_member(@s, viewer.name, "user")
    @s.switch_user(collaborator)
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    assert(props[":isMaintainer"], "Collaborators should have site management access")
    @s.switch_user(collaborator)
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    assert(props[":isMaintainer"], "Collaborators should have site management access")
    @s.switch_user(SlingUsers::User.admin_user())
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    assert(props[":isMaintainer"], "Non-member administrators should have site management access")
    @s.switch_user(viewer)
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    assert(!(props[":isMaintainer"]), "Viewers should not have site management access")
  end

  def test_delete_site
    m = "7"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    siterecreator = create_user("testrecreator#{m}")
    siterename = "Redone Test Site #{m}"
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    assert_equal("200", res.code, "Expected to create site: #{res.body}")
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    @s.switch_user(siterecreator)
    res = @s.execute_post(@s.url_for("/sites/#{siteid}.delete.html"))
    assert_not_equal("200", res.code, "Expected non-member not to delete site: #{res.body}")
    @s.switch_user(sitecreator)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    res = @s.execute_post(@s.url_for("/sites/#{siteid}.delete.html"))
    assert_equal("200", res.code, "Expected site creator to delete site: #{res.body}")
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    assert_not_equal("200", res.code, "Expected site to be gone: #{res.body}")
    @s.switch_user(siterecreator)
    res = simple_site_create(siteid, siterename)
    assert_equal("200", res.code, "Expected to recreate site: #{res.body}")
  end

  def test_membership_servlet
    m = "8"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    siteurl = @s.url_for("/sites/#{siteid}.json")
    sitecreator = create_user("testuser#{m}")
    collaborator = create_user("testcollaborator#{m}")
    viewer = create_user("testviewer#{m}")
    nonmember = create_user("testnonmember#{m}")
    @s.switch_user(sitecreator)
    res = simple_site_create(siteid, sitename)
    # These next checks rely on the current site authz and
    # template defaults and need to be updated if they change.
    res = @s.execute_get(siteurl)
    props = JSON.parse(res.body)
    rolemap = get_rolemap(props)
    collaborators = SlingUsers::Group.new(rolemap["Collaborator"])
    viewers = SlingUsers::Group.new(rolemap["Viewer"])
    collaborators.add_member(@s, collaborator.name, "user")
    viewers.add_member(@s, viewer.name, "user")
    @s.switch_user(collaborator)
    res = @s.execute_get(@s.url_for("/system/sling/membership.json"))
    @log.debug res.body
    memberships = JSON.parse(res.body)["results"]
    assert_value(memberships, "siteref", "/sites/#{siteid}", "Expected user to have Collaborator membership")
    @s.switch_user(viewer)
    res = @s.execute_get(@s.url_for("/system/sling/membership.json"))
    @log.debug res.body
    memberships = JSON.parse(res.body)["results"]
    assert_value(memberships, "siteref", "/sites/#{siteid}", "Expected user to have Viewer membership")
    @s.switch_user(nonmember)
    res = @s.execute_get(@s.url_for("/system/sling/membership.json"))
    @log.debug res.body
    memberships = JSON.parse(res.body)["results"]
    assert_equal(0, memberships.size, "Expected non-member to have no site memberships")
  end

  def assert_value(hash, key, value, message)
    found = false
    hash.each {|o|
      if (o[key] == value)
        found = true
      end
    }
    assert(found, message)
  end

  def test_site_name_duplication
    m = "9"+Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator1 = create_user("testuser#{m}")
    sitecreator2 = create_user("testrecreator#{m}")
    sitepath1 = "/#{siteid}"
    sitepath2 = "/parent#{m}/#{siteid}"
    sitedesc1 = "Delightful description #{m}"
    sitedesc2 = "Dreadful description #{m}"
    @s.switch_user(sitecreator1)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => sitepath1,
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => sitename,
      "description" => sitedesc1,
      "id" => siteid)
    assert_equal("200", res.code, "Expected to create site: #{res.body}")
    @s.switch_user(sitecreator2)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => sitepath2,
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => sitename,
      "description" => sitedesc2,
      "id" => siteid)
    assert_equal("200", res.code, "Expected to create site: #{res.body}")
    res = @s.execute_get(@s.url_for("/sites#{sitepath1}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    props1 = JSON.parse(res.body)
    res = @s.execute_get(@s.url_for("/sites#{sitepath2}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    props2 = JSON.parse(res.body)
    assert_equal(props1["name"], props2["name"], "Sites should have same name")
    assert_equal(props2["id"], props2["id"], "Sites should have same ID")
    assert_not_equal(props1["description"], props2["description"], "Sites should have different descriptions")
  end

end

