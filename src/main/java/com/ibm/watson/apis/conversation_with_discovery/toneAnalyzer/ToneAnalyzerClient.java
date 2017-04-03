/*
 * Copyright 2015 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.watson.apis.conversation_with_discovery.toneAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ibm.watson.apis.conversation_with_discovery.payload.DocumentPayload;
import com.ibm.watson.apis.conversation_with_discovery.utils.Constants;
import com.ibm.watson.apis.conversation_with_discovery.utils.Messages;
import com.ibm.watson.developer_cloud.discovery.v1.model.query.QueryResponse;

import com.ibm.watson.developer_cloud.tone_analyzer.v3_beta.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3_beta.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3_beta.model.ToneCategory;
import com.ibm.watson.developer_cloud.tone_analyzer.v3_beta.model.ToneScore;

/**
 * DiscoveryClient.
 */
public class ToneAnalyzerClient {


  private String usernameToneAnalyzer = System.getenv("TONE_ANALYZER_USERNAME");

  private String passwordToneAnalyzer = System.getenv("TONE_ANALYZER_PASSWORD");

  private static final Logger logger = LogManager.getLogger(ToneAnalyzerClient.class.getName());

  private static final int SNIPPET_LENGTH = 150;

  private Pattern pattern = Pattern.compile("((.+?)</p>){1,2}");

  /**
   * This method uses the Query object to send the user's query (the <code>input</code> param) to the discovery service.
   *
   * @param input The user's query to be sent to the discovery service
   * @return A list of DocumentPayload objects, each representing a single document the discovery service believes is a
   *         possible answer to the user's query
   * @throws Exception the exception
   */
  public ToneAnalysis getTone(String input) throws Exception {
    ToneAnalyzer serviceToneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_02_11);
    serviceToneAnalyzer.setEndPoint(Constants.TONE_ANALYZER_URL);

    if ((usernameToneAnalyzer != null) || (passwordToneAnalyzer != null)) {
      serviceToneAnalyzer.setUsernameAndPassword(usernameToneAnalyzer, passwordToneAnalyzer);
    }
    ToneAnalysis resultToneAnalysis = serviceToneAnalyzer.getTone(completeConversation.toString()).execute();
    for(ToneCategory tc : resultToneAnalysis.getDocumentTone().getTones()){
      for(ToneScore ts : tc.getTones()){
        System.out.println("Name:  "+ts.getName());
        System.out.println("Score: "+String.valueOf(ts.getScore()));
      }
    }
    return resultToneAnalysis;
  }

  /**
   * Helper Method to include highlighting information along with the Discovery response so the final payload
   * includes id,title,body,sourceUrl as json key value pairs.
   *
   * @param resultsElement the results element
   * @return A list of DocumentPayload objects, each representing a single document the discovery service believes is a
   *         possible answer to the user's query
   */
  private List<DocumentPayload> createPayload(JsonElement resultsElement) {
    logger.info(Messages.getString("Service.CREATING_DISCOVERY_PAYLOAD"));
    List<DocumentPayload> payload = new ArrayList<DocumentPayload>();
    JsonArray jarray = resultsElement.getAsJsonArray();

    for (int i = 0; (i < jarray.size()) && (i < Constants.DISCOVERY_MAX_SEARCH_RESULTS_TO_SHOW); i++) {
      DocumentPayload documentPayload = new DocumentPayload();
      String id = jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_ID).toString().replaceAll("\"", "");
      documentPayload.setId(id);
      documentPayload.setTitle(
          jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_TITLE).toString().replaceAll("\"", ""));
      if (jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_BODY) != null) {
        String body = jarray.get(i).getAsJsonObject()
            .get(Constants.DISCOVERY_FIELD_BODY)
            .toString().replaceAll("\"", "");

        // This method limits the response text in this sample
        // app to two paragraphs.
        String bodyTwoPara = limitParagraph(body);
        documentPayload.setBody(bodyTwoPara);
        documentPayload.setBodySnippet(getSniplet(body));

      } else {
        documentPayload.setBody("empty");
      }
      if (jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_SOURCE_URL) == null) {
        documentPayload.setSourceUrl("empty");
      } else {
        documentPayload.setSourceUrl(
            jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_SOURCE_URL).toString().replaceAll("\"", ""));
      }
      if (jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_CONFIDENCE) != null) {
        documentPayload.setConfidence(
            jarray.get(i).getAsJsonObject().get(Constants.DISCOVERY_FIELD_CONFIDENCE).toString().replaceAll("\"", ""));
      } else {
        documentPayload.setConfidence("0.0");
      }
      payload.add(i, documentPayload);
    }

    return payload;
  }

  /**
   * get first <code>SNIPPET_LENGTH</code> characters of body response.
   *
   * @param body discovery response
   * @return
   */
  private String getSniplet(String body) {
    if (body == null) {
      return "";
    }

    int len = body.length();
    if (len > SNIPPET_LENGTH) {
      body = body.substring(0, SNIPPET_LENGTH - 3) + "...";
    }
    return body;
  }

  /**
   * This method limits the response text in this sample app to two paragraphs. For your own application, you can
   * comment out the method to allow the full text to be returned.
   *
   * @param body
   * @return string
   */
  private String limitParagraph(String body) {
    String returnString = body;

    Matcher matcher = pattern.matcher(body);
    if (matcher.find()) {
      returnString = matcher.group(0);
    }

    return returnString;
  }
}
