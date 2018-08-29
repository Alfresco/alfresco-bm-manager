### To create an archetype:
Note: Commands are for Windows with Git tools installed

#### Copy the 'src' folder to the archetype
```
> cd sample
> cd archetype
> cp -R src archetype/src/main/resources/archetype-resources
```

#### Update dependency versions numbers
```
find . -name archetype-metadata.xml | xargs sed -i -e 's/3.0.0-SNAPSHOT/3.0.0/g'
find . -name archetype.properties | xargs sed -i -e 's/server-version=3.0.0-SNAPSHOT/server-version=3.0.0/g'
```
Remove backup files (if created in the previous step):
```
    git status
    rm src/main/resources/META-INF/maven/archetype-metadata.xml-e
    rm src/test/resources/projects/basic/archetype.properties-e
```

#### Check all properties and configuration files
```> git diff```

#### Build the archetype
```> mvn clean install```

#### Test locally
```
> cd target
> mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:generate -DarchetypeCatalog=local
> cd <your-artiface-id>
> mvn clean install
> cd ..
> rm -rf <your-artifact-id>
```

#### Deploy archetype
```
> cd ..
> mvn deploy
```

#### Clean up
```> rm -rf src/main/resources/archetype-resources/src```

To build, follow *the instructions for version 2.0* on:
   http://wiki.alfresco.com/wiki/Benchmark_Framework_2.0:_Developing_Tests
   
If you are familiar with Maven then it is just this:
```
      mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:generate
            -DarchetypeRepository=http://artifacts.alfresco.com/nexus/content/groups/public/archetype-catalog.xml
            -DarchetypeGroupId=org.alfresco
            -DarchetypeArtifactId=alfresco-bm-sample-archetype
```
   Parameters can be chosen at the prompt or specified up front:
```
      -DgroupId=org.example
      -Dpackage=com.example
      -DartifactId=mytest
      -Dversion=1.0-SNAPSHOT
      -Djunit-version=4.11
      -Dserver-version=3.0.0-SNAPSHOT
```

#### Use the New Project
```
> cd mytest
> mvn clean install
> mvn clean spring-boot:run -Dmongo.config.host=localhost
```
