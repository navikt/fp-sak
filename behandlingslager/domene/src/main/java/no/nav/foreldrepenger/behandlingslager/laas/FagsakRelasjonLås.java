package no.nav.foreldrepenger.behandlingslager.laas;

import jakarta.persistence.LockModeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;

import java.util.Objects;

/**
 * Lås, fungerer som token som indikerer at write-lock er tatt ut. Kreves av lagre metoder.
 * Er knyttet til underliggende lock på database rad ({@link LockModeType#PESSIMISTIC_WRITE}).
 * Går out-of-scope og vil ikke være gyldig transaksjonen er ferdig.
 * <p>
 * Låsen initialiseres utelukkende via {@link FagsakRelasjonRepository}. Og verfisers også her senere ved lagring som
 * påvirker {@link Fagsak}.
 * <p>
 * NB: Kan kun holdes per request/transaksjon.
 * <p>
 * Når lagring skjer vil relevante entiteter også få sin versjon inkrementert
 */
public class FagsakRelasjonLås {

    private Long fagsakRelasjonId;

    /**
     * protected ctor, kun for test-mock.
     */
    protected FagsakRelasjonLås() {
    }

    /**
     * protected, unngå å opprette utenfor denne pakken. Kan overstyres kun til test
     */
    protected FagsakRelasjonLås(Long fagsakRelasjonId) {
        if (fagsakRelasjonId == null) {
            throw new IllegalArgumentException("Minst en av fagsakId og behandlingId må være forskjellig fra null.");
        }
        this.fagsakRelasjonId = fagsakRelasjonId;
    }

    public Long getFagsakRelasjonId() {
        return this.fagsakRelasjonId;
    }

    void setFagsakRelasjonId(Long fagsakRelasjonId) {
        this.fagsakRelasjonId = fagsakRelasjonId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FagsakRelasjonLås other)) {
            return false;
        }
        return Objects.equals(getFagsakRelasjonId(), other.getFagsakRelasjonId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFagsakRelasjonId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "<fagsak=" + getFagsakRelasjonId() +
            ">";
    }
}
