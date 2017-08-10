require_relative 'erba'

if ARGV.size < 1 then
    puts "You must provide a robot!"
    exit 1
end

if ARGV.size < 2 then
    puts "You must provide a version!"
    exit 1
end

props = {
    path: ARGV.shift,
    version: ARGV.shift
}

props[:name] = props[:path].split(".")[-1]

generated = Erba.new(props).render(File.read("Roborio.properties.erb"))
File.open("out/roborio/#{props[:name]}.properties", "w") {|f|
    f.write(generated)
}

generated = Erba.new(props).render(File.read("MANIFEST.MF.erb"))
File.open("MANIFEST.MF", "w"){|f|
    f.write(generated)
}
