package com.elcom.metacen.mapping.data.controller;

import com.elcom.metacen.mapping.data.model.MappingVsatMetacen;
import com.elcom.metacen.mapping.data.model.dto.MappingAisResponseDTO;
import com.elcom.metacen.mapping.data.model.dto.MappingVsatAisRequestDTO;
import com.elcom.metacen.mapping.data.model.dto.MappingVsatAisResponse;
import com.elcom.metacen.mapping.data.model.dto.MappingVsatResponseDTO;
import com.elcom.metacen.mapping.data.service.MappingAisMetacenService;
import com.elcom.metacen.mapping.data.service.MappingVsatMetacenService;
import com.elcom.metacen.message.MessageContent;
import com.elcom.metacen.message.ResponseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Controller
public class MappingVsatAisMetacenController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingVsatAisMetacenController.class);

    @Autowired
    private MappingVsatMetacenService mappingVsatMetacenService;

    @Autowired
    private MappingAisMetacenService mappingAisMetacenService;

    @Autowired
    private ObjectMapper objectMapper;

    public ResponseMessage getMetacenIdFromVsatAndAis(Map<String, String> headerParam, Map<String, Object> bodyParam, String requestPath) {
        MappingVsatAisRequestDTO request = buildMappingVsatAisRequestDTO(bodyParam);
        Map<String, String> listVsatMapping = mappingVsatMetacenService.getListVsatMapping(request.getIpMmsiList().stream()
                        .map(MappingVsatAisRequestDTO.IpMmsiRequestDTO::getIp)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .stream()
                .collect(toMap(MappingVsatResponseDTO::getVsatIpAddress, MappingVsatResponseDTO::getObjectId));
        Map<Integer, String> listAisMapping = mappingAisMetacenService.getListAisMapping(request.getIpMmsiList().stream()
                        .map(MappingVsatAisRequestDTO.IpMmsiRequestDTO::getMmsi)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .stream()
                .collect(toMap(MappingAisResponseDTO::getAisMmsi, MappingAisResponseDTO::getObjectId));
        List<MappingVsatAisResponse> response = request.getIpMmsiList().stream()
                .map(ipMmsiPair -> {
                    String metacenId = listVsatMapping.getOrDefault(ipMmsiPair.getIp(), null);
                    if (metacenId == null) {
                        metacenId = listAisMapping.getOrDefault(ipMmsiPair.getMmsi(), null);
                    }
                    return new MappingVsatAisResponse()
                            .setMappingId(metacenId)
                            .setMmsi(ipMmsiPair.getMmsi());
                }).collect(Collectors.toList());
        return new ResponseMessage(new MessageContent(response));
    }

    private MappingVsatAisRequestDTO buildMappingVsatAisRequestDTO(Map<String, Object> bodyParam) {
        MappingVsatAisRequestDTO requestDTO = objectMapper.convertValue(bodyParam, MappingVsatAisRequestDTO.class);
        return requestDTO;
    }
}
