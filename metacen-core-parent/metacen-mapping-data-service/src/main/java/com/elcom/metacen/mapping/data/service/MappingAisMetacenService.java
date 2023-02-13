package com.elcom.metacen.mapping.data.service;

import com.elcom.metacen.mapping.data.model.MappingAisMetacen;
import com.elcom.metacen.mapping.data.model.dto.*;
import org.springframework.data.domain.Page;

import java.math.BigInteger;
import java.util.List;

public interface MappingAisMetacenService {

    MappingAisMetacen save(MappingAisRequestDTO mappingAisRequestDTO, String createBy);

    MappingAisMetacen updateMappingAis(MappingAisMetacen mappingAisMetacen, MappingAisRequestDTO mappingAisRequestDTO, String modifiedBy);

    Page<MappingAisResponseDTO> findListMappingAis(MappingAisFilterDTO mappingAisFilterDTO);

    MappingAisMetacen findByUuid(String uuid);

    MappingAisMetacen delete(MappingAisMetacen mappingAisMetacen);

    List<MappingAisResponseDTO> getListAisMapping(List<BigInteger> collect);
}
