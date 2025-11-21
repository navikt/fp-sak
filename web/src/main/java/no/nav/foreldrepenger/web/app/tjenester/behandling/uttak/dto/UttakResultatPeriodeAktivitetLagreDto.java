package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class UttakResultatPeriodeAktivitetLagreDto {

    @ValidKodeverk
    private UttakPeriodeType stønadskontoType = UttakPeriodeType.UDEFINERT;

    @Min(0)
    @Max(1000)
    @Digits(integer = 3, fraction = 1)
    @NotNull
    private BigDecimal trekkdagerDesimaler;

    @Valid
    private ArbeidsgiverLagreDto arbeidsgiver;
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverReferanse;

    private UUID arbeidsforholdId;

    @Valid
    private Utbetalingsgrad utbetalingsgrad;

    @ValidKodeverk
    private UttakArbeidType uttakArbeidType;

    UttakResultatPeriodeAktivitetLagreDto() {
        //for jackson
    }

    public UttakPeriodeType getStønadskontoType() {
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

        public Builder medStønadskontoType(UttakPeriodeType stønadskontoType) {
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
            kladd.arbeidsforholdId = internReferanse.getUUIDReferanse();
            return this;
        }
        /** skal kun ta intern arbeidsforhold refearnse. */
        public Builder medArbeidsforholdId(UUID arbeidsforholdId) {
            kladd.arbeidsforholdId = arbeidsforholdId ;
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
