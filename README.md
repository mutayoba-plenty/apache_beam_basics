Apache Beam as a part of the evaluating of the streaming architecture

Abstract:
The reason for writing this paper is to evaluate the semantics of Apache Beam, this is so that we can explore the many complex logic that we can later employ to our infrastructure if proven profitable. Thus, this paper is going to address the big data usefulness of Apache Beam where an assumption will be made on using AWS Kinesis Data Stream or Kafka as a source of unbounded or streaming data and then show two main semantic concepts as explained in 
Paragraph three.

Apache Beam is an engine used to unify other map reduce engines. In this case you have a single API that allows you to apply some semantics and choose a runner(s)(other engines to do the work, an example of an engine is spark, apex, flink etc)


These engines have to be configured on clusters the traditional way and to run a job on them you simply have to specify the runner(engine) and the job’s endpoint. In this case I have used locally hosted dockers in Kubernetes. For this documentation I have used Spark and here is a link to aid one to deploy a spark cluster. Be sure to use spark 2.2.0 and above for that is what is supported as of now.
It is important to note that in this research paper or so a summary paper, the term semantic will be used in two contexts. One is the way we write code to do certain things so that we can later submit this code to spark or direct local runner or so any map reduce engine. And the other is the how streaming patterns can be done with how to take into consideration the use case of the data to process. I shall try to make a note in each case use per context. This was we can get over some confusions. 

Beam supports many languages like Java, Go and Python(2.7 not support for 3 yet) for a few. I have used Python and Java for this research for it is easy for me but please feel free to use one of your own accord. Ideally Java is best for it has a full deck of libraries compared to other languages. Thus you are to be in better luck with implementing in Java without having to write more code to fit your purpose.

Basics
Let us ponder the following concepts:
→ Pipeline is an entire data processing task. Thus must be the first object created when initializing the driver program.
→ PCollection is a distributed data set that the beam pipeline operates on.
→ PTransform is a data processing operation or step in your pipeline. The unit upon which this step or operation is taken upon is an element or elements and the output is nothing to a number of things depending on the task at hand. Elements are objects from the PCollection object.
→ I/O transforms: Beam comes with a number of “IOs” - library PTransforms that read or write data to various external storage systems.


Apache Beam Execution Model:
Processing of elements
The serialization and communication of elements between machines is expensive and if can be avoided may have huge benefits to saving resource use, thus money. Serialization and communication between machines or nodes is important for parallelism, rebalance of work loads and load distributions on runners via master nodes.

Data within one transform is processed in parallel. Thus depending on the elements sizes which the runner gets to decide. Data is divided into bundles for which are then distributed to workers. The maximum parallelism in a transform depends on the number of elements in a collection. 

In apache beam, processing of transforms is dependent. Thus a term dependent parallelism. That is to say if a transform A that yields to transform B fails the whole collection or so called co-dependant elements are discarded and the whole reprocessing is done again. Thus another concept of co-failing. Although parallelism is good in this case for it aids with avoiding persistence which can be costly. The alternative is equally as costly for the repeat processing of the entire dependent transforms.  


Considerations when designing a pipeline:
Where is the input data stored?
In our case is kinesis, which later can expand to s3, RDS, Dynamo and more. But for now the focus is on kinesis data stream. Thus we will need to know what Read transform to use which in this case is the KinesisIO as a java SDK which will enable us to read data from Kinesis as well as perform windowing without having to write alot of code. This is key to processing our data as we will see later.

What form will the data be in and what you want to do with it?
In our case we want to make our data be in json. For it is easy to serialize the data in case of transferring it to other places or for ease of applying our logic in the key, value pair matrix. Which apache beam is heavily constructed upon.

What does the output look like?
This will determine what writes we will have to do. In our case, we know we want to branch the data to multiple sources. We want to have multiple transforms which would feed to databases, persistence storage and so on. 

Branching PCollections
It is good to note that PTransforms do not consumer PCollection. They consider each element of a PCollection and create an output which is a new PCollection. Thus, allowing multi branching.
Basic pipeline with forward branching. An example of this is the simple read from kinesis and print to console example. 
public class wrtie_to_dynamo {
	
