#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/file'
require 'sling/sites'
require 'sling/message'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingMessage
include SlingFile

class TC_MyFileTest_891 < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @ff = FileManager.new(@s)
    @ss = SiteManager.new(@s)
  end

  def test_upload_file
    m = Time.now.to_i.to_s
    @log.info("Creating user simon"+m)
    simon = create_user("simon"+m)

    # Create a site for each user.
    @s.switch_user(simon)
    @ss.create_site("/sites", title = "Simons Site", sitepath = "/simon#{m}")
    res = @s.execute_get(@s.url_for("sites/simon#{m}.json"))
    assert_equal(200, res.code.to_i(), "Expected to get the site information.")
    site = JSON.parse(res.body)

    publicSimon = simon.public_path_for(@s)

    # Upload a couple of files to the user his public space.
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "alfa", "alfa", "This is some random content: alfaalfa.", "text/plain")
    assert_equal(201, res.code.to_i(), "Expected to be able to upload a file.")
	uploadresult = JSON.parse(res.body)
	alphaID = uploadresult['alfa']
	assert_not_nil(alphaID)

    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "beta", "beta", "This is some random content: betabeta.", "text/plain")
    # This will return modified..
    assert_equal(201, res.code.to_i(), "Expected to be able to upload a file.")
	uploadresult = JSON.parse(res.body)
	betaID = uploadresult['beta']
    assert_not_nil(alphaID)

    # Create a tag.
    res = @ff.createTag("foobar", "#{publicSimon}/tags/footag")
    assert_equal(201, res.code.to_i(), "Expected to be able to create a tag.")
    # Get tag info
    res = @s.execute_get(@s.url_for("#{publicSimon}/tags/footag.json"))
    tag = JSON.parse(res.body)
    assert_not_nil(tag, "No response when creating a tag.")
    assert_not_nil(tag['jcr:uuid'], "Expected a uuid for a tag.")

    # Tag the alfa file.
    res = @ff.tag("/p/#{alphaID}", tag['jcr:uuid'])
    assert_equal(200, res.code.to_i(), "Expected to be able to tag an uploaded file.")

    # Tag a file with a non-existing tag.
    res = @ff.tag("/p/#{betaID}", "foobar")
    assert_equal(404, res.code.to_i(), "Tagging something with a non existing tag should return 404.")

    # Link a file
    res = @ff.createlink("/p/#{alphaID}", "/sites/simon#{m}/_files/alfa", nil)
    assert_equal(200, res.code.to_i(), "Expected to be able to link this thing.")

    # Link a file and associate it with a site.
    res = @ff.createlink("/p/#{alphaID}", "/sites/simon#{m}/_files/alfa", site['jcr:uuid'])
    assert_equal(200, res.code.to_i(), "Expected to be able to link this file.")



    #Try uploading as anonymous
    @log.info("Check that Anon is denied ")
    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "anon", "anon", "This is some random content: anonanon.", "text/plain")
    if ( res.code == "200" )
      assert_equal("-1",res.code,"Expected not be be able to upload a file as anon user "+res.body)
    end

    res = @ff.myfiles("*")
    myfiles = JSON.parse(res.body)
    assert_equal(0, myfiles["total"].to_i(), "Expected 0 files for anon.")

  end

  def old_functionality
    m = Time.now.to_i.to_s
    @log.info("Creating user simon"+m)
    simon = create_user("simon"+m)

    # Create a site for each user.
    @s.switch_user(simon)
    @ss.create_site("/sites", title = "Simons Site", sitepath = "/simon#{m}")

    # Upload 2 files for user simon.
    @s.switch_user(simon)
    res = @ff.upload("/sites/simon/_files/myFile.txt", "/sites/simon/myFile" )
    @log.debug(res.body)
    assert_equal("200", res.code.to_s(), "Expected to upload a file.")
    file = JSON.parse(res.body)

    assert_not_nil(file,"No Response when uploading a file.")
    assert_not_nil(file['files'],"No files array in the output.")
    assert_not_nil(file['files'][0]['filename'],"No filename specified")
    assert_not_nil(file['files'][0]['id'],"No id specified")
    assert_not_nil(file['files'][0]['path'],"No path specified")

    #Get the content and check if it match up.
    res = @ff.download(file['files'][0]['id'])
    assert_equal("200", res.code.to_s(), "Expected to download the file url was (/~#{simon.name}/files/#{file['files'][0]['id']}). "+res.body)
    assert_equal(res.body, "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>", "Content of the file does not match up.")


    #second file.
    res = @ff.upload("/sites/simon/_files/myFile.txt", "/sites/simon/myFile" )



    # Check myfiles search results
    @s.switch_user(simon)
    res = @ff.myfiles("*")
    myfiles = JSON.parse(res.body)
    assert_equal("2", myfiles["total"].to_s(), "Expected 2 files for simon.")


    @log.info("Check that Anon is denied ")
    @s.switch_user(SlingUsers::User.anonymous)
    res = @ff.upload("/sites/simon/_files/anon.txt", "/sites/simon/anon")
    if ( res.code == "200" )
      assert_equal("-1",res.code,"Expected not be be able to upload a file as anon user "+res.body)
    end



  end

  def teardown
    @created_users.each do |user|
      #@s.debug = true
      @s.switch_user(user)
      #@s.debug = false
    end

    @s.switch_user(SlingUsers::User.admin_user())

    @s.delete_file("http://localhost:8080/sites/simon")

    @log.info("Deleted /sites/simon, /sites/ian, /sites/oszkar")
    super
  end

end

