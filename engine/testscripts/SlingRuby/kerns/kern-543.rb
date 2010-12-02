#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern543Test < Test::Unit::TestCase
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
    
    return siteid
  end
  
  def create_page(sitepath)
    
    m = Time.now.to_i.to_s
    pageid = "id#{m}"
    title = "Page title#{m}"
    
    # Path of the page
    path = "#{sitepath}/_pages/#{pageid}"
    
    #Post params
    params = {
      "_charset_" => "utf-8",
      "acl" => "parent",
      "id" => pageid,
      "position" => rand(1000),
      "sling:resourceType" => "sakai/page",
      "title" => title,
      "type" => "webpage"
    }
    
    res = @s.execute_post(@s.url_for(path), params)
    assert_equal(201,res.code.to_i,"Expectect to create a page, result was #{res.code} ")
    
    return path
  end
  
  def upload_content(pagepath)
    res = @s.execute_file_post(@s.url_for(pagepath), "content", "content", "<p>This is the content of the page<p>", "text/html")    
    assert_equal(200, res.code.to_i,"Expectect to create the pagecontent, result was #{res.code} ")
    return "#{pagepath}/content"
  end
  
  def test_versioning
    m = Time.now.to_i.to_s
    # Create a user
    creatorid = "testuser_creator#{m}"
    creator = create_user(creatorid)
    @s.switch_user(creator)
    
    # Create a site
    sitepath = do_site_create()
    
    # Create a page
    pagepath = create_page(sitepath)
    
    contentpath = upload_content(pagepath)
    
    @log.info"--#{contentpath}--"
    res = @s.execute_post(@s.url_for("#{contentpath}.save.html"))
    assert_equal(200,res.code.to_i,"Expectect to be able to version the content, result was #{res.code} ")
    
    res = @s.execute_get(@s.url_for("#{contentpath}.json"))
    json = JSON.parse(res.body)
    assert_equal(json["sakai:savedBy"], creatorid, "Expected the content node to have some versioning properties.")
  end
  
end

