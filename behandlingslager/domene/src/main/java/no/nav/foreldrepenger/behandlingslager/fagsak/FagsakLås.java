package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Objects;

import jakarta.persistence.LockModeType;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

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
public record FagsakLås(Saksnummer saksnummer) {

    public FagsakLås {
        Objects.requireNonNull(saksnummer, "saksnummer");
    }

}
