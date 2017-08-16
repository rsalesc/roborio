require 'nokogiri'
require 'open-uri'

looking_for = ARGV.shift

page = Nokogiri::HTML(open("http://literumble.appspot.com/Rankings?game=roborumble"))

table = page.css("table")[0]
rows = table.css("tr")

my_bot = nil
standings = []
standings_map = {}

rows[1..-1].each do |row|
    columns = row.css("td")
    fn = columns[2].text.strip.split(" ")

    obj = {
        place: columns[0].text.strip.to_i,
        name: columns[2].text.strip,
        botname: fn[0].strip,
        version: fn[1].strip,
        aps: columns[3].text.strip.to_f / 100,
        pwin: columns[4].text.strip.to_f / 100,
        anpp: columns[5].text.strip.to_f / 100,
        vote: columns[6].text.strip.to_f,
        survival: columns[7].text.strip.to_f / 100,
        pairings: columns[8].text.strip.to_i,
        battles: columns[9].text.strip.to_i
    }
    
    if fn[0].strip == looking_for and my_bot.nil? then
        my_bot = obj
    end

    standings_map[obj[:name]] = obj
    standings << obj
end

if my_bot.nil? then
    puts "Didnt find your bot!"
    exit 1
end

my_page = Nokogiri::HTML(open("http://literumble.appspot.com/BotDetails?game=roborumble&name=#{my_bot[:name]}"))

table = my_page.css("table")[1]
rows = table.css("tr")

my_results_map = {}

rows[1..-1].each do |row|
    columns = row.css("td")
    obj = {
        name: columns[2].text.strip,
        aps: columns[4].text.strip.to_f / 100,
        npp: columns[5].text.strip.to_f / 100,
        survival: columns[6].text.strip.to_f / 100
    }

    my_results_map[obj[:name]] = obj
end

length = standings.size
k = (Math.sqrt(length)/2).ceil.to_i

(0...length).each do |i|
    if my_results_map.key?(standings[i][:name]) then
        low = [i - k, 0].max
        high = [i + k + 1, length].min

        many = 0
        sum = 0.0
        (low...high).each do |j|
            if j != i and my_results_map.key?(standings[j][:name]) then
                against = my_results_map[standings[j][:name]]
                sum += against[:aps]
                many += 1
            end
        end

        avg = sum / [many, 1].max
        aps = my_results_map[standings[i][:name]][:aps]
        my_results_map[standings[i][:name]][:knn] = aps - avg
    end 
end
