Gs-cloudwatch is a GeoServer Extension that reports various GeoServer metrics to the Amazon Web Services cloudwatch service. This will allow better autoscaling of a GeoServer cluster or notification when GeoServer is under heavy load.

These instructions assume that the Boundless Suite installer for Linux was used to install GeoServer. Paths may be different for other systems.

#Install

- Run `mvn compile`

- Copy `target/cloudwatch-1.0-SNAPSHOT.jar` to the GeoServer lib directory (`/usr/share/opengeo/geoserver/WEB-INF/lib/`)

- Run `mvn dependency:copy-dependencies`

- Copy `target/dependency/*.jar` to the GeoServer lib directory.

#Configuration

- Generate an AWS access key in the AWS console.
- Append the following environment variables to the Tomcat 8 configuration file
- File is located at '/etc/tomcat8/tomcat8.conf' in both CentOS and Ubuntu

```
### Interval in milliseconds at which to send metrics
GS_CW_INTERVAL="10000"

### AWS Authentication
AWS_ACCESS_KEY_ID="[your_access_key]"		<-- EDIT THIS LINE HERE
AWS_SECRET_KEY="[your_secret_key]"			<-- EDIT THIS LINE HERE

### Instance specific settings
GS_CW_ENABLE_PER_INSTANCE_METRICS="true" 
GS_CW_INSTANCE_ID="hal9000"

### EC2 Autoscaling
GS_CW_AUTOSCALING_GROUP_NAME="testgroup"

### JMX metrics
#export GS_CW_JMX="true"

### Geoserver metrics
#GS_CW_WATCH_WMS="true"
#GS_CW_WATCH_WPS="true"
#GS_CW_WATCH_WFS="true"
#GS_CW_WATCH_CSW="true"
#GS_CW_WATCH_OSW="true"
#GS_CW_WATCH_WCS100="true"
#GS_CW_WATCH_WCS111="true"
#GS_CW_WATCH_WCS20="true"

```
- Insert the AWS access key and secret key variables.
- Optionally edit the instance ID or autoscaling group name.
- Uncomment any desired metrics.
- Restart Tomcat.

#Todo

- Add more metrics.
- Improve configuration.
