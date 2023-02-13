package com.elcom.metacen.raw.data.service;

import com.elcom.metacen.raw.data.model.dto.ObjectTripDTO;
import com.elcom.metacen.raw.data.model.dto.TripCoordinateDTO;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ObjectTripService {
    CompletableFuture<Map.Entry<BigInteger, List<TripCoordinateDTO>>> findPositionOfShip(BigInteger mmsi, long startTime,
                                                                                                            long toTime, Integer limit);
}
