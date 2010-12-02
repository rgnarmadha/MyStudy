require 'csv'

howmany = ARGV[0].to_i
dictionary = []
words = []
users = []

# load up all the users
CSV.open('netids01.csv', 'r', ',') do |row|
  users << row[0]
end

# load up a big wordlist
file = File.new("/usr/share/dict/words", "r")
while (line = file.gets)
  dictionary << line
end
file.close


# create a subset of the words (in real life, some tags are repeated)
(1..1000).each do
  words << dictionary[rand(dictionary.size())]
end

numwords = words.size()
numusers = users.size()

(1..howmany).each do |i|
  puts "#{users[rand(numusers)]},#{words[rand(numwords)]}"
end
