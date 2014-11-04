# Lightstreamer JMS Gateway - Portfolio Demo - Java (JMS) Service

<!-- START DESCRIPTION lightstreamer-jms-example-portfolio-service-java -->

This project contains the source code and all the resources needed to install the Portfolio Demo Service for Lightstreamer JMS Gateway.<br>

Check out the sources for further explanations.

<!-- END DESCRIPTION lightstreamer-jms-example-portfolio-service-java -->

## Install

If you want to install a version of this demo pointing to your local [Lightstreamer JMS Gateway](http://download.lightstreamer.com/#jms), you have to configure a JMS broker and deploy the service components. Please follow these steps:

### Configure the JMS Broker

This demo needs a JMS infrastructure to run. You can choose whatever JMS broker you prefer to be used for this example. We will show 4 examples using <b>HornetQ (AKA JBoss Messaging)</b>, <b>TIBCO EMS</b>, <b>Apache ActiveMQ</b>, and <b>JBossMQ</b>.<br>
Once you have selected and installed your JMS broker, you will have to configure it accordingly to other components, in particular:
* You will have to copy JMS broker-specific jars in the adapter directory and in the data generator directory.
* You will have to select the specific connection parameters in the adapter configuration, the data generator, and the demo page.

#### HornetQ (AKA JBoss Messaging)

1) You should configure a topic and a queue. Open the `hornetq-jms.xml` located under [HornetQHome](http://www.jboss.org/hornetq)`/config/stand-alone/non-clustered` and add the following nodes:

```xml

   <topic name="portfolioTopic">
      <entry name="portfolioTopic"/>
   </topic>

   <queue name="portfolioQueue">
      <entry name="portfolioQueue"/>
   </queue>

```

2) Copy the following jars: `hornetq-commons.jar`, `hornetq-core-client.jar`, `hornetq-jms-client.jar`, `jnp-client.jar`, and `netty.jar` from `/HornetQHome/lib`. You will need to paste them later.

#### TIBCO EMS

1) You should create a topic and a queue. Open the `queues.conf` and `topics.conf` located under [EMSHome](http://www.tibco.com/products/automation/messaging/enterprise-messaging/enterprise-message-service/default.jsp)`/bin/` and append to them the lines containing (without apexes) "portfolioQueue" for `queues.conf` and "portfolioTopic" for `topics.conf`.

2) Copy `tibcrypt.jar`, `tibjms.jar`, and `tibjmsufo.jar` from `EMSHome/clients/java`. You will need to paste them later.

#### Apache ActiveMQ

1) There's no need to create topics or queues, because [ActiveMQ](http://activemq.apache.org/) supports dynamic configuration of destinations.

2) Copy the following jars: `activemq-core-5.6.0.jar`, `geronimo-j2ee-management_1.1_spec-1.0.1.jar`, and `slf4j-api-1.6.4.jar` from `/ActiveMQHome/lib`, changing the version number accordingly to your installation. You will need to paste them later.

#### JBossMQ

1) You should create a topic and a queue. Open the `jbossmq-destinations-service.xml` located under [JBossHome](http://www.jboss.org/products/amq)`/server/default/deploy/jms/` and add two mbean nodes as shown below:

```xml

	<mbean code="org.jboss.mq.server.jmx.Topic"
		name="jboss.mq.destination:service=Topic,name=portfolioTopic">
		<depends optional-attribute-name="DestinationManager">jboss.mq:service=DestinationManager</depends>
	</mbean>

	<mbean code="org.jboss.mq.server.jmx.Queue"
		name="jboss.mq.destination:service=Queue,name=portfolioQueue">
    	<depends optional-attribute-name="DestinationManager">jboss.mq:service=DestinationManager</depends>
	</mbean>

```

2) Copy `jbossall-client.jar` from `JBossHome/client/`. You will need to paste it later.

### Deploy the Demo Service

To configure the demo service, follow these steps:

