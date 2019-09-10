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
In our case we want to make our data be in json. For it is easy to serialize the data in case of transferring it to other places or for ease of applying our logic in the key, value pair matrix. WHich apache beam is heavily constructed upon.

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




Things to explore from the Complexity Matrix: streaming 101 and Streaming 102
Individual capabilities have been grouped by their corresponding What / Where / When / How question:
→ What results are being calculated?
	ParDo, Flatten, Combine, Composite Transforms, Stateful Processing, Source API
→ Where in event time?
	Global windows, Fixed windows, Sliding windows, Session windows, Custom windows...
→ When in processing time?
	Configurable triggering, Event-time triggers, , Processing-time triggers, Count triggers...
→ How do refinements of results relate?
	Discarding, Accumulating

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



