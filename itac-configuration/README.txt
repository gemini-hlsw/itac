This module contains the most important configuration files and resources in a centralized place.

src
    main
        resources
            itac-jetty.properties       Jetty configuration for local webservers. Will be renamed to itac.properties
                                        and copied to classpath. This file will be filtered depending on the selected
                                        profile ("test" which is the default or "build" on the build server)
                                        This configuration should be overwritten on qa and prod servers by
                                        adding a itac.properties file to WEB-INF/classes or any other folder that
                                        can hold property files and takes precedence in the classpath.
            itac-production.properties  The prototype file for the production environment.
            itac-qa.properties          The prototype file for the qa environment.

    test
        resources
            itac.properties             Configuration for running tests on development machines and build server.
                                        Will be filtered depending on the selected profile ("test" or "build").
                                        This file will only be in the classpath when running tests in a maven context.