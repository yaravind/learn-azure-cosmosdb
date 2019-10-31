package com.aravind.oss.tvsshow.domain;

import java.util.Date;
import java.util.List;

public class Show<Season> {
    private String name;
    private Network tvNetwork;
    private Date premiereDate;
    private Genre genre;
    private List<Season> seasons;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Network getTvNetwork() {
        return tvNetwork;
    }

    public void setTvNetwork(Network tvNetwork) {
        this.tvNetwork = tvNetwork;
    }

    public Date getPremiereDate() {
        return premiereDate;
    }

    public void setPremiereDate(Date premiereDate) {
        this.premiereDate = premiereDate;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public List<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
    }
}
