require_relative 'erba'

if ARGV.empty? then
    puts "You must provide a version!"
    exit 1
end

props = {
    version: ARGV.shift
}

generated = Erba.new(props).render(File.read("Roborio.properties.erb"))
File.open("out/roborio/Roborio.properties", "w") {|f|
    f.write(generated)
}