* Copy the `JMS_Portfolio_Demo_Service` directory that you can find in the `deploy.zip` file from the [latest release](https://github.com/Weswit/Lightstreamer-jms-example-Portfolio-service-java/releases) of this project anywhere in your file system, and put previously copied jars under its `lib` subdirectory.
* Download the JMS distribution (version 1.1 or 2.0 depending on the broker version in use) from Oracle's Java website. It cannot be distributed with the JMS Gateway for licensing issues. It can be found here: [v1.1](http://www.oracle.com/technetwork/java/docs-136352.html) [v2.0](https://mq.java.net/downloads/ri/).
* Extract its contents and find the `jms.jar` file under the `lib` directory. Please copy this jar under the `lib` subdirectory of the Demo Service.
* Then, configure the launch script `start_portfolio_demo_service.bat` (or `start_portfolio_demo_service.sh` if you are under Linux or Mac OS X), adding all the JMS broker-specific jars to the CUSTOM_JARS variable. <i>It is preset with jars for HornetQ</i>.
* Now you should edit the configuration file. The included `demo_service.conf` file shows all available parameters and 4 sample configuration for the 4 JMS brokers above. Note that all parameters are required.
* Finally, check the log4j configuration file.

Once everything is configured, launch the service with `start_portfolio_demo_service.bat` (or `start_portfolio_demo_service.sh` if you are under Linux or Mac OS X). The service must be running for the Portfolio Demo Client to function properly.

### Running the Demo

* The JMS Gateway StockList Demo requires a Lightstreamer instance running a properly configured JMS Gateway. Please refer to Lightstreamer web site [download page](http://download.lightstreamer.com/) to find *Lightstreamer server* and *JMS Gateway* download packages.
* Now grab the jar of the service, `JMSPortfolioDemoService.jar`, and copy it under the lib folder of the JMSGateway adapter just deployed. This is needed since the Gateway must be able to create any object sent or received by its clients. In this case the gateway requires the declaration of the `PortfolioMessage` class.

Now you can test this demo running the [Lightstreamer JMS Gateway - Basic Portfolio Demo - HTML Client](https://github.com/Weswit/Lightstreamer-JMS-example-Portfolio-client-javascript).

## Build

To build your own version of `JMSPortfolioDemoService.jar`, instead of using the ones provided in the `deploy.zip` file from the Install section above, follow these steps:

* Get the `log4j-1.2.17.jar` file from [Apache log4j](https://logging.apache.org/log4j/1.2/) and copy it into the `lib` folder.
* Make sure that the `jms.jar` file was copied into `lib` folder of this project as from the Install section above.
* Create the jar `JMSPortfolioDemoService.jar` with commands like these:
```
 >javac -source 1.6 -target 1.6 -nowarn -g -classpath lib/log4j-1.2.17.jar;lib/jms.jar -sourcepath src/ -d tmp_classes src/jms_demo_services/JmsPortfolioDemoService.java

 >jar cvf JMSPortfolioDemoService.jar -C tmp_classes jms_demo_services
```

## See Also

### Clients Using This Service
<!-- START RELATED_ENTRIES -->
* [Lightstreamer JMS Gateway - Basic Portfolio Demo - HTML Client](https://github.com/Weswit/Lightstreamer-JMS-example-Portfolio-client-javascript)

<!-- END RELATED_ENTRIES -->
### Related Projects
* [Lightstreamer - Portfolio Demo - Java Adapter](https://github.com/Weswit/Lightstreamer-example-Portfolio-adapter-java)
* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)
* [Lightstreamer JMS Gateway - Stock-List Demo - Java (JMS) Service](https://github.com/Weswit/Lightstreamer-JMS-example-StockList-service-java)
* [Lightstreamer JMS Gateway - Basic Chat Demo - Java (JMS) Service](https://github.com/Weswit/Lightstreamer-JMS-example-Chat-service-java)

## Lightstreamer Compatibility Notes

* Compatible with Lightstreamer SDK for Java In-Process Adapters since 5.1
* Compatible with Lightstreamer JMS Gateway Adapter since version 1.0 or newer.
