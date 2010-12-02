#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
include SlingUsers
include SlingFile

class TC_Kern989Test < Test::Unit::TestCase
  include SlingTest

  #
  # These are test cases for  KERN-989
  #


  def setup
    super
    @fm = FileManager.new(@s)
    @um = UserManager.new(@s)
  end

  def test_manager_users
    m = Time.now.to_i.to_s

    # Create some users
    creator = create_user("creator-#{m}")

    # Upload a file
    @s.switch_user(creator)
    res = @fm.upload_pooled_file('random.txt', '1', 'text/plain')
    file = JSON.parse(res.body)
    id = file['random.txt']
    url = @fm.url_for_pooled_file(id)
    res = @s.execute_post(url, { "testing" => "value1" })
    assert_equal("200", res.code)
    res = @s.execute_post(url+".save.json")
    @log.info("File is #{url}")
    res = @fm.upload_pooled_file('random.txt', '22', 'text/plain', id)
    assert_equal("200",res.code,res.body)
    @log.info("Update File said #{res.body}")
    res = @s.execute_post(url, { "testing" => "value2" })
    assert_equal("200", res.code)
    res = @s.execute_post(url+".save.json")
    res = @s.execute_post(url, { "testing" => "value3" })
    assert_equal("200", res.code)
    res = @fm.upload_pooled_file('random.txt', '333', 'text/plain', id)
    assert_equal("200",res.code,res.body)
    
    
    # Check that versions is working as expected. 
    res = @s.execute_get(url+".versions.json")
    @log.info(res.body)
    versionHistory = JSON.parse(res.body)
    versions = versionHistory['versions']
    assert_not_nil(versions)
    assert_equal(3,versions.size) 
    assert_not_nil(versions['jcr:rootVersion'])
    assert_not_nil(versions['1.0'])
    assert_not_nil(versions['1.1'])

    
    # Check that the properties of a node are working as expected
    res = @s.execute_get(url+".version.,1.1,.json")
    @log.info(res.body)
    version1 = JSON.parse(res.body)
    assert_equal("value2",version1['testing'])

    res = @s.execute_get(url+".version.,1.0,.json")
    @log.info(res.body)
    version1 = JSON.parse(res.body)
    assert_equal("value1",version1['testing'])
    
    res = @s.execute_get(url+".version.,jcr:rootVersion,.json")
    @log.info(res.body)
    versionCurrent = JSON.parse(res.body)
    assert_nil(versionCurrent['testing'])

    # check that the body of a file is ok
    res = @s.execute_get(url+".version.,1.1")
    @log.info(res.body)
   assert_equal("22",res.body)

    res = @s.execute_get(url+".version.,1.0")
    @log.info(res.body)
   assert_equal("1",res.body)
    
    res = @s.execute_get(url+".version.,jcr:rootVersion")
    @log.info(res.body)

 
    res = @s.execute_get(url)
    @log.info(res.body)
   assert_equal("333", res.body)

  end



end
