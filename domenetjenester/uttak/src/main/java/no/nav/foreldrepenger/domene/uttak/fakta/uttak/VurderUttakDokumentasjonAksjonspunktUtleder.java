package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class VurderUttakDokumentasjonAksjonspunktUtleder {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private AktivitetskravDokumentasjonUtleder aktivitetskravDokumentasjonUtleder;

    @Inject
    public VurderUttakDokumentasjonAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                       AktivitetskravDokumentasjonUtleder aktivitetskravDokumentasjonUtleder) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.aktivitetskravDokumentasjonUtleder = aktivitetskravDokumentasjonUtleder;
    }

    VurderUttakDokumentasjonAksjonspunktUtleder() {
        //CDI
    }

    public Optional<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        List<DokumentasjonVurderingBehov> dokumentasjonVurderingBehov = utledDokumentasjonVurderingBehov(input);
        var minstEnPeriodeMåVurderes = dokumentasjonVurderingBehov.stream().anyMatch(periodeBehov -> periodeBehov.måVurderes());
        return minstEnPeriodeMåVurderes ? Optional.of(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON) : Optional.empty();
    }

    public List<DokumentasjonVurderingBehov> utledDokumentasjonVurderingBehov(UttakInput input) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .map(yfa -> yfa
            .getGjeldendeFordeling()
            .getPerioder()
            .stream()
            .map(p -> dokumentasjonVurderingBehov(p, input))
            .toList())
            .orElse(List.of());
    }

    public DokumentasjonVurderingBehov dokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                   UttakInput input) {
        var tidligereVurdering = oppgittPeriode.getDokumentasjonVurdering();
        var familiehendelse = finnGjeldendeFamiliehendelse(input);
        var behandlingReferanse = input.getBehandlingReferanse();
        var kreverSammenhengendeUttak = behandlingReferanse.getSkjæringstidspunkt().kreverSammenhengendeUttak();
        var utsettelseDokBehov = UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, familiehendelse, kreverSammenhengendeUttak,
            finnPerioderMedPleiepengerInnleggelse(input));
        if (utsettelseDokBehov.isPresent()) {
            return new DokumentasjonVurderingBehov(oppgittPeriode, utsettelseDokBehov.get(), tidligereVurdering);
        }
        var overføringDokBehov = OverføringDokumentasjonUtleder.utledBehov(oppgittPeriode);
        if (overføringDokBehov.isPresent()) {
            return new DokumentasjonVurderingBehov(oppgittPeriode, overføringDokBehov.get(), tidligereVurdering);
        }
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingReferanse.behandlingId());
        var aktKravBehov = aktivitetskravDokumentasjonUtleder.utledBehov(input, oppgittPeriode, ytelseFordelingAggregat);
        if (aktKravBehov.isPresent()) {
            return new DokumentasjonVurderingBehov(oppgittPeriode, aktKravBehov.get(), tidligereVurdering);
        }
        var tidligOppstartFarBehov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, ytelseFordelingAggregat);
        return tidligOppstartFarBehov.map(behov -> new DokumentasjonVurderingBehov(oppgittPeriode, behov, tidligereVurdering))
            .orElseGet(() -> new DokumentasjonVurderingBehov(oppgittPeriode, null, null));
    }

    private static List<PleiepengerInnleggelseEntitet> finnPerioderMedPleiepengerInnleggelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var pleiepengerGrunnlag = ytelsespesifiktGrunnlag.getPleiepengerGrunnlag();
        if (pleiepengerGrunnlag.isPresent()) {
            var perioderMedInnleggelse = pleiepengerGrunnlag.get().getPerioderMedInnleggelse();
            if (perioderMedInnleggelse.isPresent()) {
                return perioderMedInnleggelse.get().getInnleggelser();
            }
        }
        return List.of();
    }

    private static LocalDate finnGjeldendeFamiliehendelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeFamilieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        return gjeldendeFamilieHendelse.getFamilieHendelseDato();
    }
}
