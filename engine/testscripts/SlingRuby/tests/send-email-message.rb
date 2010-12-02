#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'rubygems'
require 'daemons'
require 'mailtrap'

require 'sling/sling'
require 'sling/test'
require 'sling/message'
require 'test/unit.rb'

include SlingMessage

class TC_OutgoingMessage < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @mm = MessageManager.new(@s)

    mailtrap('start')
  end

  def teardown
    # mailtrap should be set to receive just once, so it will stop the process
    # automaticlly after that.
    #mailtrap('stop')
  end

  def test_send_message
    m = Time.now.to_i.to_s
    user = "auser#{m}"
    a = @um.create_user(user)

    @log.info "Sending mail to user #{user}"
    @mm.create("smtp:#{user}@example.com", 'smtp', 'outbox')
  end

  def mailtrap(state)
    options = {
      :ARGV => [state, '-f', '--'],
      :dir_mode => :normal,
      :dir => '/var/tmp',
      :multiple => true,
      :mode => :exec,
      :backtrace => true,
      :log_output => true
    }

    host = 'localhost'
    port = 25
    once = true
    log_file = '/var/tmp/mailtrap.log'

    Daemons.run_proc( 'mailtrap', options ) do
      Mailtrap.new(host, port, once, log_file)
    end
  end
end

