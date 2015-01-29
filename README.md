#**CMIS Server 4 ELO**

##1. Overview

The **CMIS Elo Server** provides read/write access to content and metadata stored in an [ELO Document Management Solution](http://www.elodigital.com.au/) server by exposing a CMIS interface on top of the ELO server.

The project is based on the [Apache Chemistry](http://chemistry.apache.org/) project that provides open source implementations of the [Content Management Interoperability Services (CMIS) specification] (http://docs.oasis-open.org/cmis/CMIS/v1.0/cmis-spec-v1.0.html).

The application is based on the [ OpenCMIS Server Framework] (http://chemistry.apache.org/java/developing/dev-server.html) provides a server implementation of both CMIS bindings, AtomPub and Web Services, mapping them to Java interfaces. 
Requests and data received from CMIS clients are converted by the server and pushed to an **ELO repository connector** responsible for translation of the CMIS calls into ELO native calls.

The application is available for download as a **ready-to-use WAR application** that can be deployed to your favourite application server.

Links:

* [CMIS (Content Management Interoperability Services)] (http://docs.oasis-open.org/cmis/CMIS/v1.0/cmis-spec-v1.0.html)
* [Apache Chemistry](http://chemistry.apache.org/)
* [Apache Chemistry - OpenCMIS Server Framework] (http://chemistry.apache.org/java/developing/dev-server.html)

##2. Download
You can download the release files from the <a href="http://tni-hq-artifactory/simple/tn-components/com/elo/cmis/elo-cmis-server/" target="_blank">artifactory repository</a>.

##3. Configuration
This chapter describes the required configurations for starting with success a **CMIS Elo Server** application instance.

###3.1 Resource files
The application allows the resource configuration files to be **embedded in the war** file or to be **referenced externally** as absolute paths on the server where it is deployed.

Two resource files must be created: 

* **log4j configuration file** - this is a log4g configuration file used by the application 
* **[server configuration file](/documentation/elo-cmis-properties.example.md)** - this file contains all configurations required to connect to ELO server and the configuration of pools keeping the connections to ELO server

In case of embedding the configuration resources in the .war file they must be located in /WEB-INF/classes/profiles/{profileName} path.

###3.2 Environment variables

The server and log4j configuration files are specified to the application by setting the following JAVA_OPTS environment variables before the server is started:

* **-Delo.cmis.configuration.profile**=default   (or the profileName located in the /WEB-INF/classes/profiles/)
    <BR/>This variable is optional and it is used to specify the profile name used for getting the configuration files in case the resources are embedded in the war file. In case the resources are referenced externally then this variable will not be set.
    <BR/>
    <BR/>

* **-Delo.cmis.configuration.server**=/cmis-elo-server.properties 
    <BR/>This variable is mandatory and contains the path pointing to the **server configuration file**. This path must be relative to the profile directory that is embedded in war or absolute to the server in case profile variable is not set.
    <BR/>
    <BR/>

* **-Delo.cmis.configuration.log4j**=/cmis-elo-log4j.properties 
    <BR/>This variable is optional and contains the path pointing to the **log4j configuration file** in case you need finer grained control over the logging in the server. This path must be relative to the profile directory that is embedded in war or absolute to the server in case profile variable is not set.
    <BR/>
        
Below is an example of setenv.sh or setenv.bat script that can be used for setting the environment variables for a tomcat application server (the setenv file must be created in the tomcat's **/bin** folder)

```bash
CATALINA_OPTS="$CATALINA_OPTS -Xms4096m -Xmx4096m -XX:MaxPermSize=2048m"
JAVA_OPTS="$JAVA_OPTS -Delo.cmis.configuration.server=/usr/apache-tomcat-7.0.57/conf/elo-cmis-server.properties"
JAVA_OPTS="$JAVA_OPTS -Delo.cmis.configuration.log4j=/usr/apache-tomcat-7.0.57/conf/elo-cmis-log4j.properties"
```

###3.3 Elo Document Management 

The following configurations are required in Elo document management server:

* **Keywording forms** - the elo mask with **ID = 0** (named "Elementary entry / Intrare elementară") must be configured as **document form** and **folder form**


##4 Testing

The deployed configuration can be tested using the **OpenCMIS Workbench** application that is the CMIS desktop client for developers.

The application can be downloaded from [here](http://chemistry.apache.org/java/download.html)

Below is an connection configuration that can be used for starting the workbench application:

``` 
org.apache.chemistry.opencmis.binding.spi.type=browser
org.apache.chemistry.opencmis.binding.browser.url=http://<server_name>:<server_port>/<application_name ... can be elo-cmis-server>/browser
org.apache.chemistry.opencmis.user=<user>
org.apache.chemistry.opencmis.password=<password>
org.apache.chemistry.opencmis.binding.compression=true
org.apache.chemistry.opencmis.binding.cookies=true
```


##4 Demo environment

The following environment was created as demo instance for exemplifying the configurations presented above.

Below are the relevant links to the demo environment:

<b>ELO DMS Server</b>
<a href="http://sol-w2k8-04:8080/ig2-elo2/pages/startup.jsp" target="_blank">Elo Web Client</a>
<a href="http://sol-w2k8-04:8080/AdminConsole/" target="_blank">Elo Admin Console</a>

<b>Apache Chemistry Workbench</b>
Please download the java application from the following link <a href="http://sol-w2k8-04:8080/AdminConsole/" target="_blank">Apache Chemistry Workbench</a>

After downloading it, start and send the following connection configuration: 
 
``` 
org.apache.chemistry.opencmis.binding.spi.type=browser
org.apache.chemistry.opencmis.binding.browser.url=http://<server_name>:<server_port>/<application_name ... can be elo-cmis-server>/browser
org.apache.chemistry.opencmis.user=<user>
org.apache.chemistry.opencmis.password=<password>
org.apache.chemistry.opencmis.binding.compression=true
org.apache.chemistry.opencmis.binding.cookies=true
```
 

