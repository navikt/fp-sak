package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusIntervall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;

public record MedlemskapDto(ManuellBehandlingResultat manuellBehandlingResultat,
                            LegacyManuellBehandling legacyManuellBehandling,
                            @NotNull Set<Region> regioner,
                            @NotNull Set<Personstatus> personstatuser,
                            @NotNull Set<Utenlandsopphold> utenlandsopphold,
                            @NotNull Set<PersonadresseDto> adresser,
                            @NotNull Set<Oppholdstillatelse> oppholdstillatelser,
                            @NotNull Set<MedlemskapPeriode> medlemskapsperioder,
                            @NotNull Set<MedlemskapAvvik> avvik,
                            Annenpart annenpart) {

    private static final LocalDate OPPHOLD_CUTOFF = LocalDate.of(2018, 7, 1);


    /**
     * Settes hvis det krever manuell behandling og gammel vurdering ikke finnes.
     */
    record ManuellBehandlingResultat(Avslagsårsak avslagskode, LocalDate medlemFom, LocalDate opphørFom) {
    }

    /**
     * Settes når gammel vurdering finnes, og ikke ny?
     */
    record LegacyManuellBehandling(@NotNull Set<MedlemPeriode> perioder) {

        @JsonInclude(Include.NON_NULL)
        record MedlemPeriode(@NotNull LocalDate vurderingsdato,
                             Boolean oppholdsrettVurdering,
                             Boolean erEosBorger,
                             Boolean lovligOppholdVurdering,
                             Boolean bosattVurdering,
                             MedlemskapManuellVurderingType medlemskapManuellVurderingType,
                             String begrunnelse) {
        }
    }

    record Region(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull no.nav.foreldrepenger.behandlingslager.geografisk.Region type) {
    }

    record Personstatus(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull PersonstatusType type) {
        public static Personstatus map(PersonstatusIntervall pse) {
            return new Personstatus(pse.intervall().getFomDato(), pse.intervall().getTomDato(), pse.personstatus());
        }
    }

    record Utenlandsopphold(@NotNull LocalDate fom, LocalDate tom, @NotNull Landkoder landkode) {
        public static Utenlandsopphold map(MedlemskapOppgittLandOppholdEntitet moloe) {
            return new Utenlandsopphold(moloe.getPeriodeFom(), moloe.getPeriodeTom(), moloe.getLand());
        }
    }

    record Oppholdstillatelse(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull OppholdstillatelseType type) {
        public static Oppholdstillatelse map(OppholdstillatelseEntitet oe) {
            return new Oppholdstillatelse(oe.getPeriode().getFomDato(), oe.getPeriode().getTomDato(), oe.getTillatelse());
        }
    }

    record MedlemskapPeriode(@NotNull LocalDate fom, LocalDate tom, @NotNull boolean erMedlem, String lovvalgsland, String studieland,
                             @NotNull MedlemskapType medlemskapType, @NotNull MedlemskapDekningType dekningType, @NotNull LocalDate beslutningsdato) {
        public static MedlemskapPeriode map(MedlemskapPerioderEntitet mpe) {
            return new MedlemskapPeriode(mpe.getFom(), mpe.getTom(), mpe.getErMedlem(), Landkoder.navnLesbart(mpe.getLovvalgLand()),
                Landkoder.navnLesbart(mpe.getStudieland()), mpe.getMedlemskapType(), mpe.getDekningType(), mpe.getBeslutningsdato());
        }
    }

    record Annenpart(@NotNull Set<PersonadresseDto> adresser, @NotNull Set<Region> regioner, @NotNull Set<Personstatus> personstatuser) {
    }
}

