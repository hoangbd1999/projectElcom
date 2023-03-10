package com.elcom.metacen.id.controller;

import com.elcom.metacen.id.checkPolicy.ResultCheckDto;
import com.elcom.metacen.id.constant.Constant;
import com.elcom.metacen.id.mail.MailContentDTO;
import com.elcom.metacen.id.model.Unit;
import com.elcom.metacen.id.model.User;
import com.elcom.metacen.id.model.dto.*;
import com.elcom.metacen.id.service.TokenService;
import com.elcom.metacen.id.service.UnitService;
import com.elcom.metacen.id.service.UserService;
import com.elcom.metacen.id.thread.IdThreadManager;
import com.elcom.metacen.id.utils.encrypt.AES;
import com.elcom.metacen.id.validation.UserValidation;
import com.elcom.metacen.message.MessageContent;
import com.elcom.metacen.message.ResponseMessage;
import com.elcom.metacen.utils.StringUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author anhdv
 */
@Controller
public class UserController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UnitService unitService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IdThreadManager idThreadManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TokenService tokenService;

    public ResponseMessage getAllUser(Map<String, String> headerParam, String requestPath, String method, String urlParam) {
        ResponseMessage response = null;
        // L???y th??ng tin user tu Param
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p", null));
        } else {
            //Check ABAC
            Map<String, Object> body = new HashMap<String, Object>();
            ABACResponseDTO abacStatus = authorizeABAC(body, "LIST", dto.getUuid(), requestPath);

            if (abacStatus != null && abacStatus.getStatus()) {
                    Map<String, String> params = StringUtil.getUrlParamValues(urlParam);

                    Integer currentPage = 1;
                    Integer rowsPerPage = 20;

                    if (StringUtil.isNullOrEmpty(params.get("currentPage"))) {
                        currentPage = 1;
                    } else {
                        currentPage = Integer.parseInt(params.get("currentPage"));
                    }
                    if (StringUtil.isNullOrEmpty(params.get("rowsPerPage"))) {
                        rowsPerPage = 20;
                    } else {
                        rowsPerPage = Integer.parseInt(params.get("rowsPerPage"));
                    }

                    String sort = params.get("sort");
                    String keyword = params.get("keyword");
                    String keywordDecode = "";
                    try {
                        keywordDecode = URLDecoder.decode(keyword);
                    } catch (Exception e) {
                        keywordDecode = "";
                    }

                    Integer status = null;
                    String strStatus = params.get("status");
                    if (StringUtil.isNumeric(strStatus)) {
                        status = Integer.parseInt(strStatus);
                    }
                    String startDate = params.get("startDate");
                    String endDate = params.get("endDate");
                    Integer signupType = null;
                    String strSignupType = params.get("signupType");
                    if (StringUtil.isNumeric(strSignupType)) {
                        signupType = Integer.parseInt(strSignupType);
                    }
                    Integer mobileVerify = null;
                    String strMobileVerify = params.get("mobileVerify");
                    if (StringUtil.isNumeric(strMobileVerify)) {
                        mobileVerify = Integer.parseInt(strMobileVerify);
                    }

                    if (currentPage == 0 || rowsPerPage == 0) {
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                                new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
                    } else {
                        if (!StringUtil.isNullOrEmpty(sort) && !"uuid".equalsIgnoreCase(sort) && !"email".equalsIgnoreCase(sort)
                                && !"mobile".equalsIgnoreCase(sort) && !"userName".equalsIgnoreCase(sort) && !"fullName".equalsIgnoreCase(sort)
                                && !"createdAt".equalsIgnoreCase(sort)) {
                            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Kh??ng c?? ki???u sort theo " + sort,
                                    new MessageContent(HttpStatus.BAD_REQUEST.value(), "Kh??ng c?? ki???u sort theo " + sort, null));
                        } else {
                            UserPagingDTO userDTO = userService.findAll(keywordDecode, status, currentPage, rowsPerPage,
                                    sort, signupType, mobileVerify, null, startDate, endDate);
                            if (userDTO == null || userDTO.getDataRows() == null || userDTO.getDataRows().isEmpty()) {
                                response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(),
                                        new MessageContent(HttpStatus.OK.value(), HttpStatus.OK.toString(), null));
                            } else {
                                List<User> userList = userDTO.getDataRows();
                                if (userList != null && !userList.isEmpty()) {
                                    //Get list role with uuid user list
                                    List<String> uuidList = new ArrayList<>();
                                    userList.forEach((user) -> {
                                        uuidList.add(user.getUuid());
                                    });
                                }
                                response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(),
                                        new MessageContent(HttpStatus.OK.value(), HttpStatus.OK.toString(), userList, userDTO.getTotalRows()));
                            }
                        }
                    }
            } else {
                response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i",
                        new MessageContent(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", null));
            }
        }
        return response;
    }

    public ResponseMessage getAllUserInternal() {
        ResponseMessage response = null;
        List<User> userList = userService.findAll();
        Map<String,User> userMap = new HashMap<>();
        for (User user: userList){
            userMap.put(user.getUuid(),user);
        }
        response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(),
                new MessageContent(HttpStatus.OK.value(), HttpStatus.OK.toString(),userMap));
        return response;
    }

    public ResponseMessage getDetailUser(String requestUrl, String method, String sId, Map<String, String> headerParam) {
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            return new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p", null));
        } else {
            // Check ABAC
            Map<String, Object> body = new HashMap<String, Object>();
            ABACResponseDTO abacStatus = authorizeABAC(body, "DETAIL", dto.getUuid(), requestUrl);
            if (abacStatus != null && abacStatus.getStatus()) {
                User user = userService.findByUuid(sId);
                if (user == null) {
                    return new ResponseMessage(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND,
                            new MessageContent(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND, null));
                } else {
                    UserDetailDTO detailDTO = new UserDetailDTO(user);
                    return new ResponseMessage(new MessageContent(detailDTO));
                }
            } else {
                return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n xem chi ti???t t??i kho???n",
                        new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n xem chi ti???t t??i kho???n", null));
            }
        }
    }

    public ResponseMessage getGroup(String groupId) {
        ResponseMessage response = null;
        List<User> userList = userService.findByGroup(groupId);
        List<String> users = userList.stream().map((item) -> item.getUuid()).collect(Collectors.toList());
        response = new ResponseMessage(new MessageContent(users));
        return response;
    }

    public ResponseMessage getDetailUser(Map<String, String> headerParam) {
        ResponseMessage response;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p", null));
        } else {
            //New
            UserDetailDTO detailDTO = new UserDetailDTO(dto);
            response = new ResponseMessage(new MessageContent(detailDTO));
        }
        return response;
    }

    public ResponseMessage createUser(String requestUrl, String method, Map<String, String> headerParam, Map<String, Object> bodyParam, String urlParam) {
        ResponseMessage response = null;
//        AuthorizationResponseDTO dto = GetUrlParam(urlParam);
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p", null));
        } else {
            LOGGER.info("Check Abac");
            Map<String, Object> subject = new HashMap<>();
            Map<String, Object> attributes = new HashMap<>();
            ResultCheckDto resultCheckDto = authorizeABAC(subject, attributes, dto.getUuid(), requestUrl, method);
            LOGGER.info("Nh???n k???t qu??? check abac");
            if (resultCheckDto != null) {
                if (resultCheckDto.getStatus()) {
                    String userName = (String) bodyParam.get("userName");
                    String email = (String) bodyParam.get("email");
                    String mobile = (String) bodyParam.get("mobile");
                    String fullName = (String) bodyParam.get("fullName");
                    String password = (String) bodyParam.get("password");
                    String matchingPassword = (String) bodyParam.get("matchingPassword");
                    Integer signupType = (Integer) bodyParam.get("signupType");
                    String address = (String) bodyParam.get("address");
                    String birthDay = (String) bodyParam.get("birthDay");
                    String fbId = (String) bodyParam.get("fbId");
                    String ggId = (String) bodyParam.get("ggId");
                    String avatar = (String) bodyParam.get("avatar");
                    String policeRank = (String) bodyParam.get("policeRank");
                    String position = (String) bodyParam.get("position");
                    String createDefaultRoleUser = (String) bodyParam.get("defaultRole");
                    String unitId = (String) bodyParam.get("unit");
                    Unit unit = unitService.findByUuid(unitId);
                    if (signupType == null) {
                        signupType = Constant.USER_SIGNUP_NORMAL;
                    }

                    User user = new User();
                    user.setUuid(UUID.randomUUID().toString());
                    user.setUserName(userName);
                    user.setEmail(email);
                    if (!StringUtil.isNullOrEmpty(mobile)) {
                        if (mobile.startsWith("+84")) {
                            mobile = mobile.replace("+84", "0");
                        } else if (mobile.startsWith("84") && mobile.length() == 11) {
                            mobile = mobile.replaceFirst("84", "0");
                        }
                    }
                    user.setMobile(mobile);
                    user.setFullName(fullName);
                    user.setPassword(password);
                    user.setMatchingPassword(matchingPassword);
                    user.setSignupType(signupType);
                    user.setFacebook(fbId);
                    user.setAddress(address);
                    user.setBirthDay(birthDay);
                    user.setGgId(ggId);
                    user.setFbId(fbId);
                    user.setIsDelete(0);
                    user.setStatus(1);
                    user.setIsActive(0);
                    user.setEmailVerify(0);
                    user.setMobileVerify(0);
                    user.setSetPassword(1);
                    user.setAvatar(avatar);
                    user.setPoliceRank(policeRank);
                    user.setPosition(position);
                    user.setUnit(unit);

                    String invalidData = new UserValidation().validateInsertUser(user);
                    if (invalidData != null) {
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
                    } else {
                        User existUser = null;
                        //Check email exist
                        if (signupType == Constant.USER_SIGNUP_NORMAL && StringUtil.validateEmail(user.getEmail())) {
                            existUser = userService.findByEmail(user.getEmail());
                        }
                        if (existUser != null) {
                            invalidData = "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i email " + user.getEmail();
                            response = new ResponseMessage(HttpStatus.CONFLICT.value(), invalidData, new MessageContent(HttpStatus.CONFLICT.value(), invalidData, null));
                        } else {
                            //Check mobile exist
                            if (signupType == Constant.USER_SIGNUP_NORMAL && StringUtil.checkMobilePhoneNumberNew(user.getMobile())) {
                                existUser = userService.findByMobile(user.getMobile());
                            }
                            if (existUser != null) {
                                invalidData = "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i s??? ??i???n tho???i " + user.getMobile();
                                response = new ResponseMessage(HttpStatus.CONFLICT.value(), invalidData, new MessageContent(HttpStatus.CONFLICT.value(), invalidData, null));
                            } else {
                                //Check user_name exist
                                if (signupType == Constant.USER_SIGNUP_NORMAL && !StringUtil.isNullOrEmpty(user.getUserName())) {
                                    existUser = userService.findByUserName(user.getUserName());
                                }
                                if (existUser != null) {
                                    invalidData = "???? t???n t???i user tr??n h??? th???ng ???ng v???i user_name " + user.getUserName();
                                    response = new ResponseMessage(HttpStatus.CONFLICT.value(), invalidData, new MessageContent(HttpStatus.CONFLICT.value(), invalidData, null));
                                } else {
                                    user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
                                    user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                                    try {
                                        userService.save(user);
                                        response = new ResponseMessage(HttpStatus.CREATED.value(), HttpStatus.CREATED.getReasonPhrase(), new MessageContent(user));
                                    } catch (Exception ex) {
                                        response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n t???o ng?????i d??ng", new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n t???o ng?????i d??ng", null));
                }
            } else {
                response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", new MessageContent(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", null));
            }
        }
        return response;
    }

    public ResponseMessage deleteUser(String requestUrl, String method, String sId, Map<String, String> headerParam, String urlParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
   //   dto = GetUrlParam(urlParam);
        if (dto == null) {
            response = new ResponseMessage(new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p", null));
        } else {
            if (!StringUtil.isNumberic(sId) && !StringUtil.isUUID(sId)) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            } else {
                User user = userService.findByUuid(sId);
                if (user == null) {
                    response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND,
                            new MessageContent(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND, null));
                } else {
                    LOGGER.info("check abac");
                    Map<String, Object> subject = new HashMap<>();
                    Map<String, Object> attributes = new HashMap<>();
                    List<String> groupUuids = new ArrayList<>();
                    attributes.put("groupUuid", groupUuids);
                    ResultCheckDto resultCheckDto = authorizeABAC(subject, attributes, dto.getUuid(), requestUrl, method);
                    LOGGER.info("Nh???n k???t qu??? check abac");
                    if (resultCheckDto != null) {
                        if (resultCheckDto.getStatus()) {
                            //K cho x??a user admin ho???c ch??nh m??nh
                            if ("admin".equalsIgnoreCase(user.getUserName())) {
                                response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? x??a user admin c???a h??? th???ng",
                                        new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? x??a user admin c???a h??? th???ng", null));
                            } else if (dto.getUuid().equals(user.getUuid())) {
                                response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? x??a ch??nh m??nh",
                                        new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? x??a ch??nh m??nh", null));
                            } else {
                          //    userService.remove(user);
                                userService.deleteByUuid(user);
                                response = new ResponseMessage(new MessageContent(HttpStatus.OK.value(), "X??a d??? li???u th??nh c??ng", null));
                            }
                        } else {
                            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n x??a ng?????i d??ng", new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n x??a ng?????i d??ng", null));
                        }
                    } else {
                        response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", new MessageContent(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", null));
                    }
                }
            }

        }
        return response;
    }

    public ResponseMessage deleteUser(String requestUrl, String method, Map<String, Object> bodyParam, Map<String, String> headerParam, String urlParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto;
        dto = GetUrlParam(urlParam);
        if (dto == null) {
            return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.toString(), new MessageContent(HttpStatus.BAD_REQUEST.value(), " L???i trong qu?? tr??nh l???y th??ng tin user", null));
        } else {
            List<String> uuidList = (List<String>) bodyParam.get("uuids");
            if (uuidList == null || uuidList.isEmpty()) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "uuids kh??ng ???????c b??? tr???ng ho???c kh??ng ????ng ?????nh d???ng array",
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), "uuids kh??ng ???????c b??? tr???ng ho???c kh??ng ????ng ?????nh d???ng array", null));
            } else {
                try {
                    //Check bo xoa user admin va chinh minh
                    Set<String> groupUuids = new HashSet<>();
                    List<User> userList = userService.findByUuidIn(uuidList);
                    if (userList != null && !userList.isEmpty()) {
                        userList.stream().filter((user) -> ("admin".equalsIgnoreCase(user.getUserName()) || dto.getUuid().equals(user.getUuid()))).forEachOrdered((user) -> {
                            uuidList.remove(user.getUuid());
                        });
                    }
                    LOGGER.info("check abac");
                    Map<String, Object> subject = new HashMap<>();
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("groupUuid", groupUuids);
                    ResultCheckDto resultCheckDto = authorizeABAC(subject, attributes, dto.getUuid(), requestUrl, method);
                    LOGGER.info("Nh???n k???t qu??? check abac");
                    if (resultCheckDto != null) {
                        if (resultCheckDto.getStatus()) {
                            userService.remove(uuidList);
                            response = new ResponseMessage(new MessageContent(null));
                        } else {
                            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n x??a ng?????i d??ng", new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n x??a ng?????i d??ng", null));
                        }
                    } else {
                        response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", new MessageContent(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", null));
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.toString());
                    response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR + " >>> " + ex, null));
                }
            }
        }
        return response;
    }

    public ResponseMessage updateUser(Map<String, Object> bodyParam, Map<String, String> headerParam, String pathParam, String method, String requestPath, String urlParam) {
        ResponseMessage response = null;

        AuthorizationResponseDTO dto = GetUrlParam(urlParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p", null));
        } else {
//            dto = (AuthorizationResponseDTO) response.getData().getData();
            boolean hasRight = true;
            //Truong hop la path param update => Check RBAC quyen update
            if (!StringUtil.isNullOrEmpty(pathParam)) {
                LOGGER.info("Check abac");
                Map<String, Object> subject = new HashMap<>();
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("groupUuid", bodyParam.get("groupsUuid"));
                ResultCheckDto resultCheckDto = authorizeABAC(subject, attributes, dto.getUuid(), requestPath, "PUT");
                LOGGER.info("Nh???n k???t qu??? check abac");
                if (resultCheckDto != null) {
                    hasRight = resultCheckDto.getStatus();
                } else {
                    hasRight = false;
                }
            }
            if (hasRight) {
                if (bodyParam == null || bodyParam.isEmpty()) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
                } else {
                    String uuid = !StringUtil.isNullOrEmpty(pathParam) ? pathParam : dto.getUuid();
                    String mobile = (String) bodyParam.get("mobile");
                    String email = (String) bodyParam.get("email");
                    String fullName = (String) bodyParam.get("fullName");
                    String skype = (String) bodyParam.get("skype");
                    String facebook = (String) bodyParam.get("facebook");
                    String avatar = (String) bodyParam.get("avatar");
                    String address = (String) bodyParam.get("address");
                    String birthDay = (String) bodyParam.get("birthDay");
                    Integer gender = (Integer) bodyParam.get("gender");
                    Integer status = (Integer) bodyParam.get("status");
                    String groupsUuid = (String) bodyParam.get("groupsUuid");
                    String policeRank = (String) bodyParam.get("policeRank");
                    String position = (String) bodyParam.get("position");
                    String password = (String) bodyParam.get("password");
                    String matchingPassword = (String) bodyParam.get("matchingPassword");
                    String unitId = (String) bodyParam.get("unit");
                    Unit unit = unitService.findByUuid(unitId);

                    User user = new User();
                    if (!StringUtil.isNullOrEmpty(pathParam)) {
                        user = userService.findByUuid(uuid);
                    }
                    if (user == null) {
                        String info = "Kh??ng t??m th???y th??ng tin ng?????i d??ng ???ng v???i uuid: " + uuid;
                        response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), info, new MessageContent(HttpStatus.NOT_FOUND.value(), info, null));
                    } else {
                        user.setUuid(uuid);
                        if (!StringUtil.isNullOrEmpty(mobile)) {
                            if (mobile.startsWith("+84")) {
                                mobile = mobile.replace("+84", "0");
                            } else if (mobile.startsWith("84") && mobile.length() == 11) {
                                mobile = mobile.replaceFirst("84", "0");
                            }
                        }
                        user.setMobile(mobile);
                        user.setEmail(email);
                        user.setFullName(fullName);
                        user.setSkype(skype);
                        user.setFacebook(facebook);
                        user.setAvatar(avatar);
                        user.setAddress(address);
                        user.setBirthDay(birthDay);
                        user.setGender(gender);
                        if (status != null) {
                            user.setStatus(status);
                        }
                        user.setPoliceRank(policeRank);
                        user.setPosition(position);
                        user.setUnit(unit);

                        String invalidData = new UserValidation().validateUpdateUser(user, password, matchingPassword);
                        if (invalidData != null) {
                            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
                        } else {
                            User userExist = null;
                            userExist = userService.findByEmail(email);
                            if (userExist != null && !userExist.getUuid().equalsIgnoreCase(uuid)) {
                                invalidData = "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i email " + user.getEmail();
                                response = new ResponseMessage(HttpStatus.CONFLICT.value(), invalidData, new MessageContent(HttpStatus.CONFLICT.value(), invalidData, null));
                            } else {
                                userExist = userService.findByMobile(mobile);
                                if (userExist != null && !userExist.getUuid().equalsIgnoreCase(uuid)) {
                                    invalidData = "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i s??? ??i???n tho???i " + user.getMobile();
                                    response = new ResponseMessage(HttpStatus.CONFLICT.value(), invalidData, new MessageContent(HttpStatus.CONFLICT.value(), invalidData, null));
                                } else {
                                    if (!StringUtil.isNullOrEmpty(password)) {
                                        user.setPassword(new BCryptPasswordEncoder().encode(password));
                                    }
                                    Timestamp now = new Timestamp(System.currentTimeMillis());
                                    user.setLastUpdate(now);
                                    //Check profile update
                                    if (!StringUtil.isNullOrEmpty(fullName) && !StringUtil.isNullOrEmpty(birthDay) && gender != null && !StringUtil.isNullOrEmpty(address)) {
                                        user.setProfileUpdate(now);
                                    }
                                    //Check avatar update
                                    if (!StringUtil.isNullOrEmpty(avatar)) {
                                        user.setAvatarUpdate(now);
                                    }
                                    try {
//                                boolean result = userService.update(user);
                                        userService.save(user);
//                                if (result && !StringUtil.isNullOrEmpty(password)) {
                                        user.setSetPassword(1);
                                        userService.changePassword(user);
                                        // X??c th???c th??ng tin ng?????i d??ng Request l??n, n???u kh??ng x???y ra exception t???c l?? th??ng tin h???p l???
                                        Authentication authentication = null;
                                        try {
                                            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUserName(), password));
                                        } catch (AuthenticationException ex) {
                                            LOGGER.error("Error to set new authentication >>> " + ex);
                                        }
                                        // Set th??ng tin authentication m???i v??o Security Context
                                        SecurityContextHolder.getContext().setAuthentication(authentication);
//                                }
                                        response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(user));
                                    } catch (Exception ex) {
                                        response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex, new MessageContent(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex, null));
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                String userId = !StringUtil.isNullOrEmpty(pathParam) ? pathParam : dto.getUuid();
                response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n c???p nh???t th??ng tin t??i kho???n", new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n c???p nh???t th??ng tin t??i kho???n", null));
            }
        }
        return response;
    }

    public ResponseMessage findByUuid(Map<String, String> headerParam, Map<String, Object> bodyParam) {
        ResponseMessage response = null;
        List<String> uuidList = (List<String>) bodyParam.get("uuids");
        if (uuidList == null || uuidList.isEmpty()) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "uuids kh??ng ???????c b??? tr???ng ho???c kh??ng ????ng ?????nh d???ng array", new MessageContent(HttpStatus.BAD_REQUEST.value(), "uuids kh??ng ???????c b??? tr???ng ho???c kh??ng ????ng ?????nh d???ng array", null));
        } else {
            try {
                List<User> userList = userService.findByUuidIn(uuidList);
                if (userList == null || userList.isEmpty()) {
                    response = new ResponseMessage(HttpStatus.NO_CONTENT.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i list uuid", new MessageContent(HttpStatus.NO_CONTENT.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i list uuid", null));
                } else {
                    response = new ResponseMessage(new MessageContent(userList));
                }
            } catch (Exception ex) {
                response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i kh??ng c???p nh???t " + ex, new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i kh??ng c???p nh???t " + ex, null));
                ex.printStackTrace();
            }
        }
        //}
        return response;
    }

    public ResponseMessage updateEmail(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                            null));
        } else {
            dto = (AuthorizationResponseDTO) response.getData().getData();
            if (bodyParam == null || bodyParam.isEmpty()) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            } else {
                String password = (String) bodyParam.get("password");
                String newEmail = (String) bodyParam.get("newEmail");

                String invalidData = new UserValidation().validateChangeEmail(dto, password, newEmail);
                if (invalidData != null) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
                } else {
                    User user = userService.findByUuid(dto.getUuid());
                    boolean checkPassword = false;
                    //N???u ????ng k?? qua gg ho???c fb v?? ch??a thi???t l???p m???t kh???u => K check password
                    if ((dto.getSignupType() == Constant.USER_SIGNUP_FACEBOOK || dto.getSignupType() == Constant.USER_SIGNUP_GOOGLE) && dto.getSetPassword() == 0) {
                        checkPassword = true;
                    } else {
                        //Ki???m tra m???t kh???u
                        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                        String dbPassword = user.getPassword();
                        checkPassword = passwordEncoder.matches(password, dbPassword);
                    }
                    if (checkPassword) {
                        User existUser = userService.findByEmail(newEmail);
                        //Check email exist
                        if (existUser != null && (StringUtil.isNullOrEmpty(user.getEmail()) || !user.getEmail().equals(newEmail))) {
                            response = new ResponseMessage(HttpStatus.CONFLICT.value(), "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i email " + newEmail, new MessageContent(HttpStatus.CONFLICT.value(), "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i email " + newEmail, null));
                        } else {
                            user.setEmail(newEmail);
                            user.setEmailVerify(0);
                            try {
                                userService.changeEmail(user);
                                response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(user));
                            } catch (Exception ex) {
                                response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex, new MessageContent(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex, null));
                            }
                        }
                    } else {
                        // Report error 
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "M???t kh???u kh??ng ????ng", new MessageContent(HttpStatus.BAD_REQUEST.value(), "M???t kh???u kh??ng ????ng", null));
                    }
                }
            }
        }
        return response;
    }

    public ResponseMessage updateMobile(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                            null));
        } else {
            dto = (AuthorizationResponseDTO) response.getData().getData();
            if (bodyParam == null || bodyParam.isEmpty()) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            } else {
                String password = (String) bodyParam.get("password");
                String newMobile = (String) bodyParam.get("newMobile");

                String invalidData = new UserValidation().validateChangeMobile(dto, password, newMobile);
                if (invalidData != null) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
                } else {
                    User user = userService.findByUuid(dto.getUuid());
                    boolean checkPassword = false;
                    //N???u ????ng k?? qua gg ho???c fb v?? ch??a thi???t l???p m???t kh???u => K check password
                    if ((dto.getSignupType() == Constant.USER_SIGNUP_FACEBOOK || dto.getSignupType() == Constant.USER_SIGNUP_GOOGLE) && dto.getSetPassword() == 0) {
                        checkPassword = true;
                    } else {
                        //Ki???m tra m???t kh???u
                        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                        String dbPassword = user.getPassword();
                        checkPassword = passwordEncoder.matches(password, dbPassword);
                    }
                    if (checkPassword) {
                        if (newMobile.startsWith("+84")) {
                            newMobile = newMobile.replace("+84", "0");
                        } else if (newMobile.startsWith("84") && newMobile.length() == 11) {
                            newMobile = newMobile.replaceFirst("84", "0");
                        }
                        User existUser = userService.findByMobile(newMobile);
                        //Check mobile exist
                        if (existUser != null && (StringUtil.isNullOrEmpty(user.getMobile()) || !user.getMobile().equals(newMobile))) {
                            response = new ResponseMessage(HttpStatus.CONFLICT.value(), "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i s??? ??i???n tho???i " + newMobile, new MessageContent(HttpStatus.CONFLICT.value(), "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i s??? ??i???n tho???i " + newMobile, null));
                        } else {
                            if (existUser != null && existUser.getUuid().equals(user.getUuid())) {
                                user.setMobile(existUser.getMobile());
                            }
                            LOGGER.info("=========> new Mobile: {}, current user mobile: {}", newMobile, user.getMobile());
                            if (!newMobile.equals(user.getMobile()) || user.getMobileVerify() == 0) {

                                //Check Redis contains OTP of user
                                //OtpExpiredDTO otpExpired = getOtpExpiredTime(user.getUuid());
                                OtpExpiredDTO otpExpired = null;
                                //N???u ch??a g???i l???n n??o ho???c ???? h???t 15ph l???i cho ?????i ho???c trong 15ph ???? ch??a g???i qu?? 5 l???n
                                if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || otpExpired.getCountSms() < Constant.MAX_SMS_PER_EXPIRED) {
                                    user.setOtpMobile(newMobile);
                                    user.setMobileVerify(0);
                                    try {
                                        userService.changeOtpMobile(user);
                                        if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || (user.getOtpTime() != null && user.getOtpTime().getTime() < System.currentTimeMillis())) {
                                            user.setOtpTime(new Timestamp(System.currentTimeMillis() + Constant.OTP_TIME_EXIPRED));
                                        }
                                        //Convert only here for UserOtpDTO
                                        UserOtpDTO userOtp = new UserOtpDTO(user);
                                        //
                                        response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(userOtp));
                                    } catch (Exception ex) {
                                        response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), "L???i c???p nh???t s??? ??i???n tho???i khi ????ng nh???p Facebook/Google >>> " + ex, new MessageContent(HttpStatus.NOT_MODIFIED.value(), "L???i c???p nh???t s??? ??i???n tho???i khi ????ng nh???p Facebook/Google >>> " + ex, null));
                                    }
                                    if (response.getStatus() == HttpStatus.OK.value()) {
                                        //Tao OTP & set OTP time
                                        String otp = RandomStringUtils.randomNumeric(6);
                                        if (userService.createOTP(user, otp)) {
                                            String userMobile = newMobile;
                                        }
                                    }
                                } else {
                                    String reason = "Ch??? ???????c c???p nh???t s??? ??i???n tho???i t???i ??a " + Constant.MAX_SMS_PER_EXPIRED + " l???n trong v??ng " + (Constant.OTP_TIME_EXIPRED / (60 * 1000) + " ph??t ");
                                    response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), reason, new MessageContent(HttpStatus.FORBIDDEN.value(), reason, null));
                                }
                            } else {
                                response = new ResponseMessage(HttpStatus.CONFLICT.value(), "S??? ??i???n tho???i b???n v???a nh???p tr??ng v???i s??? hi???n t???i", new MessageContent(HttpStatus.CONFLICT.value(), "S??? ??i???n tho???i b???n v???a nh???p tr??ng v???i s??? hi???n t???i", null));
                            }
                        }
                    } else {
                        // Report error 
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "M???t kh???u kh??ng ????ng", new MessageContent(HttpStatus.BAD_REQUEST.value(), "M???t kh???u kh??ng ????ng", null));
                    }
                }
            }
        }
        return response;
    }

    public ResponseMessage updatePassword(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                            null));
        } else {
//            dto = (AuthorizationResponseDTO) response.getData().getData();
            if (bodyParam == null || bodyParam.isEmpty()) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            } else {
                String curentPassword = (String) bodyParam.get("curentPassword");
                String newPassword = (String) bodyParam.get("newPassword");
                String rePassword = (String) bodyParam.get("rePassword");

                String invalidData = new UserValidation().validateUpdatePassword(dto, curentPassword, newPassword, rePassword);
                if (invalidData != null) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
                } else {
                    User user = userService.findByUuid(dto.getUuid());
                    boolean checkPassword = false;
                    //N???u ????ng k?? qua gg ho???c fb v?? ch??a thi???t l???p m???t kh???u => K check password
                    if ((dto.getSignupType() == Constant.USER_SIGNUP_FACEBOOK || dto.getSignupType() == Constant.USER_SIGNUP_GOOGLE) && dto.getSetPassword() == 0) {
                        checkPassword = true;
                    } else {
                        //Check currentPassword
                        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                        String dbPassword = user.getPassword();
                        checkPassword = passwordEncoder.matches(curentPassword, dbPassword);
                    }
                    if (checkPassword) {
                        // Encode new password and store it
                        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
                        user.setSetPassword(1);
                        try {
                            if (userService.changePassword(user)) {
                                // X??c th???c th??ng tin ng?????i d??ng Request l??n, n???u kh??ng x???y ra exception t???c l?? th??ng tin h???p l???
                                Authentication authentication = null;
                                try {
                                    authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), newPassword));
                                } catch (AuthenticationException ex) {
                                    LOGGER.error("Error to set new authentication >>> " + ex);
                                }
                                // Set th??ng tin authentication m???i v??o Security Context
                                SecurityContextHolder.getContext().setAuthentication(authentication);

                                String accessJwt = tokenService.setAccessTokenUpdate(dto.getUuid());

                                String refreshJwt = tokenService.setRefreshTokenUpdate(dto.getUuid());
                                AuthorizationResponseDTO responseDTO = new AuthorizationResponseDTO(accessJwt, refreshJwt);
//                                response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(bodyParam));
                                response = new ResponseMessage(new MessageContent(responseDTO));
                            } else {
                                response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), HttpStatus.NOT_MODIFIED.getReasonPhrase(), new MessageContent(HttpStatus.NOT_MODIFIED.value(), HttpStatus.NOT_MODIFIED.getReasonPhrase(), null));
                            }
                        } catch (Exception ex) {
                            response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null));
                        }
                    } else {
                        // Report error 
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "M???t kh???u hi???n t???i kh??ng ????ng", new MessageContent(HttpStatus.BAD_REQUEST.value(), "M???t kh???u hi???n t???i kh??ng ????ng", null));
                    }
                }
            }
        }
        return response;
    }

    public ResponseMessage checkUserExist(String urlParam) {
        ResponseMessage response = null;
        if (StringUtil.isNullOrEmpty(urlParam)) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
        } else {
            Map<String, String> params = StringUtil.getUrlParamValues(urlParam);
            String type = params.get("type");
            String email = params.get("email");
            String mobile = params.get("mobile");

            if ("email".equalsIgnoreCase(type)) {
                if (!StringUtil.validateEmail(email)) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Email kh??ng ???????c tr???ng ho???c kh??ng ????ng ?????nh d???ng", new MessageContent(HttpStatus.BAD_REQUEST.value(), "Email kh??ng ???????c tr???ng ho???c kh??ng ????ng ?????nh d???ng", null));
                } else {
                    User user = userService.findByEmail(email);
                    if (user == null) {
                        response = new ResponseMessage(new MessageContent("Kh??ng t??m th???y t??i kho???n ???ng v???i email " + email));
                    } else {
                        response = new ResponseMessage(HttpStatus.CONFLICT.value(), "Email ???? ???????c ????ng k?? b???i t??i kho???n kh??c", new MessageContent(HttpStatus.CONFLICT.value(), "Email ???? ???????c ????ng k?? b???i t??i kho???n kh??c", null));
                    }
                }
            } else if ("mobile".equalsIgnoreCase(type)) {
                if (!StringUtil.checkMobilePhoneNumberNew(mobile)) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "S??? ??i???n tho???i kh??ng ???????c tr???ng ho???c kh??ng ????ng ?????nh d???ng", new MessageContent(HttpStatus.BAD_REQUEST.value(), "S??? ??i???n tho???i kh??ng ???????c tr???ng ho???c kh??ng ????ng ?????nh d???ng", null));
                } else {
                    User user = userService.findByMobile(mobile);
                    if (user == null) {
                        response = new ResponseMessage(new MessageContent("Kh??ng t??m th???y user ???ng v???i mobile " + mobile));
                    } else {
                        response = new ResponseMessage(HttpStatus.CONFLICT.value(), "S??? ??i???n tho???i n??y ???? ???????c x??c th???c b???i m???t t??i kho???n kh??c. M???i s??? ??i???n tho???i ch??? c?? th??? x??c th???c cho m???t t??i kho???n", new MessageContent(HttpStatus.CONFLICT.value(), "S??? ??i???n tho???i n??y ???? ???????c x??c th???c b???i m???t t??i kho???n kh??c. M???i s??? ??i???n tho???i ch??? c?? th??? x??c th???c cho m???t t??i kho???n", null));
                    }
                }
            } else {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Type ch??? nh???n gi?? tr??? b???ng email ho???c mobile", new MessageContent(HttpStatus.BAD_REQUEST.value(), "Type ch??? nh???n gi?? tr??? b???ng email ho???c mobile", null));
            }
        }
        return response;
    }

    public ResponseMessage forgotPassword(String urlParam) {
        ResponseMessage response = null;
        if (StringUtil.isNullOrEmpty(urlParam)) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
        } else {
            Map<String, String> params = StringUtil.getUrlParamValues(urlParam);
            String email = params.get("email");
            String mobile = params.get("mobile");
            String type = params.get("type");
            String platform = params.get("platform");
            if (!StringUtil.isNullOrEmpty(mobile)) {
                if (mobile.startsWith("+84")) {
                    mobile = mobile.replace("+84", "0");
                } else if (mobile.startsWith("84") && mobile.length() == 11) {
                    mobile = mobile.replaceFirst("84", "0");
                }
            }

            if (StringUtil.isNullOrEmpty(platform) || "web".equalsIgnoreCase(platform) || "wap".equalsIgnoreCase(platform)) {
                response = processWebForgotPassword(type, email, mobile);
            } else if ("app".equalsIgnoreCase(platform)) {
                response = processAppForgotPassword(type, email, mobile);
            } else {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Platform ch??? nh???n m???t trong c??c gi?? tr??? web/wap/app", new MessageContent(HttpStatus.BAD_REQUEST.value(), "Platform ch??? nh???n m???t trong c??c gi?? tr??? web/wap/app", null));
            }
        }
        return response;
    }

    private ResponseMessage processWebForgotPassword(String type, String email, String mobile) {
//        ResponseMessage response = null;
//        if (!StringUtil.validateEmail(email)) {
//            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Email kh??ng ???????c tr???ng ho???c kh??ng ????ng ?????nh d???ng", new MessageContent(HttpStatus.BAD_REQUEST.value(), "Email kh??ng ???????c tr???ng ho???c kh??ng ????ng ?????nh d???ng", null));
//        } else {
//            User user = userService.findByEmail(email);
//            if (user == null) {
//                response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i email " + email, new MessageContent(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i email " + email, null));
//            } else {
//                //Code m???i tr??? v??? link
//                String genCheckInfo = user.getUuid() + "&" + (System.currentTimeMillis() + ApplicationConfig.FORGOTPASS_EXPIRED_TIME * 60 * 1000);
//                String token = AES.encryptAESbase(genCheckInfo, Constant.AES_KEY);
//                String link = ApplicationConfig.FRONTEND_FORGOTPASS_URL + "?token=" + token;
//                MailContentDTO item = new MailContentDTO();
//                item.setType("one");
//                item.setFromName("CoLearn.vn");
//                item.setEmailTitle(String.format(Constant.MAIL_FORGOT_PW_TITLE, user.getEmail()));
//                item.setEmailContent(String.format(Constant.MAIL_FORGOT_PW_CONTENT_LINK, user.getFullName(), link, link, ApplicationConfig.FORGOTPASS_EXPIRED_TIME));
//                item.setEmailTo(email);
//                if (idThreadManager.sendEmail(item)) {
//                    response = new ResponseMessage(new MessageContent("G???i email qu??n m???t kh???u th??nh c??ng"));
//                } else {
//                    response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null));
//                }
//            }
//        }
        return null;
    }

    private ResponseMessage processAppForgotPassword(String type, String email, String mobile) {
//        ResponseMessage response = null;
//        if (StringUtil.isNullOrEmpty(type) || "mobile".equalsIgnoreCase(type)) {
//            if (!StringUtil.checkMobilePhoneNumberNew(mobile)) {
//                String invalid = "Email ho???c s??? ??i???n tho???i kh??ng ????ng ?????nh d???ng";
//                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalid, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalid, null));
//            } else {
//                User user = userService.findByMobile(mobile);
//                if (user == null) {
//                    response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i s??? ??i???n tho???i " + mobile, new MessageContent(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i s??? ??i???n tho???i " + mobile, null));
//                } else {
//                    //Check Redis contains OTP forgot password of user
//                    OtpExpiredDTO otpExpired = null;//getOtpForgotPasswordExpiredTime(user.getUuid());
//                    //N???u ch??a g???i l???n n??o ho???c ???? h???t 15ph l???i cho ?????i ho???c trong 15ph ???? ch??a g???i qu?? 5 l???n
//                    if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || otpExpired.getCountSms() < Constant.MAX_SMS_PER_EXPIRED) {
//                        //Tao OTP & set OTP Password time
//                        String otp = RandomStringUtils.randomNumeric(6);
//                        if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || (user.getOtpPasswordTime() != null && user.getOtpPasswordTime().getTime() < System.currentTimeMillis())) {
//                            user.setOtpPasswordTime(new Timestamp(System.currentTimeMillis() + Constant.OTP_TIME_EXIPRED));
//                        }
//                        if (userService.createOTPPassword(user, otp)) {
//                            //Convert only here for UserOtpDTO
//                            UserOtpDTO userOtp = new UserOtpDTO(user);
//                            //
//                            response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(userOtp));
//                        } else {
//                            response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i g???i OTP", new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i g???i OTP", null));
//                        }
//                    } else {
//                        String reason = "Ch??? ???????c g???i OTP qu??n m???t kh???u t???i ??a " + Constant.MAX_SMS_PER_EXPIRED + " l???n trong v??ng " + (Constant.OTP_TIME_EXIPRED / (60 * 1000) + " ph??t ");
//                        response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), reason, new MessageContent(HttpStatus.FORBIDDEN.value(), reason, null));
//                    }
//                }
//            }
//        } else if ("email".equalsIgnoreCase(type)) {
//            if (!StringUtil.validateEmail(email)) {
//                String invalid = "Email ho???c s??? ??i???n tho???i kh??ng ????ng ?????nh d???ng";
//                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalid, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalid, null));
//            } else {
//                User user = userService.findByEmail(email);
//                if (user == null) {
//                    response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i email " + email, new MessageContent(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i email " + email, null));
//                } else {
//                    //Check Redis contains OTP forgot password of user
//                    OtpExpiredDTO otpExpired = null;//getOtpForgotPasswordExpiredTime(user.getUuid());
//                    //N???u ch??a g???i l???n n??o ho???c ???? h???t 15ph l???i cho ?????i ho???c trong 15ph ???? ch??a g???i qu?? 5 l???n
//                    if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || otpExpired.getCountSms() < Constant.MAX_SMS_PER_EXPIRED) {
//                        //Tao OTP & set OTP Password time
//                        String otp = RandomStringUtils.randomNumeric(6);
//                        if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || (user.getOtpPasswordTime() != null && user.getOtpPasswordTime().getTime() < System.currentTimeMillis())) {
//                            user.setOtpPasswordTime(new Timestamp(System.currentTimeMillis() + Constant.OTP_TIME_EXIPRED));
//                        }
//                        if (userService.createOTPPassword(user, otp)) {
//                            //Convert only here for UserOtpDTO
//                            UserOtpDTO userOtp = new UserOtpDTO(user);
//                            //Return response
//                            response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(userOtp));
//
//                            //Send OTP via Email
//                            MailContentDTO item = new MailContentDTO();
//                            item.setType("one");
//                            item.setFromName("CoLearn.vn");
//                            item.setEmailTitle(String.format(Constant.MAIL_FORGOT_PW_TITLE, user.getEmail()));
//                            item.setEmailContent(String.format(Constant.MAIL_FORGOT_PW_CONTENT_OTP, user.getFullName(), otp, ApplicationConfig.FORGOTPASS_EXPIRED_TIME));
//                            item.setEmailTo(email);
//                            if (idThreadManager.sendEmail(item)) {
//                                LOGGER.info("Send OTP {} to email {} => OK", otp, user.getEmail());
//                                OtpExpiredDTO tmpOtpExpired = otpExpired;
//                                //Push 2 redis otpExpired
//                                if (tmpOtpExpired == null || tmpOtpExpired.getOtpExpiredTime() < System.currentTimeMillis()) {
//                                    tmpOtpExpired = new OtpExpiredDTO();
//                                    tmpOtpExpired.setUuid(user.getUuid());
//                                    tmpOtpExpired.setOtpExpiredTime(user.getOtpPasswordTime().getTime());
//                                    tmpOtpExpired.setCountSms(1);
//                                } else {
//                                    tmpOtpExpired.setCountSms(tmpOtpExpired.getCountSms() + 1);
//                                }
//                                //pushOtpForgotPasswordExpiredTime(user.getUuid(), tmpOtpExpired);
//                            } else {
//                                LOGGER.info("Send OTP {} to email {} => Fail", otp, user.getEmail());
//                            }
//                        } else {
//                            response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i g???i OTP", new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i g???i OTP", null));
//                        }
//                    } else {
//                        String reason = "Ch??? ???????c g???i OTP qu??n m???t kh???u t???i ??a " + Constant.MAX_SMS_PER_EXPIRED + " l???n trong v??ng " + (Constant.OTP_TIME_EXIPRED / (60 * 1000) + " ph??t ");
//                        response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), reason, new MessageContent(HttpStatus.FORBIDDEN.value(), reason, null));
//                    }
//                }
//            }
//        } else {
//            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "type ch??? nh???n m???t trong c??c gi?? tr??? email/mobile", new MessageContent(HttpStatus.BAD_REQUEST.value(), "type ch??? nh???n m???t trong c??c gi?? tr??? email/mobile", null));
//        }
        // return response;
        return null;
    }

    public ResponseMessage updateStatus(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                            null));
        } else {
            dto = (AuthorizationResponseDTO) response.getData().getData();
            String uuid = (String) bodyParam.get("uuid");
            Integer status = (Integer) bodyParam.get("status");
            User user = new User();
            user.setUuid(uuid);
            user.setStatus(status);
            String invalidData = new UserValidation().validateUpdateStatus(user);
            if (invalidData != null) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, invalidData));
            } else {
                User existUser = userService.findByUuid(uuid);
                if (existUser == null) {
                    response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i uuid " + uuid,
                            new MessageContent(HttpStatus.NOT_FOUND.value(), "Kh??ng t??m th???y t??i kho???n ???ng v???i uuid " + uuid, null));
                } else {
                    Map<String, Object> subject = new HashMap<>();
                    Map<String, Object> attributes = new HashMap<>();
                    ResultCheckDto resultCheckDto = authorizeABAC(subject, attributes, dto.getUuid(), "/v1.0/user/status", "PUT");
                    LOGGER.info("Nh???n k???t qu??? check abac");
                    if (resultCheckDto != null) {
                        if (resultCheckDto.getStatus()) {
                            if (bodyParam == null || bodyParam.isEmpty()) {
                                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                                        new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
                            } else {
                                user.setLastUpdate(new Timestamp(System.currentTimeMillis()));
                                try {
                                    userService.changeStatus(user);
                                    response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(bodyParam));
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex,
                                            new MessageContent(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex, null));
                                }
                            }
                        } else {
                            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n c???p nh???t tr???ng th??i t??i kho???n",
                                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n c???p nh???t tr???ng th??i t??i kho???n", null));
                        }
                    } else {
                        response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i",
                                new MessageContent(HttpStatus.FORBIDDEN.value(), "Module ki???m tra quy???n kh??ng ph???n h???i", null));
                    }
                }
            }
        }
        return response;
    }

    public ResponseMessage updateActive(String requestPath, Map<String, String> headerParam, String pathParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                            null));
        }
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "PUT", dto.getUuid(), requestPath);

        if (abacStatus != null && abacStatus.getStatus()) {
            User user = userService.findByUuid(pathParam);
            if (user == null) {
                response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), "T??i kho???n kh??ng t???n t???i",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "T??i kho???n kh??ng t???n t???i", null));
            }
            if(user.getIsActive() == 0) {
                this.userService.updateActive(pathParam);
                response = new ResponseMessage(new MessageContent(HttpStatus.OK.value(), "K??ch ho???t t??i kho???n th??nh c??ng", null));
            } else if (user.getIsActive() == 1){
                if ("admin".equalsIgnoreCase(user.getUserName())) {
                    return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? v?? hi???u h??a user admin c???a h??? th???ng",
                            new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? v?? hi???u h??a user admin c???a h??? th???ng", null));
                } else if (dto.getUuid().equals(user.getUuid())) {
                    return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? v?? hi???u h??a ch??nh m??nh",
                            new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng th??? v?? hi???u h??a ch??nh m??nh", null));
                }
                this.userService.updateInActive(pathParam);
                response = new ResponseMessage(new MessageContent(HttpStatus.OK.value(), "V?? hi???u h??a t??i kho???n th??nh c??ng", null));
            }
        } else {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
        return response;
    }

    public ResponseMessage findAvatarByUuid(Map<String, Object> bodyParam) {
        ResponseMessage response = null;
        List<String> uuidList = (List<String>) bodyParam.get("uuids");
        if (uuidList == null || uuidList.isEmpty()) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "uuids kh??ng ???????c b??? tr???ng ho???c kh??ng ????ng ?????nh d???ng array",
                    new MessageContent(HttpStatus.BAD_REQUEST.value(), "uuids kh??ng ???????c b??? tr???ng ho???c kh??ng ????ng ?????nh d???ng array", null));
        } else {
            try {
                List<User> userList = userService.findByUuidIn(uuidList);
                if (userList == null || userList.isEmpty()) {
                    LOGGER.info("userList null");
                    response = new ResponseMessage(HttpStatus.NO_CONTENT.value(), "Kh??ng t??m th???y avatar t??i kho???n ???ng v???i list uuid",
                            new MessageContent(HttpStatus.NO_CONTENT.value(), "Kh??ng t??m th???y avatar t??i kho???n ???ng v???i list uuid", null));
                } else {
                    LOGGER.info("userList size: " + userList.size());
                    List<UserAvatarDTO> userAvatarList = UserAvatarDTO.getAvatarList(userList);
                    response = new ResponseMessage(new MessageContent(userAvatarList));
                }
            } catch (Exception ex) {
                response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i kh??ng c???p nh???t " + ex,
                        new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), "L???i kh??ng c???p nh???t " + ex, ex.toString()));
                ex.printStackTrace();
            }
        }
        return response;
    }

    public ResponseMessage updateSocialMobile(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n ch??a ????ng nh???p",
                            null));
        } else {
            dto = (AuthorizationResponseDTO) response.getData().getData();
            if (bodyParam == null || bodyParam.isEmpty()) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            } else {
                String mobile = (String) bodyParam.get("mobile");
                if (!StringUtil.isNullOrEmpty(mobile)) {
                    if (mobile.startsWith("+84")) {
                        mobile = mobile.replace("+84", "0");
                    } else if (mobile.startsWith("84") && mobile.length() == 11) {
                        mobile = mobile.replaceFirst("84", "0");
                    }
                }

                String invalidData = new UserValidation().validateUpdateSocialMobile(dto, mobile);
                if (invalidData != null) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData,
                            new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
                } else {
                    User user = userService.findByUuid(dto.getUuid());
                    if (mobile.startsWith("+84")) {
                        mobile = mobile.replace("+84", "0");
                    } else if (mobile.startsWith("84") && mobile.length() == 11) {
                        mobile = mobile.replaceFirst("84", "0");
                    }
                    User existUser = userService.findByMobile(mobile);
                    //Check mobile exist
                    if (existUser != null && (StringUtil.isNullOrEmpty(user.getMobile()) || !user.getMobile().equals(mobile))) {
                        response = new ResponseMessage(HttpStatus.CONFLICT.value(), "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i s??? ??i???n tho???i " + mobile,
                                new MessageContent(HttpStatus.CONFLICT.value(), "???? t???n t???i t??i kho???n tr??n h??? th???ng ???ng v???i s??? ??i???n tho???i " + mobile, null));
                    } else {
                        //Check Redis contains OTP of user
                        OtpExpiredDTO otpExpired = null;//getOtpExpiredTime(user.getUuid());
                        //N???u ch??a g???i l???n n??o ho???c ???? h???t 15ph l???i cho ?????i ho???c trong 15ph ???? ch??a g???i qu?? 3 l???n
                        if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || otpExpired.getCountSms() < Constant.MAX_SMS_PER_EXPIRED) {
                            user.setOtpMobile(mobile);
                            user.setMobileVerify(0);
                            try {
                                userService.changeOtpMobile(user);
                                if (otpExpired == null || otpExpired.getOtpExpiredTime() < System.currentTimeMillis() || (user.getOtpTime() != null && user.getOtpTime().getTime() < System.currentTimeMillis())) {
                                    user.setOtpTime(new Timestamp(System.currentTimeMillis() + Constant.OTP_TIME_EXIPRED));
                                }
                                //Convert only here for UserOtpDTO
                                UserOtpDTO userOtp = new UserOtpDTO(user);
                                //
                                response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(userOtp));
                            } catch (Exception ex) {
                                response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), "L???i c???p nh???t s??? ??i???n tho???i khi ????ng nh???p Facebook/Google >>> " + ex, new MessageContent(HttpStatus.NOT_MODIFIED.value(), "L???i c???p nh???t s??? ??i???n tho???i khi ????ng nh???p Facebook/Google >>> " + ex, null));
                            }
                            if (response.getStatus() == HttpStatus.OK.value()) {
                                //Tao OTP & set OTP time
                                String otp = RandomStringUtils.randomNumeric(6);
                                if (userService.createOTP(user, otp)) {
                                    String userMobile = mobile;
                                }
                            }
                        } else {
                            String reason = "Ch??? ???????c c???p nh???t s??? ??i???n tho???i t???i ??a " + Constant.MAX_SMS_PER_EXPIRED + " l???n trong v??ng " + (Constant.OTP_TIME_EXIPRED / (60 * 1000) + " ph??t ");
                            response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), reason, new MessageContent(HttpStatus.FORBIDDEN.value(), reason, null));
                        }
                    }
                }
            }
        }
        return response;
    }

    public ResponseMessage updateUserInternal(Map<String, Object> bodyParam, Map<String, String> headerParam, String pathParam) {
        ResponseMessage response = null;

        if (bodyParam == null || bodyParam.isEmpty()) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
        } else {
            String uuid = pathParam;
            String mobile = (String) bodyParam.get("mobile");
            String fullName = (String) bodyParam.get("fullName");
            String skype = (String) bodyParam.get("skype");
            String facebook = (String) bodyParam.get("facebook");
            String avatar = (String) bodyParam.get("avatar");
            String address = (String) bodyParam.get("address");
            String birthDay = (String) bodyParam.get("birthDay");
            Integer gender = (Integer) bodyParam.get("gender");

            User user = new User();
            user.setUuid(uuid);
            if (!StringUtil.isNullOrEmpty(mobile)) {
                if (mobile.startsWith("+84")) {
                    mobile = mobile.replace("+84", "0");
                } else if (mobile.startsWith("84") && mobile.length() == 11) {
                    mobile = mobile.replaceFirst("84", "0");
                }
            }
            user.setMobile(mobile);
            user.setFullName(fullName);
            user.setSkype(skype);
            user.setFacebook(facebook);
            user.setAvatar(avatar);
            user.setAddress(address);
            user.setBirthDay(birthDay);
            user.setGender(gender);

            String invalidData = new UserValidation().validateUpdateUser(user, null, null);
            if (invalidData != null) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
            } else {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                user.setLastUpdate(now);
                //Check profile update
                if (!StringUtil.isNullOrEmpty(fullName) && !StringUtil.isNullOrEmpty(birthDay) && gender != null && !StringUtil.isNullOrEmpty(address)) {
                    user.setProfileUpdate(now);
                }
                //Check avatar update
                if (!StringUtil.isNullOrEmpty(avatar)) {
                    user.setAvatarUpdate(now);
                }
                try {
                    userService.update(user);
                    response = new ResponseMessage(HttpStatus.OK.value(), HttpStatus.OK.toString(), new MessageContent(user));
                } catch (Exception ex) {
                    response = new ResponseMessage(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex,
                            new MessageContent(HttpStatus.NOT_MODIFIED.value(), "L???i kh??ng c???p nh???t " + ex, null));
                }
            }
        }
        return response;
    }

    public ResponseMessage checkToken(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        if (bodyParam == null || bodyParam.isEmpty()) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                    new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
        } else {
            String token = (String) bodyParam.get("token");
            if (StringUtil.isNullOrEmpty(token)) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            } else {
                String decryptResult = AES.decryptAESbase(token, Constant.AES_KEY);
                if (StringUtil.isNullOrEmpty(decryptResult)) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng",
                            new MessageContent(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng", null));
                } else {
                    String[] decryptInfo = decryptResult.split("&");
                    if (decryptInfo == null || decryptInfo.length < 2) {
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng",
                                new MessageContent(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng", null));
                    } else {
                        String userUuid = decryptInfo[0];
                        User user = userService.findByUuid(userUuid);

                        if (user == null || (user.getIsDelete() != null && (user.getStatus() == -1 || user.getIsDelete() == 1))) {
                            response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND,
                                    new MessageContent(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND, null));
                        } else {
                            long now = System.currentTimeMillis();
                            long expiredTime = Long.parseLong(decryptInfo[1]);
                            if (expiredTime < now) {
                                response = new ResponseMessage(HttpStatus.REQUEST_TIMEOUT.value(), "Link qu??n m???t kh???u h???t h???n",
                                        new MessageContent(HttpStatus.REQUEST_TIMEOUT.value(), "Link qu??n m???t kh???u h???t h???n", null));
                            } else {
                                response = new ResponseMessage(new MessageContent(bodyParam));
                            }
                        }
                    }
                }
            }
        }
        return response;
    }

    //Kieu link chua token
    public ResponseMessage updateForgotPassword(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        if (bodyParam == null || bodyParam.isEmpty()) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                    new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
        } else {
            String token = (String) bodyParam.get("token");
            String newPassword = (String) bodyParam.get("newPassword");
            String rePassword = (String) bodyParam.get("rePassword");

            String invalidData = new UserValidation().validateUpdateForgotPassword(token, newPassword, rePassword);
            if (invalidData != null) {
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
            } else {
                String decryptResult = AES.decryptAESbase(token, Constant.AES_KEY);
                if (StringUtil.isNullOrEmpty(decryptResult)) {
                    response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng",
                            new MessageContent(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng", null));
                } else {
                    String[] decryptInfo = decryptResult.split("&");
                    if (decryptInfo == null || decryptInfo.length < 2) {
                        response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng",
                                new MessageContent(HttpStatus.BAD_REQUEST.value(), "Token kh??ng ????ng", null));
                    } else {
                        String userUuid = decryptInfo[0];
                        User user = userService.findByUuid(userUuid);

                        if (user == null || (user.getIsDelete() != null && (user.getStatus() == -1 || user.getIsDelete() == 1))) {
                            response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND,
                                    new MessageContent(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND, null));
                        } else {
                            long now = System.currentTimeMillis();
                            //15ph + 1ph cho ngoi nhin la 16ph
                            long expiredTime = Long.parseLong(decryptInfo[1]) + 60 * 1000;
                            if (expiredTime < now) {
                                response = new ResponseMessage(HttpStatus.REQUEST_TIMEOUT.value(), "Link qu??n m???t kh???u h???t h???n",
                                        new MessageContent(HttpStatus.REQUEST_TIMEOUT.value(), "Link qu??n m???t kh???u h???t h???n", null));
                            } else {
                                //Update password
                                user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
                                user.setSetPassword(1);
                                try {
                                    if (userService.changePassword(user)) {
                                        // X??c th???c th??ng tin ng?????i d??ng Request l??n, n???u kh??ng x???y ra exception t???c l?? th??ng tin h???p l???
                                        Authentication authentication = null;
                                        try {
                                            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), newPassword));
                                        } catch (AuthenticationException ex) {
                                            LOGGER.error("Error to set new authentication >>> " + ex);
                                        }
                                        // Set th??ng tin authentication m???i v??o Security Context
                                        SecurityContextHolder.getContext().setAuthentication(authentication);
                                        response = new ResponseMessage(new MessageContent(bodyParam));
                                    } else {
                                        response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null));
                                    }
                                } catch (Exception ex) {
                                    response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.toString(), null));
                                }
                            }
                        }
                    }
                }
            }
        }
        return response;
    }

    public ResponseMessage sendEmail(Map<String, Object> bodyParam, Map<String, String> headerParam) {
        ResponseMessage response = null;
        if (bodyParam == null || bodyParam.isEmpty()) {
            response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
        } else {
            String emailTo = (String) bodyParam.get("emailTo");
            String title = (String) bodyParam.get("title");
            String content = (String) bodyParam.get("content");
            String sign = (String) bodyParam.get("sign");
            LOGGER.info("emailTo: {}, title: {}, content: {}, sign: {}", emailTo, title, content, sign);
            String invalidData = new UserValidation().validateSendEmail(emailTo, title, content, sign);
            if (invalidData != null) {
                LOGGER.info("invalidData: {}", invalidData);
                response = new ResponseMessage(HttpStatus.BAD_REQUEST.value(), invalidData, new MessageContent(HttpStatus.BAD_REQUEST.value(), invalidData, null));
            } else {
                MailContentDTO item = new MailContentDTO();
                item.setType("one");
                item.setFromName("CoLearn.vn");
                item.setEmailTitle(title);
                item.setEmailContent(content);
                item.setEmailTo(emailTo);
                if (idThreadManager.sendEmail(item)) {
                    LOGGER.info("Send email ok");
                    response = new ResponseMessage(new MessageContent("G???i email th??nh c??ng"));
                } else {
                    LOGGER.info("Send email INTERNAL_SERVER_ERROR");
                    response = new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null));
                }
            }
        }
        return response;
    }

    public ResponseMessage listUserUnit(Map<String, Object> bodyParam) {
        ResponseMessage response = null;
        List<String> units = (List<String>) bodyParam.get("units");
        List<User> userList = userService.getUserByUnits(units);
        if (userList == null || userList.isEmpty()) {
            response = new ResponseMessage(new MessageContent(new ArrayList<>()));
        } else {
            List<String> users = userList.stream().map((item) -> item.getUuid()).collect(Collectors.toList());
            response = new ResponseMessage(new MessageContent(users));
        }

        return response;
    }

    private String getLastNLogLines(File file, int nLines) {
        StringBuilder s = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("tail -" + nLines + " " + file);
            java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line = null;
            //Here we first read the next line into the variable
            //line and then check for the EOF condition, which
            //is the return value of null
            while ((line = input.readLine()) != null) {
                s.append(line + '\n');
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return s.toString();
    }

    public ResponseMessage testRestTemplateInterceptor() {
        String urlRequest = "http://103.21.151.158:8689/v1.0/dbm/management/users?page=1&size=20";
        String result = restTemplate.getForObject(urlRequest, String.class);
        LOGGER.info("testRestTemplateInterceptor get all user result: {}", result);
        ResponseMessage response = new ResponseMessage(new MessageContent(result));
        return response;
    }

    public ResponseMessage testRestTemplateInterceptor2() {
        String urlRequest = "http://103.21.151.158:8689/v1.0/dbm/management/cameras";
        String result = restTemplate.getForObject(urlRequest, String.class);
        LOGGER.info("testRestTemplateInterceptor2 get all camera result: {}", result);
        ResponseMessage response = new ResponseMessage(new MessageContent(result));
        return response;
    }

    public ResponseMessage testRestTemplateInterceptor3() {
        boolean result = tokenService.removeAccessToken();
        LOGGER.info("testRestTemplateInterceptor3 remove cache BearerToken: {}", result);
        ResponseMessage response = new ResponseMessage(new MessageContent(result));
        return response;
    }

    public ResponseMessage getUserListForInternalService() {
        List<UserReceiverDTO> dtoList = null;
        List<User> userList = userService.findAll();
        if (userList != null && !userList.isEmpty()) {
            dtoList = new ArrayList<>();
            for (User user : userList) {
                dtoList.add(new UserReceiverDTO(user));
            }
        }
        return new ResponseMessage(new MessageContent(dtoList));
    }

    public ResponseMessage getListUserSelectbox(String urlParam, String requestPath) {

        AuthorizationResponseDTO dto = GetUrlParam(urlParam);
        ResponseMessage response = null;
        if (dto == null) {
            return new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "L???i l???y d??? li???u ng?????i d??ng t??? Urlparam", new MessageContent(HttpStatus.UNAUTHORIZED.value(), "L???i l???y d??? li???u ng?????i d??ng t??? Urlparam", null));
        } else {
            //Check ABAC quy???n xem traffic track
            LOGGER.info("Check abac");
            ResultCheckDto resultCheckDto = authorizeABAC(null, null, dto.getUuid(), requestPath, "LIST");
            LOGGER.info("Nh???n k???t qu??? check abac");
            if (resultCheckDto != null && resultCheckDto.getStatus()) {
                List<UserReceiverDTO> dtoList = null;
                List<User> userList = userService.findAll();
                if (userList != null && !userList.isEmpty()) {
                    dtoList = new ArrayList<>();
                    for (User user : userList) {
                        dtoList.add(new UserReceiverDTO(user));
                    }
                }
                return new ResponseMessage(new MessageContent(dtoList));
            } else {
                response = new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th??m/s???a ?????i t?????ng theo d??i", new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th??m/s???a ?????i t?????ng theo d??i", null));
            }
        }
        return response;

    }

    public ResponseMessage findUserStage(String stage) {
        ResponseMessage response;
        List<User> users = userService.getUserByStage(stage);
        List<String> userList = new ArrayList<>();
        if (users != null && !users.isEmpty()) {
            userList = users.stream().map((item) -> item.getUuid()).collect(Collectors.toList());
        }
        response = new ResponseMessage(new MessageContent(userList));
        return response;
    }

    public ResponseMessage findById(String id) {
        ResponseMessage response = null;
        User user = userService.findByUuid(id);
        if (user == null) {
            response = new ResponseMessage(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND, new MessageContent(HttpStatus.NOT_FOUND.value(), Constant.VALIDATION_DATA_NOT_FOUND, null));
        } else {
            UserDetailDTO detailDTO = new UserDetailDTO(user);
            response = new ResponseMessage(new MessageContent(detailDTO));
        }
        return response;
    }

    public ResponseMessage findUserUnitId(Map<String, String> headerParam, @PathVariable String id) {
        ResponseMessage response;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p", null));
        } else {

            List<User> userList = userService.findByGroup(id);
            response = new ResponseMessage(new MessageContent(userList));
        }
        return response;
    }

    public ResponseMessage getUserBySite(Map<String, String> headerParam, String requestPath, String method, String paramPath) {
        ResponseMessage response;
        AuthorizationResponseDTO dto = getAuthorFromToken(headerParam);
        if (dto == null) {
            response = new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p",
                    new MessageContent(HttpStatus.UNAUTHORIZED.value(), "B???n ch??a ????ng nh???p", null));
        } else {
            //New
            List<User> userList = userService.getUserBySiteId(paramPath);
            response = new ResponseMessage(new MessageContent(userList));
        }
        return response;
    }

    public ResponseMessage getUserBySiteList(Map<String, String> headerParam, String requestPath, String method, String paramPath) {
        ResponseMessage response;
        List<User> userList = userService.getUserBySiteId(paramPath);
        if (userList == null || userList.isEmpty()) {
            List<String> strings = new ArrayList<>();
            return response = new ResponseMessage(new MessageContent(strings));
        }
        List<String> result = userList.stream().map((item) -> item.getUuid()).collect(Collectors.toList());
        response = new ResponseMessage(new MessageContent(result));
        return response;
    }

    public ResponseMessage getListUserInternal(String urlParam) {
        ResponseMessage response = null;
        if (!StringUtil.isNullOrEmpty(urlParam)) {
            Map<String, String> params = StringUtil.getUrlParamValues(urlParam);
            String userIds = params.get("userIds");
            List<String> listIdUser = Arrays.asList(userIds.split(","));
            List<User> listUser = userService.findByUuidIn(listIdUser);
            response = new ResponseMessage(new MessageContent(HttpStatus.OK.value(), HttpStatus.OK.toString(), listUser));
        }
        return response;
    }
}
