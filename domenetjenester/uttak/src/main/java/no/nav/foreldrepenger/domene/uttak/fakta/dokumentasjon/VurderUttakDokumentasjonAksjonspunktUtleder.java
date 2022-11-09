package no.nav.foreldrepenger.domene.uttak.fakta.dokumentasjon;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class VurderUttakDokumentasjonAksjonspunktUtleder {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public VurderUttakDokumentasjonAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
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
        if (!input.isSkalBrukeNyFaktaOmUttak()) {
            return List.of();
        }
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var familiehendelse = finnGjeldendeFamiliehendelse(input);
        var kreverSammenhengendeUttak = input.getBehandlingReferanse().getSkjæringstidspunkt().kreverSammenhengendeUttak();
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder()
            .getOppgittePerioder()
            .stream()
            .map(p -> dokumentasjonVurderingBehov(p, familiehendelse, kreverSammenhengendeUttak))
            .toList();
    }

    private DokumentasjonVurderingBehov dokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode,
                                                                    LocalDate gjeldendeFamilieHendelse,
                                                                    boolean kreverSammenhengendeUttak) {
        if (oppgittPeriode.isUtsettelse()) {
            var tidligereVurdering = oppgittPeriode.getDokumentasjonVurdering();
            var behov = UtsettelseDokumentasjonUtleder.utledBehov(oppgittPeriode, gjeldendeFamilieHendelse, kreverSammenhengendeUttak);
            return new DokumentasjonVurderingBehov(oppgittPeriode, behov.orElse(null), tidligereVurdering);
        }
        return new DokumentasjonVurderingBehov(oppgittPeriode, null, null);
    }

    private static LocalDate finnGjeldendeFamiliehendelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeFamilieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        return gjeldendeFamilieHendelse.getFamilieHendelseDato();
    }
}
