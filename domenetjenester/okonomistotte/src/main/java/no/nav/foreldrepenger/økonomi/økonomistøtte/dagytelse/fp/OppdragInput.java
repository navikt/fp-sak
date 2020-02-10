package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.BehandlingVedtakOppdrag;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.ForrigeOppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class OppdragInput {

    private PersonIdent personIdent;
    private Saksnummer saksnummer;
    private Long behandingId;
    private FagsakYtelseType fagsakYtelseType;
    private BehandlingVedtakOppdrag behandlingVedtak;
    private TilkjentYtelse tilkjentYtelse;
    private List<TilkjentYtelsePeriode> tilkjentYtelsePerioderFomEndringsdato;
    private ForrigeOppdragInput forrigeOppdragInput;
    private boolean opphørEtterStpEllerIkkeOpphør;
    private boolean avslåttInntrekk;

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Long getBehandlingId() {
        return behandingId;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public String getAnsvarligSaksbehandler() {
        return behandlingVedtak.getAnsvarligSaksbehandler();
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingVedtak.getBehandlingResultatType();
    }

    public LocalDate getVedtaksdato() {
        return behandlingVedtak.getVedtaksdato();
    }

    public boolean gjelderOpphør() {
        return BehandlingResultatType.OPPHØR.equals(getBehandlingResultatType());
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public Optional<TilkjentYtelse> getTilkjentYtelse() {
        return Optional.ofNullable(tilkjentYtelse);
    }

    public List<TilkjentYtelsePeriode> getTilkjentYtelsePerioderFomEndringsdato() {
        return Collections.unmodifiableList(tilkjentYtelsePerioderFomEndringsdato);
    }

    public List<Oppdrag110> getAlleTidligereOppdrag110() {
        return forrigeOppdragInput.getAlleTidligereOppdrag110();
    }

    public List<TilkjentYtelsePeriode> getForrigeTilkjentYtelsePerioder() {
        return forrigeOppdragInput.getForrigeTilkjentYtelsePerioder();
    }

    public List<TilkjentYtelseAndel> getTilkjentYtelseAndelerFomEndringsdato() {
        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeListe = getTilkjentYtelsePerioderFomEndringsdato();

        return tilkjentYtelsePeriodeListe.stream()
            .sorted(Comparator.comparing(TilkjentYtelsePeriode::getFom))
            .map(TilkjentYtelsePeriode::getTilkjentYtelseAndeler)
            .flatMap(List::stream)
            .filter(a -> a.getDagsats() > 0)
            .collect(Collectors.toList());
    }

    public Optional<LocalDate> getEndringsdato() {
        return getTilkjentYtelse().flatMap(TilkjentYtelse::getEndringsdato);
    }

    public boolean erOpphørEtterStpEllerIkkeOpphør() {
        return opphørEtterStpEllerIkkeOpphør;
    }

    public FamilieYtelseType getFamilieYtelseType() {
        return tilkjentYtelse.getTilkjentYtelsePerioder()
            .stream()
            .flatMap(periode -> periode.getTilkjentYtelseAndeler().stream())
            .map(TilkjentYtelseAndel::getFamilieYtelseType)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Forventer å ha minst en periode fra tilkjent ytelse, men har ingen perioder."));
    }

    public boolean gjelderForeldrepenger() {
        return FamilieYtelseType.FØDSEL.equals(getFamilieYtelseType())
            || FamilieYtelseType.ADOPSJON.equals(getFamilieYtelseType());
    }

    public boolean isAvslåttInntrekk() {
        return avslåttInntrekk;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OppdragInput kladd = new OppdragInput();

        public Builder medBehandlingId(Long behandlingId) {
            kladd.behandingId = behandlingId;
            return this;
        }

        public Builder medSaksnummer(Saksnummer saksnummer) {
            kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
            kladd.fagsakYtelseType = fagsakYtelseType;
            return this;
        }

        public Builder medBehandlingVedtak(BehandlingVedtakOppdrag behandlingVedtak) {
            kladd.behandlingVedtak = behandlingVedtak;
            return this;
        }

        public Builder medPersonIdent(PersonIdent personIdent) {
            kladd.personIdent = personIdent;
            return this;
        }

        public Builder medForenkletBeregningsresultat(TilkjentYtelse tilkjentYtelse) {
            kladd.tilkjentYtelse = tilkjentYtelse;
            return this;
        }

        public Builder medTilkjentYtelsePerioderFomEndringsdato(List<TilkjentYtelsePeriode> tilkjentYtelsePerioderFomEndringsdato) {
            kladd.tilkjentYtelsePerioderFomEndringsdato = tilkjentYtelsePerioderFomEndringsdato;
            return this;
        }

        public Builder medTidligereBehandlingInfo(ForrigeOppdragInput forrigeOppdragInput) {
            kladd.forrigeOppdragInput = forrigeOppdragInput;
            return this;
        }

        public Builder medOpphørEtterStpEllerIkkeOpphør(boolean opphørEtterStpEllerIkkeOpphør) {
            kladd.opphørEtterStpEllerIkkeOpphør = opphørEtterStpEllerIkkeOpphør;
            return this;
        }

        public Builder medAvslåttInntrekk(boolean avslåttInntrekk) {
            kladd.avslåttInntrekk = avslåttInntrekk;
            return this;
        }

        public OppdragInput build() {
            return kladd;
        }
    }
}
