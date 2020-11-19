# pubsubplus-container-junit

When developing event driven Java services for Solace PubSub+, at some stage you want to move from unit testing Java code to testing services against broker.

This repository demonstrates a base class for JUnit tests that launches a new PubSub+ instance in docker for use in tests.
It utilises [testcontainers](https://www.testcontainers.org/), manages the container life cycle manually as there is no testcontainers wrapper (yet) for PubSub+ and follows the [singleton container pattern](https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/).

## Port Mapping and Ports Exposed by Container

When the container is created the mapped ports are provided. This tells testcontainers which ports it shall expose for the container. [testcontainers exposes random ports and maps these to the internal ports of the container](https://www.testcontainers.org/features/networking/).
For example - the base class maps port 55555 (SMF plain). This may be exposed as any port. The base class logs the SMF port used and output will look similar to this:
```
INFO: Started Solace PubSub+ Docker Container, available on host [localhost], SMF port [32872]
```

So an SMF client needs to connect on port `32872`.

## Base class - AbstractPubSubPlusTestCase

This class starts the docker container. It logs a message when the container before and after start up.
It exposes many of the plain services such as SMF, REST/HTTP, MQTT/TCP. The ports used by the container can be obtained from the class with `getXxxxx` methods.
The class also provides `getAdminUser` and `getAdminPassword` methods so SEMP v2 calls can be sent to configure the broker.

## Example test - SolaceIntegrationTest

This is an example unit test, extends the `AbstractPubSubPlusTestCase` class.

It includes some test cases to demo how the base class can be used:
* Publish message using SMF
* Publish and subscribe to message using SMF
* Publish using REST/HTTP and subscribe using SMF
* Obtain a. list of message vpns form the broker using SEMPv2/HTTP



