package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    public boolean utledAksjonspunktFor(UttakInput input) {
        var dokumentasjonVurderingBehov = utledDokumentasjonVurderingBehov(input);
        return dokumentasjonVurderingBehov.stream().anyMatch(DokumentasjonVurderingBehov::måVurderes);
    }

    public List<DokumentasjonVurderingBehov> utledDokumentasjonVurderingBehov(UttakInput input) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .map(yfa -> yfa.getGjeldendeFordeling()
                .getPerioder()
                .stream()
                .map(p -> dokumentasjonVurderingBehov(p, input))
                .flatMap(Optional::stream)
                .toList())
            .orElse(List.of());
    }

    private Optional<DokumentasjonVurderingBehov> dokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                              UttakInput input) {
        var tidligereVurdering = oppgittPeriode.getDokumentasjonVurdering();
        var familiehendelse = finnGjeldendeFamiliehendelse(input);
        var behandlingReferanse = input.getBehandlingReferanse();
        var utsettelseDokBehov = UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, familiehendelse, finnPerioderMedPleiepengerInnleggelse(input));
        if (utsettelseDokBehov.isPresent()) {
            return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, utsettelseDokBehov.get(), tidligereVurdering));
        }
        var overføringDokBehov = OverføringDokumentasjonUtleder.utledBehov(oppgittPeriode);
        if (overføringDokBehov.isPresent()) {
            return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, overføringDokBehov.get(), tidligereVurdering));
        }
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingReferanse.behandlingId());
        var aktKravBehov = aktivitetskravDokumentasjonUtleder.utledBehov(input, oppgittPeriode, ytelseFordelingAggregat);
        if (aktKravBehov.isPresent()) {
            return Optional.of(new DokumentasjonVurderingBehov(oppgittPeriode, aktKravBehov.get(), tidligereVurdering));
        }
        var tidligOppstartFarBehov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, ytelseFordelingAggregat);
        return tidligOppstartFarBehov.map(behov -> new DokumentasjonVurderingBehov(oppgittPeriode, behov, tidligereVurdering));
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
