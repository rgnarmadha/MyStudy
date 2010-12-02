require 'csv'

howmany = ARGV[0].to_i
users = []
CSV.open('netids01.csv', 'r', ',') do |row|
  users << row[0]
end

size = users.size()

(1..howmany).each do |i|
  puts "#{users[rand(size)]},test"
end
  