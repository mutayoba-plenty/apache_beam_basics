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
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptors;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;

public class mergeTwoPCollections {
	public static void main(String[] argvs) {
		String aws_access_key_id = "***********";
		String aws_secret_access_key = "+++++++++++++++++";
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
	}
}