	public static void main(String[] argvs) {
	String aws_access_key_id = "*************";
	String aws_secret_access_key = "**********************";
	String streamName = "test";
	PipelineOptions options = PipelineOptionsFactory.create();
	Pipeline p = Pipeline.create(options);
	p.apply(KinesisIO.read()
		.withStreamName(streamName)
		.withInitialPositionInStream(InitialPositionInStream.LATEST)
		.withAWSClientsProvider(aws_access_key_id, aws_secret_access_key, Regions.US_EAST_1))
		.apply(
			ParDo.of(
				new DoFn<KinesisRecord, byte[]>(){
				@ProcessElement
				public void processElement(ProcessContext c) {
					KinesisRecord record = c.element();
					byte[] data = record.getData().array();
					System.out.println(new String(data));
						c.output(data);
					}}));
		p.run().waitUntilFinish();
		
	}
}

A pipeline that branches to to multiple PCollections from one PCollections. Here we will build upon the same code example above to add a transformation that extracts the thing_id as device_id and holds the output to be queried later. In the diagram below please note that our source is kinesis stream and not a DB. In the code below we did use the data from kinesis stream to branch and create a PCollect of deviceIs, which i do return a map of the device and the timestamp. Later this may be valuable if mixed with e event processing heuristic watermark to go about taking into account what devices have reported of or an error. A lot can come out of this functionality. We will keep exploring later.

	

If we are to merge our pipelines meaning PCollections. There are two functions that aid with this but not limited to so. 
Flatten - Combine multiple PCollections of the same type to one PCollection
Join - Use COGroupByKey transform to do a relation join of two PCollections to a single PCollection.
	
A single transform can produce multiple outputs
To do this we have to use tagged outputs. Let us visualize a concept of this and then look at some code to go with it. The diagram is key not the words in them as I borrowed the example and am gonna write code differently to come back to relate to our work,







To use apache we need to create a driver program. Here is a simple way to create a driver program(link):
→ Create a Pipeline object and set the pipeline execution options, including the Pipeline Runner.
→ Create an initial PCollection for pipeline data, either using the IOs to read data from an external storage system, or using a Create transform to build a PCollection from in-memory data.
→ Apply PTransforms to each PCollection. Transforms can change, filter, group, analyze, or otherwise process the elements in a PCollection. A transform creates a new output PCollection without modifying the input collection. A typical pipeline applies subsequent transforms to each new output PCollection in turn until processing is complete. However, note that a pipeline does not have to be a single straight line of transforms applied one after another: think of PCollections as variables and PTransforms as functions applied to these variables: the shape of the pipeline can be an arbitrarily complex processing graph.
→ Use IOs to write the final, transformed PCollection(s) to an external source.
→ Run the pipeline using the designated Pipeline Runner.






Some Classes: link for all classes
→ ParDo is the core element-wise transform in Apache Beam, invoking a user-specified function on each of the elements of the input PCollection to produce zero or more output elements, all of which are collected into the output PCollection. Thus, one needs to create a separate function to run with pardo or an inline function as seen below in the examples of branching code.
→ Flatten<T> takes multiple PCollection<T>s bundled into a PCollectionList<T> and returns a single PCollection<T> containing all the elements in all the input PCollections. The name "Flatten" suggests taking a list of lists and flattening them into a single list.

Please look for the rest in the classes link provided above.

Bounded Data is straight forward. Thus we will deal with unbounded data same as streaming data. In this context we need to understand event time and processing time. We care about the correctness and context of data thus we have to use event time and not processing time. But processing time window for data as it comes and is being observed is a good use case. This captures things like usage volume monitoring or so but not useful for specific events that we want to compare or coordinate.

Given that we are to be with much interest in event time. Then we out to know about the following terms:
Windowing → To make sense of unbounded data that is by theory ever ending, then we have to have a window which is time frame to which we are going to observe the data. And here we have fixed, session and sliding. Fixed we use a definite time frame. Sliding we use a time frame and then process a fraction of that time frame. An example is kinesis data stream 24hour retention. Thus we will check every minute of the 24hour window. Session is on that captures a session id of a device, then process what the device is doing or so. This is scattered and may not have valuable insights unless we are targeting that entity.

Watermarks →  A watermark is a notion of input completeness with respect to event times. This is used to mark where data has been observed, useful in the sliding window. Thus a metric of progress. Now there are perfect watermarks for a perfect world and heuristic for a world where we allow lateness.

Triggers →  A trigger is a mechanism for declaring when the output for a window should be materialized relative to some external signal. 

