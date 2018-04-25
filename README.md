# Eidetic

## General Overview

Eidetic is an automated backup system, which interacts with Amazon Web Services API to programmatically take a backup of a marked EBS volume by utilizing Amazons EBS Snapshot feature. The EBS volume is marked by the addition of a tag. The tag key allows Eidetic to find the volume through search filtering functionality provided by Amazon and the tag value specifies actions and methods Eidetic will use to interact with the aforementioned EBS volume. There are multiple Eidetic extensions which enhance functionality including extra region redundancy and snapshot validation. Each piece of the Eidetic follows:

List of Eidetic Automated Backup System components:

- Eidetic
- Eidetic Express
- Amnesia
- Eidetic Checker
- Snapshot Time Poller
- Tag Checker
- Error Checker
- Snapshot Synchronizer
- RDS Automated Manual Snapshot
- RDS Automated Snapshot Checker
- RDS Automated Snapshot Cleaner
- RDS Automated Tag Propagator

You are allowed to have any combination of components to run at any time. You can make your choice via the application.properties configuration file. 

Eidetic has the capability to run over multiple AWS accounts. 

logback_config.xml has the parameters for the logging of Eidetic. Logging is an essential aspect to Eidetic as it alerts when there are misconfigured tags, snapshots in error states, or any general error when running the process. Eidetic will also write out general amazon API interaction information. Processing these logs with systems such as Splunk or Logstash would be best practice.
  
  
## Component Overview

### Eidetic  
	
Eidetic is the heart of the Eidetic automated backup system. The main process which will filter tagged volumes by keys and acts accordingly. When passed an AWS account in the application.properties file, it will iterate through each region provided by Amazon searching for volumes to act upon. To interact with Eidetic:

Create tags in this format (tag-key : tag-value): 

 `Eidetic : { "CreateSnapshot" : { "Interval" : "x", "Retain" : "y" } }`

where:

- x: the interval, possible choices are hour, day, week, or month

- y: the quantity of snapshots to retain, values range from a minimum of two and upwards

  
  
Optional: add extra parameter, "RunAt" to "day" interval value selection. 

`{ "CreateSnapshot" : { "Interval" : "day", "Retain" : "y",  "RunAt" : "z"} }`

where:

- day: The interval must be set to "day"

- y: the quantity of snapshots to retain, values range from a minimum of two and upwards

- z: the time to take the snapshot. Values range from "00:00:00" to "23:59:59"

Example Tags:

```
Eidetic : { "CreateSnapshot" : { "Interval" : "hour", "Retain" : "24" } }

Eidetic : { "CreateSnapshot" : { "Interval" : "day", "Retain" : "10" } }

Eidetic : { "CreateSnapshot" : { "Interval" : "week", "Retain" : "2" } }

Eidetic : { "CreateSnapshot" : { "Interval" : "day", "Retain" : "5",  "RunAt" : "09:30:00"} }

Eidetic : { "CreateSnapshot" : { "Interval" : "day", "Retain" : "7",  "RunAt" : "18:45:00"} }

```
  
  
### Eidetic Express

Eidetic Express is an extension to Eidetic where Eidetic tagged EBS volumes have an additional parameter inside the Eidetic tag value. Eidetic Express will then ship the created Eidetic snapshots from one region to another region for redundancy purposes. 

As an important note, CreateSnapshot parameter is the parameter that will actually create the snapshot. If there is just CopySnapshot without the CreateSnapshot, it will have no created snapshots to copy over. Eidetic Express will retain as many snapshots as specified in the source region.
  
  
Usage:
`Eidetic : { "CopySnapshot" : "r" }`

where:

- r: the destination region for the snapshot to be copied to.

Example Tags:

```
Eidetic : { "CopySnapshot" : "us-east-1", "CreateSnapshot" : { "Interval" : "hour", "Retain" : "4" } }

Eidetic : { "CreateSnapshot" : { "Interval" : "day", "Retain" : "10" }, "CopySnapshot" : "us-west-2"  }

Eidetic : { "CopySnapshot" : "ap-northeast-1", "CreateSnapshot" : { "Interval" : "day", "Retain" : "5",  "RunAt" : "12:30:00"} }

```
  
### Amnesia 

