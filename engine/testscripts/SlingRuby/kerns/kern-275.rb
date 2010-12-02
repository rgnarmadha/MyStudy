#!/usr/bin/ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'base64'
require 'sling/test'
include SlingInterface

$image="
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
"

class CropitTest < Test::Unit::TestCase
  include SlingTest
 
  def test_upload_image
    imagedata = upload_image()
    r = @s.execute_get(@s.url_for("/logo_test/logo"))
    assert_equal(imagedata, r.body, "Expected image uploaded")
  end

  def test_crop_image_bad_mime_type
    @s.switch_user(SlingUsers::User.admin_user())
    data = "This is plain text!"
    create_file_node("logo_test", "logo", "logo", data, "text/plain")
    cropreq = { "dimensions" => "250x100;100x50", 
                "height" => 10, 
                "width" => 50, 
                "x" => 15, 
                "y" => 20,
                "img" => "/logo_test/logo", 
                "save" => "/logo_results" }
    res = @s.execute_post(@s.url_for("var/image/cropit"), cropreq)
    assert_equal("406", res.code, "Expected invalid image type")
  end

  def test_crop_image
    @s.switch_user(SlingUsers::User.admin_user())
    upload_image("image/png")
    cropreq = { "dimensions" => "250x100;100x50", 
                "height" => 10, 
                "width" => 50, 
                "x" => 15, 
                "y" => 20,
                "img" => "/logo_test/logo", 
                "save" => "/logo_results" }
    #@s.debug = true
    res = @s.execute_post(@s.url_for("var/image/cropit"), cropreq)
    #@s.debug = false
    assert_equal("200", res.code, "Expected crop to succeed")
    result = JSON.parse(res.body) 
    assert_equal(2, result["files"].size, "Expected two files back")
    result["files"].each do |file|
      assert_not_nil(file, "Expected file not to be nil")
      assert_not_equal("null", file, "Expected file not to be null")
      res = @s.execute_get(@s.url_for(file))
      assert_equal("200", res.code, "Expected image file to exist")
      res = @s.delete_node(file)
      assert_equal("200", res.code, "Expected to be able to delete image")
    end
  end

  private
  def upload_image(mime_type="image/png")
    imagedata = Base64.decode64($image)
    create_file_node("logo_test", "logo", "logo", imagedata, mime_type)
    return imagedata
  end

end

