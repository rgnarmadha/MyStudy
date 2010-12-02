#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'set'
require 'sling/test'
require 'sling/message'
require 'rexml/document'
require 'rexml/streamlistener'
include REXML
include SlingUsers

class TC_Kern566Test < Test::Unit::TestCase
  include SlingTest

  def setup
    super;
    @now = Time.now.to_f.to_s.gsub('.', '');
    @creator = create_user("creator-test#{@now}");
    @user = create_user("user-test#{@now}");
    @admin = SlingUsers::User.admin_user();
    @anonymous = SlingUsers::User.anonymous();
  end
  
  def teardown
    super;
#    @s.switch_user(@admin);
#    @created_users.each do |user|
#      @log.info(user);
#      puts @um.delete_user("#{user}");
#    end
  end
  
  def test_basiclti
    # We create a test site.
    @now = Time.now.to_f.to_s.gsub('.', '');
    @siteid = "testsite#{@now}";
    @sitename = "Test Site #{@now}";
    
    # create test site
    @s.switch_user(@creator);
    @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{@siteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => @sitename,
      "description" => @sitename,
      "id" => @siteid);
      
    # create a sakai/basiclti node
    @saveUrl = "/sites/#{@siteid}/_widgets/id#{@now}/basiclti";
    @ltiurl = "http://dr-chuck.com/ims/php-simple/tool.php";
    @ltikey = "12345";
    ltisecret = "secret";
    postData = {
      ":operation" => "basiclti",
      "sling:resourceType" => "sakai/basiclti",
      "ltiurl" => @ltiurl,
      "ltikey" => @ltikey,
      "ltisecret" => ltisecret,
      "debug@TypeHint" => "Boolean",
      "debug" => "false",
      "release_names@TypeHint" => "Boolean",
      "release_names" => "true",
      "release_principal_name@TypeHint" => "Boolean",
      "release_principal_name" => "true",
      "release_email@TypeHint" => "Boolean",
      "release_email" => "true"
    };
    resp = @s.execute_post(@s.url_for("#{@saveUrl}"), postData);
    assert_equal(200, resp.code.to_i, "Expected to be able to create a sakai/basiclti node.");
    
    # verify the creator can read all the properties
    resp = @s.execute_get(@s.url_for("#{@saveUrl}"));
    assert_equal(200, resp.code.to_i, "Expected to be able to retrieve sakai/basiclti node.");
    props = JSON.parse(resp.body);
    assert_equal("sakai/basiclti", props["sling:resourceType"]);
    assert_equal(false, props.empty?);
    assert_equal(@ltiurl, props["ltiurl"]);
    assert_equal(@ltikey, props["ltikey"]);
    assert_equal(ltisecret, props["ltisecret"]);
    assert_equal(false, props["debug"]);
    assert_equal(true, props["release_names"]);
    assert_equal(true, props["release_principal_name"]);
    assert_equal(true, props["release_email"]);
    
    # expect normal launch from creator
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, "/sites/#{@siteid}");
    
    # verify creator cannot access data contained in sensitive node
    sensitive = @s.execute_get(@s.url_for("#{@saveUrl}/ltiKeys.json"));
    assert_equal(404, sensitive.code.to_i, "404 Expected on sensitive node.");
    
    # switch to regular user
    @s.switch_user(@user);
    resp = @s.execute_get(@s.url_for("#{@saveUrl}"));
    assert_equal(200, resp.code.to_i, "Expected to be able to retrieve sakai/basiclti node.");
    props = JSON.parse(resp.body);
    assert_equal("sakai/basiclti", props["sling:resourceType"]);
    assert_equal(false, props.empty?);
    assert_equal(@ltiurl, props["ltiurl"]);
    # normal user should not be able to read ltiurl value
    assert_equal(nil, props["ltikey"]);
    # normal user should not be able to read ltikey value
    assert_equal(nil, props["ltisecret"]);
    assert_equal(false, props["debug"]);
    assert_equal(true, props["release_names"]);
    assert_equal(true, props["release_principal_name"]);
    assert_equal(true, props["release_email"]);

    # expect normal launch from user
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, "/sites/#{@siteid}");
    
    # verify user cannot access data contained in sensitive node
    sensitive = @s.execute_get(@s.url_for("#{@saveUrl}/ltiKeys.json"));
    assert_equal(404, sensitive.code.to_i, "404 Expected on sensitive node.");

    # switch to admin user
    @s.switch_user(@admin);
    resp = @s.execute_get(@s.url_for("#{@saveUrl}"));
    assert_equal(200, resp.code.to_i, "Expected to be able to retrieve sakai/basiclti node.");
    props = JSON.parse(resp.body);
    assert_equal("sakai/basiclti", props["sling:resourceType"]);
    assert_equal(false, props.empty?);
    assert_equal(@ltiurl, props["ltiurl"]);
    assert_equal(@ltikey, props["ltikey"]);
    assert_equal(ltisecret, props["ltisecret"]);
    assert_equal(false, props["debug"]);
    assert_equal(true, props["release_names"]);
    assert_equal(true, props["release_principal_name"]);
    assert_equal(true, props["release_email"]);

    # verify admin *can* access data contained in sensitive node
    sensitive = @s.execute_get(@s.url_for("#{@saveUrl}/ltiKeys.json"));
    assert_equal(200, sensitive.code.to_i, "200 Expected on sensitive node.");
    sprops = JSON.parse(sensitive.body);
    assert_equal(false, sprops.empty?);
    assert_equal(@ltikey, sprops["ltikey"]);
    assert_equal(ltisecret, sprops["ltisecret"]);

    # expect normal launch from admin
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, "/sites/#{@siteid}");
    end

  def test_basiclti_virtualTool
    @now = Time.now.to_f.to_s.gsub('.', '');
    @siteid = "testsite#{@now}";
    @sitename = "Test Site #{@now}";
    
    # verify anonymous user cannot read /var/basiclti
    @s.switch_user(@anonymous);
    resp = @s.execute_get(@s.url_for("/var.json"));
    assert_equal(200, resp.code.to_i, "Should be able to read /var.");
    resp = @s.execute_get(@s.url_for("/var/basiclti.json"));
    assert_equal(404, resp.code.to_i, "Should NOT be able to read /var/basiclti.");
    resp = @s.execute_get(@s.url_for("/var/basiclti/sakai.singleuser.json"));
    assert_equal(404, resp.code.to_i, "Should NOT be able to read /var/basiclti/sakai.singleuser.");
    resp = @s.execute_get(@s.url_for("/var/basiclti/sakai.singleuser/ltiKeys.json"));
    assert_equal(404, resp.code.to_i, "Should NOT be able to read /var/basiclti/sakai.singleuser/ltiKeys.json.");
    
    # verify normal user cannot read /var/basiclti
    @s.switch_user(@creator);
    resp = @s.execute_get(@s.url_for("/var.json"));
    assert_equal(200, resp.code.to_i, "Should be able to read /var.");
    resp = @s.execute_get(@s.url_for("/var/basiclti.json"));
    assert_equal(404, resp.code.to_i, "Should NOT be able to read /var/basiclti.");
    resp = @s.execute_get(@s.url_for("/var/basiclti/sakai.singleuser.json"));
    assert_equal(404, resp.code.to_i, "Should NOT be able to read /var/basiclti/sakai.singleuser.");
    resp = @s.execute_get(@s.url_for("/var/basiclti/sakai.singleuser/ltiKeys.json"));
    assert_equal(404, resp.code.to_i, "Should NOT be able to read /var/basiclti/sakai.singleuser/ltiKeys.json.");
    
    # verify admin user can read /var/basiclti
    @s.switch_user(@admin);
    resp = @s.execute_get(@s.url_for("/var.json"));
    assert_equal(200, resp.code.to_i, "Admin should be able to read /var.");
    resp = @s.execute_get(@s.url_for("/var/basiclti.json"));
    assert_equal(200, resp.code.to_i, "Admin should be able to read /var/basiclti.");
    resp = @s.execute_get(@s.url_for("/var/basiclti/sakai.singleuser.json"));
    assert_equal(200, resp.code.to_i, "Admin should be able to read /var/basiclti/sakai.singleuser.");
    resp = @s.execute_get(@s.url_for("/var/basiclti/sakai.singleuser/ltiKeys.json"));
    assert_equal(200, resp.code.to_i, "Admin should be able to read /var/basiclti/sakai.singleuser/ltiKeys.json.");
    
    # create test site
    @s.switch_user(@creator);
    @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{@siteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => @sitename,
      "description" => @sitename,
      "id" => @siteid);
      
    # create a sakai/basiclti VirtualTool node
    @saveUrl = "/sites/#{@siteid}/_widgets/id#{@now}/basiclti";
    @ltiurl = "http://localhost/imsblti/provider/sakai.singleuser"; # in the policy file
    @ltikey = "12345"; # in the policy file
    postData = {
      "lti_virtual_tool_id" => "sakai.singleuser",
      ":operation" => "basiclti",
      "sling:resourceType" => "sakai/basiclti"
    };
    resp = @s.execute_post(@s.url_for("#{@saveUrl}"), postData);
    assert_equal(200, resp.code.to_i, "Expected to be able to create a sakai/basiclti node.");
    
    # verify the creator can read all the properties
    resp = @s.execute_get(@s.url_for("#{@saveUrl}"));
    assert_equal(200, resp.code.to_i, "Expected to be able to retrieve sakai/basiclti node.");
    props = JSON.parse(resp.body);
    assert_equal("sakai/basiclti", props["sling:resourceType"]);
    assert_equal(false, props.empty?);
    
    # expect normal launch from creator
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, "/sites/#{@siteid}");
    
    # verify creator cannot access data contained in sensitive node
    sensitive = @s.execute_get(@s.url_for("#{@saveUrl}/ltiKeys.json"));
    assert_equal(404, sensitive.code.to_i, "404 Expected on sensitive node.");
    
    # switch to regular user
    @s.switch_user(@user);
    resp = @s.execute_get(@s.url_for("#{@saveUrl}"));
    assert_equal(200, resp.code.to_i, "Expected to be able to retrieve sakai/basiclti node.");
    props = JSON.parse(resp.body);
    assert_equal("sakai/basiclti", props["sling:resourceType"]);
    assert_equal(false, props.empty?);
    assert_equal(@ltiurl, props["ltiurl"]);
    # normal user should not be able to read ltiurl value
    assert_equal(nil, props["ltikey"]);
    # normal user should not be able to read ltikey value
    assert_equal(nil, props["ltisecret"]);
    assert_equal(false, props["debug"]);
    assert_equal(true, props["release_names"]);
    assert_equal(true, props["release_principal_name"]);
    assert_equal(true, props["release_email"]);

    # expect normal launch from user
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, "/sites/#{@siteid}");
    
    # verify user cannot access data contained in sensitive node
    sensitive = @s.execute_get(@s.url_for("#{@saveUrl}/ltiKeys.json"));
    assert_equal(404, sensitive.code.to_i, "404 Expected on sensitive node.");

    # switch to admin user
    @s.switch_user(@admin);
    resp = @s.execute_get(@s.url_for("#{@saveUrl}"));
    assert_equal(200, resp.code.to_i, "Expected to be able to retrieve sakai/basiclti node.");
    props = JSON.parse(resp.body);
    assert_equal("sakai/basiclti", props["sling:resourceType"]);
    assert_equal(false, props.empty?);
    assert_equal(@ltiurl, props["ltiurl"]);
    assert_equal(false, props["debug"]);
    assert_equal(true, props["release_names"]);
    assert_equal(true, props["release_principal_name"]);
    assert_equal(true, props["release_email"]);

    # verify 404 on sensitive node
    sensitive = @s.execute_get(@s.url_for("#{@saveUrl}/ltiKeys.json"));
    assert_equal(404, sensitive.code.to_i, "There should be no sensitive node for a virtual tool.");

    # expect normal launch from admin
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, "/sites/#{@siteid}");
  end
  
  def test_basicltiTrustedContextId
    # We create a test site.
    m = Time.now.to_f.to_s.gsub('.', '');
    @siteid = "testsite#{@now}";
    @sitename = "Test Site #{@now}";
    
    # create test site
    @s.switch_user(@creator);
    lti_context_id = "#{@now}";
    @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{@siteid}",
      "sakai:site-template" => "/var/templates/sitetest/systemtemplate",
      "name" => @sitename,
      "description" => @sitename,
      "id" => @siteid,
      "lti_context_id" => lti_context_id);
      
    # create a sakai/basiclti node
    @saveUrl = "/sites/#{@siteid}/_widgets/id#{@now}/basiclti";
    @ltiurl = "http://dr-chuck.com/ims/php-simple/tool.php";
    @ltikey = "12345";
    ltisecret = "secret";
    postData = {
      ":operation" => "basiclti",
      "sling:resourceType" => "sakai/basiclti",
      "ltiurl" => @ltiurl,
      "ltikey" => @ltikey,
      "ltisecret" => ltisecret,
      "debug@TypeHint" => "Boolean",
      "debug" => "false",
      "release_names@TypeHint" => "Boolean",
      "release_names" => "true",
      "release_principal_name@TypeHint" => "Boolean",
      "release_principal_name" => "true",
      "release_email@TypeHint" => "Boolean",
      "release_email" => "true"
    };
    resp = @s.execute_post(@s.url_for("#{@saveUrl}"), postData);
    assert_equal(200, resp.code.to_i, "Expected to be able to create a sakai/basiclti node.");
    
    # expect normal launch from creator
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, lti_context_id);
    
    # switch to regular user
    @s.switch_user(@user);
    # expect normal launch from user
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, lti_context_id);
    
    # switch to admin user
    @s.switch_user(@admin);
    # expect normal launch from admin
    launch = @s.execute_get(@s.url_for("#{@saveUrl}.launch.html"));
    assert_equal(200, launch.code.to_i, "200 Expected on launch.");
    assert_equal(false, launch.body.empty?);
    validateHtml(launch.body, lti_context_id);
  end

  def validateHtml(html, context_id)
    listener = Listener.new;
    parser = Parsers::StreamParser.new(html, listener);
    parser.parse;
    hash = listener.hash;
    assert_equal(hash["form.action"], @ltiurl, "Form action should equal ltiurl");
    assert_equal(hash["context_id"], context_id, "context_id should equal path to #{context_id}");
    assert_equal(hash["context_label"], @siteid, "context_label should equal path to site id");
    assert_equal(hash["context_title"], @sitename, "context_title should equal path to site name");
    assert_equal(hash["context_type"].empty?, false, "context_type should not be empty");
    assert_equal(hash["oauth_consumer_key"], @ltikey, "oauth_consumer_key should equal ltikey");
    assert_equal(hash["resource_link_id"], @saveUrl, "resource_link_id should equal saveUrl");
    assert_equal(hash["roles"].empty?, false, "roles should not be empty");
    assert_equal(hash["tool_consumer_instance_contact_email"].empty?, false, "tool_consumer_instance_contact_email should not be empty");
    assert_equal(hash["tool_consumer_instance_description"].empty?, false, "tool_consumer_instance_description should not be empty");
    assert_equal(hash["tool_consumer_instance_guid"].empty?, false, "tool_consumer_instance_guid should not be empty");
    assert_equal(hash["tool_consumer_instance_name"].empty?, false, "tool_consumer_instance_name should not be empty");
    assert_equal(hash["tool_consumer_instance_url"].empty?, false, "tool_consumer_instance_url should not be empty");
    assert_equal(hash["user_id"].empty?, false, "user_id should not be empty");
  end

  class Listener
    include StreamListener

    def initialize
      # Use instance variables to mantain information
      # between different callbacks.
      @hash = {}
    end

    def tag_start(name, attributes)
      if(name == "form")
        action = attributes["action"];
        @hash["form.action"] = action;
      end
      if(name == "input")
        name = attributes["name"];
        value = attributes["value"];
        @hash["#{name}"] = value;
      end
    end
    
    def hash
      @hash
    end

  end

end

