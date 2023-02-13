package com.elcom.metacen.raw.data.service.impl;

import com.elcom.metacen.raw.data.model.dto.PositionResponseDTO;
import com.elcom.metacen.raw.data.repository.VsatAisDataRepository;
import com.elcom.metacen.raw.data.service.VsatAisDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
public class VsatAisDataServiceImpl implements VsatAisDataService {
    @Autowired
    private VsatAisDataRepository vsatAisDataRepository;

    @Override
    public List<PositionResponseDTO> getLstVsatShipDistinct(List<BigInteger> mmsiLst) {
        return vsatAisDataRepository.findLastPositionOfShips(mmsiLst);
    }
}
