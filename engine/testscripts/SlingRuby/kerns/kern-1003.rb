#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
include SlingUsers
include SlingFile

class TC_Kern1003Test < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @fm = FileManager.new(@s)
    @um = UserManager.new(@s)
  end

  def test_manager_can_delete_pooled_content
    m = Time.now.to_f.to_s.gsub('.', '');

    # Create some users
    creator = create_user("creator-#{m}")
    manager = create_user("manager-#{m}")

    # Upload a file
    @s.switch_user(creator)
    res = @fm.upload_pooled_file('random.txt', 'This is some random content that should be stored in the pooled content area.', 'text/plain')
    file = JSON.parse(res.body)
    id = file['random.txt']
    url = @fm.url_for_pooled_file(id)

    # Add a manager
    @s.switch_user(creator)
    @fm.manage_members(id, nil, nil, manager.name, nil)

    # Check that our manager can delete the pooled content.
    @s.switch_user(manager)
    res = @s.execute_post(url, {":operation" => "delete"})
    assert_equal(200, res.code.to_i, "Expected to be able to delete the file as a manager.")

    @s.switch_user(creator)
    res = @s.execute_get(url)
    assert_equal(404, res.code.to_i, "The file was not actually deleted")
  end


  def test_manager_can_version_pooled_content
    m = Time.now.to_f.to_s.gsub('.', '');

    # Create some users
    creator = create_user("creator-#{m}")
    manager = create_user("manager-#{m}")

    # Upload a file and save a version
    @s.switch_user(creator)
    res = @fm.upload_pooled_file('random.txt', '1', 'text/plain')
    file = JSON.parse(res.body)
    id = file['random.txt']
    url = @fm.url_for_pooled_file(id)
    res = @s.execute_post(url, { "testing" => "value1" })
    assert_equal("200", res.code)
    res = @s.execute_post(url+".save.json")
    @log.info("File is #{url}")

    # Add a manager
    @s.switch_user(creator)
    @fm.manage_members(id, nil, nil, manager.name, nil)
    assert_equal(200, res.code.to_i, "Expected to be able to manipulate the member lists as a creator.")

    # Try to upload a new version as the manager
    @s.switch_user(manager)
    res = @fm.upload_pooled_file('random.txt', '22', 'text/plain', id)
    assert_equal("200",res.code,res.body)
    @log.info("Update File said #{res.body}")
    res = @s.execute_post(url, { "testing" => "value2" })
    assert_equal("200", res.code)
    res = @s.execute_post(url+".save.json")

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

    res = @s.execute_get(url+".version.,1.1")
    @log.info(res.body)
    assert_equal("22",res.body)
    res = @s.execute_get(url+".version.,1.0")
    @log.info(res.body)
    assert_equal("1",res.body)

    res = @s.execute_get(url+".version.,1.0,.json")
    @log.info(res.body)
    version1 = JSON.parse(res.body)
    assert_equal("value1",version1['testing'])
    res = @s.execute_get(url+".version.,1.1,.json")
    @log.info(res.body)
    version2 = JSON.parse(res.body)
    assert_equal("value2",version2['testing'])
  end

end
