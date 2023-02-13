package com.elcom.metacen.contact.model.dto;

import com.elcom.metacen.contact.model.dto.EventDTO.EventDTO;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author hoangbd
 */
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Accessors(chain = true)
public class MarineVesselDTO extends BaseDTO<MarineVesselDTO> {
    private String id;
    private String uuid;
    private Long mmsi;
    private String name;
    private String imo;
    private Long countryId;
    private String typeId;
    private Double dimA;
    private Double dimC;
    private String payroll;
    private String description;
    private String equipment;
    private Long draught;
    private Long grossTonnage;
    private Double speedMax;
    private String sideId;
    private List<FileDTO> imageLst;
    private List<FileDTO> fileAttachmentLst;
}
