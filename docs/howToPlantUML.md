## How to work with plant UML in Alfresco

Detailed description can be found for Alfresco employees in the architecture map project (not really maintained these days): 
https://github.com/Alfresco/map/blob/master/contributor-guide.md

We have decided to keep this documentation in this project so it can be public and to evolve with the project itself.

This document is a small, minimal overview of what you need in order to get started.

1. Go to http://plantuml.com/ and get familiar with the project if you have not done so already
2. To work with .puml files locally, you will [need](http://plantuml.com/graphviz-dot) the [graphviz software](https://www.graphviz.org/download/). 
On Windows I just used the msi installer with the default settings.
3. If you want to work with .puml files in your IDE, you will need to download plugins for it:
   * Intellij: Settings -> Plugins -> search for PlantUML Integration plugin and install it -> restart
   * Eclipse:  Select Help -> Install New Software -> Enter Work with: 'http://basar.idi.ntnu.no/svn/tdt4100/anonymous/trunk/updatesite/' ->  
   Select 'PlantUML' -> install -> restart

**Note**: If you modify the .puml files in this project, you must safe (generate) the png image associated with that .puml file 
and save it with the same name (but with the .png extension) in the same folder and commit both files.

You could also use https://www.draw.io/ tool to quickly create some diagram, but make sure you save the source and image file 
in git, so we could edit/enhance it later.