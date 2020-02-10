package no.nav.foreldrepenger.behandlingslager;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;
import no.nav.vedtak.util.FPDateUtil;

/**
 * En basis {@link Entity} klasse som håndtere felles standarder for utformign av tabeller (eks. sporing av hvem som har
 * opprettet eller oppdatert en rad, og når).
 */
@MappedSuperclass
public abstract class BaseEntitet implements Serializable {

    private static final String BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL";

    @DiffIgnore
    @Column(name = "opprettet_av", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "opprettet_tid", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    @DiffIgnore
    @Column(name = "endret_av")
    private String endretAv;

    @DiffIgnore
    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt; // NOSONAR

    @PrePersist
    protected void onCreate() {
        this.opprettetAv = opprettetAv != null ? opprettetAv : finnBrukernavn();
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : FPDateUtil.nå();
    }

    @PreUpdate
    protected void onUpdate() {
        endretAv = finnBrukernavn();
        endretTidspunkt = FPDateUtil.nå();
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


    private static String finnBrukernavn() {
        String brukerident = SubjectHandler.getSubjectHandler().getUid();
        return brukerident != null ? brukerident : BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES;
    }
    
    /**
     * Kan brukes til å eksplisitt sette opprettet tidspunkt, f.eks. ved migrering av data fra et annet system. Ivaretar da opprinnelig
     * tidspunkt istdf å sette likt now().
     */
    protected void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }
}
