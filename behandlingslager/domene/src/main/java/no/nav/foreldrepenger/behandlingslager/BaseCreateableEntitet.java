package no.nav.foreldrepenger.behandlingslager;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * En basis {@link Entity} klasse som håndtere felles standarder for utformign
 * av tabeller (eks. sporing av hvem som har opprettet en rad og når).
 */
@MappedSuperclass
public abstract class BaseCreateableEntitet implements Serializable {

    private static final String BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL";

    @DiffIgnore
    @Column(name = "opprettet_av", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    @PrePersist
    protected void onCreate() {
        this.opprettetAv = opprettetAv != null ? opprettetAv : BaseCreateableEntitet.finnBrukernavn();
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public void setOpprettetAv(String opprettetAv) {
        this.opprettetAv = opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    /**
     * Kan brukes til å eksplisitt sette opprettet tidspunkt, f.eks. ved migrering
     * av data fra et annet system. Ivaretar da opprinnelig tidspunkt istdf å sette
     * likt now().
     */
    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    protected static String finnBrukernavn() {
        return Optional.ofNullable(KontekstHolder.getKontekst()).map(Kontekst::getKompaktUid).orElse(BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES);
    }
}
