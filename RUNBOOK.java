
---------
RUNBOOK
---------

-----
CICD
-----
To make a standalone deployable JAR file:
mvn clean package

To run servers with this package:
java -jar /Users/kprasad/Dropbox/Focus/DIST/kublai/target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8081

-----
SERVERS:
-----
To run servers with this package:
java -jar /Users/kprasad/Dropbox/Focus/DIST/kublai/target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8081
java -jar /Users/kprasad/Dropbox/Focus/DIST/kublai/target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8082
java -jar /Users/kprasad/Dropbox/Focus/DIST/kublai/target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8083
java -jar /Users/kprasad/Dropbox/Focus/DIST/kublai/target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8084

pinging replica directly for status:
curl localhost:9080/status  

write to replica directly:
curl -X POST -d "dataLeader" localhost:8081/write

with load balancer: 
curl -X POST -d "dataLeader" localhost/write

write bulk data to database with offset:
	curl -X POST -d "200" localhost/bulkrandomwrite

when deploying a data center as an image:
copy the whole project directory into the webapp folder + docker compose up --build

-----
ZOOKEEPER:
-----
/Users/kprasad/Dropbox/Focus/DIST/zookeeper/bin/zkServer.sh start 
/Users/kprasad/Dropbox/Focus/DIST/zookeeper/bin/zkServer.sh status
/Users/kprasad/Dropbox/Focus/DIST/zookeeper/bin/zkServer.sh stop

client:
/Users/kprasad/Dropbox/Focus/DIST/zookeeper/bin/zkCli.sh
	list all znodes:
		ls /l  
	create a znode:
		create /election "" 
	delete a znode:
		delete /election

-----
KAFKA:
-----
/Users/kprasad/Dropbox/Focus/DIST/kafka/bin/.kafka-server-start.sh config/server.properties
(do one for each server, with its own server.properties)

creating a topic and setting replication:
/Users/kprasad/Dropbox/Focus/DIST/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic events

listing all topics:
/Users/kprasad/Dropbox/Focus/DIST/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

getting more info on topic:
/Users/kprasad/Dropbox/Focus/DIST/kafka/bin/kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic events


listing messages in 'events' topic with test consumer:
/Users/kprasad/Dropbox/Focus/DIST/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic events --from-beginning

putting messages in 'events' topic with test producer:
/Users/kprasad/Dropbox/Focus/DIST/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic events


-----
MONGODB:
-----

Configuring replica sets:

---
mongod:
---
0. Starting clean:
rm -rf /usr/local/var/mongodb/rep-set-0-0
rm -rf /usr/local/var/mongodb/rep-set-0-1
rm -rf /usr/local/var/mongodb/rep-set-0-2
rm -rf /usr/local/var/mongodb/rep-set-0-3

rm -rf /usr/local/var/mongodb/rep-set-1-0
rm -rf /usr/local/var/mongodb/rep-set-1-1
rm -rf /usr/local/var/mongodb/rep-set-1-2
rm -rf /usr/local/var/mongodb/rep-set-1-3

rm -rf /usr/local/var/mongodb/config-set-0-0
rm -rf /usr/local/var/mongodb/config-set-0-1
rm -rf /usr/local/var/mongodb/config-set-0-2

---
A. To start a replica set:
---

1. Create the directories for each replica:
mkdir -p /usr/local/var/mongodb/rep-set-0-0
mkdir -p /usr/local/var/mongodb/rep-set-0-1
mkdir -p /usr/local/var/mongodb/rep-set-0-2
mkdir -p /usr/local/var/mongodb/rep-set-0-3

2. Start the replica servers (mongod)
mongod --replSet rep-set-0 --port 27017 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-0 --oplogSize 128
mongod --replSet rep-set-0 --port 27018 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-1 --oplogSize 128
mongod --replSet rep-set-0 --port 27019 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-2 --oplogSize 128
mongod --replSet rep-set-0 --port 27020 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-3 --oplogSize 128

3. To connect to any of these:
mongo --port 27017
mongo --port 27018
mongo --port 27019
mongo --port 27020


4. Now to elect leader and initiate replica set, log on to any one mongod server using mongo client and run:
rs.initiate({
_id: "rep-set-0",
members: [
	{
		_id: 0,
		host: "127.0.0.1:27017"
	},
	{
		_id: 1,
		host: "127.0.0.1:27018"
	},
	{
		_id: 2,
		host: "127.0.0.1:27019"
	},
	{
		_id: 3,
		host: "127.0.0.1:27020"
	}
]
})

5. Figure out which one is primary from any mongo client to these servers:
rs.status() 

---
B. To convert replica set to a sharded setup: https://docs.mongodb.com/manual/tutorial/convert-replica-set-to-replicated-shard-cluster/ 
---

1. Determine which one is primary

2. Then shut down the secondaries and start them all up as shard servers like this:

mongod --replSet rep-set-0 --shardsvr --port 27017 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-0 --oplogSize 128
mongod --replSet rep-set-0 --shardsvr --port 27019 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-2 --oplogSize 128
mongod --replSet rep-set-0 --shardsvr --port 27020 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-3 --oplogSize 128

3. Now from a client connected to primary, step down: 
rs.stepDown()

Now restart that one too as a shardsvr:
mongod --replSet rep-set-0 --shardsvr --port 27018 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-0-1 --oplogSize 128

4. Start three config servers:

mkdir -p /usr/local/var/mongodb/config-set-0-0
mkdir -p /usr/local/var/mongodb/config-set-0-1
mkdir -p /usr/local/var/mongodb/config-set-0-2

