package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;

public record MedlemskapV3Dto(Aksjonspunkt aksjonspunkt,
                              Vurderinger vurderinger,
                              Set<Region> regioner,
                              Set<Personstatus> personstatuser,
                              Set<Utenlandsopphold> utenlandsopphold,
                              Set<Adresse> adresser, Set<Oppholdstillatelse> oppholdstillatelser,
                              Set<MedlemskapPeriode> medlemskapsperioder,
                              Annenpart annenpart) {

    record Aksjonspunkt(Set<MedlemskapAksjonspunktÅrsak> årsaker) {
    }

    // TODO: Legge inn i aksjonspunkt eller være ute for seg selv?
    record Vurderinger(LocalDate vurderingsdato, Set<MedlemskapAksjonspunktÅrsak> årsaker, Vurdering vurdering, String begrunnelse) {
        public enum Vurdering {
            MEDLEM,
            IKKE_MEDLEM
        }
    }

    record Region(LocalDate fom, LocalDate tom, no.nav.foreldrepenger.behandlingslager.geografisk.Region type) {
    }

    record Personstatus(LocalDate fom, LocalDate tom, PersonstatusType type) {
        public static Personstatus map(PersonstatusEntitet pse) {
            return new Personstatus(pse.getPeriode().getFomDato(), pse.getPeriode().getTomDato(), pse.getPersonstatus());
        }
    }

    record Utenlandsopphold(LocalDate fom, LocalDate tom, Landkoder landkode) {
        public static Utenlandsopphold map(MedlemskapOppgittLandOppholdEntitet moloe) {
            return new Utenlandsopphold(moloe.getPeriodeFom(), moloe.getPeriodeTom(), moloe.getLand());
        }
    }

    record Adresse(LocalDate fom, LocalDate tom, PersonadresseDto adresse) {
        public static Adresse map(PersonAdresseEntitet personAdresseEntitet) {
            return new Adresse(personAdresseEntitet.getPeriode().getFomDato(), personAdresseEntitet.getPeriode().getTomDato(),
                PersonadresseDto.tilDto(personAdresseEntitet));
        }
    }

    record Oppholdstillatelse(LocalDate fom, LocalDate tom, OppholdstillatelseType type) {
        public static Oppholdstillatelse map(OppholdstillatelseEntitet oe) {
            return new Oppholdstillatelse(oe.getPeriode().getFomDato(), oe.getPeriode().getTomDato(), oe.getTillatelse());
        }
    }

    record MedlemskapPeriode(LocalDate fom, LocalDate tom, MedlemskapType medlemskapType, MedlemskapDekningType dekningType,
                             LocalDate beslutningsdato) {
        public static MedlemskapPeriode map(MedlemskapPerioderEntitet mpe) {
            return new MedlemskapPeriode(mpe.getFom(), mpe.getTom(), mpe.getMedlemskapType(), mpe.getDekningType(), mpe.getBeslutningsdato());
        }
    }

    record Annenpart(Set<Adresse> adresser, Set<Region> regioner, Set<Personstatus> personstatuser){ }
}

