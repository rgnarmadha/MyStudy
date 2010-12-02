#!/usr/bin/env ruby

require 'set'
require 'sling/test'
require 'sling/message'
include SlingSearch
include SlingMessage

class TC_Kern458Test < Test::Unit::TestCase
  include SlingTest
  

  
  def test_getdev
    # We create a test site.
    m = Time.now.to_i.to_s
    testnode = "/test"+m
    res = @s.execute_post(@s.url_for(testnode),
      "badContent1" => "<script",
      "badContent2" => "<script>alert('gotya');</script>",
      "badContent3" => " >alert('gotya'); ",
      "badContent4" => "</script>")
    assert_equal("201",res.code)

    res = @s.execute_get(@s.url_for(testnode+".json"))
    assert_equal("200",res.code)
    @log.debug(res.body)
    props = JSON.parse(res.body)
    assert_equal("<script",props["badContent1"])
    assert_equal("<script>alert('gotya');</script>",props["badContent2"])
    assert_equal(" >alert('gotya'); ",props["badContent3"])
    assert_equal("</script>",props["badContent4"])

    res = @s.execute_get(@s.url_for(testnode+".noxss.json"))
    assert_equal("200",res.code)
    props = JSON.parse(res.body)
    assert_equal("",props["badContent1"],"XSS can happen "+res.body)
    assert_equal("",props["badContent2"],"XSS can happen "+res.body)
    assert_equal(" &gt;alert('gotya');",props["badContent3"],"XSS can happen "+res.body)    

  end

end

