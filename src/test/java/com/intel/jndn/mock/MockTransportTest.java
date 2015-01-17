/*
 * File name: MockTransportTest.java
 * 
 * Purpose: Test the MockTransport
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.mock;

import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Mock the transport class
 * TODO add face.registerPrefix() example
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class MockTransportTest {

  /**
   * Setup logging
   */
  private static final Logger logger = LogManager.getLogger();

  /**
   * Test sending a Data packet.
   * 
   * @throws java.io.IOException
   * @throws net.named_data.jndn.encoding.EncodingException
   */
  @Test
  public void testSendData() throws IOException, EncodingException {
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // setup return data
    Data response = new Data(new Name("/a/b/c"));
    response.setContent(new Blob("..."));
    transport.respondWith(response);

    // express interest on the face
    final Counter count = new Counter();
    face.expressInterest(new Interest(new Name("/a/b/c")), new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        count.inc();
        logger.debug("Received data");
        assertEquals(data.getContent().buf(), new Blob("...").buf());
      }
    });
    
    while(count.get() == 0){
      face.processEvents();
    }
  }
  
  
  /**
   * Test sending multiple Data packets.
   * 
   * @throws java.io.IOException
   * @throws net.named_data.jndn.encoding.EncodingException
   */
  @Test
  public void testSendMultipleData() throws IOException, EncodingException {
    MockTransport transport = new MockTransport();
    Face face = new Face(transport, null);

    // setup return data
    Data response1 = new Data(new Name("/a/b/c/1"));
    response1.setContent(new Blob("..."));
    transport.respondWith(response1);
    Data response2 = new Data(new Name("/a/b/c/2"));
    response2.setContent(new Blob("..."));
    transport.respondWith(response2);

    // express interest on the face
    final Counter count = new Counter();
    face.expressInterest(new Interest(new Name("/a/b/c/1")), new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        count.inc();
        logger.debug("Received data");
        assertEquals(data.getContent().buf(), new Blob("...").buf());
      }
    });
    
    while(count.get() == 0){
      face.processEvents();
    }
    
    // express interest again, but this time it should time out because there 
    // is no data left on the wire; the first processEvents() has already 
    // picked it up
    final Counter count2 = new Counter();
    Interest failingInterest = new Interest(new Name("/a/b/c/2"));
    failingInterest.setInterestLifetimeMilliseconds(50);
    face.expressInterest(failingInterest, new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        count2.inc();
        fail("Should not return data; data should already be cleared");
      }
    }, new OnTimeout(){
      @Override
      public void onTimeout(Interest interest) {
        count2.inc();
        assertTrue(true);
      }
    });
    
    while(count2.get() == 0){
      face.processEvents();
    }
  }
  
  /**
   * Count reference
   */
  class Counter{
    int count = 0;
    public void inc(){
      count++;
    }
    public int get(){
      return count;
    }
  }
}
