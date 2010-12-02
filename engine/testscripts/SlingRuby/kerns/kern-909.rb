#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingSearch

class TC_Kern909Test < Test::Unit::TestCase
  include SlingTest

  #
  # Batch post test
  #

  def test_normal
    m = Time.now.to_i.to_s
    user1 = create_user("user1-"+m)

    @s.switch_user(user1)
    path = user1.private_path_for(@s)
    res = @s.execute_post(@s.url_for("#{path}/test/b"), {"foo" => "bar"})
    res = @s.execute_get(@s.url_for("#{path}/test/b.json"))
    @log.info(res.body)

    # Batch post to private store
    str = [{
          "url" => "#{path}/test/b",
          "method" => "POST",
          "parameters" => {
              "title" => "alfa",
              "foo" => "barnone",
              "unit" => 10,
              "unit@TypeHint" => "Long"
      }
    },
    {
          "url" => "#{path}/test/b.modifyAce.html",
          "method" => "POST",
          "parameters" => {
              "principalId" => "#{user1.name}",
              "privilege@jcr:read" => "granted"
      }
    },
    {
          "url" => "#{path}/test/b.json",
          "method" => "GET"
    }
    ]

    parameters = {
      "requests" => JSON.generate(str)
    }

    res = @s.execute_post(@s.url_for("system/batch"), parameters)

    jsonRes = JSON.parse(res.body)["results"]

    assert_equal(jsonRes[0]["url"], "#{path}/test/b")
    assert_equal(jsonRes[0]["status"], 200, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[1]["url"], "#{path}/test/b.modifyAce.html")
    assert_equal(jsonRes[1]["status"], 200, "Expexted to get a created statuscode. #{jsonRes[1]["body"]} ")
    assert_equal(jsonRes[2]["url"], "#{path}/test/b.json")
    assert_equal(jsonRes[2]["status"], 200, "Expected to get a modified statuscode. #{jsonRes[1]["body"]} ")
    innerBody = jsonRes[2]["body"]
    assert_not_nil(innerBody)
    assert(! innerBody.empty?)
    innerJson = JSON.parse(innerBody)
    assert_equal(innerJson["title"], "alfa", "Expected changed title")
    assert_equal(innerJson["foo"], "barnone", "Expected changed string property")
    assert_equal(innerJson["unit"], 10, "Expected numeric property")
  end

end

