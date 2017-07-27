require 'erb'
require 'ostruct'

class Erba < OpenStruct
  def render(template)
    ERB.new(template).result(binding)
  end
end
