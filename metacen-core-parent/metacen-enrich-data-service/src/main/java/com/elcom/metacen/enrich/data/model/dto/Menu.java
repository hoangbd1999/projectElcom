/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elcom.metacen.enrich.data.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Admin
 */
@Getter
@Setter
@NoArgsConstructor
public class Menu implements Serializable {

    private Integer id;

    private String name;

    private String description;

    private String url;

    private Integer orderNo;

    private Integer parentId;

    private Date createdAt;

    public Menu(Integer id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Menu)) {
            return false;
        }
        Menu other = (Menu) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.elcom.metacen.acos.model.dto.Menu[ id=" + id + " ]";
    }

}
