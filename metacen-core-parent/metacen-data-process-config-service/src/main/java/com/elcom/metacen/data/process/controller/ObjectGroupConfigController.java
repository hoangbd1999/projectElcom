package com.elcom.metacen.data.process.controller;

import com.elcom.metacen.data.process.constant.Constant;
import com.elcom.metacen.data.process.model.ObjectGroupConfig;
import com.elcom.metacen.data.process.model.ObjectGroupGeneralConfig;
import com.elcom.metacen.data.process.model.dto.ABACResponseDTO;
import com.elcom.metacen.data.process.model.dto.AuthorizationResponseDTO;
import com.elcom.metacen.data.process.model.dto.ObjectGroupConfigDTO.ObjectGroupConfigFilterDTO;
import com.elcom.metacen.data.process.model.dto.ObjectGroupConfigDTO.ObjectGroupConfigRequestDTO;
import com.elcom.metacen.data.process.model.dto.ObjectGroupConfigDTO.ObjectGroupConfigResponseDTO;
import com.elcom.metacen.data.process.service.ObjectGroupConfigService;
import com.elcom.metacen.data.process.service.ObjectGroupGeneralConfigService;
import com.elcom.metacen.data.process.validation.ObjectGroupConfigValidation;
import com.elcom.metacen.message.MessageContent;
import com.elcom.metacen.message.ResponseMessage;
import com.elcom.metacen.utils.DateUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class ObjectGroupConfigController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectGroupConfigController.class);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ObjectGroupConfigService objectGroupConfigService;

    @Autowired
    private ObjectGroupGeneralConfigService objectGroupGeneralConfigService;

    @Autowired
    protected ModelMapper modelMapper;

    public ResponseMessage insert(Map<String, String> headerParam, Map<String, Object> bodyParam, String requestPath) throws ParseException {
        LOGGER.info("Create object group with request >>> {}", bodyParam);

        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "POST", dto.getUuid(), requestPath);
        if (abacStatus != null && abacStatus.getStatus()) {
            if (bodyParam == null || bodyParam.isEmpty()) {
                return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            }

            ObjectGroupConfigRequestDTO objectGroupConfigRequestDTO = buildObjectGroupConfigDTO(bodyParam);
            String validationMsg = new ObjectGroupConfigValidation().validateObjectGroupConfig(objectGroupConfigRequestDTO);
            if (validationMsg != null) {
                return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), validationMsg,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), validationMsg, null));
            }
            ObjectGroupConfig findByName = objectGroupConfigService.findByName(objectGroupConfigRequestDTO.getName());
            if (findByName != null) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "T??n c???u h??nh b??? tr??ng, vui l??ng ?????t t??n kh??c",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "T??n c???u h??nh b??? tr??ng, vui l??ng ?????t t??n kh??c", null));
            }
            ObjectGroupConfig objectGroupConfig = objectGroupConfigService.save(objectGroupConfigRequestDTO, dto.getUserName());
            if (objectGroupConfig != null) {
                return new ResponseMessage(new MessageContent(objectGroupConfig));
            } else {
                return new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), null));
            }
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage update(Map<String, String> headerParam, Map<String, Object> bodyParam, String pathParam, String requestPath) throws ParseException {
        LOGGER.info("Update object group id {} with request >>> {}", pathParam, bodyParam);

        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "PUT", dto.getUuid(), requestPath);
        if (abacStatus != null && abacStatus.getStatus()) {
            String uuid = pathParam;
            ObjectGroupConfig objectGroupConfig = objectGroupConfigService.findByUuid(uuid);
            if (objectGroupConfig == null) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i", null));
            }
            ObjectGroupConfigRequestDTO objectGroupConfigRequestDTO = buildObjectGroupConfigDTO(bodyParam);
            String validationMsg = new ObjectGroupConfigValidation().validateObjectGroupConfig(objectGroupConfigRequestDTO);
            if (validationMsg != null) {
                return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), validationMsg,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), validationMsg, null));
            }
            ObjectGroupConfig findByName = objectGroupConfigService.findByName(objectGroupConfigRequestDTO.getName());
            ObjectGroupConfig result = new ObjectGroupConfig();
            if (findByName == null) {
                result = objectGroupConfigService.update(objectGroupConfig, objectGroupConfigRequestDTO, dto.getUserName());
            } else if (findByName != null) {
                if (findByName.getName().equalsIgnoreCase(objectGroupConfig.getName())) {
                    result = objectGroupConfigService.update(objectGroupConfig, objectGroupConfigRequestDTO, dto.getUserName());
                } else {
                    return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "T??n nh??m b??? tr??ng, vui l??ng ?????t t??n kh??c",
                            new MessageContent(HttpStatus.NOT_FOUND.value(), "T??n nh??m b??? tr??ng, vui l??ng ?????t t??n kh??c", null));
                }
            }
            if (result != null) {
                return new ResponseMessage(new MessageContent(result));
            } else {
                return new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), null));
            }
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage updateTimeAndDistance(Map<String, String> headerParam, Map<String, Object> bodyParam, String requestPath) {
        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "PUT", dto.getUuid(), requestPath);
        if (abacStatus != null && abacStatus.getStatus()) {
            Integer togetherTime = bodyParam.get("togetherTime") != null ? (Integer) bodyParam.get("togetherTime") : 1;
            Integer distanceLevel = bodyParam.get("distanceLevel") != null ? (Integer) bodyParam.get("distanceLevel") : 1;
            objectGroupGeneralConfigService.update(togetherTime,distanceLevel,dto.getUserName());
            return new ResponseMessage(new MessageContent(HttpStatus.OK.value(), "success", null));
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage getById(Map<String, String> headerParam, String requestPath) {
        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "DETAIL", dto.getUuid(), requestPath);

        if (abacStatus != null && abacStatus.getStatus()) {
            ObjectGroupGeneralConfig objectGroupGeneralConfig = objectGroupGeneralConfigService.findByTogetherTime();
            if (objectGroupGeneralConfig == null) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i", null));
            }
            return new ResponseMessage(new MessageContent(objectGroupGeneralConfig));
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage statusChange(Map<String, String> headerParam, Map<String, Object> bodyParam, String requestPath) {
        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "POST", dto.getUuid(), requestPath);

        if (abacStatus != null && abacStatus.getStatus()) {
            String uuid = (String) bodyParam.getOrDefault("uuid", "");
            Integer isActive = bodyParam.get("isActive") != null ? (Integer) bodyParam.get("isActive") : 1;

            ObjectGroupConfig objectGroupConfig = objectGroupConfigService.findByUuid(uuid);
            if (objectGroupConfig == null) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i", null));
            }
            objectGroupConfigService.statusChange(objectGroupConfig, isActive);
            return new ResponseMessage(new MessageContent(HttpStatus.OK.value(), "Thay ?????i tr???ng th??i th??nh c??ng", null));
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage delete(String requestPath, Map<String, String> headerParam, String pathParam) {
        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "DELETE", dto.getUuid(), requestPath);

        if (abacStatus != null && abacStatus.getStatus()) {
            String uuid = pathParam;
            ObjectGroupConfig objectGroupConfig = objectGroupConfigService.findByUuid(uuid);
            if (objectGroupConfig == null) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i", null));
            }
//            ResponseMessage messageContent = this.checkExistObjectGroup(uuid);
//            String message = messageContent.getData().getData().toString();
//            if(message.contains("D??? li???u kh??ng t???n t???i")) {
                objectGroupConfigService.delete(objectGroupConfig);
                return new ResponseMessage(new MessageContent(HttpStatus.OK.value(), "X??a d??? li???u th??nh c??ng", null));
//            } else {
//                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "C???u h??nh ???? c?? nh??m ?????i t?????ng ????? xu???t, kh??ng ???????c x??a!",
//                        new MessageContent(HttpStatus.NOT_FOUND.value(), "C???u h??nh ???? c?? nh??m ?????i t?????ng ????? xu???t, kh??ng ???????c x??a!", null));
//            }
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage checkExistObjectGroup(String requestPath, Map<String, String> headerParam, String pathParam) {
        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "DELETE", dto.getUuid(), requestPath);

        if (abacStatus != null && abacStatus.getStatus()) {
            String uuid = pathParam;
            ObjectGroupConfig objectGroupConfig = objectGroupConfigService.findByUuid(uuid);
            if (objectGroupConfig == null) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "D??? li???u kh??ng t???n t???i", null));
            }
            ResponseMessage messageContent = this.checkExistObjectGroup(uuid);
            String message = messageContent.getData().getData().toString();
            if(!message.contains("D??? li???u kh??ng t???n t???i")) {
                return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "C???u h??nh ???? c?? nh??m ?????i t?????ng ????? xu???t, kh??ng ???????c x??a!",
                        new MessageContent(HttpStatus.NOT_FOUND.value(), "C???u h??nh ???? c?? nh??m ?????i t?????ng ????? xu???t, kh??ng ???????c x??a!", null));
            } else {
                return new ResponseMessage(HttpStatus.OK.value(), "",
                        new MessageContent(HttpStatus.OK.value(), "", null));
            }
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    public ResponseMessage filterObjectGroupConfig(Map<String, String> headerParam, Map<String, Object> bodyParam, String requestPath) {
        // Check isLogged
        AuthorizationResponseDTO dto = authenToken(headerParam);
        if (dto == null) {
            return unauthorizedResponse();
        }
        // Check ABAC
        Map<String, Object> body = new HashMap<String, Object>();
        ABACResponseDTO abacStatus = authorizeABAC(body, "LIST", dto.getUuid(), requestPath);

        if (abacStatus != null && abacStatus.getStatus()) {
            if (bodyParam == null || bodyParam.isEmpty()) {
                return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE,
                        new MessageContent(HttpStatus.BAD_REQUEST.value(), Constant.VALIDATION_INVALID_PARAM_VALUE, null));
            }
            try {
                ObjectGroupConfigFilterDTO objectGroupConfigFilterDTO = buildObjectGroupConfigFilterDTO(bodyParam);
                String validationMsg = new ObjectGroupConfigValidation().validateFilterObjectGroupConfig(objectGroupConfigFilterDTO);
                if (validationMsg != null) {
                    return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), validationMsg,
                            new MessageContent(HttpStatus.BAD_REQUEST.value(), validationMsg, null));
                }
                Page<ObjectGroupConfigResponseDTO> pagedResult = objectGroupConfigService.findListObjectGroupConfig(objectGroupConfigFilterDTO);
                return new ResponseMessage(new MessageContent(pagedResult.getContent(), pagedResult.getTotalElements()));
            } catch (Exception e) {
                LOGGER.error("Filter fail", e);
                return new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        new MessageContent(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), null));
            }
        } else {
            return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y",
                    new MessageContent(HttpStatus.FORBIDDEN.value(), "B???n kh??ng c?? quy???n th???c hi???n h??nh ?????ng n??y", null));
        }
    }

    private ObjectGroupConfigRequestDTO buildObjectGroupConfigDTO(Map<String, Object> bodyParam) throws ParseException {
        String name = (String) bodyParam.getOrDefault("name", "");
        String coordinates = (String) bodyParam.getOrDefault("coordinates", "");
        String areaUuid = (String) bodyParam.getOrDefault("areaUuid", "");
        Date startTime = (bodyParam.get("startTime") != "") && (bodyParam.get("startTime") != null) ? dateFormat.parse((String) bodyParam.get("startTime")) : null;
        Date endTime = (bodyParam.get("endTime") != "") && (bodyParam.get("endTime") != null) ? dateFormat.parse((String) bodyParam.get("endTime")) : null;

        ObjectGroupConfigRequestDTO objectGroupConfigRequestDTO = ObjectGroupConfigRequestDTO.builder()
                .name(name)
                .coordinates(coordinates)
                .areaUuid(areaUuid)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        return objectGroupConfigRequestDTO;
    }

    private ObjectGroupConfigFilterDTO buildObjectGroupConfigFilterDTO(Map<String, Object> bodyParam) throws ParseException {
        Integer page = bodyParam.get("page") != null ? (Integer) bodyParam.get("page") : 0;
        Integer size = bodyParam.get("size") != null ? (Integer) bodyParam.get("size") : 20;
        String sort = (String) bodyParam.getOrDefault("sort", "");
        String term = (String) bodyParam.getOrDefault("term", "");
        String coordinates = (String) bodyParam.getOrDefault("coordinates", "");
        String name = (String) bodyParam.getOrDefault("name", "");
        List<Integer> isActive = bodyParam.get("isActive") != null ? (List<Integer>) bodyParam.get("isActive") : null;
        Date startTime = (bodyParam.get("startTime") != "") && (bodyParam.get("startTime") != null) ? dateFormat.parse((String) bodyParam.get("startTime")) : null;
        Date endTime = (bodyParam.get("endTime") != "") && (bodyParam.get("endTime") != null) ? dateFormat.parse((String) bodyParam.get("endTime")) : null;

        ObjectGroupConfigFilterDTO objectGroupConfigFilterDTO = ObjectGroupConfigFilterDTO.builder()
                .page(page)
                .size(size)
                .sort(sort)
                .term(term)
                .coordinates(coordinates)
                .name(name)
                .isActive(isActive)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return objectGroupConfigFilterDTO;
    }

}
