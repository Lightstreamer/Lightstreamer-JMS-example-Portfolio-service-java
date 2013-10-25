set CUSTOM_JARS=.\lib\hornetq-commons.jar;.\lib\hornetq-core-client.jar;.\lib\hornetq-jms-client.jar;.\lib\jnp-client.jar;.\lib\netty.jar

set cpath=.\lib\JMSPortfolioDemoService.jar;.\lib\log4j-1.2.15.jar;.\lib\jms.jar;%CUSTOM_JARS%
set class=jms_demo_services.JmsPortfolioDemoService

set command=java -cp %cpath% %class% conf\demo_service.conf

%command%
pause

