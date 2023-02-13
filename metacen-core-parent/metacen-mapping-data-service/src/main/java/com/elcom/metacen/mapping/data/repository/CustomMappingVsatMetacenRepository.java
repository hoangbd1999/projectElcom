package com.elcom.metacen.mapping.data.repository;

import com.elcom.metacen.mapping.data.model.MappingVsatMetacen;
import com.elcom.metacen.mapping.data.model.dto.MappingVsatFilterDTO;
import com.elcom.metacen.mapping.data.model.dto.MappingVsatResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 *
 * @author Admin
 */
public interface CustomMappingVsatMetacenRepository extends BaseCustomRepository<MappingVsatMetacen> {

    Page<MappingVsatResponseDTO> search(MappingVsatFilterDTO mappingVsatFilterDTO, Pageable pageable);

    List<MappingVsatResponseDTO> getMappingVsatByIpLst(List<String> ipLst);
}
