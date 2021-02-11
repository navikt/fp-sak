package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class UttakResultatPeriodeAktivitetLagreDto {

    @ValidKodeverk
    private StønadskontoType stønadskontoType = StønadskontoType.UDEFINERT;

    @Min(0)
    @Max(1000)
    @Digits(integer = 3, fraction = 1)
    @NotNull
    private BigDecimal trekkdagerDesimaler;

    @Valid
    private ArbeidsgiverLagreDto arbeidsgiver;
    @Pattern(regexp = "\\d{9}|\\d{13}")
    private String arbeidsgiverReferanse;

    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @Size(max = 200)
    private String arbeidsforholdId;

    @Valid
    private Utbetalingsgrad utbetalingsgrad;

    @ValidKodeverk
    private UttakArbeidType uttakArbeidType;

    UttakResultatPeriodeAktivitetLagreDto() { //NOSONAR
        //for jackson
    }

    public StønadskontoType getStønadskontoType() {
        return stønadskontoType;
    }

    public Trekkdager getTrekkdagerDesimaler() {
        return new Trekkdager(trekkdagerDesimaler);
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        if (arbeidsgiver == null) {
            return arbeidsgiverFraReferanse();
        }
        if (arbeidsgiver.erVirksomhet()) {
            return Optional.of(Arbeidsgiver.virksomhet(arbeidsgiver.getIdentifikator()));
        }
        return Optional.of(Arbeidsgiver.person(arbeidsgiver.getAktørId()));
    }

    private Optional<Arbeidsgiver> arbeidsgiverFraReferanse() {
        return Optional.ofNullable(arbeidsgiverReferanse)
            .map(r -> OrgNummer.erGyldigOrgnr(r) ? Arbeidsgiver.virksomhet(r) : Arbeidsgiver.person(new AktørId(r)));
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public InternArbeidsforholdRef getArbeidsforholdId() {
        return arbeidsforholdId == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(arbeidsforholdId);
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public static class Builder {

        private UttakResultatPeriodeAktivitetLagreDto kladd = new UttakResultatPeriodeAktivitetLagreDto();

        public Builder medStønadskontoType(StønadskontoType stønadskontoType) {
            kladd.stønadskontoType = stønadskontoType;
            return this;
        }

        public Builder medTrekkdager(BigDecimal trekkdager) {
            kladd.trekkdagerDesimaler = trekkdager;
            return this;
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            kladd.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medArbeidsforholdId(InternArbeidsforholdRef internReferanse) {
            kladd.arbeidsforholdId = internReferanse.getReferanse();
            return this;
        }
        /** skal kun ta intern arbeidsforhold refearnse. */
        public Builder medArbeidsforholdId(String arbeidsforholdId) {
            if(arbeidsforholdId!=null) {
                // kjapp validering - sjekk at ingen blander inn ekstern arbeidsforholdId her (fra LPS system, AAregister, Inntektsmelding)
                UUID.fromString(arbeidsforholdId);
            }
            kladd.arbeidsforholdId = arbeidsforholdId;
            return this;
        }

        public Builder medArbeidsgiver(ArbeidsgiverLagreDto arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            if (arbeidsgiver != null) {
                kladd.arbeidsgiverReferanse = arbeidsgiver.getAktørId() != null ? arbeidsgiver.getAktørId().getId() : arbeidsgiver.getIdentifikator();
            }
            return this;
        }

        public Builder medUttakArbeidType(UttakArbeidType uttakArbeidType) {
            kladd.uttakArbeidType = uttakArbeidType;
            return this;
        }

        public UttakResultatPeriodeAktivitetLagreDto build() {
            return kladd;
        }
    }
}
