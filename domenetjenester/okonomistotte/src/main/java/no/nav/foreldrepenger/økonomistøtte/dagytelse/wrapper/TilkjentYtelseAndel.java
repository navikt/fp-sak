package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;

public class TilkjentYtelseAndel {

    private TilkjentYtelsePeriode tilkjentYtelsePeriode;
    private Boolean brukerErMottaker;
    private FamilieYtelseType familieYtelseType;
    private int dagsats;
    private BigDecimal utbetalingsgrad;
    private Inntektskategori inntektskategori;
    private Arbeidsgiver arbeidsgiver;

    public TilkjentYtelsePeriode getTilkjentYtelsePeriode() {
        return tilkjentYtelsePeriode;
    }

    public LocalDate getOppdragPeriodeFom() {
        return getTilkjentYtelsePeriode().getFom();
    }

    public LocalDate getOppdragPeriodeTom() {
        return getTilkjentYtelsePeriode().getTom();
    }

    public boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public FamilieYtelseType getFamilieYtelseType() {
        return familieYtelseType;
    }

    public boolean erPrivatPersonSomArbeidsgiver() {
        return arbeidsgiver != null && arbeidsgiver.erAktørId();
    }

    public boolean skalTilBrukerEllerPrivatperson() {
        return brukerErMottaker || this.erPrivatPersonSomArbeidsgiver();
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public int getDagsats() {
        return dagsats;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public String getArbeidsforholdOrgnr() {
        if (!arbeidsgiver.getErVirksomhet()) {
            throw new IllegalStateException("Utviklerfeil: Forsøker å hente orgnr til en arbeidsgiver som ikke er en virksomhet");
        }
        return arbeidsgiver.getIdentifikator();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    @Override
    public String toString() {
        return "TilkjentYtelseAndel{" +
            "tilkjentYtelsePeriodeFom=" + tilkjentYtelsePeriode.getFom() +
            ", brukerErMottaker=" + brukerErMottaker +
            ", familieYtelseType=" + familieYtelseType +
            ", dagsats=" + dagsats +
            ", utbetalingsgrad=" + utbetalingsgrad +
            ", inntektskategori=" + inntektskategori +
            ", arbeidsgiver=" + arbeidsgiver +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TilkjentYtelseAndel eksisterendeTilkjentYtelseAndel) {
        return new Builder(eksisterendeTilkjentYtelseAndel);
    }

    public static class Builder {
        private TilkjentYtelseAndel tilkjentYtelseAndelMal;

        public Builder() {
            tilkjentYtelseAndelMal = new TilkjentYtelseAndel();
        }

        public Builder(TilkjentYtelseAndel eksisterendeTilkjentYtelseAndel) {
            tilkjentYtelseAndelMal = eksisterendeTilkjentYtelseAndel;
        }

        public Builder medBrukerErMottaker(boolean brukerErMottaker) {
            tilkjentYtelseAndelMal.brukerErMottaker = brukerErMottaker;
            return this;
        }

        public Builder medFamilieYtelseType(FamilieYtelseType familieYtelseType) {
            tilkjentYtelseAndelMal.familieYtelseType = familieYtelseType;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            tilkjentYtelseAndelMal.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medDagsats(int dagsats) {
            tilkjentYtelseAndelMal.dagsats = dagsats;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            tilkjentYtelseAndelMal.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            tilkjentYtelseAndelMal.inntektskategori = inntektskategori;
            return this;
        }

        public TilkjentYtelseAndel build(TilkjentYtelsePeriode tilkjentYtelsePeriode) {
            tilkjentYtelseAndelMal.tilkjentYtelsePeriode = tilkjentYtelsePeriode;
            TilkjentYtelsePeriode.builder(tilkjentYtelsePeriode).leggTilOppdragAndel(tilkjentYtelseAndelMal);
            verifyStateForBuild();
            return tilkjentYtelseAndelMal;
        }

        void verifyStateForBuild() {
            Objects.requireNonNull(tilkjentYtelseAndelMal.tilkjentYtelsePeriode, "tilkjentYtelsePeriode");
            Objects.requireNonNull(tilkjentYtelseAndelMal.utbetalingsgrad, "utbetalingsgrad");
            Objects.requireNonNull(tilkjentYtelseAndelMal.inntektskategori, "inntektskategori");
            Objects.requireNonNull(tilkjentYtelseAndelMal.familieYtelseType, "familieYtelseType");
            Objects.requireNonNull(tilkjentYtelseAndelMal.brukerErMottaker, "familieYtelseType");
        }
    }
}
