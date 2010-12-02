#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/sling'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingSearch

class TC_Kern309Test < Test::Unit::TestCase
  include SlingTest


 def do_site_create
    # the location of the site container
    sitecontainerid = "sites"
    # the name of the site (under the container)
    sitename = "/sitetests/"+@m
    # the final id of the site
    @siteid = sitecontainerid+sitename
    # the final url of the site
    @siteurl = @s.url_for(@siteid)
    @log.info("Site id is #{@siteid} ")
    res = create_site(sitecontainerid,"Site "+@m,sitename)
    assert_not_nil(res, "Expected site to be created ")
    @log.info("Site path #{res} ")
        
    res = @s.execute_get(@siteurl+".json");
    assert_equal("200",res.code,"Expectect to get site json at #{@siteurl}.json, result was #{res.body} ")
    @log.debug(res.body)
    props = JSON.parse(res.body)
    assert_equal("sakai/site", props["sling:resourceType"], "Expected resource type to be set")
  end

  def test_309
    @m = Time.now.to_i.to_s
	u = create_user("ian"+@m)
	nico = create_user("nico"+@m)
	@s.switch_user(u)
    do_site_create()
	
	# try and create subnodes.
	
	node = create_node(@siteid+"/tessubnode", {})
	assert_not_nil(node,"expected the child node at #{@siteid}/tessubnode to have been created by the site owner")
	
	# set an acl on the subnode, using an existing group
	@s.set_node_acl_entries(node, nico, { "jcr:removeNode" => "granted",
                                             "jcr:modifyProperties" => "granted",
                                             "jcr:removeChildNodes" => "granted",
                                             "jcr:write" => "granted", 
                                             "jcr:addChildNodes" => "granted" })
	@s.switch_user(nico)
	child = create_node(@siteid+"/tessubnode/nicosubnode", {})
	assert_not_nil(child,"expected the child node at #{@siteid}//tessubnode/nicosubnode to be created by a user granted access")
	
	
  end


end


