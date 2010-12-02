require 'csv'

users = []
CSV.open('netids01.csv', 'r', ',') do |row|
  users << row[0]
end

size = users.size()

file = File.new("content.txt", "r")
while (line = file.gets)
  path = line.chomp
  mimetype = `file -Ib "#{path}"`.chomp
  puts "#{users[rand(size)]},#{path},#{mimetype}"
end
file.close