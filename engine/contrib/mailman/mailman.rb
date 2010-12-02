#!/usr/bin/ruby

require 'rubygems'
require 'mechanize'

## mmsitepass -c "Your password" to enable list creation
## add OWNERS_CAN_DELETE_THEIR_OWN_LISTS = Yes to /etc/mailman/mm_cfg.py to enable list deletion

MAILMAN_HOST = "example.com"
MAILMAN_PATH = "http://#{MAILMAN_HOST}/cgi-bin/mailman"
PASSWORD = "adminpassword"
NEWLIST_PASSWORD = "newlistpassword"

class MailManager < WWW::Mechanize
  def initialize
    super
    @html_parser = Nokogiri::HTML
    self.user_agent_alias = 'Linux Mozilla'
  end

  def create_list(name, owner)
    get("#{MAILMAN_PATH}/create") do |page|
      form = page.form_with(:action => 'create') do |create|
        create.listname = name
        create.owner = owner
        create.password = NEWLIST_PASSWORD
        create.confirm = NEWLIST_PASSWORD
        create.auth = PASSWORD
      end
      create_result = form.submit(button=form.buttons[0])
      
      puts create_result.inspect
    end
  end

  def delete_list(name)
    get("#{MAILMAN_PATH}/rmlist/#{name}") do |page|
      form = page.form_with(:action => "../rmlist/#{name}") do |remove|
        remove.password = NEWLIST_PASSWORD
      end
      remove_result = form.submit(button=form.buttons[0])
      
      puts remove_result.inspect
    end
  end

end

a = MailManager.new
a.create_list("testlist", "test.email@example.com")
a.delete_list("testlist")

