/*
 * Licensed to Laurent Broudoux (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.chadwiki.elasticsearch.river.drive.rest;

import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestRequest.Method;
import org.elasticsearch.rest.RestStatus;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
/**
 * REST actions definition for starting and stopping a Google Drive river.
 * @author laurent
 */
public class DriveManageAction extends BaseRestHandler{

   /** The constant for 'start river' command. */
   public static final String START_COMMAND = "_start";
   /** The constant for 'stop river' command. */
   public static final String STOP_COMMAND = "_stop";
   
   @Inject
   public DriveManageAction(Settings settings, Client client, RestController controller){
      super(settings, client);

      // Define S3 REST endpoints.
      controller.registerHandler(Method.GET, "/_drive/{rivername}/{command}", this);
   }
   
   @Override
   public void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception{
      if (logger.isDebugEnabled()){
         logger.debug("REST DriveManageAction called");
      }
      
      String rivername = request.param("rivername");
      String command = request.param("command");
      
      String status = null;
      if (START_COMMAND.equals(command)){
         status = "STARTED";
      } else if (STOP_COMMAND.equals(command)){
         status = "STOPPED";
      }
      
      try{
         if (status != null){
            XContentBuilder xb = jsonBuilder()
               .startObject()
                  .startObject("google-drive")
                     .field("feedname", rivername)
                     .field("status", status)
                  .endObject()
               .endObject();
            client.prepareIndex("_river", rivername, "_drivestatus").setSource(xb).execute().actionGet();
         }
         
         XContentBuilder builder = jsonBuilder();
         builder
            .startObject()
               .field(new XContentBuilderString("ok"), true)
            .endObject();
         channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
      } catch (IOException e) {
         onFailure(request, channel, e);
      }
   }
   
   /** */
   protected void onFailure(RestRequest request, RestChannel channel, Exception e) throws Exception{
      try{
          channel.sendResponse(new BytesRestResponse(channel, e));
      } catch (IOException ioe){
         logger.error("Sending failure response fails !", e);
         channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR));
      }
   }
}
