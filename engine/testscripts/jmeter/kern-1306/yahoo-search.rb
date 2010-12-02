# usage: ruby yahoo-search.rb <destination path> <html|msword|pdf|ppt|rss|txt|xls> <Yahoo! appid>
# Get your own app id at https://developer.apps.yahoo.com/wsregapp/
require 'rubygems'
require 'json'
require 'net/http'

# load up a big wordlist
dictionary = []
file = File.new("/usr/share/dict/words", "r")
while (line = file.gets)
  dictionary << line.chomp
end
file.close

dest = ARGV[0]
format = ARGV[1]
appid = ARGV[2]

searchterm = dictionary[rand(dictionary.size())]
uri = URI.parse("http://search.yahooapis.com/WebSearchService/V1/webSearch?appid=#{appid}&query=#{searchterm}&format=#{format}&output=json")
response = Net::HTTP.get uri
result = JSON.parse(response)
result['ResultSet']['Result'].each { |result|
  `curl -silent -O "#{result['Url']}"`
}



