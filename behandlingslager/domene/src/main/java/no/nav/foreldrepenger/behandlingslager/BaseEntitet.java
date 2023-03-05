package no.nav.foreldrepenger.behandlingslager;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PreUpdate;

import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;

/**
 * En basis {@link Entity} klasse som håndtere felles standarder for utformign
 * av tabeller (eks. sporing av hvem som har oppdatert en rad og når).
 */
@MappedSuperclass
public abstract class BaseEntitet extends BaseCreateableEntitet implements Serializable {

    @DiffIgnore
    @Column(name = "endret_av")
    private String endretAv;

    @DiffIgnore
    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    @PreUpdate
    protected void onUpdate() {
        endretAv = finnBrukernavn();
        endretTidspunkt = LocalDateTime.now();
    }

    public void setEndretAv(String endretAv) {
        this.endretAv = endretAv;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    public void setEndretTidspunkt(LocalDateTime endretTidspunkt) {
        this.endretTidspunkt = endretTidspunkt;
    }

}
