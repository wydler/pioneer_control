/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package eu.wydler.pub;

import org.ros.message.geometry_msgs.Twist;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import com.google.common.base.Preconditions;

/**
 * @author michael.wydler@googlemail.com (Michael Wydler)
 */
public class Talker implements NodeMain {

	private Node node;
	private Publisher<org.ros.message.geometry_msgs.Twist> publisher;

	@Override
	public void main(NodeConfiguration configuration) {
		Preconditions.checkState(node == null);
		Preconditions.checkNotNull(configuration);
		try {
			node = new DefaultNodeFactory().newNode("talker", configuration);
			publisher = node.newPublisher("rpy", "geometry_msgs/Twist");
		} catch (Exception e) {
			if (node != null) {
				node.getLog().fatal(e);
			} else {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void shutdown() {
		if (node != null) {
			node.shutdown();
			node = null;
		}
	}
	
	public void publish(Twist t) {
		try {
			publisher.publish(t);
		} catch (Exception e) {
			if (node != null) {
				node.getLog().fatal(e);
			} else {
				e.printStackTrace();
			}
		}
	}
}
