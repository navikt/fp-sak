package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil.annenForelderHarUttakMedUtbetaling;
import static no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.AnnenForelderHarRettAksjonspunktUtleder.oppgittHarAnnenForeldreRett;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.FaktaUttakAksjonspunktUtleder;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private UttakRepository uttakRepository;

    @Inject
    public AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder(UttakRepositoryProvider provider) {
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
        this.uttakRepository = provider.getUttakRepository();
    }

    AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder() {
        // CDI
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.getBehandlingId());
        var annenpartsGjeldendeUttaksplan = hentAnnenpartsGjeldendeUttak(input.getYtelsespesifiktGrunnlag());
        if (ytelseFordeling.isPresent() &&
            !oppgittHarAnnenForeldreRett(ytelseFordeling.get()) &&
            annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return List.of(AksjonspunktDefinisjon.ANNEN_FORELDER_IKKE_RETT_OG_LØPENDE_VEDTAK);
        }

        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return true;
    }

    private Optional<UttakResultatEntitet> hentAnnenpartsGjeldendeUttak(ForeldrepengerGrunnlag fpGrunnlag) {
        if (fpGrunnlag.getAnnenpart().isPresent()) {
            var gjeldendeVedtakBehandlingId = fpGrunnlag.getAnnenpart().get().getGjeldendeVedtakBehandlingId();
            return uttakRepository.hentUttakResultatHvisEksisterer(gjeldendeVedtakBehandlingId);
        }
        return Optional.empty();
    }
}
