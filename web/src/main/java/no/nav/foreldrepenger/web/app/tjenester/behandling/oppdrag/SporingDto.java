package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;

public class SporingDto {

    private Long id;
    private String opprettetAv;
    private LocalDateTime opprettetTidspunkt;
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

    public Long getVersjon() {
        return versjon;
    }

    public SporingDto(BaseCreateableEntitet baseEntitet, Long versjon, Long id) {
        opprettetAv = baseEntitet.getOpprettetAv();
        opprettetTidspunkt = baseEntitet.getOpprettetTidspunkt();
        this.versjon = versjon;
        this.id = id;
    }
}
