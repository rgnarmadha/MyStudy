#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingUsers


class TC_Kern857 < Test::Unit::TestCase
  include SlingTest

  def test_widgetlisting
    m = Time.now.to_i.to_s

    # Store the original widget settings somewhere.
    @widget_options = getWidgetServiceConfiguration()
    widgetsPath = "/testarea/widgets-#{m}/widgets"
    folders = @widget_options["sakai.batch.widgets.widget_folders"]["values"]

    # Add our own widgets to it.
    folders.concat([widgetsPath])
    setWidgetServiceConfigurationPath(folders)

    # Create a couple of widgets in this directory.
    config = {
      "id" => "good",
      "name" => "A good widget"
    }
    @s.create_file_node(widgetsPath + "/good-#{m}", "good-#{m}", "config.json", JSON.dump(config), "application/json")
    @s.create_file_node(widgetsPath + "/bad-#{m}", "bad-#{m}", "config.json", "This is not JSON", "application/json")

    # Fetch aggregate
    res = @s.execute_get(@s.url_for("/var/widgets"))
    assert_equal(200, res.code.to_i)
    json = JSON.parse(res.body)

    # The good widget and all it's properties should be in there
    assert_not_nil(json["good-#{m}"])
    assert_equal("good", json["good-#{m}"]["id"])
    assert_equal("A good widget", json["good-#{m}"]["name"])

    # The bad widget cannot be in there.
    assert_equal(nil, json["bad-#{m}"])
    
    # Add a widget and make sure that the cache is updated.
    @s.create_file_node(widgetsPath + "/second-#{m}", "second-#{m}", "config.json", JSON.dump(config), "application/json")


   # sleep just long enough for the widget to be updated.
   sleep(1)
    
    # Fetch new aggregate
    res = @s.execute_get(@s.url_for("/var/widgets"))
    assert_equal(200, res.code.to_i)
    json = JSON.parse(res.body)
    
    # Assert that the new widget is in there.
    assert_not_nil(json["second-#{m}"])

  end

  def setWidgetServiceConfigurationPath(folders)
    params = {
      "action" => "ajaxConfigManager",
      "apply" => true,
      "propertylist" => "sakai.batch.widgets.widget_folders",
      "sakai.batch.widgets.widget_folders" => folders
    }

    @s.execute_post("http://localhost:8080/system/console/configMgr/org.sakaiproject.nakamura.batch.WidgetServiceImpl", params)
  end

  def getWidgetServiceConfiguration
    @s.switch_user(User.admin_user)
    # Get the config options
    res = @s.execute_post("http://localhost:8080/system/console/configMgr/org.sakaiproject.nakamura.batch.WidgetServiceImpl", {})
    assert_equal(200, res.code.to_i)
    return JSON.parse(res.body)
  end

  def teardown
    # Reset the widget service again.
    setWidgetServiceConfigurationPath(@widget_options["sakai.batch.widgets.widget_folders"]["values"])
  end

end
