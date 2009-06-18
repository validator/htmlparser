require 'mkmf'

# htmlparser dependencies
deps  = with_config('dependencies', '../../dependencies')
icu4j  = with_config('icu4j', "#{deps}/icu4j-4_0.jar")
chardet = with_config('chardet',
  "#{deps}/mozilla/intl/chardet/java/dist/lib/chardet.jar")

# system dependencies
gcj  = with_config('gcj', '/usr/share/java/libgcj.jar')
jaxp = with_config('jaxp', '/usr/share/java/jaxp-1.3.jar')

CONFIG['CC'] = Config::CONFIG['CC'] = 'g++'
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

  # header for the main entry point
  hdb = 'nu/validator/htmlparser/dom/HtmlDocumentBuilder'
  unless find_header("#{hdb}.h", 'headers')

    FileUtils.mkdir_p 'classes'
    cmd = "javac -classpath #{icu4j}:#{chardet} -d classes " +
      "-sourcepath ../src ../src/#{hdb}.java"
    puts cmd
    system cmd

    dest = 'headers/' + hdb + '.h'
    name = hdb.gsub('/','.')
    cmd = "gcjh -cp #{icu4j}:#{chardet}:#{gcj}:classes -o #{dest} #{name}"
    puts cmd
    system cmd

    exit unless find_header("#{hdb}.h", 'headers')
  end

  # ICU and chardet dependencies
  [['icu',icu4j],['chardet',chardet]].each do |name, jar|
    unless find_library("nu-#{name}", 'main', 'lib')
      FileUtils.mkdir_p 'lib'
      cmd = "gcj -shared -fPIC -o lib/libnu-#{name}.so #{jar}"
      puts cmd
      system cmd
      exit unless find_library("nu-#{name}", 'main', 'lib')
    end
  end

  # htmlparser itself (as a shared library)
  unless find_library("nu-htmlparser", 'main', 'lib')
    javas = Dir['../src/**/*.java'].reject {|name| name.include?('/xom/')}
    cmd = "gcj -shared --classpath=#{icu4j}:#{chardet} -fPIC " +
      "-o lib/libnu-htmlparser.so " + javas.join(' ')
    puts cmd
    system cmd
    exit unless find_library("nu-htmlparser", 'main', 'lib')
  end
end

# Ruby library
create_makefile 'nu/validator'
