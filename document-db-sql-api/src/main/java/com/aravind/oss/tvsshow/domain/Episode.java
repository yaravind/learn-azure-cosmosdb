package com.aravind.oss.tvsshow.domain;

import java.util.Date;
import java.util.List;

public class Episode {
    private String name;
    private Date airDate;
    private List<Actor> cast;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getAirDate() {
        return airDate;
    }

    public void setAirDate(Date airDate) {
        this.airDate = airDate;
    }

    public List<Actor> getCast() {
        return cast;
    }

    public void setCast(List<Actor> cast) {
        this.cast = cast;
    }
}
