package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_ARBEID_OG_UTDANNING;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_IKKE_OPPGITT;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_INNLAGT;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_INTROPROG;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_KVALPROG;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_TRENGER_HJELP;
import static no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov.Behov.Årsak.AKTIVITETSKRAV_UTDANNING;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

@ApplicationScoped
public class AktivitetskravDokumentasjonUtleder {

    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    public AktivitetskravDokumentasjonUtleder(ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    AktivitetskravDokumentasjonUtleder() {
        //CDI
    }

    Optional<DokumentasjonVurderingBehov.Behov> utledBehov(UttakInput input,
                                                           OppgittPeriodeEntitet periode,
                                                           YtelseFordelingAggregat ytelseFordelingAggregat) {
        var behandlingReferanse = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        if (helePeriodenErHelg(periode) || RelasjonsRolleType.erMor(behandlingReferanse.relasjonRolle()) || ytelseFordelingAggregat.robustHarAleneomsorg(input.getBehandlingReferanse().relasjonRolle())
            || familieHendelse.erStebarnsadopsjon() || MorsAktivitet.UFØRE.equals(periode.getMorsAktivitet()) || MorsAktivitet.IKKE_OPPGITT.equals(periode.getMorsAktivitet())) {
            return Optional.empty();
        }

        var periodeType = periode.getPeriodeType();
        var bareFarHarRettOgSøkerUtsettelse = bareFarHarRettOgSøkerUtsettelse(periode, ytelseFordelingAggregat, fpGrunnlag);
        var harKravTilAktivitet =
            !periode.isFlerbarnsdager() && (Set.of(FELLESPERIODE, FORELDREPENGER).contains(periodeType) || bareFarHarRettOgSøkerUtsettelse);
        if (!harKravTilAktivitet) {
            return Optional.empty();
        }
        var type = bareFarHarRettOgSøkerUtsettelse ? DokumentasjonVurderingBehov.Behov.Type.UTSETTELSE : DokumentasjonVurderingBehov.Behov.Type.UTTAK;
        return Optional.of(new DokumentasjonVurderingBehov.Behov(type, mapÅrsak(periode.getMorsAktivitet())));
    }

    private DokumentasjonVurderingBehov.Behov.Årsak mapÅrsak(MorsAktivitet morsAktivitet) {
        if (morsAktivitet == null) {
            return AKTIVITETSKRAV_IKKE_OPPGITT;
        }
        return switch (morsAktivitet) {
            case ARBEID -> AKTIVITETSKRAV_ARBEID;
            case UTDANNING -> AKTIVITETSKRAV_UTDANNING;
            case KVALPROG -> AKTIVITETSKRAV_KVALPROG;
            case INTROPROG -> AKTIVITETSKRAV_INTROPROG;
            case TRENGER_HJELP -> AKTIVITETSKRAV_TRENGER_HJELP;
            case INNLAGT -> AKTIVITETSKRAV_INNLAGT;
            case ARBEID_OG_UTDANNING -> AKTIVITETSKRAV_ARBEID_OG_UTDANNING;
            case IKKE_OPPGITT, UDEFINERT -> AKTIVITETSKRAV_IKKE_OPPGITT;
            case UFØRE -> throw new IllegalArgumentException("Er ikke behov for avklaring av " + morsAktivitet);
        };
    }

    private boolean bareFarHarRettOgSøkerUtsettelse(OppgittPeriodeEntitet periode,
                                                    YtelseFordelingAggregat ytelseFordelingAggregat,
                                                    ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return (Set.of(UtsettelseÅrsak.ARBEID, UtsettelseÅrsak.FERIE).contains(periode.getÅrsak())
            || UtsettelseÅrsak.FRI.equals(periode.getÅrsak()) && MorsAktivitet.forventerDokumentasjon(periode.getMorsAktivitet()))
            && !harAnnenForelderRett(ytelseFordelingAggregat, foreldrepengerGrunnlag);
    }

    private boolean helePeriodenErHelg(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) == 0;
    }

    private boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        return ytelseFordelingAggregat.harAnnenForelderRett(annenpartHarForeldrepengerUtbetaling(ytelsespesifiktGrunnlag));
    }

    private boolean annenpartHarForeldrepengerUtbetaling(ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        return ytelsespesifiktGrunnlag.getAnnenpart()
            .flatMap(a -> foreldrepengerUttakTjeneste.hentHvisEksisterer(a.gjeldendeVedtakBehandlingId()))
            .stream().anyMatch(ForeldrepengerUttak::harUtbetaling);
    }
}
