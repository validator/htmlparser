require 'nu/validator'

ARGV.each do |arg|
  puts Nu::Validator::parse(open(arg)).encoding
end
