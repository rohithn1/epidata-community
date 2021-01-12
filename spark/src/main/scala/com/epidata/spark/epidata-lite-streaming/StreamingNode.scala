/*
* Copyright (c) 2020 EpiData, Inc.
*/
package com.epidata.spark

import com.epidata.spark.ops.Transformation
import org.json.simple.JSONObject
import reactivemongo.play.json._
import org.zeromq.ZMQ

object StreamingNode {
  var subSocket: ZMQ.Socket = _ //add as parameter
  var forwardSocket: ZMQ.Socket = _ //add as parameter
  var publishTopic: String = _
  var transformation: Transformation = _

  def init(
    context: ZMQ.Context,
    receivePort: String,
    publishPort: String,
    receiveTopic: String,
    publishTopic: String,
    transformation: Transformation): StreamingNode.type = {
    subSocket = context.socket(ZMQ.SUB)
    subSocket.connect("tcp://127.0.0.1:" + receivePort)
    subSocket.subscribe(receiveTopic.getBytes(ZMQ.CHARSET))

    forwardSocket = context.socket(ZMQ.PUB)
    forwardSocket.bind("tcp://127.0.0.1:" + publishPort)

    this.publishTopic = publishTopic

    this.transformation = transformation

    this
  }

  //  def init(context: ZMQ.context,
  //           receivePort: String,
  //           publishPort: String,
  //           receiveTopic: Array[String],
  //           publishTopic: String,
  //           transformation: Transformation): StreamingNode.type = {
  //    subSocket = context.socket(ZMQ.SUB)
  //    subSocket.connect("tcp://127.0.0.1:" + receivePort)
  //    for (topic <- receiveTopic) {
  //      subSocket.subscribe(topic.getBytes(ZMQ.CHARSET))
  //    }
  //
  //    forwardSocket = context.socket(ZMQ.PUB)
  //    forwardSocket.bind("tcp://127.0.0.1:" + publishPort)
  //
  //    this.publishTopic = publishTopic
  //
  //    this.transformation = transformation
  //    this
  //  }
  //
  //  def init(context: ZMQ.context,
  //           receivePort: String,
  //           publishPort: String,
  //           receiveTopic: Array[String],
  //           publishTopic: String,
  //           transformation: Transformation): StreamingNode.type = {
  //    subSocket = context.socket(ZMQ.SUB)
  //    subSocket.connect("tcp://127.0.0.1:" + receivePort)
  //    for (topic <- receiveTopic) {
  //      subSocket.subscribe(topic.getBytes(ZMQ.CHARSET))
  //    }
  //
  //    forwardSocket = context.socket(ZMQ.PUB)
  //    forwardSocket.bind("tcp://127.0.0.1:" + publishPort)
  //
  //    this.publishTopic = publishTopic
  //
  //    this.transformation = transformation
  //    this
  //  }
  //
  //  def init(context: ZMQ.context,
  //           receivePort: String,
  //           publishPort: String,
  //           receiveTopic: Array[String],
  //           publishTopic: String,
  //           transformation: util.ArrayList[Transformation]): StreamingNode.type = {
  //    subSocket = context.socket(ZMQ.SUB)
  //    subSocket.connect("tcp://127.0.0.1:" + receivePort)
  //    for (topic <- receiveTopic) {
  //      subSocket.subscribe(topic.getBytes(ZMQ.CHARSET))
  //    }
  //
  //    forwardSocket = context.socket(ZMQ.PUB)
  //    forwardSocket.bind("tcp://127.0.0.1:" + publishPort)
  //
  //    this.publishTopic = publishTopic
  //
  //    for (transform <- transformation) {
  //      this.transformation.add(transform)
  //    }
  //    this
  //  }

  def receive(): Unit = {
    val topic = subSocket.recvStr() //measurements or passBack
    //val messageObject = new JSONObject(subSocket.recvStr()) //JSON formatted Message {"topic":[topic]"key":[key],"value":[message]}
    val messageObject = (Json.parse(subSocket.recvStr()) \ "key_value").as[Map[String, String]]
    publish(new Message(publishTopic, messageObject("key"), this.transformation.apply(messageObject("value"))))
  }

  def publish(processedMessage: Message): Unit = {
    //val processedMessage: Message = epidataLiteStreamingContext(ZMQInit.streamQueue.dequeue)
    forwardSocket.sendMore(this.publishTopic)
    val msg: String = JSON.format(processedMessage)
      //JSON.format(processedMessage)
    forwardSocket.send(msg.getBytes(), 0)
  }

}