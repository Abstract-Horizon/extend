

## SSL Error

If you get `javax.net.ssl.SSLException: Received fatal alert: internal_error` (with jdk14 for instance)
there are two options to fix it:

```
 -Dhttps.protocols=TLSv1.1,TLSv1.2
```

(even at `mvn clean install`)

or

```
export JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.1,TLSv1.2"
```

(or
```
JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2" mvn clean install
```
)
