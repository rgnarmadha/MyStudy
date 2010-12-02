#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'test/unit.rb'
include SlingUsers


class TC_Kern854 < Test::Unit::TestCase
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

    # Create a widget in this directory.
    config = {
      "id" => "good",
      "name" => "A good widget"
    }
    @s.create_file_node(widgetsPath + "/good-#{m}", "good-#{m}", "config.json", JSON.dump(config), "application/json")
    @s.create_file_node(widgetsPath + "/good-#{m}", "good-#{m}", "good-#{m}.html", "<html><body>Widget</body></html>", "text/html")
    @s.create_file_node(widgetsPath + "/good-#{m}/javascript", "good-#{m}", "good-#{m}.js", "var foo = function() { alert('bla'); };", "text/plain")
    
    # Create some language bundles
    @s.create_file_node(widgetsPath + "/good-#{m}/bundles", "good-#{m}", "en_UK.json", "{'WELCOME' : 'Welcome'}", "application/json")
    @s.create_file_node(widgetsPath + "/good-#{m}/bundles", "good-#{m}", "nl_BE.json", "{'WELCOME' : 'Welkom'}", "application/json")
    @s.create_file_node(widgetsPath + "/good-#{m}/bundles", "good-#{m}", "fr_BE.json", "{'WELCOME' : 'Bienvenue'}", "application/json")
    
    # Fetch bundled widget
    res = @s.execute_get(@s.url_for(widgetsPath + "/good-#{m}.widgetize.json?locale=nl_BE"))
    assert_equal(200, res.code.to_i)
    json = JSON.parse(res.body)

    # All of the created things should be in there.
    assert_equal("<html><body>Widget</body></html>", json["good-#{m}.html"]["content"])
    assert_equal("var foo = function() { alert('bla'); };", json["javascript"]["good-#{m}.js"]["content"])
    assert_equal("Welkom", json["bundles"]["nl_BE"]["WELCOME"])
    
    # Add a file to the widget and make sure that the cache is updated.
    @s.create_file_node(widgetsPath + "/good-#{m}", "good-#{m}", "foo.json", JSON.dump(config), "application/json")
    
    # Because the widget event listener is async we sleep for a bit
    sleep(2)
    # Fetch bundled widget
    res = @s.execute_get(@s.url_for(widgetsPath + "/good-#{m}.widgetize.json?locale=nl_BE"))
    assert_equal(200, res.code.to_i)
    json = JSON.parse(res.body)
    
    # Assert that the new file is in there.
    assert_not_nil(json["foo.json"])

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
