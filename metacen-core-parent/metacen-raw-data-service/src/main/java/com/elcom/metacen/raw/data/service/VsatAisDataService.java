package com.elcom.metacen.raw.data.service;

import com.elcom.metacen.raw.data.model.dto.PositionResponseDTO;
import com.elcom.metacen.raw.data.model.dto.VsatAisDTO;

import java.math.BigInteger;
import java.util.List;

public interface VsatAisDataService {
    List<PositionResponseDTO> getLstVsatShipDistinct(List<BigInteger> mmsiLst);
}
