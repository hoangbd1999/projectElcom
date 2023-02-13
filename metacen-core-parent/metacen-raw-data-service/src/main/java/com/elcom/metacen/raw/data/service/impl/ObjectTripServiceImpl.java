package com.elcom.metacen.raw.data.service.impl;

import com.elcom.metacen.raw.data.model.dto.ObjectTripDTO;
import com.elcom.metacen.raw.data.model.dto.PositionOverallRequest;
import com.elcom.metacen.raw.data.model.dto.PositionResponseDTO;
import com.elcom.metacen.raw.data.model.dto.TripCoordinateDTO;
import com.elcom.metacen.raw.data.repository.AisDataRepository;
import com.elcom.metacen.raw.data.repository.PositionRepository;
import com.elcom.metacen.raw.data.repository.VsatAisDataRepository;
import com.elcom.metacen.raw.data.service.ObjectTripService;
import org.hibernate.type.BigIntegerType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ObjectTripServiceImpl implements ObjectTripService {
    @Autowired
    private VsatAisDataRepository vsatAisDataRepository;
    @Autowired
    private AisDataRepository aisDataRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Async
    @Override
    public CompletableFuture<Map.Entry<BigInteger, List<TripCoordinateDTO>>> findPositionOfShip(BigInteger mmsi, long start,
                                                               long end, Integer limit) {
        List<PositionResponseDTO> vsatPositions = vsatAisDataRepository.getVsatPositionsOfShip(mmsi, start, end, limit);
        List<PositionResponseDTO> aisPositions = aisDataRepository.getAisPositionOfShip(mmsi, start, end, limit);

        // Gộp thông tin của vsat và ais, ưu tiên lấy của vsat nếu trùng long lat
        List<PositionResponseDTO> result = new LinkedList<>();

        for (int i = 0, j = 0; i < vsatPositions.size() || j < aisPositions.size(); ) {
            if (i < vsatPositions.size() && j < aisPositions.size()) {
                PositionResponseDTO vsat = vsatPositions.get(i);
                PositionResponseDTO ais = aisPositions.get(j);
                if (ais.getEventTime().before(vsat.getEventTime())) {
                    result.add(ais);
                    j++;
                } else if (vsat.getEventTime().equals(ais.getEventTime())) {
                    result.add(vsat);
                    i++;
                    j++;
                } else if (vsat.getEventTime().before(ais.getEventTime())) {
                    result.add(vsat);
                    i++;
                }
//            } else if (i >= vsatPositions.size() && j < aisPositions.size()) {
            } else if (i >= vsatPositions.size()) {
                result.add(aisPositions.get(j));
                j++;
//            } else if (i < vsatPositions.size() && j >= aisPositions.size()) {
            } else {
                result.add(vsatPositions.get(i));
                i++;
            }
            if (result.size() >= limit) {
                break;
            }
        }
//        return CompletableFuture.completedFuture(Map.entry(mmsi, result));
        return CompletableFuture.completedFuture(result)
                .thenApply(positions -> {
                    if (positions.size() == 0) return null;
                    return Map.entry(mmsi, positions.stream()
                            .map(position -> modelMapper.map(position, TripCoordinateDTO.class))
                            .collect(Collectors.toList()));
                });
    }
}