Amnesia is a snapshot cleanup utility. When a tag’s interval value is changed on a volume, the old snapshots pertaining to that interval will remain as they are outside the normal eidetic clean up process. Volumes, which are deleted and also tagged, will fall into the same category. Thus these are stranded snapshots. Amnesia will inspect these Eidetic snapshots, and if they are older than x days, where x is configured in the application.properties file, it will delete them. You may also add in an overall snapshot retention time, where if any snapshot is older than the set time, it will delete them. 
 
### Eidetic Checker

Eidetic Checker is one of the validation extensions for Eidetic. Just as Eidetic iterates through regions for tagged EBS volumes, Eidetic Checker will do the same but instead of acting on the tags, it will validate the execution of said tags. 

For example, take this tag value:

`{ "CreateSnapshot" : { "Interval" : "day", "Retain" : "7" } }`

This tag value has been applied to an EBS volume. It will see if there are in fact snapshots executing in a daily time frame. If not, it will take a snapshot immediately and write out an error log for human inspection. 
  
  
### Snapshot Time Poller

As of the writing of this Readme, Amazon does not provide timing information for how long a snapshot processing up to S3 takes. Snapshot Time Poller will query all snapshots that are in a pending state and add a tag saying:

`CreationPending : n`

where:

- n: the number of minutes this snapshot has been in a pending state.
  
  
Once the snapshot is in a complete state, Snapshot Time Poller will replace the CreationPending tag with:

`CreationComplete : t`

where:

- t: the total number of minutes for this snapshot to complete.

This can provide useful data for a number of purposes. For example, how frequently to take snapshots of a highly active volume, deciding whether to use Amazon’s snapshot utility for data migration in a finite deployment window, etc. As a note, the minutes reported may have a margin of error due to API saturation, the server hosting the Eidetic JVM CPU bottlenecking, Eidetic not running, etc.  Snapshot Time Poller should be used as an estimate.
  
  
### Tag Checker

Since Eidetic and extensions work off on tags, there needs to be validation that tags exist for volumes of importance. We can add a tag to our instance to declare which mount point is the location of our important data. 

For example, lets say on our database instance, the mount point of /dev/xvdk is where our important business related data is stored. The tag on the instance would appear like so:

`Data : /dev/xvdk`

Tag Checker will find tagged instances and ensure an Eidetic tag is on the volume at that specific mount point. If not, it will create an Eidetic tag on aforementioned volume. The tag value is specified in the application.properties configuration file.
  
  
### Error Checker

There is the possibility of snapshot resulting in an error state at some point after creation. If we are snapshotting a volume of importance, we need to know immediately that an error has occurred. Error Checker will scan for all snapshots in an error state and write a log detailing the snapshot. Error Checker will clean the snapshot afterwards.

### Snapshot Synchronizer 

Enables an API to take snapshots. Endpoint will be located at configured port (default is 80) and at /api/syncsnapshot. Desired volumes must be tagged using the syntax below to enable Eidetic to take snapshots of requested volumes. It is possible to mix in other eidetic parameters into the tag. Retain for synchronization will be considered independently than that of Create Snapshot's Retain. It is possible to add in an optional Validate parameter which checks to see that at least a single synchronized snapshot has occurred in the Cluster in the time frame indicated by CreateAfter (which is in hours.) If there has not be at least a single synchronized snapshot in the cluster by $CreateAfter hours, Eidetic will create a cluster wide snapshot on every volume in that region which share that specific cluster name.  

Usage:
`Eidetic : { "SyncSnapshot" : { "Retain" : "x" } }`

where:

- x: the quantity of snapshots to retain, values range from a minimum of two and upwards

Optional: add an extra nested parameter Validate, containing Cluster and CreateAfter. 

`{ "SyncSnapshot" : { "Retain" : "x", "Validate" : { "Cluster" : "y", "CreateAfter" : "z"} } }`

where:

- y: Identifies a grouping of volumes to check

- z: The number of hours to wait before snapshotting the entire cluster (identified by the cluster parameter)

Example Tags:

