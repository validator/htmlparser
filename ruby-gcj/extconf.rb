require 'mkmf'

# system dependencies
gcj  = with_config('gcj', '/usr/share/java/libgcj.jar')
jaxp = with_config('jaxp', '/usr/share/java/jaxp-1.3.jar')

CONFIG['CC'] = 'gcj'
Config::CONFIG['CC'] = 'g++'
with_cppflags('-xc++') do

  # headers for JAXP
  unless find_header('org/w3c/dom/Document.h', 'headers')
  
    `jar tf #{jaxp}`.split.each do |file|
      next unless file =~ /\.class$/
      next if file.include? '$'
    
      dest = 'headers/' + file.sub(/\.class$/,'.h')
      name = file.sub(/\.class$/,'').gsub('/','.')
    
      next if File.exist? dest
    
      cmd = "gcjh -cp #{jaxp}:#{gcj} -o #{dest} #{name}"
      puts cmd
      break unless system cmd
    end

    exit unless find_header('org/w3c/dom/Document.h', 'headers')
  end

  find_header 'nu/validator/htmlparser/dom/HtmlDocumentBuilder.h', 'headers'
  find_library 'nu-htmlparser', 'main', 'lib'
end

# Ruby library
create_makefile 'nu/validator'