mongod --configsvr --replSet config-set-0 --port 28017 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/config-set-0-0 --oplogSize 128
mongod --configsvr --replSet config-set-0 --port 28018 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/config-set-0-1 --oplogSize 128
mongod --configsvr --replSet config-set-0 --port 28019 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/config-set-0-2 --oplogSize 128

5. Run initiate on the config server (only one) VIA THE mongo client:

mongo --port 28017


rs.initiate({
_id: "config-set-0",
configsvr: true,
members: [
	{
		_id: 0,
		host: "127.0.0.1:28017"
	},
	{
		_id: 1,
		host: "127.0.0.1:28018"
	},
	{
		_id: 2,
		host: "127.0.0.1:28019"
	}
]
})

6. Start mongos server for routing to these config servers and start it on its own unique port:
mongos --configdb config-set-0/127.0.0.1:28017,127.0.0.1:28018,127.0.0.1:28019   --bind_ip 127.0.0.1 --port 29017

7. Connect to mongos via mongo shell:
mongo 127.0.0.1:29017/admin

8. Add shards for the replication set:
sh.addShard("rep-set-0/127.0.0.1:27017,127.0.0.1:27018,127.0.0.1:27019,127.0.0.1:27020")
sh.addShard("127.0.0.1:27017")
sh.addShard("127.0.0.1:27018")
sh.addShard("127.0.0.1:27019")
sh.addShard("127.0.0.1:27020")


	9. OPTIONAL 9-12 : At this point, we have only one shard, with 4 servers for replication. 
	Now, we need another shard, again with a replication set of 4 servers

	10. To add replica set two, follow the same steps fropm 1-4

	11. Create the second replica set:
	mkdir -p /usr/local/var/mongodb/rep-set-1-0
	mkdir -p /usr/local/var/mongodb/rep-set-1-1
	mkdir -p /usr/local/var/mongodb/rep-set-1-2
	mkdir -p /usr/local/var/mongodb/rep-set-1-3

	mongod --replSet rep-set-1 --port 26017 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-1-0 --oplogSize 128
	mongod --replSet rep-set-1 --port 26018 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-1-1 --oplogSize 128
	mongod --replSet rep-set-1 --port 26019 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-1-2 --oplogSize 128
	mongod --replSet rep-set-1 --port 26020 -bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rep-set-1-3 --oplogSize 128

	mongo --port 26017

	rs.initiate({
	_id: "rep-set-1",
	members: [
		{
			_id: 0,
			host: "127.0.0.1:26017"
		},
		{
			_id: 1,
			host: "127.0.0.1:26018"
		},
		{
			_id: 2,
			host: "127.0.0.1:26019"
		},
		{
			_id: 3,
			host: "127.0.0.1:26020"
		}
	]
	})

	12. Now, add the second shard in mongos:
	sh.addShard("rep-set-1/127.0.0.1:26017,127.0.0.1:26018,127.0.0.1:26019,127.0.0.1:26020")

	--- and that failed -- you can only have shards added to servers started with --shardsvr flag

Now, do all the commands down from here on mongos not mongo:

13. Decrease chunk size so the data will split faster:
use config 
db.settings.save({ _id:"chunksize", value: 1 })

14. Get shard status:
sh.status()

15. To actually shard a table/document:
use testdb
sh.enableSharding("testdb")

	15.a. Create some collection:
db.data.insertOne({"name" : 1})

	15.a. For sharding by range:
db.data.createIndex({name : 1})

	15.b. For actually sharding:
sh.shardCollection("testdb.data",{name: 1})

16. To remove a shard:
db.adminCommand({ removeShard : "rep-set-0"})

17. To drop all collections in a DB:
db.getCollectionNames().forEach(function(x){db[x].drop()})


-----
HAPROXY:
-----
Start haproxy:
haproxy -f haproxy.cfg

Configuration:
---
for single leader setup:
frontend http-in
    bind *:80
    acl write_methods method POST DELETE PUT
    use_backend write_nodes if write_methods
    default_backend read_nodes

backend read_nodes
    balance roundrobin
    option httpchk GET /status
    http-check expect string "Follower"
    server leader    localhost:8081 check
    server follower1 localhost:8082 check
    server follower2 localhost:8083 check
    server follower3 localhost:8084 check
    ##server debug     localhost:9081 check


backend write_nodes
    balance roundrobin
    option httpchk GET /status
    http-check expect string "Leader"
    server leader    localhost:8081 check
    server follower1 localhost:8082 check
    server follower2 localhost:8083 check
    server follower3 localhost:8084 check


for multi leader setup:
frontend http-in
     bind *:80
     acl even path_end -i /even
     acl odd path_end -i /odd
     use backend replica_nodes_even if even
     use backend replica_nodes_off if odd
     
backend replica_nodes_even
 balance roundrobin
 server follower1 localhost:8082 check
 server follower3 localhost:8084 check

 backend replica_nodes_odd
 balance roundrobin
 server leader localhost:8081 check
 server follower2 localhost:8083 check



for random configuration:
global
    maxconn 500

defaults
    mode http
    timeout connect 10s
    timeout client 50s
    timeout server 50s

frontend http-in
    bind *:80
    default_backend replica_nodes

backend replica_nodes
    balance roundrobin
    server leader    localhost:8081 check
    ##server follower1 localhost:8082 check
    ##server follower2 localhost:8083 check
    ##server follower3 localhost:8084 check
    ##server debug     localhost:9081 check

listen stats_page
    bind *:83
    stats enable
    stats uri /