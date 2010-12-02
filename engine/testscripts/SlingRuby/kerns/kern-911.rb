#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingSearch

class TC_Kern911Test < Test::Unit::TestCase
  include SlingTest

  #
  # Make sure the batch servlet streams HTML file resources correctly.
  #

  def test_batch_fetch_html
    m = Time.now.to_i.to_s
    htmlBody = "<html><body>#{m}</body></html>"
    fileName = "testpage-#{m}.html"
    @s.create_file_node("/", fileName, fileName, htmlBody, "text/html")
    res = @s.execute_get(@s.url_for("/#{fileName}"))
    assert_equal(htmlBody, res.body, "Expected content to upload cleanly")

    # Batch GET of new resource.
    str = [{
          "url" => "/#{fileName}",
          "method" => "GET"
    }
    ]
    parameters = {
      "requests" => JSON.generate(str)
    }
    res = @s.execute_get(@s.url_for("system/batch"), parameters)
    @log.info(res.body)
    jsonRes = JSON.parse(res.body)["results"]
    assert_equal(jsonRes[0]["url"], "/#{fileName}", "Expected the requested URL")
    assert_equal(jsonRes[0]["status"], 200, "Expected to get a successful statuscode. #{jsonRes[0]["body"]} ")
    innerBody = jsonRes[0]["body"]
    assert_equal(htmlBody, innerBody, "Expected the same content as with a non-batched GET")
  end

end
