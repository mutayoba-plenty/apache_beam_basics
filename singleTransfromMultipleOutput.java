/**
* Example to show how we can use single transform to get multiple outputs
* This can translate to pumps or all sort of data aggregation we can make
*/
package org.apache.beam.examples;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.kinesis.KinesisIO;
import org.apache.beam.sdk.io.kinesis.KinesisRecord;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.DoFn.Element;
import org.apache.beam.sdk.transforms.DoFn.OutputReceiver;
import org.apache.beam.sdk.transforms.DoFn.ProcessContext;
import org.apache.beam.sdk.transforms.DoFn.ProcessElement;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptors;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;


public class single_transform_multiple_outputs {

	
	final static TupleTag<String> startsWithATag = new TupleTag<String>(){};
	final static TupleTag<String> startsWithBTag = new TupleTag<String>(){};

	public static void main(String[] argvs) {
		String aws_access_key_id = "AKIAZRGYLAEH3OYBUP5S";
		String aws_secret_access_key = "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";
		String streamName = "test";
		PipelineOptions options = PipelineOptionsFactory.create();
		Pipeline p = Pipeline.create(options);
		PCollection<byte[]> getDataAsByte = p.apply(KinesisIO.read()
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
								JSONObject obj = new JSONObject(new String(data));
								System.out.println(obj);
								c.output(data);
							}
						}));
		PCollection<PCollectionTuple> unsorted_pump_data = getDataAsByte.apply("SeparateStreamByDevice", 
				ParDo.of(new DoFn<byte[], PCollectionTuple>(){
					@ProcessElement
					public void processElement(ProcessContext c) {
						JSONObject obj = new JSONObject(new String(c.element()));
						String pumpA = "tsse";
						String pumpB = "tes";
						if (obj.get("thing_id").equals(pumpA)){
							c.output(c.element());
						}
						if (obj.get("thing_id").equals(pumpA)){
							c.output(startsWithBTag,c.element());
						}
					}
				})
					
			).withOutputTags(startsWithATag,
			        // Specify the output with tag startsWithBTag, as a TupleTagList.
                    TupleTagList.of(startsWithBTag))));
                    
        PCollection<PCollectionTuple> sortedA = unsorted_pump_data.get(startsWithATag);
        PCollection<PCollectionTuple> sortedB = unsorted_pump_data.get(startsWithBTag);
		p.run().waitUntilFinish();
		
	}
}
