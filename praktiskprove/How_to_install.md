# *Secure message hub installation guide


###  1. Application manager is "Apache maven" so make sure maven is installed.

###  2.  Application uses *Oxalis* outbound module to send messages
###      &nbsp;&nbsp;&nbsp;&nbsp;so first of all you need to set up home catalog ".oxalis" under user catalog "C:\Users\user\\.oxalis windows or ~/.oxalis in unix".

###  3.  Put oxalis-global.properties, oxalis-keystore.jks, truststore-test.jks in the ".oxalis" catalog. Dont have the proper files contact to peppol authority in Difi

###  4.  Build with maven in root catalog "mvn clean install"

###  5.  Optional: You can use adresseregister-web module for address registry simulation under development. To turn on 
###      &nbsp;&nbsp;&nbsp;&nbsp;the service you just have to run: "mvn install jetty:run" in adresseregister-web module.

###  6.  To start the message hub "knutepunkt" write "mvn install jetty:run" in knutepunkt module.

# Now you have a working instance of secure message hub knutepunkt, enjoy.



