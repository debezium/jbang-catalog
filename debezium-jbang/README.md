## Install Debezium-JBang

```shell
jbang app install debezium@debezium
```

If you however like to install debezium-jbang from this project build you create an alias to the local entry point.

```shell
jbang alias add --name debezium -Ddebezium.jbang.version=3.6.0-SNAPSHOT ./debezium-jbang/debezium-jbang-main/dist/DebeziumJBang.java

jbang debezium version  
Debezium JBang version: 3.6.0-SNAPSHOT
```