Accumulation →  An accumulation mode specifies the relationship between multiple results that are observed for the same window. 

Semantics Exploration 
The reference of semantics here is to the data processing.
To be efficient at processing streaming data, we need to take the following into consideration:
Correctness
Local state fundamental to processing. Thus, being consistent for exactly once processing of the data is key to correctness which will aid us to go over the lambda architecture. This comes to the picture with having fault tolerant to our environment which aws already provides given we configure things well. Shall give us an advantage.
Tools for reasoning about time should be based on time domain as well as unbounded(unordered or ordered) data of many event time.

Thus let us introduce event time vs processing time:
Event time processing →  is processing an event in our apache beam code or app with accordance with the actual time the event happened. This has significance with knowing when actual events happened like time for a robot to do a transplant so as to be sure of the time windows.
Processing time → is processing an event with accordance of arrival. Thus we do not care or careless of the events that happened on actual time as it may not have an impact on our processing and what we want to get out of the data.

Now we need to note that processing time and event time should be the same in an idle world but that is not the case. An example is if we want to do streaming of data from a motor to aws iot. A motor may go down. Now that the motor has gone down, we go reboot it and it starts to work again and send the failure message. At this point, the event time and the processing time of the failure are going to be different. Thus given the relationship with the graph below.


In the figure above skewing is caused by the processing time not matching the real time. To solve this we introduce windowing and the problem of completeness. Thus, a system to allow new data to alive, old to be retracted or updated will allow us to solve the completeness problem which is a concept we are going to embed in the things to think about when creating our pipeline as explained above.


Data Processing Patterns
Bounded data
This will start with a sata set that is finite and full of entropy. Meaning this could be data from a database that is yet to be structured in a way you want. Such is the ingestion process we have in our pipeline. In our case we are going to look at the word count example that we are to run via locally using apache beam, read from a file and then submit to a local spark installed engine. Thus a spark runner.
Please refer to Bounded example.

Unbounded data - batch
This involves slicing up unbounded data into a collection of bounded data sets which makes this appropriate for batch processing. We have two ways to do this one is by using fixed windows and another b using sessions.

Fixed Windows
Let us say that we have data coming from AWS IOT at the rate of 200miliseconds. Now our need for processing is not that urgent. Thus we want to process the data every minute. What the outcome of this is that we have by definition defined our window of a minute. As such our data in a kinesis stream or kafka stream will be read by a chunk window of 1 minute. The backside to this is that if by any means if an AWS IOT delivery was late. Then we have will an event by a minute. Which causes a completeness issue. An example is we want to know the state of all the pumps. And if we need to have a threshold of 6 out 10 pumps running. Then we miss the 6th pump event sent on the true time event of that processing window. By definition we will get a result yes  but not a correct result for not all the needed data sets where present at the time of processing as needed. To solve this we may need to retract the data or update the current window. Please see example in the folder named unbounded/fixed_windows_batch

Sessions
These are periods of activity that are terminated by a gap of periods of inactivity. Using this method to process thee unbounded data, always ends up with sessions split across batches.
To solve this we can increase the session window which may cost us with higher latencies. This method is complex and not of recommendation to use for unbounded batch streaming. There will not be an example for this one.

Unbounded data - streaming
Given the nature of a stream and processing need. We will have four ways to deal with unbounded data as well as unbounded unordered data. For the more part, the latter is the situation of the real world.
Time agnostic
Approximation
Windowing by processing time
WIndowing by event time

Time agnostic:
This applies to data that we do not care about the time correctness. Such we may want to consider things like counting events like clicks on a website. This can also be used in the following context.
FIltering
Suppose we are only interested with pump events from a stream that have pump events, thermometer events as well as other events. Since we do not really care for time to begin with, but want to only care for processing for pump events, then within that window we can listen for pump events and then do processing when we get the pump event otherwise do nothing or no process.
Inner Joins or has joins
From the idea of having two streams of so. We may be interested to see two events that occur together. Like if a pump is OFF and the light ON, this may mean that the plants may be starved as they need water to do photosynthesis. Given with hypotheticals, that a pump stream and a light stream, we can read from both streams and with the same window of definition. Thus, an action such as sending a notification if this condition is met may be taken. At this point, we see that this is like an AND set join. In a way we can see that this processing will have a completeness idle. For nothing can happen unless we have the conditions necessary. Time is not an element we have to introduce
Out Joins
This is like a OR set join. Thus we only have to get data from one domain of time for an action the above to fire up. Although yes this will need to have a windowing domain to make completeness of why we will chose to process then.

