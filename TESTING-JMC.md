# Test a local Fireplace build in JMC

The Fireplace and JMC checkouts can be in unrelated directories. Replace `/path/to/fireplace` and `/path/to/jmc` below with their locations.

## 1. Deploy Fireplace to the JMC sources

From the Fireplace repository:

```shell
cd /path/to/fireplace
./gradlew deployToJmcSources -Plocal.jmc.clone.path=/path/to/jmc
```

This publishes Fireplace to Maven Local, updates JMC's Maven version and newest target's OSGi bundle versions, and prints the remaining commands. The modified JMC files contain local-only versions; do not commit them.

## 2. Package and run JMC

```shell
cd /path/to/jmc
mvn clean --file releng/third-party/pom.xml
./build.sh --packageJmc
./build.sh --run
```

The packaging command rebuilds and temporarily serves JMC's local p2 repository. When JMC starts, open a JFR recording and exercise the Flame Graph page.
