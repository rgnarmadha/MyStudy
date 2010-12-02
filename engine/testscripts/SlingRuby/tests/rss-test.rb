#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'test/unit.rb'
include SlingInterface
include SlingUsers

class TC_RSSTest < Test::Unit::TestCase
  include SlingTest

  def url_broken_test_valid_rss_file
    # Do a GET request to a valid RSS file.
    @log.info("Getting BBCNews")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://newsrss.bbc.co.uk/rss/newsonline_uk_edition/front_page/rss.xml"})
    @log.info("Done Getting BBCNews")
    assert_equal(200, res.code.to_i, "This is a valid XML file, this should return 200."+res.body)
  end

  def test_regular_file
    # Do a GET request to a non XML file.
    @log.info("Getting Google.com")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://www.google.com"})
    @log.info("Done Getting Google.com")
    assert_equal(500, res.code.to_i, "This is not an XML file. Service should return 403."+res.body)
  end

  def test_invalid_xml_file
    # Do a GET request to a valid XML file but it is not an RSS file.
    @log.info("Getting W3Schools.com")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://www.w3schools.com/xml/note.xml"})
    @log.info("Done Getting W3Schools.com")
    assert_equal(500, res.code.to_i, "This is a plain XML (non-RSS) file. Service should return 403."+res.body)
  end


  def test_big_file
    # Do a GET request to a huge file.
    @log.info("Getting Huge file")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://ftp.belnet.be/packages/apache/sling/org.apache.sling.launchpad.app-5-incubator-bin.tar.gz"})
    @log.info("Done Getting Huge file")
    assert_equal(500, res.code.to_i, "This file is way to big. Service should return 403"+res.body)
  end



end

