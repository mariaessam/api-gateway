package com.cit.vericash.api.gateway.filter;


import com.cit.vericash.api.gateway.communication.WorkflowService;
import com.cit.vericash.api.gateway.exception.ApiGatewayException;
import com.cit.vericash.api.gateway.model.*;
import com.cit.vericash.api.gateway.security.DuplicateRequestPreventerComponent;
import com.cit.vericash.api.gateway.security.MacComponent;
import com.cit.vericash.api.gateway.service.ApprovalCycleCacheService;
import com.cit.vericash.api.gateway.service.ApprovalCycleService;
import com.cit.vericash.api.gateway.util.*;
import com.cit.vericash.portal.backend.data.QueryService;
import com.cit.vericash.portal.backend.model.dao.Criteria;
import com.cit.vericash.portal.backend.model.dao.QueryMessage;
import com.cit.vericash.portal.backend.model.dao.Record;
import com.cit.vericash.portal.backend.model.dao.ResultSet;
import com.cit.vericash.portal.backend.model.message.Message;
import com.cit.vericash.portal.backend.model.request.NonSslRequest;
import com.cit.vericash.portal.backend.model.request.Request;
import com.cit.vericash.portal.backend.model.request.SslRequest;
import com.cit.vericash.portal.backend.util.PropertyLoader;
import com.cit.vericash.portal.backend.util.ServiceUtil;
import com.cit.vericash.portal.backend.util.VericashLogger;
import com.google.gson.Gson;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class PreFilter extends ZuulFilter {
    private static final String APPROVAL_CYCLE = "Approval Cycle";

    @Autowired
    PropertyLoader propLoader;

    @Autowired
    ErrorsUtil errorsUtil;

    @Autowired
    private MacComponent macComponent;

    @Autowired
    private DuplicateRequestPreventerComponent duplicateRequestPreventerComponent;

    @Autowired
    private QueryService queryService;

    @Autowired
    WorkflowService workflowService;

    @Autowired
    ApprovalCycleCacheService approvalCycleCacheService;

    @Autowired
    ServiceUtil serviceUtil;

    @Autowired
    PropertyLoader propertyLoader;

    @Autowired
    ApprovalCycleService approvalCycleService;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        //VericashLogger.info("Method: "+ RequestContext.getCurrentContext().getRequest().toString());
        return !RequestContext.getCurrentContext().getRequest().getMethod().equalsIgnoreCase("get");
    }

    @Override
    public Object run(){
        RequestContext context = RequestContext.getCurrentContext();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        String requestURI=context.getRequest().getRequestURI();
        VericashLogger.info("requestURI: "+requestURI);
        String method=context.getRequest().getMethod();
        if(method.equalsIgnoreCase("post")) {
            InputStream in = (InputStream) context.get("requestEntity");
            try {
                String uploadedFilesURI = propLoader.getPropertyAsString("upload.file.uri", Constants.GATEWAY_FOLDER_NAME, Constants.GATEWAY_CONFIG_FILE_NAME);
                if (CommonUtil.isUploadFileService(uploadedFilesURI)) {
                    if (in == null) {
                        in = context.getRequest().getInputStream();
                    }
//            in.close();
                    return context;
                }
                String startDateStr = DateFormatterUtil.format(new Date());


                if (in == null) {
                    in = context.getRequest().getInputStream();
                }
                String body = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
//                if(body.toLowerCase().contains("select ") || body.toLowerCase().contains("select *") || body.toLowerCase().contains("select*") || body.toLowerCase().contains("insert into") || body.toLowerCase().contains("delete ") || body.toLowerCase().contains("update "))
//                {
//                    throw new ApiGatewayException("Security Error",-1,"Invalid request");
//                }
                body=new String(body.getBytes(), StandardCharsets.UTF_8);
//                VericashLogger.info("PreFilter:Body before decryption:"+body);
                Request request = null;
                Gson gson = JsonUtil.getGson();
                Boolean isSSLEnabled = propLoader.getPropertyAsBoolean("ssl.enabled", Constants.GATEWAY_FOLDER_NAME, Constants.GATEWAY_CONFIG_FILE_NAME);
                SslRequest sslRequest = null;
                if (isSSLEnabled) {
                    request = gson.fromJson(body, SslRequest.class);
                    sslRequest = (SslRequest) request;
//                    VericashLogger.info(sslRequest.getMessage().toString());
                    if (sslRequest.getMessage() == null) {
                        setContextParameters(context, in, startDateStr, body, sslRequest);
                        throw new ApiGatewayException(Constants.GENERAL_ERROR_TITLE, 1000, errorsUtil.getDescription("API1000"));
                    }
                } else {
                    request = gson.fromJson(body, NonSslRequest.class);
                    sslRequest = new SslRequest(request.getMac());
                    if (((NonSslRequest) request).getEncryptedMessage() == null) {
                        setContextParameters(context, in, startDateStr, body, sslRequest);
                        throw new ApiGatewayException(Constants.GENERAL_ERROR_TITLE, 1000, errorsUtil.getDescription("API1000"));
                    }
                }
                /*If ssl.enabled is true, message will be decrypted,
                otherwise index 0 in the array of the request will be taken as the message*/
                Message message = sslRequest.getMessage();
                requestAttributes.setAttribute("message", message, RequestAttributes.SCOPE_REQUEST);
                String serviceCode=message.getHeader().getAttributeAsString("serviceCode");
                Long userId=message.getHeader().getAttributeAsLong("userId");
                /*If mac.enabled is true, checks the mac string
                 *if it's the same sent from the front-end*
                 */

                if (macComponent.isDataAltered(serviceCode,body, request.getMac(),userId)) {
                    throw new ApiGatewayException(Constants.SECURITY_ERROR_TITLE, 1002, errorsUtil.getDescription("API1002"));
                }

                /*If the same timestamp in the request exists in the database, an exception is thrown*/
                if (duplicateRequestPreventerComponent.isDuplicateRequest(message)) {
                    throw new ApiGatewayException(Constants.SECURITY_ERROR_TITLE, 1002, errorsUtil.getDescription("API1002"));
                }

                sslRequest.setMessage(message.clone());
                addAdditionalParamToHeader(message, Constants.HEADER_DATABASE_CONNECTION_TYPE);
                body = gson.toJson(message);
                VericashLogger.info("PreFilter:Body after decryption:"+body+"\n"+requestURI);


                setContextParameters(context, in, startDateStr, body, sslRequest);

                //  accept and reject workflow Service not enter the approval cycle
                if (serviceCode != null && !serviceCode.equals(Constants.APPROVAL_SERVICE) && !serviceCode.equals(Constants.REJECT_SERVICE))// accept and reject workflow Service
                {
                    boolean isEnabled = approvalCycleCacheService.isApprovalCycleEnabled(message);
                    if (isEnabled) {
                        String errorCode = "";
                        setContextParameters(context, in, startDateStr, body, sslRequest);
                        Boolean isGroupWorkflowEnabled=propertyLoader.getPropertyAsBoolean("enable.group.workflow","workflow-service","application");
                        if(isGroupWorkflowEnabled)
                        {
                            LinkedHashMap approvalResponse = (LinkedHashMap)serviceUtil.post("http://workflow-service/start-approvalCycle",message);
                            errorCode = (String) approvalResponse.get("errorCode");
                            String errorMessage = (String) approvalResponse.get("errorMessage");
                            if(errorCode!=null && (errorCode.equals("WFL1006") || errorCode.equals("WFL1007") || errorCode.equals("WFL1008")))
                            {
                                throw new ApiGatewayException(APPROVAL_CYCLE, 1007,errorMessage);
                            }
                        }else { //Normal Approval Cycle
                            setContextParameters(context, in, startDateStr, body, sslRequest);
                            Long teamId=0L;
                            Record team = getTeamsInvolved("" + userId);
                            if (team != null && team.size() != 0) {
                                teamId = Long.parseLong(team.get("T.TEAM_ID") + "");
                            } else {
                                String errorDesc = propLoader.getPropertyAsString("user.is.not.assigned.any.teams.error.desc");
                                throw new ApiGatewayException(APPROVAL_CYCLE, 1009, errorDesc);
                            }
                            message.getAdditionalData().setAttribute("teamId",teamId);
                            LinkedHashMap approvalResponse= approvalCycleService.start(message);
                        }

                        throw new ApiGatewayException(APPROVAL_CYCLE, 1008, "Approval cycle is started for this action");
                    }

                } else if (serviceCode != null && serviceCode.equals(Constants.APPROVAL_SERVICE)) {
                    message.getAdditionalData().setAttribute("apprvalcycle-serviceName", getApprovalCycleServiceName(message));
                    if (IsNewWorkflow(message) != null)
                        message.getAdditionalData().setAttribute("newWorkflow", "true");
                }




            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        return context;
    }

    private void setContextParameters(RequestContext context, InputStream in, String startDateStr, String body, SslRequest sslRequest) throws IOException {
        context.addZuulRequestHeader("Content-Type","application/json");
        context.getZuulRequestHeaders().remove("Content-Length");
        context.setRequest(modifyRequest(context.getRequest(), body));
//            VericashLogger.info("start date"+ startDateStr);
        context.set("startDate", startDateStr);
        context.set("originalRequest", sslRequest);
    }

    /*Takes the message,
     *loads the property from the configuration
     *and adds it to the header of the Message
     */
    private void addAdditionalParamToHeader(Message message,String property){
        String propertyValue = propLoader.getPropertyAsString(property, Constants.GATEWAY_FOLDER_NAME,Constants.GATEWAY_CONFIG_FILE_NAME);
        message.getHeader().put(property,propertyValue);
    }

    /*
     *This class is used for overriding the request
     */
    private static HttpServletRequestWrapper modifyRequest(HttpServletRequest request, final String body) {

        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {

            public byte[] getContentData() {
                byte[] data = null;
                data = body.getBytes(StandardCharsets.UTF_8);
                return data;
            }

            @Override
            public int getContentLength() {
                return body.getBytes().length;
            }

            @Override
            public long getContentLengthLong() {
                return body.getBytes().length;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))));
            }

            @Override
            public ServletInputStream getInputStream() throws UnsupportedEncodingException {
                return new ServletInputStreamWrapper(body.getBytes(StandardCharsets.UTF_8));
            }
        };

        return wrapper;
    }


    private String getApprovalCycleServiceName(Message message)
    {
        String approvalCycleQuery=propLoader.loadQuery("workflowServiceName.query");
        QueryMessage queryMessage=new QueryMessage();
        queryMessage.setQuery(approvalCycleQuery);
        List<Criteria> wherecriteriaList=new ArrayList<Criteria>();
        Long taskId = message.getPayload().getAttributeAsLong("taskId");

        String serviceCode = message.getHeader().getAttributeAsString("serviceCode");
        wherecriteriaList.add(new Criteria("var.name_","apprvalcycle-serviceName", Criteria.Type.Text, Criteria.Operator.Equals));
        wherecriteriaList.add(new Criteria("TASK.ID_",taskId, Criteria.Type.Number, Criteria.Operator.Equals));
        queryMessage.setCriteriaList(wherecriteriaList  );
        ResultSet resultSet =  queryService.executeQuery(queryMessage);
        String serviceName = null;
        if(resultSet != null && resultSet.getRecords().size() >0)
            serviceName = "" + resultSet.getRecords().get(0).get("text_");



        return serviceName;
    }

    private String IsNewWorkflow(Message message)
    {
        String approvalCycleQuery=propLoader.loadQuery("workflowServiceName.query");
        QueryMessage queryMessage=new QueryMessage();
        queryMessage.setQuery(approvalCycleQuery);
        List<Criteria> wherecriteriaList=new ArrayList<Criteria>();
        Long taskId = message.getPayload().getAttributeAsLong("taskId");

        String serviceCode = message.getHeader().getAttributeAsString("serviceCode");
        wherecriteriaList.add(new Criteria("var.name_","newWorkflow", Criteria.Type.Text, Criteria.Operator.Equals));
        wherecriteriaList.add(new Criteria("TASK.ID_",taskId, Criteria.Type.Number, Criteria.Operator.Equals));
        queryMessage.setCriteriaList(wherecriteriaList  );
        ResultSet resultSet =  queryService.executeQuery(queryMessage);
        String IsNewWorkflow = null;
        if(resultSet != null && resultSet.getRecords().size() >0)
            IsNewWorkflow = "" + resultSet.getRecords().get(0).get("text_");

        return IsNewWorkflow;
    }

    private Record getTeamsInvolved(String UserId){

        Record team=null;
        String teamQuery = propertyLoader.getPropertyAsString("teamInvloved.query","workflow-service","queries");
        QueryMessage queryMessage=new QueryMessage();
        queryMessage.setQuery(teamQuery);
        List<Criteria> criteriaList=new ArrayList<Criteria>();
        criteriaList.add(new Criteria("BU.BUSINESS_USER_ID",UserId, Criteria.Type.Text, Criteria.Operator.Equals));
        queryMessage.setCriteriaList(criteriaList);
        ResultSet resultSet=queryService.executeQuery(queryMessage);
        if(resultSet.getRecords()!=null && resultSet.getRecords().size()>0)
        {
            team=resultSet.getRecords().get(0);
        }

        return  team;
    }
}
