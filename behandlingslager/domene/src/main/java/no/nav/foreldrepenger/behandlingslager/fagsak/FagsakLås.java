package no.nav.foreldrepenger.behandlingslager.fagsak;

import jakarta.persistence.LockModeType;

import java.util.Objects;

/**
 * Lås, fungerer som token som indikerer at write-lock er tatt ut. Kreves av lagre metoder.
 * Er knyttet til underliggende lock på database rad ({@link LockModeType#PESSIMISTIC_WRITE}).
 * Går out-of-scope og vil ikke være gyldig transaksjonen er ferdig.
 * <p>
 * Låsen initialiseres utelukkende via {@link FagsakRepository}. Og verfisers også her senere ved lagring som
 * påvirker {@link Fagsak}.
 * <p>
 * NB: Kan kun holdes per request/transaksjon.
 * <p>
 * Når lagring skjer vil relevante entiteter også få sin versjon inkrementert
 */
public class FagsakLås {

    private Long fagsakId;

    /**
     * protected ctor, kun for test-mock.
     */
    protected FagsakLås() {
    }

    /**
     * protected, unngå å opprette utenfor denne pakken. Kan overstyres kun til test
     */
    protected FagsakLås(Long fagsakId) {
        if (fagsakId == null) {
            throw new IllegalArgumentException("Minst en av fagsakId og behandlingId må være forskjellig fra null.");
        }
        this.fagsakId = fagsakId;
    }

    public Long getFagsakId() {
        return this.fagsakId;
    }

    void setFagsakId(Long fagsakId) {
        if (this.fagsakId != null && !Objects.equals(fagsakId, this.fagsakId)) {
            throw new IllegalStateException(
                "Kan ikke endre fagsakId til annen verdi, var [" +
                    this.fagsakId + "], forsøkte å sette til [" +
                    fagsakId + "]");
        }
        this.fagsakId = fagsakId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FagsakLås other)) {
            return false;
        }
        return Objects.equals(getFagsakId(), other.getFagsakId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFagsakId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "<fagsak=" + getFagsakId() +
            ">";
    }
}
