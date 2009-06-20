require 'nu/validator'

ARGV.each do |arg|
  puts Nu::Validator::parse(arg).encoding
end
