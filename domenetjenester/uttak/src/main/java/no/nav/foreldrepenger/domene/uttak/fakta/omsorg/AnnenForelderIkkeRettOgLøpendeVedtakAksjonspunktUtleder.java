package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil.annenForelderHarUttakMedUtbetaling;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @Inject
    public AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder(UttakRepositoryProvider provider, ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
        this.uttakTjeneste = uttakTjeneste;
    }

    AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder() {
        // CDI
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.getBehandlingId());
        var annenpartsGjeldendeUttaksplan = hentAnnenpartsGjeldendeUttak(input.getYtelsespesifiktGrunnlag());
        if (!ref.erRevurdering() &&
            ytelseFordeling.isPresent() &&
            !AnnenForelderHarRettAksjonspunktUtleder.oppgittHarAnnenForeldreRett(ytelseFordeling.get()) &&
            annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return List.of(AksjonspunktDefinisjon.ANNEN_FORELDER_IKKE_RETT_OG_LØPENDE_VEDTAK);
        }

        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsGjeldendeUttak(ForeldrepengerGrunnlag fpGrunnlag) {
        if (fpGrunnlag.getAnnenpart().isPresent()) {
            var gjeldendeVedtakBehandlingId = fpGrunnlag.getAnnenpart().get().gjeldendeVedtakBehandlingId();
            return uttakTjeneste.hentUttakHvisEksisterer(gjeldendeVedtakBehandlingId);
        }
        return Optional.empty();
    }
}
