# Bolt Proxy Example

This app implements a bolt reverse proxy.  It:
* Listens on a configurable port for websocket requests.
* Consumes the bolt messages, unpacks them, and logs all messages.
* Modifies all Bolt LOGON requests to add a configurable set of parameters to the AuthToken (custom token)

## Setup

* Configure the boltproxy.conf file on the classpath
* Install the neo4j-bolt-utils jar manually into your repo.
```
mvn install:install-file \
   -Dfile=neo4j-bolt-utils-1.0.1.jar \
   -DgroupId=org.neo4j \
   -DartifactId=bolt-utils \
   -Dversion=1.0.1 \
   -Dpackaging=jar \
   -DgeneratePom=true
```
* Run the proxy and configure Neo4j Browser/Bloom to access this port instead of Neo4j directly.
* For Bloom, you will need to deploy the Bloom Assets and a discovery.json separately onto a web server.

## Work In Progress
* SSL Support
* Proxying of Routing Requests (SSR 7688)
* Directions/Updates for Bloom