```
Eidetic : { "SyncSnapshot" : { "Retain" : "5" } }

Eidetic : { "SyncSnapshot" : { "Retain" : "30", "Validate" : { "Cluster" : "prod-mongo", "CreateAfter" : "24"} } }

Eidetic : { "SyncSnapshot" : { "Retain" : "3", "Validate" : { "Cluster" : "graphite", "CreateAfter" : "40"} }, "CopySnapshot" : "us-west-2" }

Eidetic : { "CreateSnapshot" : { "Interval" : "day", "Retain" : "5",  "RunAt" : "09:30:00"}, "SyncSnapshot" : { "Retain" : "3" } }
```

A sample http post using curl showcasing the correct syntax follows. The required parameters in the http post are accountid, region, and volumes (which is a comma separated list of vol_ids). AWS accountid will be written to logs if the value is unknown or easily accessible. In application.properties, the port has been changed to 8080 for this example.

`curl --data "accountid=912399967890&region=US_EAST_1&volumes=vol-9c314473,vol-9131447e,vol-6f314480,vol-0f3144e0" 127.0.0.1:8080/api/syncsnapshot`

Note again that all volumes must have a SyncSnapshot parameter to enable snapshotting. 

### RDS Automated Manual Snapshot

As of the writing of this README, RDS still enables the deletion of all RDS snapshots when the RDS node gets deleted without the capability of taking a final snapshot. As such, this plugin allows the user to create an automated process of creating manual snapshots on particular RDS Instances by a series of tags (RDS tagging also prohibits the use of symbols commonly found in JSON). An example tagging follows for one particular instance.

Usage (Not optional tags):

`Eidetic_Interval : x`

`Eidetic_Retain : y`

Optional:

`Eidetic_RunAt : z`

Where:

- x: the interval, possible choices are hour, day, week, or month

- y: the quantity of snapshots to retain, values range from a minimum of number two and upwards

- z: the time to take the snapshot. Values range from "00:00:00" to "23:59:59"

In such a way the usage of this is similar to that of the above eidetic components. 

Example:

I add the following 3 tags to a DB Instance, I want it to run every day at 06:15:00 UTC and keep 30 snapshots in total (including the one I just created)

`Eidetic_Interval : day`

`Eidetic_Retain : 30`

`Eidetic_RunAt : 06:15:00`


### RDS Automated Snapshot Checker

This plugin will look at all tagged clusters and instances and see whether a snapshot has occurred in the time specified. For example, if using `Eidetic_Interval : day` and a snapshot has not occurred in the last 24 hours, the checker will automatically create one. Note that if tagging a resource for the first time, the checker will usually automatically create a snapshot and put it into the normal RDS Eidetic workflow.

### RDS Automated Snapshot Cleaner

This plugin will use the provided eidetic_snapshot_retain_days and all_snapshot_retain_days of the amnesia plugin above and will act in a similar fashion to the EBS snapshots but with the RDS snapshots instead. To reiterate, all eidetic tagged snapshots are compared to the eidetic retain date and if they are older than the given x, they will be deleted. Two, eidetic can clean up all snapshots if those snapshots are older than the total snapshot retain time. 

### RDS Automated Tag Propagator

This plugin is very useful when it comes to running in an RDS style environment. As of the writing of this README, clusters do not allow to be tagged in the console interface but can be tagged using APIs. As such, this plugin will copy any tags on an instance and put them on a cluster. Note an important function here in the RDS specific automation. If an instance is tagged and not a cluster, the instance will have a snapshot taken of it. If the instance and the cluster are both tagged, only the cluster will have a snapshot and not the instance. As such, if you have multiple instances in a cluster that are all tagged, there will only be one resulting snapshot from the cluster. The cluster tags will be from one of the instances, so it is important to ensure that all instances have the same tags or only one instance has a tag when using this tag propagator plugin's functionality. That functionality is specifically, iterate through all tagged instances that are in a cluster and copy the currently selected instance's tags to the cluster's tags if they differ.

### Caching

When using Eidetic to cover thousands of EC2 volumes or RDS Instances, the amount of AWS API usage will escalate quite highly. This will run into multiple rate throttling issues from AWS, which may affect other tools.  With caching enabled on a per account basis, eidetic will use a TTL Cache to reduce API usage. Depending on the call, there is a reduction up to 99% on total API calls. This will make Eidetic less reactive to changes (new Eidetic tags, Eidetic tag changes, etc.) that happen in the AWS account, but will still work as expected.
