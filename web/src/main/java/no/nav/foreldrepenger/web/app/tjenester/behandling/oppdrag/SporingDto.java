package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

public class SporingDto {

    private Long id;
    private String opprettetAv;
    private LocalDateTime opprettetTidspunkt;
    private String endretAv;
    private LocalDateTime endretTidspunkt;
    private Long versjon;

    public Long getId() {
        return id;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    public Long getVersjon() {
        return versjon;
    }

    public SporingDto(BaseEntitet baseEntitet, Long versjon, Long id) {
        endretAv = baseEntitet.getEndretAv();
        endretTidspunkt = baseEntitet.getEndretTidspunkt();
        opprettetAv = baseEntitet.getOpprettetAv();
        opprettetTidspunkt = baseEntitet.getOpprettetTidspunkt();
        this.versjon = versjon;
        this.id = id;
    }
}
