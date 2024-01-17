package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record TilgrensendeYtelser(RelatertYtelseType relatertYtelseType, LocalDate periodeFra, LocalDate periodeTil, String status, String statusNavn, Saksnummer saksNummer) implements Comparable<TilgrensendeYtelser> {

    public TilgrensendeYtelser(Ytelse ytelse) {
        this(ytelse.getRelatertYtelseType(), ytelse.getPeriode().getFomDato(), ytelse.getPeriode().getTomDato(), ytelse.getStatus().getKode(), ytelse.getStatus().getNavn(), ytelse.getSaksnummer());
    }

    @Override
    public int compareTo(TilgrensendeYtelser other) {
        return Comparator.nullsLast(LocalDate::compareTo).compare(other.periodeFra(), this.periodeFra());
    }

}
