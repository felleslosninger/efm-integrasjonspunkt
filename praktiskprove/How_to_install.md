# *Secure message hub installation guide, powered by:*  *Inmeta*


###  1.  Application uses *Oxalis* outbound module to send messages
###      &nbsp;&nbsp;&nbsp;&nbsp;so first off all you need to set up home catalog ".oxalis" under user catalog "C:\Users\user\.oxalis windows or ~/.oxalis in unix".

###  2.  Build with maven in root catalog "mvn clean install"

###  3.  Optional: You can use adresseregister-web module for address registry simulation under development. To turn on 
###      &nbsp;&nbsp;&nbsp;&nbsp;the service you just have to run: "mvn install jetty:run" in adresseregister-web module.

###  4.  To start the message hub "knutepunkt" write "mvn install jetty:run" in knutepunkt module.

# Now you have a working instance of secure message hub knutepunkt, enjoy.



