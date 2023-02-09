package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.domene.uttak.fakta.v2.UtsettelseDokumentasjonUtleder.søktPeriodeInnenforTidsperiodeForbeholdtMor;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

final class TidligOppstartFarDokumentasjonUtleder {

    private TidligOppstartFarDokumentasjonUtleder() {
    }

    public static Optional<DokumentasjonVurderingBehov.Behov> utledBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                         UttakInput input,
                                                                         YtelseFordelingAggregat ytelseFordelingAggregat) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var familiehendelseDato = familieHendelse.getFamilieHendelseDato();
        if (behandlingKanIkkeHaTidligOppstart(input, ytelseFordelingAggregat, familieHendelse)
            || !søktPeriodeInnenforTidsperiodeForbeholdtMor(oppgittPeriode, familiehendelseDato)
            || oppgittPeriode.isFlerbarnsdager()
            || erBalansertUttakRundtFødsel(oppgittPeriode, input)) {
            return Optional.empty();
        }

        //Fellesperiode/foreldrepenger tas i aktivitetskrav dok
        if (Objects.equals(FEDREKVOTE, oppgittPeriode.getPeriodeType())) {
            return Optional.of(new DokumentasjonVurderingBehov.Behov(DokumentasjonVurderingBehov.Behov.Type.UTTAK,
                DokumentasjonVurderingBehov.Behov.UttakÅrsak.TIDLIG_OPPSTART_FAR));
        }
        return Optional.empty();
    }

    private static boolean behandlingKanIkkeHaTidligOppstart(UttakInput input, YtelseFordelingAggregat ytelseFordelingAggregat, FamilieHendelse familieHendelse) {
        return !familieHendelse.gjelderFødsel() || RelasjonsRolleType.erMor(input.getBehandlingReferanse().relasjonRolle())
            || UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat);
    }

    private static boolean erBalansertUttakRundtFødsel(OppgittPeriodeEntitet søknadsperiode, UttakInput input) {
        // FAB-direktiv - far/medmor kan ta ut "ifm" fødsel (før termin og første 6 uker).
        // FEDREKVOTE er begrenset til et antall dager - derfor sjekk på om periden er innenfor
        // FORELEDREPENGER kan tas ut ifm fødsel og videre dvs >6uker fom fødsel er ok. Derfor kun sjekk om periode/fom er innenfor intervall
        var farUttakRundtFødsel = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(input);
        var fedrekvoteMedSamtidigUttak =
            FEDREKVOTE.equals(søknadsperiode.getPeriodeType()) && søknadsperiode.isSamtidigUttak() && farUttakRundtFødsel.filter(
                p -> p.encloses(søknadsperiode.getFom()) && p.encloses(søknadsperiode.getTom())).isPresent();
        var foreldrepengerUtenomSykdom =
            FORELDREPENGER.equals(søknadsperiode.getPeriodeType()) && farUttakRundtFødsel.filter(p -> p.encloses(søknadsperiode.getFom()))
                .isPresent();
        return fedrekvoteMedSamtidigUttak || foreldrepengerUtenomSykdom;
    }
}
