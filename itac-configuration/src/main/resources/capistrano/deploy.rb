# Much mangled cut and paste job on capistrano's deploy recipe that is too much rails to deal with.


set :application, "International Time Allocation Committee"
set :repository,  "http://source.gemini.edu/software/time-allocation-committee" 
set :scm, :subversion
set :deploy_to, "/gemsoft/opt/tac/jetty/"
set :user, "software"
set :use_sudo, false

role :web, "hbf-qa-tac"
role :app, "hbf-qa-tac"
role :db, "hbf-qa-tac-db", :primary => true

def _cset(name, *args, &block)
  unless exists?(name)
    set(name, *args, &block)
  end
end

[:application, :repository, :scm, :deploy_to, :user, :use_sudo].each do | s |
  _cset(s)
end

_cset(:release_name)      { Time.now.utc.strftime("%Y%m%d%H%M%S") }

_cset :version_dir,       "releases"
_cset :current_dir,       "webapps"

_cset(:releases_path)     { File.join(deploy_to, version_dir) }
_cset(:current_path)      { File.join(deploy_to, current_dir) }
_cset(:release_path)      { File.join(releases_path, release_name) }

_cset(:releases)          { capture("ls -xt #{releases_path}").split.reverse }
_cset(:current_release)   { File.join(releases_path, releases.last) }
_cset(:previous_release)  { File.join(releases_path, releases[-2]) }

now_path = Time.now.strftime("%Y%m%d%H%M%S")
dirs = [deploy_to, release_path]
try_sudo = (use_sudo) ? 'sudo ' : ''

namespace :qa do
  QaAppHost = "hbf-qa-tac"
  QaDbHost = "hbf-qa-tac-db"

  task :default do
    setup
    update
  end

  desc "Setup the next release directory"
  task :setup, :hosts => QaAppHost do
    run "#{try_sudo} mkdir -p #{dirs.join(' ')} && #{try_sudo} chmod g+w #{dirs.join(' ')}"
    run "ls -la #{ deploy_to }"
  end

  desc <<-DESC
    Copies the project and updates the symlink. It does this in a \
    transaction, so that if either `update_code' or `symlink' fail, all \
    changes made to the remote servers will be rolled back, leaving your \
    system in the same state it was in before `update' was invoked. Usually, \
    you will want to call `deploy' instead of `update', but `update' can be \
    handy if you want to deploy, but not immediately restart your application.
  DESC
  task :update, :hosts => QaAppHost do
    transaction do
      update_version
      build_qa_war
      copy_war
      symlink
    end 
  end 

  desc <<-DESC
    Copies your project to the remote servers. 
  DESC
  task :copy_war, :hosts => "hbf-qa-tac" do
    on_rollback { run "rm -rf #{release_path}; true" }

    system("scp ./itac-web/target/itac-web-*.war #{ user }@#{ QaAppHost }:#{ current_release }/tac.war")

    symlink
  end

  desc <<-DESC
    Updates the symlink to the most recently deployed version. Capistrano works \                                                      
    by putting each new release of your application in its own directory. When \
    you deploy a new version, this task's job is to update the `current' symlink \
    to point at the new version. You will rarely need to call this task \
    directly; instead, use the `deploy' task (which performs a complete \
    deploy, including `restart') or the 'update' task (which does everything \
    except `restart').
  DESC
  task :symlink, :hosts => QaAppHost do
    on_rollback { run "rm -f #{current_path}; ln -s #{previous_release} #{current_path}; true" }                                       
    run "rm -f #{current_path} && ln -s #{current_release} #{current_path}"
  end

  desc <<-DESC
    Stops the QA app server.
  DESC
  task :stop_app, :hosts => QaAppHost do
    run "/gemsoft/opt/tac/jetty/bin/jetty.sh stop"
  end

  desc <<-DESC
    Starts the QA app server.
  DESC
  task :start_app, :hosts => QaAppHost do
    run "/gemsoft/opt/tac/jetty/bin/jetty.sh start"
  end

  desc <<-DESC
    Restarts the QA app server.
  DESC
  task :bounce_app, :hosts => QaAppHost do
    run "/gemsoft/opt/tac/jetty/bin/jetty.sh restart"
  end

  desc <<-DESC
    Executes new release on DB.  Updates db scripts and then runs initialize.
  DESC
  task :db_release, :hosts => QaDbHost do
    transaction do
      update_db_scripts
      db_init
    end 
    run "cd /gemsoft/var/tac/database; svn update --password S0ftware --no-auth-cache"
  end
  
  desc <<-DESC
    Updates database scripts on remote host
  DESC
  task :update_db_scripts, :hosts => QaDbHost do
    run "cd /gemsoft/var/tac/database; svn update --password S0ftware --no-auth-cache"
  end

  desc <<-DESC
    Re-initialize database
  DESC
  task :db_init, :hosts => QaDbHost do
    run "cd /gemsoft/var/tac/database; ./initialize_database.sh itac itac_qa base qa_data;"
  end

  desc <<-DESC
    Updates the versions in two files to the version specified at the command line \
    Should look something like cap qa:update_version version=.4
  DESC
  task :update_version do
    version = ENV['version']
    puts "Updating to version #{ENV['version']}"
    app_controller = './itac-web/src/main/java/edu/gemini/tac/itac/web/AbstractApplicationController.java'
    java_version_string = "\tprotected static final String VERSION = \"#{ version } #{ now_path }\";"

    system("sed -i \"\" 's/.*String VERSION.*/#{ java_version_string }/g' itac-web/src/main/java/edu/gemini/tac/itac/web/AbstractApplicationController.java")
    system("sed -i \"\" '1,10 s_.*<version>.*</version>.*_\t<version>#{ version }-SNAPSHOT</version>_g' itac-web/pom.xml")

  end

  desc <<-DESC
    Repackages itac-web application with correct properties file.
  DESC
  task :build_qa_war do
    system("cd itac-web; mvn -o clean -Dmaven.test.skip; cd ..;")
    system("mkdir -p itac-web/target/classes")
    system("cp itac-configuration/target/classes/itac-qa.properties itac-web/target/classes/itac.properties")
    system("cd itac-web; mvn -o package -Dmaven.test.skip; cd ..;")
  end
end
  
