#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'set'
require 'sling/test'
require 'sling/message'
include SlingSearch
include SlingMessage

class TC_Kern723Test < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @mm = MessageManager.new(@s)
  end


  def test_message_to_site
    # We create a test site.
    m = Time.now.to_i.to_s
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    sitetemplate = "/var/templates/sitetest/systemtemplate"

    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteid}",
      "sakai:site-template" => sitetemplate,
      "name" => sitename,
      "description" => sitename,
      "id" => siteid)
    assert_equal("200", res.code, "Expected site to be created successfully.")
    res = @s.execute_get(@s.url_for("/sites/#{siteid}/store.json"))
    assert_equal("200", res.code, "Expected a successful response from the server.")
    props = JSON.parse(res.body)
    assert_equal(props["sling:resourceType"], "sakai/messagestore", "Expected to find a sakai/messagestore resource type.")

    # We send a message to the site.
    extra = {
        "sakai:marker" => m,
        "sakai:subject" => "Test message",
        "sakai:body" => "Test body"
    }
    res = @mm.create("internal:s-#{siteid}", "comment", "outbox", extra)
    assert_equal(200, res.code.to_i, "Expected to be able to create a message.")

    sleep(2)
    # Do a search
    res = @s.execute_get(@s.url_for("/var/search/comments/flat.json?marker=#{m}&path=/sites/#{siteid}/store&page=0&items=10"))
    props = JSON.parse(res.body)
    @log.debug res.body
    assert_equal(1, props["total"]);
    assert_equal("Test body", props["results"][0]["post"]["sakai:body"])
  end

end