Approximation:
This method of processing streaming data is not easy to account for. For one needs to have an algorithm that computes a time decay. For the sake of time and this documentation. Let us kno that this exists.

WIndowing:
At last we come to a place where i can define windowing as : Taking a data source i.e kinesis stream, chop it to temporal boundedness into finite chunks for processing. Also to save you from confusion of the flow of this document I have kept the Windowing as a major heading for it is of importance to what we are trying to achieve in this paper. But this is still part of the Unbounded data - streaming mechanism.

Now inside windowing. We can have three ways to window data:
Fixed windows - time segment applied uniformly across the entire data set.
Sliding window - we define a window by a fixed length and a fixed period
Sessions - these are dynamic windows, are sequences of events terminated by a gap of inactivity greater than some timeout. This is not the same notion as we talked about earlier.


Wondowing by Processing Time:
In this place semantics of processing data is easy. So data is buffered into the system in time windows until some amount of processing time has passed. Thus system can jusge data completeness by the processing time set. Which then does not take into account late due to network latency and so forth.

Windowing by Event Time:
Since in real time we may encounter the same issue of lateness as what the above windowing does not take into account. We are forced to employ the following methods to at least reach certain expectations. 
Buffering: Here we extend a window’s lifetime, thus more data into a window thus we can see this as an increase in completeness. This will this will increase persistency states at the expense of resource costs like RAM, CPU etc…
Completeness: 
Using things like watermarks to tell of already seen data, we can then have a way to see when we can produce the data ready for processing. Thus use of watermarks can be seen as a measure of progress which can infer to completeness. 
Using Trigger are mechanisms to fire up processing of data upon a certain external signal. Thus gives us the ability to decide when the output of data to be processed should be done. This same mechanisms allows for data reprocessing if need be. It will be up to the complexity of code at this point. Like in a window if we receive new data per given lateness amount then we can trigger to reprocess the data from a watermark relative position. This is ideal for the 6 out of 10 pumps scenario above. Where we would receive the 6th pump data out of window but within the lateness boundary and then reprocess the event and send the notification if need be.
 Accumulation is a concept of keeping data of the same window for a given time. The point is that we use this data to update, change the results or observe for correctness. Thus this comes with a tough point of cost for resources and semantics.

Some extra things to note:
Watermarks Types:
→ Perfect : These assume no late data, assume that the data is known and all data is early or on time.
→ Heuristic: This is the progressiveof the above, and does aim to fill in the blanks of the perfect model. Meaning that since we do not know the data nor the arrival of it. It then allows you to have an estimate of a progressive marker of where you are, thus a measure of completeness. These can be too slow or too fast. Thus to handle these we may need a bound set of early trigger and late triggers to aid handle the question of when to materialise results. An example of signals used to trigger are:
Watermark progress(event time progress)
Processing time progress
Element count , thus a trigger happens after a certain count
 Punctuations such as EOF or end of large message mark
Repetition is used with processing time to regular provided updates or period updates
Conjunction which is a logical AND, thus fires only when the children triggers have fired
Disjoint(logical OR) trigger, which is fire when any child trigger fires. 
 Sequence fire only when a certain set of children triggers have fired in a certain defined order.

From the above defined points, we can come to ask pivotal questions which will aid in the streaming of our data.
What results are calculated?
→ This is answered by what time of transformation we do on the data upon getting in the PCollections. This is in the context of programming semantics. This can be computing sums or so.

 Where in event time are results calculated?
→ This is answered by the use of event time processing as well as processing time windowing if need be.


When in processing time are results materialised?
→ This is answered by the use of watermarks and triggers as seen in the code examples. With this, there are many parttens to which we can apply to aid materialisation of results. An example is we can declare a watermark and then trigger before the watermark, meaning we are going to computer speculative partial results which may be useful for certain situations. To see where i am going with this, we cna therefore say we can as well trigger after a water mark or at the watermark. Which would mean a lot of things given a scenario at hand.

How do refinement of results relate?
→ This is answered by the type of accumulation we use. If we choose to discard the data, we can assume that the results are independent and distinct. Accumulate where later results build upon the current one. And lastly accumulate and retract where in case of summation of clicks in minutes or so can be gotten by adding the previous window.



